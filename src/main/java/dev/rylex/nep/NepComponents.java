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

    private static DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>> recipePattern(
            String name) {
        return COMPONENTS.registerComponentType(name, builder -> builder.persistent(EncodedRecipePattern.CODEC)
                .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_INFUSION_PATTERN = recipePattern("encoded_infusion_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_AWAKENING_PATTERN = recipePattern("encoded_awakening_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_ENCHANTING_PATTERN = recipePattern("encoded_enchanting_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_PARTICLE_COLLISION_PATTERN = recipePattern("encoded_particle_collision_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_RITUAL_PATTERN = recipePattern("encoded_ritual_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> SOURCE_RECIPE =
            COMPONENTS.registerComponentType("source_recipe", builder -> builder.persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PatternGrid>> PATTERN_GRID =
            COMPONENTS.registerComponentType("pattern_grid", builder -> builder.persistent(PatternGrid.CODEC)
                    .networkSynchronized(PatternGrid.STREAM_CODEC));
}
