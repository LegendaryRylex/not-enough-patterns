package dev.rylex.nep;

import dev.rylex.nep.pattern.EncodedMechanicalPattern;
import dev.rylex.nep.pattern.EncodedRecipePattern;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
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
            ENCODED_ENCHANTING_PATTERN = COMPONENTS.registerComponentType(
                    "encoded_enchanting_pattern", builder -> builder.persistent(EncodedRecipePattern.CODEC)
                            .networkSynchronized(EncodedRecipePattern.STREAM_CODEC));
}
