package dev.rylex.nep.mixin;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import dev.rylex.nep.provider.ImportUpgradeHost;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PatternProviderLogicHost.class)
public interface PatternProviderLogicHostMixin extends ImportUpgradeHost {

    @Override
    default IUpgradeInventory nepImportUpgrades() {
        return ((PatternProviderLogicHost) this).getLogic() instanceof ImportUpgradeHost upgradeHost
                ? upgradeHost.nepImportUpgrades()
                : UpgradeInventories.empty();
    }
}
