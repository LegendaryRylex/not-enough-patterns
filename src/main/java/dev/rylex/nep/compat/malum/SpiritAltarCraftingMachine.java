package dev.rylex.nep.compat.malum;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEItems;
import com.sammy.malum.common.block.curiosities.spirit_altar.AltarCraftingHelper;
import com.sammy.malum.common.block.curiosities.spirit_altar.SpiritAltarBlockEntity;
import com.sammy.malum.common.block.storage.IMalumSpecialItemAccessPoint;
import com.sammy.malum.registry.common.block.MalumBlocks;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.PushOutcome;
import dev.rylex.nep.pattern.SpiritInfusionPattern;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import team.lodestar.lodestone.systems.blockentity.LodestoneBlockEntityInventory;

public class SpiritAltarCraftingMachine extends MemoizedPushMachine {

    private static final Memo MEMO = new Memo();

    private final SpiritAltarBlockEntity altar;

    public SpiritAltarCraftingMachine(SpiritAltarBlockEntity altar) {
        super(altar, MEMO, "Spirit Altar");
        this.altar = altar;
    }

    static void clearCache() {
        MEMO.clear();
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        ItemStack icon = new ItemStack(MalumBlocks.SPIRIT_ALTAR.get());
        return new PatternContainerGroup(
                AEItemKey.of(icon), MalumBlocks.SPIRIT_ALTAR.get().getName(), List.of());
    }

    @Override
    boolean enabled() {
        return NepConfig.malumSpiritInfusion();
    }

    @Override
    PushOutcome attempt(Level level, IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        if (!(patternDetails instanceof SpiritInfusionPattern)
                && patternDetails.getDefinition().getItem() != AEItems.PROCESSING_PATTERN.asItem()) {
            return PushOutcome.unsatisfiable("not a spirit infusion or processing pattern");
        }

        SpiritInfusionResolver.Plan plan = SpiritInfusionResolver.resolve(patternDetails, level);
        if (plan == null) {
            return PushOutcome.unsatisfiable("no spirit infusion recipe matched the pattern");
        }

        Map<AEItemKey, Long> provided = collectItems(inputs);
        if (provided == null) {
            return PushOutcome.unsatisfiable("spirit infusion takes items only");
        }
        if (!provided.equals(plan.expectedItems())) {
            return PushOutcome.unsatisfiable(
                    "pushed items " + provided + " do not match the recipe's inputs " + plan.expectedItems());
        }

        if (plan.input().amount() > altar.inventory.getSlotLimit(0)) {
            return PushOutcome.unsatisfiable("the altar slot cannot hold " + plan.input());
        }
        for (GenericStack spirit : plan.spirits()) {
            if (spirit.amount() > altar.spiritInventory.getSlotLimit(0)) {
                return PushOutcome.unsatisfiable("a spirit slot cannot hold " + spirit);
            }
        }

        if (!isEmpty(altar.inventory)) {
            return PushOutcome.retry("altar is already holding an item");
        }
        if (!isEmpty(altar.spiritInventory)) {
            return PushOutcome.retry("altar is still holding spirits");
        }
        if (!isEmpty(altar.extrasInventory)) {
            return PushOutcome.retry("altar is still holding ingredients it took from its pedestals");
        }

        List<IMalumSpecialItemAccessPoint> free = new ArrayList<>();
        for (IMalumSpecialItemAccessPoint pedestal : AltarCraftingHelper.capturePedestals(level, altar.getBlockPos())) {
            if (isEmpty(pedestal.getSuppliedInventory())) {
                free.add(pedestal);
            }
        }
        if (free.size() < plan.extras().size()) {
            return PushOutcome.retry(
                    "recipe needs " + plan.extras().size() + " free pedestal(s) in range, found " + free.size());
        }

        return commit(plan, free, ejectionDirection);
    }

    private PushOutcome commit(
            SpiritInfusionResolver.Plan plan, List<IMalumSpecialItemAccessPoint> pedestals, Direction ejection) {
        List<LodestoneBlockEntityInventory> staged = new ArrayList<>();
        for (int slot = 0; slot < plan.extras().size(); slot++) {
            LodestoneBlockEntityInventory inventory = pedestals.get(slot).getSuppliedInventory();
            inventory.setStackInSlot(0, toStack(plan.extras().get(slot)));
            staged.add(inventory);
        }
        for (GenericStack spirit : plan.spirits()) {
            if (!ItemHandlerHelper.insertItem(altar.spiritInventory, toStack(spirit), false)
                    .isEmpty()) {
                unstage(staged);
                return PushOutcome.retry("altar would not take " + spirit);
            }
        }
        if (!ItemHandlerHelper.insertItem(altar.inventory, toStack(plan.input()), false)
                .isEmpty()) {
            unstage(staged);
            return PushOutcome.retry("altar would not take " + plan.input());
        }

        if (altar.recipe != plan.holder().value()) {
            unstage(staged);
            return PushOutcome.retry("altar settled on "
                    + (altar.recipe == null ? "no recipe at all" : "a different recipe")
                    + " once it was loaded");
        }

        SpiritReclaimer.expect(altar, ejection);
        altar.setChanged();
        return PushOutcome.accepted(
                "infusing " + plan.result() + " over " + plan.extras().size() + " pedestal(s)");
    }

    private void unstage(List<LodestoneBlockEntityInventory> pedestals) {
        altar.inventory.clear();
        altar.spiritInventory.clear();
        for (LodestoneBlockEntityInventory pedestal : pedestals) {
            pedestal.clear();
        }
        SpiritReclaimer.forget(altar);
    }
}
