package dev.rylex.nep;

import dev.rylex.nep.pattern.EncodedRecipePattern;
import dev.rylex.nep.pattern.encoding.PatternGrid;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NepComponents {
    private NepComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Nep.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_INFUSION_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_infusion_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_AWAKENING_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_awakening_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_ENCHANTING_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_enchanting_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> SOURCE_RECIPE =
            COMPONENTS.registerComponentType("source_recipe", builder -> builder.persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PatternGrid>> PATTERN_GRID =
            COMPONENTS.registerComponentType("pattern_grid", builder -> builder.persistent(PatternGrid.CODEC)
                    .networkSynchronized(PatternGrid.STREAM_CODEC));
}
