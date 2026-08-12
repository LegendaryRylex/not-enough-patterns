package dev.rylex.nep.pattern.encoding;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record PatternOrigin(@Nullable ResourceLocation recipe, boolean recipeViewer) {

    public static final PatternOrigin MANUAL = new PatternOrigin(null, false);

    public static final PatternOrigin RECIPE_VIEWER = new PatternOrigin(null, true);

    public static final StreamCodec<ByteBuf, PatternOrigin> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC),
            origin -> Optional.ofNullable(origin.recipe()),
            ByteBufCodecs.BOOL,
            PatternOrigin::recipeViewer,
            (recipe, recipeViewer) -> new PatternOrigin(recipe.orElse(null), recipeViewer));

    public static PatternOrigin ofRecipe(@Nullable ResourceLocation recipe) {
        return recipe == null ? MANUAL : new PatternOrigin(recipe, false);
    }
}
