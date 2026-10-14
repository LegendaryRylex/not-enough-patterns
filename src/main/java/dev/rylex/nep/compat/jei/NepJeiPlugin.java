package dev.rylex.nep.compat.jei;

import appeng.menu.me.items.PatternEncodingTermMenu;
import dev.rylex.nep.Nep;
import dev.rylex.nep.client.MachineHubScreen;
import dev.rylex.nep.compat.actuallyadditions.ActuallyAdditionsJeiCompat;
import dev.rylex.nep.compat.ae2lt.Ae2LtJeiCompat;
import dev.rylex.nep.compat.ae2wtlib.Ae2WtLibJeiCompat;
import dev.rylex.nep.compat.apothic.ApothicJeiCompat;
import dev.rylex.nep.compat.ars.ArsJeiCompat;
import dev.rylex.nep.compat.compactcrafting.CompactCraftingJeiCompat;
import dev.rylex.nep.compat.create.CreateJeiCompat;
import dev.rylex.nep.compat.draconic.DraconicJeiCompat;
import dev.rylex.nep.compat.malum.MalumJeiCompat;
import dev.rylex.nep.compat.mysticalagriculture.MysticalJeiCompat;
import dev.rylex.nep.compat.occultism.OccultismJeiCompat;
import dev.rylex.nep.compat.oritech.OritechJeiCompat;
import dev.rylex.nep.compat.viewer.EncodableCategories;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

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
                    public <R extends Recipe<?>> void addViewing(
                            RecipeType<RecipeHolder<R>> recipeType,
                            PatternTransferHandler.ViewExtractor<RecipeHolder<R>> extractor) {
                        register(recipeType, extractor, (holder, level) -> holder.id());
                    }

                    @Override
                    public <T> void addUnwrapped(
                            RecipeType<T> recipeType,
                            PatternTransferHandler.Extractor<T> extractor,
                            PatternTransferHandler.Identifier<T> identifier) {
                        register(recipeType, (recipe, slots, level) -> extractor.extract(recipe, level), identifier);
                    }

                    private <T> void register(
                            RecipeType<T> recipeType,
                            PatternTransferHandler.ViewExtractor<T> extractor,
                            PatternTransferHandler.Identifier<T> identifier) {
                        registration.addRecipeTransferHandler(
                                new PatternTransferHandler<>(
                                        menuClass, menuType, recipeType, helper, extractor, identifier),
                                recipeType);
                        EncodableCategories.add(recipeType.getUid());
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
        if (ModList.get().isLoaded("ae2lt")) {
            Ae2LtJeiCompat.bindPatternMenus(binder);
        }
        for (Integration integration : INTEGRATIONS) {
            if (integration.manualTransfers() != null && ModList.get().isLoaded(integration.modId())) {
                integration.manualTransfers().add(registration, helper);
            }
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(MachineHubScreen.class, new HubGhostIngredientHandler());
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
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        ConfigJeiVisibility.attach(runtime);
        if (ModList.get().isLoaded("malum")) {
            MalumJeiCompat.onRuntimeAvailable(runtime);
        }
    }

    @Override
    public void onRuntimeUnavailable() {
        ConfigJeiVisibility.detach();
    }

    @Override
    public void registerIngredientAliases(IIngredientAliasRegistration registration) {
        for (JeiTransferSource source : sources()) {
            source.addAliases(registration);
        }
    }

    private static List<JeiTransferSource> sources() {
        List<JeiTransferSource> sources = new ArrayList<>();
        for (Integration integration : INTEGRATIONS) {
            if (ModList.get().isLoaded(integration.modId())) {
                sources.add(integration.source().get());
            }
        }
        return List.copyOf(sources);
    }

    @FunctionalInterface
    private interface ManualTransfers {
        void add(IRecipeTransferRegistration registration, IRecipeTransferHandlerHelper helper);
    }

    private record Integration(
            String modId,
            Supplier<JeiTransferSource> source,
            @Nullable ManualTransfers manualTransfers) {}

    private static final List<Integration> INTEGRATIONS = List.of(
            new Integration("create", CreateJeiCompat::source, CreateJeiCompat::addManualTransfers),
            new Integration("draconicevolution", DraconicJeiCompat::source, DraconicJeiCompat::addManualTransfers),
            new Integration("apothic_enchanting", ApothicJeiCompat::source, null),
            new Integration(
                    "actuallyadditions",
                    ActuallyAdditionsJeiCompat::source,
                    ActuallyAdditionsJeiCompat::addManualTransfers),
            new Integration(
                    "compactcrafting", CompactCraftingJeiCompat::source, CompactCraftingJeiCompat::addManualTransfers),
            new Integration("mysticalagriculture", MysticalJeiCompat::source, MysticalJeiCompat::addManualTransfers),
            new Integration("malum", MalumJeiCompat::source, MalumJeiCompat::addManualTransfers),
            new Integration("ars_nouveau", ArsJeiCompat::source, ArsJeiCompat::addManualTransfers),
            new Integration("oritech", OritechJeiCompat::source, OritechJeiCompat::addManualTransfers),
            new Integration("occultism", OccultismJeiCompat::source, OccultismJeiCompat::addManualTransfers));
}
