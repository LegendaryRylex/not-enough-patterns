package dev.rylex.nep.mixin;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.rylex.nep.NepItems;
import java.util.List;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AEBaseMenu.class)
public abstract class AEBaseMenuMixin {

    @Shadow
    public abstract SlotSemantic getSlotSemantic(Slot s);

    @ModifyReturnValue(method = "getQuickMoveDestinationSlots", at = @At("RETURN"))
    private List<Slot> nep$preferImportCardUpgradeSlot(
            List<Slot> destinations, ItemStack stackToMove, boolean fromPlayerSide) {
        if (!fromPlayerSide || !stackToMove.is(NepItems.IMPORT_CARD.get())) {
            return destinations;
        }

        var upgradeSlots = destinations.stream()
                .filter(slot -> !slot.hasItem() && getSlotSemantic(slot) == SlotSemantics.UPGRADE)
                .toList();

        return upgradeSlots.isEmpty() ? destinations : upgradeSlots;
    }
}
