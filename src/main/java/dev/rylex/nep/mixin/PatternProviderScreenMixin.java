package dev.rylex.nep.mixin;

import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.Upgrades;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.core.localization.GuiText;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.PatternProviderMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PatternProviderScreen.class)
public abstract class PatternProviderScreenMixin extends AEBaseScreen<PatternProviderMenu> {

    private PatternProviderScreenMixin(
            PatternProviderMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nep$addUpgradePanel(
            PatternProviderMenu menu, Inventory playerInventory, Component title, ScreenStyle style, CallbackInfo ci) {
        var upgradeSlots = menu.getSlots(SlotSemantics.UPGRADE);
        if (!upgradeSlots.isEmpty()) {
            widgets.add("upgrades", new UpgradesPanel(upgradeSlots, this::nep$getCompatibleUpgrades));
        }
    }

    @Unique
    private List<Component> nep$getCompatibleUpgrades() {
        if (!(menu.getTarget() instanceof PatternProviderLogicHost host)
                || !(host.getLogic() instanceof IUpgradeableObject upgradeable)) {
            return List.of();
        }

        var lines = new ArrayList<Component>();
        lines.add(GuiText.CompatibleUpgrades.text());
        lines.addAll(
                Upgrades.getTooltipLinesForMachine(upgradeable.getUpgrades().getUpgradableItem()));
        return lines;
    }
}
