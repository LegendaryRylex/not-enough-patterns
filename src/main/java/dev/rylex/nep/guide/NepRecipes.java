package dev.rylex.nep.guide;

import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.encoding.ProcessingPatternConversionRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NepRecipes {
    private NepRecipes() {}

    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Nep.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Nep.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ModuleStatusRecipe>> MODULE_STATUS =
            TYPES.register("module_status", name -> RecipeType.simple(name));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ModuleStatusRecipe>>
            MODULE_STATUS_SERIALIZER = SERIALIZERS.register(
                    "module_status",
                    name -> new RecipeSerializer<>(ModuleStatusRecipe.CODEC, ModuleStatusRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ProcessingPatternConversionRecipe>>
            PROCESSING_PATTERN_CONVERSION = SERIALIZERS.register(
                    "processing_pattern_conversion",
                    name -> new RecipeSerializer<>(
                            ProcessingPatternConversionRecipe.MAP_CODEC,
                            ProcessingPatternConversionRecipe.STREAM_CODEC));

    public static void init(IEventBus modBus) {
        TYPES.register(modBus);
        SERIALIZERS.register(modBus);
        NeoForge.EVENT_BUS.addListener(NepRecipes::onDatapackSync);
    }

    /**
     * The guide reads these banners on the client, which now holds only the recipe types a mod asks for.
     */
    private static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(MODULE_STATUS.get());
    }
}
