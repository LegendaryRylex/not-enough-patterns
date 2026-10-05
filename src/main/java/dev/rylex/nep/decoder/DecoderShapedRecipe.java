package dev.rylex.nep.decoder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.guide.NepRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
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
                    Codec.STRING.optionalFieldOf("group", "").forGetter(DecoderShapedRecipe::getGroup),
                    CraftingBookCategory.CODEC
                            .fieldOf("category")
                            .orElse(CraftingBookCategory.MISC)
                            .forGetter(DecoderShapedRecipe::category),
                    ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.shape),
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.output),
                    Codec.BOOL
                            .optionalFieldOf("show_notification", true)
                            .forGetter(DecoderShapedRecipe::showNotification))
            .apply(instance, DecoderShapedRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DecoderShapedRecipe> STREAM_CODEC =
            StreamCodec.of(DecoderShapedRecipe::toNetwork, DecoderShapedRecipe::fromNetwork);

    private final ShapedRecipePattern shape;
    private final ItemStack output;

    public DecoderShapedRecipe(
            String group,
            CraftingBookCategory category,
            ShapedRecipePattern shape,
            ItemStack output,
            boolean showNotification) {
        super(group, category, shape, output, showNotification);
        this.shape = shape;
        this.output = output;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return craftable(output) && super.matches(input, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return NepRecipes.DECODER_SHAPED.get();
    }

    public static boolean craftable(ItemStack result) {
        if (result.getItem() instanceof EncodingModuleItem module) {
            return PatternDecoding.required(module.module());
        }
        return NepConfig.requireDecoder();
    }

    private static DecoderShapedRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
        ShapedRecipePattern shape = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
        ItemStack output = ItemStack.STREAM_CODEC.decode(buffer);
        boolean showNotification = buffer.readBoolean();
        return new DecoderShapedRecipe(group, category, shape, output, showNotification);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, DecoderShapedRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        buffer.writeEnum(recipe.category());
        ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.shape);
        ItemStack.STREAM_CODEC.encode(buffer, recipe.output);
        buffer.writeBoolean(recipe.showNotification());
    }
}
