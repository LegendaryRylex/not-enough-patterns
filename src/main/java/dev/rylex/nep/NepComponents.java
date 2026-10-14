package dev.rylex.nep;

import dev.rylex.nep.pattern.EncodedMechanicalPattern;
import dev.rylex.nep.pattern.EncodedRecipePattern;
import dev.rylex.nep.pattern.encoding.PatternGrid;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedMechanicalPattern>>
            ENCODED_MECHANICAL_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_mechanical_pattern", builder -> builder.persistent(EncodedMechanicalPattern.CODEC)
                            .networkSynchronized(EncodedMechanicalPattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_ANDESITE_CRAFTING_PATTERN = recipePattern("encoded_andesite_crafting_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_SEQUENCED_ASSEMBLY_PATTERN = recipePattern("encoded_sequenced_assembly_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_FUSION_CRAFTING_PATTERN = recipePattern("encoded_fusion_crafting_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_EMPOWERING_PATTERN = recipePattern("encoded_empowering_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_ATOMIC_RECONSTRUCTION_PATTERN = recipePattern("encoded_atomic_reconstruction_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_MINIATURIZATION_PATTERN = recipePattern("encoded_miniaturization_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_INFUSION_PATTERN = recipePattern("encoded_infusion_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_AWAKENING_PATTERN = recipePattern("encoded_awakening_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_SPIRIT_INFUSION_PATTERN = recipePattern("encoded_spirit_infusion_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_SPIRIT_FOCUSING_PATTERN = recipePattern("encoded_spirit_focusing_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_RUNEWORKING_PATTERN = recipePattern("encoded_runeworking_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_APPARATUS_PATTERN = recipePattern("encoded_apparatus_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_IMBUEMENT_PATTERN = recipePattern("encoded_imbuement_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_ENCHANTING_PATTERN = recipePattern("encoded_enchanting_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_PARTICLE_COLLISION_PATTERN = recipePattern("encoded_particle_collision_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_RITUAL_PATTERN = recipePattern("encoded_ritual_pattern");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> SOURCE_RECIPE =
            COMPONENTS.registerComponentType("source_recipe", builder -> builder.persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PatternGrid>> PATTERN_GRID =
            COMPONENTS.registerComponentType("pattern_grid", builder -> builder.persistent(PatternGrid.CODEC)
                    .networkSynchronized(PatternGrid.STREAM_CODEC));
}
