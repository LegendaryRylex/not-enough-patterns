package dev.rylex.nep.guide;

import com.mojang.serialization.MapCodec;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.encoding.ProcessingPatternConversionRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NepRecipes {
    private NepRecipes() {}

    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Nep.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Nep.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ModuleStatusRecipe>> MODULE_STATUS =
            TYPES.register("module_status", RecipeType::simple);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ModuleStatusRecipe>>
            MODULE_STATUS_SERIALIZER = SERIALIZERS.register("module_status", () -> new ModuleStatusSerializer());

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ProcessingPatternConversionRecipe>>
            PROCESSING_PATTERN_CONVERSION = SERIALIZERS.register(
                    "processing_pattern_conversion",
                    () -> new SimpleCraftingRecipeSerializer<>(ProcessingPatternConversionRecipe::new));

    public static void init(IEventBus modBus) {
        TYPES.register(modBus);
        SERIALIZERS.register(modBus);
    }

    private static final class ModuleStatusSerializer implements RecipeSerializer<ModuleStatusRecipe> {
        @Override
        public MapCodec<ModuleStatusRecipe> codec() {
            return ModuleStatusRecipe.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ModuleStatusRecipe> streamCodec() {
            return ModuleStatusRecipe.STREAM_CODEC;
        }
    }
}
