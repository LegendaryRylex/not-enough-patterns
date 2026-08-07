package dev.rylex.nep.compat.advancedae.mixin;

import appeng.menu.AEBaseMenu;
import dev.rylex.nep.NepSlotSemantics;
import dev.rylex.nep.provider.ImportCardSlot;
import dev.rylex.nep.provider.ImportUpgradeHost;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogicHost;
import net.pedroksl.advanced_ae.gui.advpatternprovider.AdvPatternProviderMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvPatternProviderMenu.class)
public abstract class AdvPatternProviderMenuMixin extends AEBaseMenu {

    private AdvPatternProviderMenuMixin(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(
            method =
                    "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/pedroksl/advanced_ae/common/logic/AdvPatternProviderLogicHost;)V",
            at = @At("RETURN"))
    private void nep$addUpgradeSlots(
            MenuType<? extends AdvPatternProviderMenu> menuType,
            int id,
            Inventory playerInventory,
            AdvPatternProviderLogicHost host,
            CallbackInfo ci) {
        if (!(host.getLogic() instanceof ImportUpgradeHost upgradeHost)) {
            return;
        }

        var upgrades = upgradeHost.nepImportUpgrades();
        for (int i = 0; i < upgrades.size(); i++) {
            var slot = new ImportCardSlot(upgrades, i);
            slot.setNotDraggable();
            addSlot(slot, NepSlotSemantics.IMPORT_UPGRADE);
        }
    }
}
