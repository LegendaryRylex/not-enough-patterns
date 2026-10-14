package dev.rylex.nep;

import appeng.api.crafting.EncodedPatternDecoder;
import appeng.api.crafting.IPatternDetails;
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
import dev.rylex.nep.pattern.ParticleCollisionPattern;
import dev.rylex.nep.pattern.RitualPattern;
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

    private static <T extends IPatternDetails> DeferredItem<Item> pattern(
            String name, EncodedPatternDecoder<T> factory) {
        return ITEMS.register(name, () -> PatternDetailsHelper.encodedPatternItemBuilder(factory)
                .build());
    }

    public static final DeferredItem<Item> MECHANICAL_CRAFTING_PATTERN =
            pattern("mechanical_crafting_pattern", MechanicalCraftingPattern::new);

    public static final DeferredItem<Item> ANDESITE_CRAFTING_PATTERN =
            pattern("andesite_crafting_pattern", AndesiteCraftingPattern::new);

    public static final DeferredItem<Item> SEQUENCED_ASSEMBLY_PATTERN =
            pattern("sequenced_assembly_pattern", SequencedAssemblyPattern::new);

    public static final DeferredItem<Item> FUSION_CRAFTING_PATTERN =
            pattern("fusion_crafting_pattern", FusionCraftingPattern::new);

    public static final DeferredItem<Item> EMPOWERING_PATTERN = pattern("empowering_pattern", EmpoweringPattern::new);

    public static final DeferredItem<Item> ATOMIC_RECONSTRUCTION_PATTERN =
            pattern("atomic_reconstruction_pattern", AtomicReconstructionPattern::new);

    public static final DeferredItem<Item> MINIATURIZATION_PATTERN =
            pattern("miniaturization_pattern", MiniaturizationPattern::new);

    public static final DeferredItem<Item> INFUSION_PATTERN = pattern("infusion_pattern", InfusionPattern::new);

    public static final DeferredItem<Item> AWAKENING_PATTERN = pattern("awakening_pattern", AwakeningPattern::new);

    public static final DeferredItem<Item> SPIRIT_INFUSION_PATTERN =
            pattern("spirit_infusion_pattern", SpiritInfusionPattern::new);

    public static final DeferredItem<Item> SPIRIT_FOCUSING_PATTERN =
            pattern("spirit_focusing_pattern", SpiritFocusingPattern::new);

    public static final DeferredItem<Item> RUNEWORKING_PATTERN =
            pattern("runeworking_pattern", RuneworkingPattern::new);

    public static final DeferredItem<Item> APPARATUS_PATTERN = pattern("apparatus_pattern", ApparatusPattern::new);

    public static final DeferredItem<Item> IMBUEMENT_PATTERN = pattern("imbuement_pattern", ImbuementPattern::new);

    public static final DeferredItem<Item> ENCHANTING_PATTERN = pattern("enchanting_pattern", EnchantingPattern::new);

    public static final DeferredItem<Item> PARTICLE_COLLISION_PATTERN =
            pattern("particle_collision_pattern", ParticleCollisionPattern::new);

    public static final DeferredItem<Item> RITUAL_PATTERN = pattern("ritual_pattern", RitualPattern::new);

    public static final DeferredItem<Item> IMPORT_CARD =
            ITEMS.registerItem("import_card", Upgrades::createUpgradeCardItem, new Item.Properties());

    public static final DeferredItem<Item> MATRIX_CIRCUITRY = ITEMS.registerSimpleItem("matrix_circuitry");
}
