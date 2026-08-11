package dev.rylex.nep;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.upgrades.Upgrades;
import dev.rylex.nep.pattern.AwakeningPattern;
import dev.rylex.nep.pattern.EnchantingPattern;
import dev.rylex.nep.pattern.InfusionPattern;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NepItems {
    private NepItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nep.MOD_ID);

    public static final DeferredItem<Item> INFUSION_PATTERN = ITEMS.registerItem(
            "infusion_pattern", props -> PatternDetailsHelper.encodedPatternItemBuilder(InfusionPattern::new)
                    .build(props));

    public static final DeferredItem<Item> AWAKENING_PATTERN = ITEMS.registerItem(
            "awakening_pattern", props -> PatternDetailsHelper.encodedPatternItemBuilder(AwakeningPattern::new)
                    .build(props));

    public static final DeferredItem<Item> ENCHANTING_PATTERN = ITEMS.registerItem(
            "enchanting_pattern", props -> PatternDetailsHelper.encodedPatternItemBuilder(EnchantingPattern::new)
                    .build(props));

    public static final DeferredItem<Item> IMPORT_CARD =
            ITEMS.registerItem("import_card", Upgrades::createUpgradeCardItem);

    public static final DeferredItem<Item> MATRIX_CIRCUITRY = ITEMS.registerSimpleItem("matrix_circuitry");
}
