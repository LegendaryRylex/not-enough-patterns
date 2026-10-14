package dev.rylex.nep.compat.malum;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEItems;
import com.sammy.malum.common.block.curiosities.spirit_crucible.SpiritCrucibleCoreBlockEntity;
import com.sammy.malum.registry.common.block.MalumBlocks;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.PushOutcome;
import dev.rylex.nep.pattern.SpiritFocusingPattern;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class SpiritCrucibleCraftingMachine extends MemoizedPushMachine {

    private static final Memo MEMO = new Memo();

    private final SpiritCrucibleCoreBlockEntity crucible;

    public SpiritCrucibleCraftingMachine(SpiritCrucibleCoreBlockEntity crucible) {
        super(crucible, MEMO, "Spirit Crucible");
        this.crucible = crucible;
    }

    static void clearCache() {
        MEMO.clear();
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        ItemStack icon = new ItemStack(MalumBlocks.SPIRIT_CRUCIBLE.get());
        return new PatternContainerGroup(
                AEItemKey.of(icon), MalumBlocks.SPIRIT_CRUCIBLE.get().getName(), List.of());
    }

    @Override
    boolean enabled() {
        return NepConfig.malumSpiritFocusing();
    }

    @Override
    PushOutcome attempt(Level level, IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        if (!(patternDetails instanceof SpiritFocusingPattern)
                && patternDetails.getDefinition().getItem() != AEItems.PROCESSING_PATTERN.asItem()) {
            return PushOutcome.unsatisfiable("not a spirit focusing or processing pattern");
        }

        SpiritFocusingResolver.Plan plan = SpiritFocusingResolver.resolve(patternDetails, level);
        if (plan == null) {
            return PushOutcome.unsatisfiable("no spirit focusing recipe matched the pattern");
        }

        Map<AEItemKey, Long> provided = collectItems(inputs);
        if (provided == null) {
            return PushOutcome.unsatisfiable("spirit focusing takes items only");
        }
        if (!provided.equals(plan.expectedItems())) {
            return PushOutcome.unsatisfiable(
                    "pushed items " + provided + " do not match the recipe's spirits " + plan.expectedItems());
        }

        for (GenericStack spirit : plan.spirits()) {
            if (spirit.amount() > crucible.spiritInventory.getSlotLimit(0)) {
                return PushOutcome.unsatisfiable("a spirit slot cannot hold " + spirit);
            }
        }
        if (plan.spirits().size() > crucible.spiritInventory.getSlots()) {
            return PushOutcome.unsatisfiable("the crucible has fewer spirit slots than the recipe needs");
        }

        if (!SpiritFocusingResolver.acceptsImpetus(plan.holder().value(), crucible.inventory.getStackInSlot(0))) {
            return PushOutcome.retry("crucible is not holding the impetus this recipe focuses through");
        }
        if (crucible.recipe != null || crucible.isCrafting) {
            return PushOutcome.retry("crucible is already focusing");
        }
        if (!isEmpty(crucible.spiritInventory)) {
            return PushOutcome.retry("crucible is still holding spirits");
        }

        return commit(plan, ejectionDirection);
    }

    private PushOutcome commit(SpiritFocusingResolver.Plan plan, Direction ejection) {
        for (GenericStack spirit : plan.spirits()) {
            if (!ItemHandlerHelper.insertItem(crucible.spiritInventory, toStack(spirit), false)
                    .isEmpty()) {
                unstage();
                return PushOutcome.retry("crucible would not take " + spirit);
            }
        }

        crucible.updateRecipe();
        if (crucible.recipe != plan.holder().value()) {
            unstage();
            return PushOutcome.retry("crucible settled on "
                    + (crucible.recipe == null ? "no recipe at all" : "a different recipe")
                    + " once its spirits were loaded");
        }

        SpiritReclaimer.expect(crucible, ejection);
        crucible.setChanged();
        return PushOutcome.accepted("focusing " + plan.result());
    }

    private void unstage() {
        crucible.spiritInventory.clear();
        crucible.updateRecipe();
        SpiritReclaimer.forget(crucible);
    }
}
