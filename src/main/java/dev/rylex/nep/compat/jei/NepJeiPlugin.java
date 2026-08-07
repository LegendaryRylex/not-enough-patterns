package dev.rylex.nep.compat.jei;

import appeng.menu.me.items.PatternEncodingTermMenu;
import dev.rylex.nep.Nep;
import dev.rylex.nep.compat.actuallyadditions.ActuallyAdditionsJeiCompat;
import dev.rylex.nep.compat.ae2wtlib.Ae2WtLibJeiCompat;
import dev.rylex.nep.compat.apothic.ApothicJeiCompat;
import dev.rylex.nep.compat.compactcrafting.CompactCraftingJeiCompat;
import dev.rylex.nep.compat.create.CreateJeiCompat;
import dev.rylex.nep.compat.draconic.DraconicJeiCompat;
import dev.rylex.nep.compat.mysticalagriculture.MysticalJeiCompat;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
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
                            RecipeType<RecipeHolder<R>> recipeType,
                            PatternTransferHandler.Extractor<RecipeHolder<R>> extractor) {
                        addUnwrapped(recipeType, extractor, (holder, level) -> holder.id());
                    }

                    @Override
                    public <T> void addUnwrapped(
                            RecipeType<T> recipeType,
                            PatternTransferHandler.Extractor<T> extractor,
                            PatternTransferHandler.Identifier<T> identifier) {
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
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (JeiTransferSource source : sources()) {
            source.addRecipes(registration, level);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (JeiTransferSource source : sources()) {
            source.addCatalysts(registration);
        }
    }

    @Override
    public void registerIngredientAliases(IIngredientAliasRegistration registration) {
        for (JeiTransferSource source : sources()) {
            source.addAliases(registration);
        }
    }

    private static List<JeiTransferSource> sources() {
        List<JeiTransferSource> sources = new ArrayList<>();
        if (ModList.get().isLoaded("create")) {
            sources.add(CreateJeiCompat.source());
        }
        if (ModList.get().isLoaded("draconicevolution")) {
            sources.add(DraconicJeiCompat.source());
        }
        if (ModList.get().isLoaded("apothic_enchanting")) {
            sources.add(ApothicJeiCompat.source());
        }
        if (ModList.get().isLoaded("actuallyadditions")) {
            sources.add(ActuallyAdditionsJeiCompat.source());
        }
        if (ModList.get().isLoaded("compactcrafting")) {
            sources.add(CompactCraftingJeiCompat.source());
        }
        if (ModList.get().isLoaded("mysticalagriculture")) {
            sources.add(MysticalJeiCompat.source());
        }
        return List.copyOf(sources);
    }
}
