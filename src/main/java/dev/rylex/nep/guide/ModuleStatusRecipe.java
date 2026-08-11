package dev.rylex.nep.guide;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record ModuleStatusRecipe(String module) implements Recipe<RecipeInput> {

    public static final MapCodec<ModuleStatusRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    com.mojang.serialization.Codec.STRING.fieldOf("module").forGetter(ModuleStatusRecipe::module))
            .apply(instance, ModuleStatusRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModuleStatusRecipe> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ModuleStatusRecipe::module, ModuleStatusRecipe::new);

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
        return NepRecipes.MODULE_STATUS_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<RecipeInput>> getType() {
        return NepRecipes.MODULE_STATUS.get();
    }
}
