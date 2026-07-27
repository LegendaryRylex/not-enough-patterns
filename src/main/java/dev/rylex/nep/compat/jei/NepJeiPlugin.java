package dev.rylex.nep.compat.jei;

import appeng.menu.me.items.PatternEncodingTermMenu;
import dev.rylex.nep.Nep;
import dev.rylex.nep.compat.ae2wtlib.Ae2WtLibJeiCompat;
import dev.rylex.nep.compat.create.CreateJeiCompat;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;

@JeiPlugin
public final class NepJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return Nep.id("jei");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        List<JeiTransferSource> sources = sources();
        if (sources.isEmpty()) {
            return;
        }
        IRecipeTransferHandlerHelper helper = registration.getTransferHelper();
        PatternMenuBinder binder = new PatternMenuBinder() {
            @Override
            public <T extends PatternEncodingTermMenu> void bind(Class<T> menuClass, MenuType<T> menuType) {
                JeiTransferSource.TransferCollector collector = new JeiTransferSource.TransferCollector() {
                    @Override
                    public <R extends Recipe<?>> void add(
                            RecipeType<RecipeHolder<R>> recipeType, PatternTransferHandler.Extractor<R> extractor) {
                        registration.addRecipeTransferHandler(
                                new PatternTransferHandler<>(menuClass, menuType, recipeType, helper, extractor),
                                recipeType);
                    }
                };
                for (JeiTransferSource source : sources) {
                    source.addTransfers(collector);
                }
            }
        };
        binder.bind(PatternEncodingTermMenu.class, PatternEncodingTermMenu.TYPE);
        if (ModList.get().isLoaded("ae2wtlib")) {
            Ae2WtLibJeiCompat.bindPatternMenus(binder);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (JeiTransferSource source : sources()) {
            source.addCatalysts(registration);
        }
    }

    private static List<JeiTransferSource> sources() {
        return ModList.get().isLoaded("create") ? List.of(CreateJeiCompat.source()) : List.of();
    }
}
