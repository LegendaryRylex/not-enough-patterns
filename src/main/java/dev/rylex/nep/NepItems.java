package dev.rylex.nep;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.upgrades.Upgrades;
import dev.rylex.nep.pattern.AndesiteCraftingPattern;
import dev.rylex.nep.pattern.ApparatusPattern;
import dev.rylex.nep.pattern.AtomicReconstructionPattern;
import dev.rylex.nep.pattern.AwakeningPattern;
import dev.rylex.nep.pattern.EmpoweringPattern;
import dev.rylex.nep.pattern.EnchantingPattern;
import dev.rylex.nep.pattern.FusionCraftingPattern;
import dev.rylex.nep.pattern.ImbuementPattern;
import dev.rylex.nep.pattern.InfusionPattern;
import dev.rylex.nep.pattern.MechanicalCraftingPattern;
import dev.rylex.nep.pattern.MiniaturizationPattern;
import dev.rylex.nep.pattern.RuneworkingPattern;
import dev.rylex.nep.pattern.SequencedAssemblyPattern;
import dev.rylex.nep.pattern.SpiritFocusingPattern;
import dev.rylex.nep.pattern.SpiritInfusionPattern;
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

    public static final DeferredItem<Item> FUSION_CRAFTING_PATTERN = ITEMS.register(
            "fusion_crafting_pattern", () -> PatternDetailsHelper.encodedPatternItemBuilder(FusionCraftingPattern::new)
                    .build());

    public static final DeferredItem<Item> EMPOWERING_PATTERN = ITEMS.register(
            "empowering_pattern", () -> PatternDetailsHelper.encodedPatternItemBuilder(EmpoweringPattern::new)
                    .build());

    public static final DeferredItem<Item> ATOMIC_RECONSTRUCTION_PATTERN =
            ITEMS.register("atomic_reconstruction_pattern", () -> PatternDetailsHelper.encodedPatternItemBuilder(
                            AtomicReconstructionPattern::new)
                    .build());

    public static final DeferredItem<Item> MINIATURIZATION_PATTERN = ITEMS.register(
            "miniaturization_pattern", () -> PatternDetailsHelper.encodedPatternItemBuilder(MiniaturizationPattern::new)
                    .build());

    public static final DeferredItem<Item> INFUSION_PATTERN = ITEMS.register(
            "infusion_pattern", () -> PatternDetailsHelper.encodedPatternItemBuilder(InfusionPattern::new)
                    .build());

    public static final DeferredItem<Item> AWAKENING_PATTERN = ITEMS.register(
            "awakening_pattern", () -> PatternDetailsHelper.encodedPatternItemBuilder(AwakeningPattern::new)
                    .build());

    public static final DeferredItem<Item> SPIRIT_INFUSION_PATTERN = ITEMS.register(
            "spirit_infusion_pattern", () -> PatternDetailsHelper.encodedPatternItemBuilder(SpiritInfusionPattern::new)
                    .build());

    public static final DeferredItem<Item> SPIRIT_FOCUSING_PATTERN = ITEMS.register(
            "spirit_focusing_pattern", () -> PatternDetailsHelper.encodedPatternItemBuilder(SpiritFocusingPattern::new)
                    .build());

    public static final DeferredItem<Item> RUNEWORKING_PATTERN = ITEMS.register(
            "runeworking_pattern", () -> PatternDetailsHelper.encodedPatternItemBuilder(RuneworkingPattern::new)
                    .build());

    public static final DeferredItem<Item> APPARATUS_PATTERN = ITEMS.register(
            "apparatus_pattern", () -> PatternDetailsHelper.encodedPatternItemBuilder(ApparatusPattern::new)
                    .build());

    public static final DeferredItem<Item> IMBUEMENT_PATTERN = ITEMS.register(
            "imbuement_pattern", () -> PatternDetailsHelper.encodedPatternItemBuilder(ImbuementPattern::new)
                    .build());

    public static final DeferredItem<Item> ENCHANTING_PATTERN = ITEMS.register(
            "enchanting_pattern", () -> PatternDetailsHelper.encodedPatternItemBuilder(EnchantingPattern::new)
                    .build());

    public static final DeferredItem<Item> IMPORT_CARD =
            ITEMS.registerItem("import_card", Upgrades::createUpgradeCardItem, new Item.Properties());

    public static final DeferredItem<Item> MATRIX_CIRCUITRY = ITEMS.registerSimpleItem("matrix_circuitry");
}
