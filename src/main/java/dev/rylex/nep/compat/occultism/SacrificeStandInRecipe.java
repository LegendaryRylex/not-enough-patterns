package dev.rylex.nep.compat.occultism;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record SacrificeStandInRecipe(TagKey<EntityType<?>> sacrifice, Ingredient standIn)
        implements Recipe<RecipeInput> {

    public static final MapCodec<SacrificeStandInRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    TagKey.codec(Registries.ENTITY_TYPE)
                            .fieldOf("sacrifice")
                            .forGetter(SacrificeStandInRecipe::sacrifice),
                    Ingredient.CODEC.fieldOf("stand_in").forGetter(SacrificeStandInRecipe::standIn))
            .apply(instance, SacrificeStandInRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SacrificeStandInRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Identifier.STREAM_CODEC.map(id -> TagKey.create(Registries.ENTITY_TYPE, id), TagKey::location),
                    SacrificeStandInRecipe::sacrifice,
                    Ingredient.CONTENTS_STREAM_CODEC,
                    SacrificeStandInRecipe::standIn,
                    SacrificeStandInRecipe::new);

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input) {
        return ItemStack.EMPTY;
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
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
        return NepOccultismContent.STAND_IN_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<RecipeInput>> getType() {
        return NepOccultismContent.STAND_IN.get();
    }
}
