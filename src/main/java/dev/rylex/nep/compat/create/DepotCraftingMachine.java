package dev.rylex.nep.compat.create;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.PushOutcome;
import dev.rylex.nep.machine.RejectLog;
import dev.rylex.nep.pattern.AndesiteCraftingPattern;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class DepotCraftingMachine implements ICraftingMachine {

    private static final int MAX_MEMOIZED_REJECTS = 512;

    private static final Set<PatternKey> UNSATISFIABLE = new HashSet<>();
    private static final RejectLog REJECT_LOG = new RejectLog();

    private final DepotBlockEntity depot;

    public DepotCraftingMachine(DepotBlockEntity depot) {
        this.depot = depot;
    }

    static void clearCache() {
        UNSATISFIABLE.clear();
        REJECT_LOG.clear();
    }

    static int memoisedRejectCount() {
        return UNSATISFIABLE.size();
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        return switch (kindAbove()) {
            case DEPLOYER ->
                group(AllBlocks.DEPLOYER.asStack(), AllBlocks.DEPLOYER.get().getName());
            case SPOUT -> group(AllBlocks.SPOUT.asStack(), AllBlocks.SPOUT.get().getName());
            case NONE -> group(AllBlocks.DEPOT.asStack(), AllBlocks.DEPOT.get().getName());
        };
    }

    private static PatternContainerGroup group(ItemStack icon, Component name) {
        return new PatternContainerGroup(AEItemKey.of(icon), name, List.of());
    }

    @Override
    public boolean acceptsPlans() {
        return switch (kindAbove()) {
            case DEPLOYER -> NepConfig.createDeploying();
            case SPOUT -> NepConfig.createFilling();
            case NONE -> false;
        };
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = depot.getLevel();
        if (level == null || level.isClientSide()) {
            return false;
        }

        BlockEntity above = machineAbove();
        PatternKey key = new PatternKey(kindOf(above), patternDetails.getDefinition());
        if (UNSATISFIABLE.contains(key)) {
            return false;
        }

        PushOutcome outcome = attempt(level, above, patternDetails, inputs, ejectionDirection);
        if (outcome.unsatisfiable() && UNSATISFIABLE.size() < MAX_MEMOIZED_REJECTS) {
            UNSATISFIABLE.add(key);
        }
        log(level, patternDetails, inputs, outcome);
        return outcome.accepted();
    }

    private PushOutcome attempt(
            Level level,
            BlockEntity above,
            IPatternDetails patternDetails,
            KeyCounter[] inputs,
            Direction ejectionDirection) {
        if (!(patternDetails instanceof AndesiteCraftingPattern)
                && patternDetails.getDefinition().getItem() != AEItems.PROCESSING_PATTERN.asItem()) {
            return PushOutcome.unsatisfiable("not an andesite crafting or processing pattern");
        }
        if (above instanceof DeployerBlockEntity deployer && NepConfig.createDeploying()) {
            return pushDeploying(level, deployer, patternDetails, inputs, ejectionDirection);
        }
        if (above instanceof SpoutBlockEntity spout && NepConfig.createFilling()) {
            return pushFilling(level, spout, patternDetails, inputs);
        }
        return PushOutcome.retry("no enabled Deployer or Spout two blocks above the depot (found " + above + ")");
    }

    private PushOutcome pushDeploying(
            Level level,
            DeployerBlockEntity deployer,
            IPatternDetails patternDetails,
            KeyCounter[] inputs,
            Direction ejectionDirection) {
        ApplicationRecipeResolver.Plan plan = ApplicationRecipeResolver.resolve(patternDetails, level);
        if (plan == null) {
            return PushOutcome.unsatisfiable("no deploying/item_application recipe matched the pattern");
        }

        Map<AEItemKey, Long> items = new HashMap<>();
        Map<AEFluidKey, Long> fluids = new HashMap<>();
        if (!DepotMachines.collectInputs(inputs, items, fluids) || !fluids.isEmpty()) {
            return PushOutcome.unsatisfiable("unexpected fluid input for a deploying pattern");
        }
        if (!items.equals(plan.expectedItems())) {
            return PushOutcome.unsatisfiable(
                    "pushed items " + items + " do not match the recipe's inputs " + plan.expectedItems());
        }

        if (!DepotMachines.depotReady(depot)) {
            return PushOutcome.retry("depot is occupied");
        }
        if (!facesDown(deployer)) {
            return PushOutcome.retry("deployer is not facing down");
        }
        if (deployer.getSpeed() == 0) {
            return PushOutcome.retry("deployer has no rotational speed");
        }

        IItemHandler depotHandler = DepotMachines.itemHandler(depot);
        IItemHandler deployerHandler = DepotMachines.itemHandler(deployer);
        if (depotHandler == null || deployerHandler == null) {
            return PushOutcome.retry(
                    "missing item handler (depot=" + depotHandler + ", deployer=" + deployerHandler + ")");
        }

        int heldSlot = deployerHandler.getSlots() - 1;
        ItemStack held = deployerHandler.getStackInSlot(heldSlot);
        ItemStack suppliedTool = plan.suppliedTool() == null
                ? ItemStack.EMPTY
                : plan.suppliedTool().toStack();
        boolean loadTool = false;

        if (plan.keptTool() != null) {
            if (suppliedTool.isEmpty()) {
                if (held.isEmpty() || !plan.keptTool().test(held)) {
                    return PushOutcome.retry("deployer must hold the non-consumed tool for this recipe");
                }
            } else if (held.isEmpty()) {
                loadTool = true;
            } else if (!plan.keptTool().test(held)) {
                return PushOutcome.retry("deployer is holding " + held + ", which this recipe does not use");
            }
        }

        BlockPos providerPos = depot.getBlockPos().relative(ejectionDirection);
        Direction providerFace = ejectionDirection.getOpposite();
        IItemHandler provider = DeployerReclaimer.providerHandler(level, providerPos, providerFace);
        boolean returnTool = !suppliedTool.isEmpty() && !loadTool;
        if (returnTool
                && (provider == null
                        || !ItemHandlerHelper.insertItem(provider, suppliedTool, true)
                                .isEmpty())) {
            return PushOutcome.retry("nowhere to hand the spare " + suppliedTool + " back to");
        }

        ItemStack depotStack = plan.depotItem().toStack();
        ItemStack consumedTool = plan.consumedTool() == null
                ? ItemStack.EMPTY
                : plan.consumedTool().toStack();
        ItemStack deployed = loadTool ? suppliedTool : consumedTool;

        if (!depotHandler.insertItem(0, depotStack, true).isEmpty()) {
            return PushOutcome.retry("depot rejected the base item");
        }
        if (!deployed.isEmpty()
                && !deployerHandler.insertItem(heldSlot, deployed, true).isEmpty()) {
            return PushOutcome.retry("deployer rejected the deployed item " + deployed);
        }

        depotHandler.insertItem(0, depotStack, false);
        if (!deployed.isEmpty()) {
            deployerHandler.insertItem(heldSlot, deployed, false);
        }
        if (loadTool) {
            DeployerReclaimer.expect(deployer, plan.suppliedTool(), providerPos, providerFace);
        } else if (returnTool) {
            ItemHandlerHelper.insertItem(provider, suppliedTool, false);
        }
        return PushOutcome.accepted("deploying " + depotStack + " with " + (deployed.isEmpty() ? held : deployed));
    }

    private PushOutcome pushFilling(
            Level level, SpoutBlockEntity spout, IPatternDetails patternDetails, KeyCounter[] inputs) {
        FillingRecipeResolver.Plan plan = FillingRecipeResolver.resolve(patternDetails, level);
        if (plan == null) {
            return PushOutcome.unsatisfiable("no filling recipe matched the pattern (check the fluid amount is exact)");
        }

        Map<AEItemKey, Long> items = new HashMap<>();
        Map<AEFluidKey, Long> fluids = new HashMap<>();
        if (!DepotMachines.collectInputs(inputs, items, fluids)) {
            return PushOutcome.unsatisfiable("could not read pushed inputs");
        }
        if (!items.equals(plan.expectedItems()) || fluids.size() != 1) {
            return PushOutcome.unsatisfiable(
                    "pushed inputs items=" + items + " fluids=" + fluids + " do not match recipe");
        }
        Long providedFluid = fluids.get(plan.fluid());
        if (providedFluid == null || providedFluid != plan.fluidAmount()) {
            return PushOutcome.unsatisfiable(
                    "fluid amount " + providedFluid + " does not match required " + plan.fluidAmount());
        }

        if (!DepotMachines.depotReady(depot)) {
            return PushOutcome.retry("depot is occupied");
        }

        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, spout.getBlockPos(), Direction.UP);
        if (tank == null) {
            return PushOutcome.retry("spout exposed no fluid handler");
        }
        if (!tankEmpty(tank)) {
            return PushOutcome.retry("spout tank is not empty");
        }

        IItemHandler depotHandler = DepotMachines.itemHandler(depot);
        if (depotHandler == null) {
            return PushOutcome.retry("depot exposed no item handler");
        }

        ItemStack depotStack = plan.depotItem().toStack();
        FluidStack fluidStack = plan.fluid().toStack((int) plan.fluidAmount());

        if (!depotHandler.insertItem(0, depotStack, true).isEmpty()) {
            return PushOutcome.retry("depot rejected the item");
        }
        if (tank.fill(fluidStack, IFluidHandler.FluidAction.SIMULATE) != fluidStack.getAmount()) {
            return PushOutcome.retry("spout tank rejected the fluid " + fluidStack);
        }

        depotHandler.insertItem(0, depotStack, false);
        tank.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);
        return PushOutcome.accepted("filling " + depotStack + " with " + fluidStack);
    }

    private Kind kindAbove() {
        return kindOf(machineAbove());
    }

    private static Kind kindOf(BlockEntity above) {
        if (above instanceof DeployerBlockEntity) {
            return Kind.DEPLOYER;
        }
        if (above instanceof SpoutBlockEntity) {
            return Kind.SPOUT;
        }
        return Kind.NONE;
    }

    private BlockEntity machineAbove() {
        Level level = depot.getLevel();
        if (level == null) {
            return null;
        }
        BlockPos pos = depot.getBlockPos().above(2);
        return level.getBlockEntity(pos);
    }

    private void log(Level level, IPatternDetails patternDetails, KeyCounter[] inputs, PushOutcome outcome) {
        if (!NepConfig.debugLogging()) {
            return;
        }
        if (!outcome.accepted()
                && !outcome.unsatisfiable()
                && !REJECT_LOG.shouldLog(level, depot.getBlockPos(), outcome.detail())) {
            return;
        }
        Nep.LOGGER.info(
                "Depot {} {} push: {} (output={} inputs=[{}])",
                depot.getBlockPos(),
                outcome.accepted() ? "accepted" : "rejected",
                outcome.detail(),
                describeOutput(patternDetails),
                describeInputs(inputs));
    }

    private static String describeOutput(IPatternDetails patternDetails) {
        return patternDetails.getOutputs().isEmpty()
                ? "?"
                : patternDetails.getOutputs().get(0).toString();
    }

    private static String describeInputs(KeyCounter[] inputs) {
        StringBuilder in = new StringBuilder();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                in.append(entry.getKey())
                        .append(" x")
                        .append(entry.getLongValue())
                        .append("; ");
            }
        }
        return in.toString().trim();
    }

    private static boolean facesDown(DeployerBlockEntity deployer) {
        return deployer.getBlockState().getValue(DirectionalKineticBlock.FACING) == Direction.DOWN;
    }

    private static boolean tankEmpty(IFluidHandler tank) {
        for (int i = 0; i < tank.getTanks(); i++) {
            if (!tank.getFluidInTank(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private enum Kind {
        DEPLOYER,
        SPOUT,
        NONE
    }

    private record PatternKey(Kind kind, AEItemKey definition) {}
}
