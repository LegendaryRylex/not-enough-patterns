package dev.rylex.nep.provider;

import appeng.api.upgrades.IUpgradeInventory;
import net.minecraft.world.level.ItemLike;

public interface ImportUpgradeHost {

    IUpgradeInventory nepImportUpgrades();

    default int getInstalledUpgrades(ItemLike upgradeCard) {
        return nepImportUpgrades().getInstalledUpgrades(upgradeCard);
    }

    default boolean isUpgradedWith(ItemLike upgradeCard) {
        return nepImportUpgrades().isInstalled(upgradeCard);
    }
}
