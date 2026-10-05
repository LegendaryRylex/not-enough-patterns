package dev.rylex.nep.compat.jei;

import appeng.menu.me.items.PatternEncodingTermMenu;
import dev.rylex.nep.client.ProcessingEncoding;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.PatternRecipeHolder;
import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class PatternTransferHandler<M extends PatternEncodingTermMenu, T>
        implements IRecipeTransferHandler<M, T> {

    @FunctionalInterface
    public interface Extractor<T> {
        @Nullable
        EncodedIngredients extract(T recipe, Level level);
    }

    @FunctionalInterface
    public interface ViewExtractor<T> {
        @Nullable
        EncodedIngredients extract(T recipe, IRecipeSlotsView slots, Level level);
    }

    @FunctionalInterface
    public interface Identifier<T> {
        @Nullable
        ResourceLocation id(T recipe, Level level);
    }

    private final Class<M> menuClass;
    private final MenuType<M> menuType;
    private final RecipeType<T> recipeType;
    private final IRecipeTransferHandlerHelper helper;
    private final ViewExtractor<T> extractor;
    private final Identifier<T> identifier;

    PatternTransferHandler(
            Class<M> menuClass,
            MenuType<M> menuType,
            RecipeType<T> recipeType,
            IRecipeTransferHandlerHelper helper,
            ViewExtractor<T> extractor,
            Identifier<T> identifier) {
        this.menuClass = menuClass;
        this.menuType = menuType;
        this.recipeType = recipeType;
        this.helper = helper;
        this.extractor = extractor;
        this.identifier = identifier;
    }

    @Override
    public Class<? extends M> getContainerClass() {
        return menuClass;
    }

    @Override
    public Optional<MenuType<M>> getMenuType() {
        return Optional.of(menuType);
    }

    @Override
    public RecipeType<T> getRecipeType() {
        return recipeType;
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(
            M menu, T recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {

        EncodedIngredients encoded = extractor.extract(recipe, recipeSlots, player.level());
        if (encoded == null || encoded.inputs().isEmpty() || encoded.outputs().isEmpty()) {
            return helper.createInternalError();
        }
        if (encoded.inputs().size() > menu.getProcessingInputSlots().length) {
            return helper.createUserErrorWithTooltip(Component.translatable("jei.nep.pattern.too_many_inputs"));
        }
        if (encoded.outputs().size() > menu.getProcessingOutputSlots().length) {
            return helper.createUserErrorWithTooltip(Component.translatable("jei.nep.pattern.too_many_outputs"));
        }

        if (!doTransfer) {
            return null;
        }

        ProcessingEncoding.encode(menu, encoded);
        ResourceLocation id = identifier.id(recipe, player.level());
        if (id != null) {
            ((PatternRecipeHolder) menu).nep$setRecipeId(id);
        }
        return null;
    }
}
