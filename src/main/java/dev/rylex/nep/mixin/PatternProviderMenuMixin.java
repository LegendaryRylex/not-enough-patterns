package dev.rylex.nep.mixin;

import appeng.api.upgrades.IUpgradeableObject;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.PatternProviderMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PatternProviderMenu.class)
public abstract class PatternProviderMenuMixin extends AEBaseMenu {

    private PatternProviderMenuMixin(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nep$addUpgradeSlots(
            MenuType<? extends PatternProviderMenu> menuType,
            int id,
            Inventory playerInventory,
            PatternProviderLogicHost host,
            CallbackInfo ci) {
        if (host.getLogic() instanceof IUpgradeableObject upgradeable) {
            setupUpgrades(upgradeable.getUpgrades());
        }
    }
}
