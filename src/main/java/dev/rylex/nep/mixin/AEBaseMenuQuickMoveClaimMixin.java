package dev.rylex.nep.mixin;

import appeng.menu.AEBaseMenu;
import dev.rylex.nep.NepItems;
import dev.rylex.nep.NepSlotSemantics;
import dev.rylex.nep.provider.ImportCardSlot;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AEBaseMenu.class, priority = 500)
public abstract class AEBaseMenuQuickMoveClaimMixin {

    @Shadow
    protected abstract List<Slot> getQuickMoveDestinationSlots(ItemStack stackToMove, boolean fromPlayerSide);

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void nep$claimImportCardQuickMove(Player player, int idx, CallbackInfoReturnable<ItemStack> cir) {
        var menu = (AEBaseMenu) (Object) this;
        if (menu.isClientSide()) {
            return;
        }

        var clickSlot = menu.getSlot(idx);
        var stackToMove = clickSlot.getItem();
        if (stackToMove.isEmpty() || !stackToMove.is(NepItems.IMPORT_CARD.get())) {
            return;
        }

        var importSlots = menu.getSlots(NepSlotSemantics.IMPORT_UPGRADE);
        if (importSlots.isEmpty()) {
            return;
        }

        var fromImportSlot = clickSlot instanceof ImportCardSlot;
        if (!fromImportSlot && !menu.isPlayerSideSlot(clickSlot)) {
            return;
        }

        if (!clickSlot.mayPickup(player)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        List<Slot> destinations;
        if (fromImportSlot) {
            destinations = getQuickMoveDestinationSlots(stackToMove, false);
        } else {
            destinations = importSlots.stream()
                    .filter(slot -> slot instanceof ImportCardSlot && !slot.hasItem())
                    .toList();
        }

        var remaining = stackToMove.copy();
        for (var dest : destinations) {
            if (dest.hasItem() && (remaining = dest.safeInsert(remaining)).isEmpty()) {
                break;
            }
        }
        if (!remaining.isEmpty()) {
            for (var dest : destinations) {
                if (!dest.hasItem() && (remaining = dest.safeInsert(remaining)).isEmpty()) {
                    break;
                }
            }
        }

        if (remaining.getCount() != stackToMove.getCount()) {
            clickSlot.setByPlayer(remaining.isEmpty() ? ItemStack.EMPTY : remaining);
        }
        cir.setReturnValue(ItemStack.EMPTY);
    }
}
