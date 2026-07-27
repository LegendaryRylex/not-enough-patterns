package dev.rylex.nep;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.upgrades.Upgrades;
import dev.rylex.nep.pattern.AndesiteCraftingPattern;
import dev.rylex.nep.pattern.MechanicalCraftingPattern;
import dev.rylex.nep.pattern.SequencedAssemblyPattern;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NepItems {
    private NepItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nep.MOD_ID);

    public static final DeferredItem<Item> MECHANICAL_CRAFTING_PATTERN =
            ITEMS.register("mechanical_crafting_pattern", () -> PatternDetailsHelper.encodedPatternItemBuilder(
                            MechanicalCraftingPattern::new)
                    .build());

    public static final DeferredItem<Item> ANDESITE_CRAFTING_PATTERN =
            ITEMS.register("andesite_crafting_pattern", () -> PatternDetailsHelper.encodedPatternItemBuilder(
                            AndesiteCraftingPattern::new)
                    .build());

    public static final DeferredItem<Item> SEQUENCED_ASSEMBLY_PATTERN =
            ITEMS.register("sequenced_assembly_pattern", () -> PatternDetailsHelper.encodedPatternItemBuilder(
                            SequencedAssemblyPattern::new)
                    .build());

    public static final DeferredItem<Item> IMPORT_CARD =
            ITEMS.registerItem("import_card", Upgrades::createUpgradeCardItem, new Item.Properties());

    public static final DeferredItem<Item> MATRIX_CIRCUITRY = ITEMS.registerSimpleItem("matrix_circuitry");
}
