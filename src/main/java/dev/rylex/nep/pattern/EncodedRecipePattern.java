package dev.rylex.nep.pattern;

import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record EncodedRecipePattern(
        ResourceLocation recipe, List<GenericStack> inputs, List<GenericStack> retained, GenericStack result) {

    public EncodedRecipePattern {
        inputs = List.copyOf(inputs);
        retained = List.copyOf(retained);
    }

    public EncodedRecipePattern(ResourceLocation recipe, List<GenericStack> inputs, GenericStack result) {
        this(recipe, inputs, List.of(), result);
    }

    public static final Codec<EncodedRecipePattern> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                    ResourceLocation.CODEC.fieldOf("recipe").forGetter(EncodedRecipePattern::recipe),
                    GenericStack.FAULT_TOLERANT_LIST_CODEC.fieldOf("inputs").forGetter(EncodedRecipePattern::inputs),
                    GenericStack.FAULT_TOLERANT_LIST_CODEC
                            .optionalFieldOf("retained", List.of())
                            .forGetter(EncodedRecipePattern::retained),
                    GenericStackCodecs.FAULT_TOLERANT.fieldOf("result").forGetter(EncodedRecipePattern::result))
            .apply(builder, EncodedRecipePattern::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EncodedRecipePattern> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            EncodedRecipePattern::recipe,
            GenericStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            EncodedRecipePattern::inputs,
            GenericStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            EncodedRecipePattern::retained,
            GenericStack.STREAM_CODEC,
            EncodedRecipePattern::result,
            EncodedRecipePattern::new);

    public boolean containsMissingContent() {
        if (AEItems.MISSING_CONTENT.is(result.what())) {
            return true;
        }
        for (GenericStack input : inputs) {
            if (AEItems.MISSING_CONTENT.is(input.what())) {
                return true;
            }
        }
        for (GenericStack kept : retained) {
            if (AEItems.MISSING_CONTENT.is(kept.what())) {
                return true;
            }
        }
        return false;
    }
}
