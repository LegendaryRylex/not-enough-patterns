package dev.rylex.nep.mixin;

import appeng.menu.AEBaseMenu;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.rylex.nep.NepItems;
import dev.rylex.nep.provider.ImportCardSlot;
import java.util.List;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AEBaseMenu.class, priority = 3500)
public abstract class AEBaseMenuMixin {

    @ModifyReturnValue(method = "getQuickMoveDestinationSlots", at = @At("RETURN"))
    private List<Slot> nep$preferImportCardUpgradeSlot(
            List<Slot> destinations, ItemStack stackToMove, boolean fromPlayerSide) {
        if (!fromPlayerSide || !stackToMove.is(NepItems.IMPORT_CARD.get())) {
            return destinations;
        }

        var importSlots = destinations.stream()
                .filter(slot -> slot instanceof ImportCardSlot && !slot.hasItem())
                .toList();

        return importSlots.isEmpty() ? destinations : importSlots;
    }
}
