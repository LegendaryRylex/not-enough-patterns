package dev.rylex.nep.compat.advancedae.mixin;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import dev.rylex.nep.provider.ImportUpgradeHost;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogicHost;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AdvPatternProviderLogicHost.class)
public interface AdvPatternProviderLogicHostMixin extends ImportUpgradeHost {

    @Override
    default IUpgradeInventory nepImportUpgrades() {
        return ((AdvPatternProviderLogicHost) this).getLogic() instanceof ImportUpgradeHost upgradeHost
                ? upgradeHost.nepImportUpgrades()
                : UpgradeInventories.empty();
    }
}
