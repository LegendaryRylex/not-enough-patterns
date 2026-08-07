package dev.rylex.nep.compat.mysticalagriculture;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEItems;
import com.blakebr0.mysticalagriculture.init.ModBlocks;
import com.blakebr0.mysticalagriculture.tileentity.AwakeningAltarTileEntity;
import com.blakebr0.mysticalagriculture.tileentity.AwakeningPedestalTileEntity;
import com.blakebr0.mysticalagriculture.tileentity.EssenceVesselTileEntity;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.PushOutcome;
import dev.rylex.nep.machine.RejectLog;
import dev.rylex.nep.pattern.AwakeningPattern;
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

public class AwakeningAltarCraftingMachine implements ICraftingMachine {

    private static final int MAX_MEMOIZED_REJECTS = 512;

    private static final Set<AEItemKey> UNSATISFIABLE = new HashSet<>();
    private static final RejectLog REJECT_LOG = new RejectLog();

    private final AwakeningAltarTileEntity altar;

    public AwakeningAltarCraftingMachine(AwakeningAltarTileEntity altar) {
        this.altar = altar;
    }

    static void clearCache() {
        UNSATISFIABLE.clear();
        REJECT_LOG.clear();
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        ItemStack icon = new ItemStack(ModBlocks.AWAKENING_ALTAR.get());
        return new PatternContainerGroup(
                AEItemKey.of(icon), ModBlocks.AWAKENING_ALTAR.get().getName(), List.of());
    }

    @Override
    public boolean acceptsPlans() {
        return NepConfig.mysticalAwakening();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = altar.getLevel();
        if (level == null || level.isClientSide() || !NepConfig.mysticalAwakening()) {
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
        if (!(patternDetails instanceof AwakeningPattern)
                && patternDetails.getDefinition().getItem() != AEItems.PROCESSING_PATTERN.asItem()) {
            return PushOutcome.unsatisfiable("not an awakening or processing pattern");
        }

        MysticalRecipeResolver.AwakeningPlan plan = MysticalRecipeResolver.resolveAwakening(patternDetails, level);
        if (plan == null) {
            return PushOutcome.unsatisfiable("no awakening recipe matched the pattern");
        }

        Map<AEItemKey, Long> provided = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return PushOutcome.unsatisfiable("awakening takes items only");
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

        List<AwakeningPedestalTileEntity> pedestals = new ArrayList<>();
        List<EssenceVesselTileEntity> vessels = new ArrayList<>();
        for (BlockPos pos : altar.getPedestalPositions()) {
            if (level.getBlockEntity(pos) instanceof AwakeningPedestalTileEntity pedestal) {
                pedestals.add(pedestal);
            } else if (level.getBlockEntity(pos) instanceof EssenceVesselTileEntity vessel) {
                vessels.add(vessel);
            }
        }
        if (pedestals.size() != plan.pedestals().size()) {
            return PushOutcome.retry("recipe needs exactly " + plan.pedestals().size() + " awakening pedestals, found "
                    + pedestals.size());
        }
        if (vessels.size() != plan.essences().size()) {
            return PushOutcome.retry(
                    "recipe needs exactly " + plan.essences().size() + " essence vessels, found " + vessels.size());
        }
        for (AwakeningPedestalTileEntity pedestal : pedestals) {
            if (!pedestal.getInventory().getStackInSlot(0).isEmpty()) {
                return PushOutcome.retry("pedestal at " + pedestal.getBlockPos() + " is already holding an item");
            }
        }
        for (EssenceVesselTileEntity vessel : vessels) {
            if (!vessel.getInventory().getStackInSlot(0).isEmpty()) {
                return PushOutcome.retry("essence vessel at " + vessel.getBlockPos() + " is not empty");
            }
        }

        return commit(plan, pedestals, vessels);
    }

    private PushOutcome commit(
            MysticalRecipeResolver.AwakeningPlan plan,
            List<AwakeningPedestalTileEntity> pedestals,
            List<EssenceVesselTileEntity> vessels) {
        altar.getInventory().setStackInSlot(0, InfusionAltarCraftingMachine.toStack(plan.altar()));
        for (int slot = 0; slot < plan.pedestals().size(); slot++) {
            pedestals
                    .get(slot)
                    .getInventory()
                    .setStackInSlot(
                            0,
                            InfusionAltarCraftingMachine.toStack(
                                    plan.pedestals().get(slot)));
        }
        for (int slot = 0; slot < plan.essences().size(); slot++) {
            vessels.get(slot)
                    .getInventory()
                    .setStackInSlot(
                            0,
                            InfusionAltarCraftingMachine.toStack(plan.essences().get(slot)));
        }
        altar.activate();
        altar.setChanged();
        return PushOutcome.accepted("awakening " + plan.result());
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
                "Awakening Altar {} {} push: {} (output={})",
                altar.getBlockPos(),
                outcome.accepted() ? "accepted" : "rejected",
                outcome.detail(),
                patternDetails.getOutputs().isEmpty()
                        ? "?"
                        : patternDetails.getOutputs().get(0).toString());
    }
}
