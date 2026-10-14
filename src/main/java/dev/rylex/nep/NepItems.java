package dev.rylex.nep;

import appeng.api.crafting.EncodedPatternDecoder;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.upgrades.Upgrades;
import dev.rylex.nep.pattern.AwakeningPattern;
import dev.rylex.nep.pattern.EnchantingPattern;
import dev.rylex.nep.pattern.InfusionPattern;
import dev.rylex.nep.pattern.ParticleCollisionPattern;
import dev.rylex.nep.pattern.RitualPattern;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NepItems {
    private NepItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nep.MOD_ID);

    private static <T extends IPatternDetails> DeferredItem<Item> pattern(
            String name, EncodedPatternDecoder<T> factory) {
        return ITEMS.registerItem(name, props -> PatternDetailsHelper.encodedPatternItemBuilder(factory)
                .build(props));
    }

    public static final DeferredItem<Item> INFUSION_PATTERN = pattern("infusion_pattern", InfusionPattern::new);

    public static final DeferredItem<Item> AWAKENING_PATTERN = pattern("awakening_pattern", AwakeningPattern::new);

    public static final DeferredItem<Item> ENCHANTING_PATTERN = pattern("enchanting_pattern", EnchantingPattern::new);

    public static final DeferredItem<Item> PARTICLE_COLLISION_PATTERN =
            pattern("particle_collision_pattern", ParticleCollisionPattern::new);

    public static final DeferredItem<Item> RITUAL_PATTERN = pattern("ritual_pattern", RitualPattern::new);

    public static final DeferredItem<Item> IMPORT_CARD =
            ITEMS.registerItem("import_card", Upgrades::createUpgradeCardItem);

    public static final DeferredItem<Item> MATRIX_CIRCUITRY = ITEMS.registerSimpleItem("matrix_circuitry");
}
