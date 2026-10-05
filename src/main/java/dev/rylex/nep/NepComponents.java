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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedMechanicalPattern>>
            ENCODED_MECHANICAL_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_mechanical_pattern", builder -> builder.persistent(EncodedMechanicalPattern.CODEC)
                            .networkSynchronized(EncodedMechanicalPattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_ANDESITE_CRAFTING_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_andesite_crafting_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_SEQUENCED_ASSEMBLY_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_sequenced_assembly_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_FUSION_CRAFTING_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_fusion_crafting_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_EMPOWERING_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_empowering_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_ATOMIC_RECONSTRUCTION_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_atomic_reconstruction_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_MINIATURIZATION_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_miniaturization_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_INFUSION_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_infusion_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_AWAKENING_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_awakening_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_SPIRIT_INFUSION_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_spirit_infusion_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_SPIRIT_FOCUSING_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_spirit_focusing_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_RUNEWORKING_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_runeworking_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_APPARATUS_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_apparatus_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_IMBUEMENT_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_imbuement_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedRecipePattern>>
            ENCODED_ENCHANTING_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_enchanting_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> SOURCE_RECIPE =
            COMPONENTS.registerComponentType("source_recipe", builder -> builder.persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PatternGrid>> PATTERN_GRID =
            COMPONENTS.registerComponentType("pattern_grid", builder -> builder.persistent(PatternGrid.CODEC)
                    .networkSynchronized(PatternGrid.STREAM_CODEC));
}
