package dev.rylex.nep.compat.mysticalagriculture;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEItems;
import com.blakebr0.mysticalagriculture.init.ModBlocks;
import com.blakebr0.mysticalagriculture.tileentity.InfusionAltarTileEntity;
import com.blakebr0.mysticalagriculture.tileentity.InfusionPedestalTileEntity;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.PushOutcome;
import dev.rylex.nep.machine.RejectLog;
import dev.rylex.nep.pattern.InfusionPattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class InfusionAltarCraftingMachine implements ICraftingMachine {

    private static final int MAX_MEMOIZED_REJECTS = 512;

    private static final Set<AEItemKey> UNSATISFIABLE = new HashSet<>();
    private static final RejectLog REJECT_LOG = new RejectLog();

    private final InfusionAltarTileEntity altar;

    public InfusionAltarCraftingMachine(InfusionAltarTileEntity altar) {
        this.altar = altar;
    }

    static void clearCache() {
        UNSATISFIABLE.clear();
        REJECT_LOG.clear();
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        ItemStack icon = new ItemStack(ModBlocks.INFUSION_ALTAR.get());
        return new PatternContainerGroup(
                AEItemKey.of(icon), ModBlocks.INFUSION_ALTAR.get().getName(), List.of());
    }

    @Override
    public boolean acceptsPlans() {
        return NepConfig.mysticalInfusion();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = altar.getLevel();
        if (level == null || level.isClientSide() || !NepConfig.mysticalInfusion()) {
            return false;
        }

        AEItemKey key = patternDetails.getDefinition();
        if (UNSATISFIABLE.contains(key)) {
            return false;
        }

        PushOutcome outcome = attempt(level, patternDetails, inputs);
        if (outcome.unsatisfiable() && UNSATISFIABLE.size() < MAX_MEMOIZED_REJECTS) {
            UNSATISFIABLE.add(key);
        }
        log(level, patternDetails, outcome);
        return outcome.accepted();
    }

    private PushOutcome attempt(Level level, IPatternDetails patternDetails, KeyCounter[] inputs) {
        if (!(patternDetails instanceof InfusionPattern)
                && patternDetails.getDefinition().getItem() != AEItems.PROCESSING_PATTERN.asItem()) {
            return PushOutcome.unsatisfiable("not an infusion or processing pattern");
        }

        MysticalRecipeResolver.InfusionPlan plan = MysticalRecipeResolver.resolveInfusion(patternDetails, level);
        if (plan == null) {
            return PushOutcome.unsatisfiable("no infusion recipe matched the pattern");
        }

        Map<AEItemKey, Long> provided = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return PushOutcome.unsatisfiable("infusion takes items only");
                }
                provided.merge(itemKey, entry.getLongValue(), Long::sum);
            }
        }
        if (!provided.equals(plan.expectedItems())) {
            return PushOutcome.unsatisfiable(
                    "pushed items " + provided + " do not match the recipe's inputs " + plan.expectedItems());
        }

        if (!altar.getInventory().getStackInSlot(0).isEmpty()) {
            return PushOutcome.retry("altar is already holding an input");
        }
        if (!altar.getInventory().getStackInSlot(1).isEmpty()) {
            return PushOutcome.retry("altar output slot still holds a finished item");
        }

        List<InfusionPedestalTileEntity> pedestals = new ArrayList<>();
        for (BlockPos pos : altar.getPedestalPositions()) {
            if (level.getBlockEntity(pos) instanceof InfusionPedestalTileEntity pedestal) {
                pedestals.add(pedestal);
            }
        }
        if (pedestals.size() != plan.pedestals().size()) {
            return PushOutcome.retry(
                    "recipe needs exactly " + plan.pedestals().size() + " pedestals, found " + pedestals.size());
        }
        for (InfusionPedestalTileEntity pedestal : pedestals) {
            if (!pedestal.getInventory().getStackInSlot(0).isEmpty()) {
                return PushOutcome.retry("pedestal at " + pedestal.getBlockPos() + " is already holding an item");
            }
        }

        return commit(plan, pedestals);
    }

    private PushOutcome commit(MysticalRecipeResolver.InfusionPlan plan, List<InfusionPedestalTileEntity> pedestals) {
        altar.getInventory().setStackInSlot(0, toStack(plan.altar()));
        for (int slot = 0; slot < plan.pedestals().size(); slot++) {
            pedestals
                    .get(slot)
                    .getInventory()
                    .setStackInSlot(0, toStack(plan.pedestals().get(slot)));
        }
        altar.activate();
        altar.setChanged();
        return PushOutcome.accepted("infusing " + plan.result());
    }

    static ItemStack toStack(GenericStack stack) {
        return stack.what() instanceof AEItemKey key ? key.toStack((int) stack.amount()) : ItemStack.EMPTY;
    }

    private void log(Level level, IPatternDetails patternDetails, PushOutcome outcome) {
        if (!NepConfig.debugLogging()) {
            return;
        }
        if (!outcome.accepted()
                && !outcome.unsatisfiable()
                && !REJECT_LOG.shouldLog(level, altar.getBlockPos(), outcome.detail())) {
            return;
        }
        Nep.LOGGER.info(
                "Infusion Altar {} {} push: {} (output={})",
                altar.getBlockPos(),
                outcome.accepted() ? "accepted" : "rejected",
                outcome.detail(),
                patternDetails.getOutputs().isEmpty()
                        ? "?"
                        : patternDetails.getOutputs().get(0).toString());
    }
}
