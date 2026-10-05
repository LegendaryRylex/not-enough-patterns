package dev.rylex.nep.decoder;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.guide.NepRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

/**
 * Checks the server config when matched rather than through a recipe condition, because the server config is not
 * loaded yet when datapacks are read.
 */
public final class DecoderShapedRecipe extends ShapedRecipe {

    public static final MapCodec<DecoderShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
                    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(recipe -> recipe.bookInfo),
                    ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.output))
            .apply(instance, DecoderShapedRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DecoderShapedRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC,
            recipe -> recipe.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC,
            recipe -> recipe.bookInfo,
            ShapedRecipePattern.STREAM_CODEC,
            recipe -> recipe.pattern,
            ItemStackTemplate.STREAM_CODEC,
            recipe -> recipe.output,
            DecoderShapedRecipe::new);

    private final ItemStackTemplate output;

    public DecoderShapedRecipe(
            Recipe.CommonInfo commonInfo,
            CraftingRecipe.CraftingBookInfo bookInfo,
            ShapedRecipePattern pattern,
            ItemStackTemplate output) {
        super(commonInfo, bookInfo, pattern, output);
        this.output = output;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return craftable(output) && super.matches(input, level);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) NepRecipes.DECODER_SHAPED.get();
    }

    public static boolean craftable(ItemStackTemplate result) {
        if (result.item().value() instanceof EncodingModuleItem module) {
            return PatternDecoding.required(module.module());
        }
        return NepConfig.requireDecoder();
    }
}
