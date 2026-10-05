package dev.rylex.nep.compat.jei;

import appeng.menu.me.items.PatternEncodingTermMenu;
import dev.rylex.nep.Nep;
import dev.rylex.nep.client.MachineHubScreen;
import dev.rylex.nep.compat.ae2wtlib.Ae2WtLibJeiCompat;
import dev.rylex.nep.compat.apothic.ApothicJeiCompat;
import dev.rylex.nep.compat.mysticalagriculture.MysticalJeiCompat;
import dev.rylex.nep.util.Recipes;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;

@JeiPlugin
public final class NepJeiPlugin implements IModPlugin {

    @Override
    public Identifier getPluginUid() {
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
                            IRecipeType<RecipeHolder<R>> recipeType,
                            PatternTransferHandler.Extractor<RecipeHolder<R>> extractor) {
                        addUnwrapped(recipeType, extractor, (holder, level) -> Recipes.idOf(holder));
                    }

                    @Override
                    public <T> void addUnwrapped(
                            IRecipeType<T> recipeType,
                            PatternTransferHandler.Extractor<T> extractor,
                            PatternTransferHandler.RecipeId<T> identifier) {
                        registration.addRecipeTransferHandler(
                                new PatternTransferHandler<>(
                                        menuClass, menuType, recipeType, helper, extractor, identifier),
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
        if (ModList.get().isLoaded("mysticalagriculture")) {
            MysticalJeiCompat.addManualTransfers(registration, helper);
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(MachineHubScreen.class, new HubGhostIngredientHandler());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        DecoderJeiVisibility.apply(runtime);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (JeiTransferSource source : sources()) {
            source.addCatalysts(registration);
        }
    }

    private static List<JeiTransferSource> sources() {
        List<JeiTransferSource> sources = new ArrayList<>();
        if (ModList.get().isLoaded("apothic_enchanting")) {
            sources.add(ApothicJeiCompat.source());
        }
        if (ModList.get().isLoaded("mysticalagriculture")) {
            sources.add(MysticalJeiCompat.source());
        }
        return List.copyOf(sources);
    }
}
