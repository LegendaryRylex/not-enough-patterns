package dev.rylex.nep.compat.jei;

import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.PatternRecipeHolder;
import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class PatternTransferHandler<M extends PatternEncodingTermMenu, R extends Recipe<?>>
        implements IRecipeTransferHandler<M, RecipeHolder<R>> {

    @FunctionalInterface
    public interface Extractor<R extends Recipe<?>> {
        @Nullable
        EncodedIngredients extract(RecipeHolder<R> holder, Level level);
    }

    private final Class<M> menuClass;
    private final MenuType<M> menuType;
    private final RecipeType<RecipeHolder<R>> recipeType;
    private final IRecipeTransferHandlerHelper helper;
    private final Extractor<R> extractor;

    PatternTransferHandler(
            Class<M> menuClass,
            MenuType<M> menuType,
            RecipeType<RecipeHolder<R>> recipeType,
            IRecipeTransferHandlerHelper helper,
            Extractor<R> extractor) {
        this.menuClass = menuClass;
        this.menuType = menuType;
        this.recipeType = recipeType;
        this.helper = helper;
        this.extractor = extractor;
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
    public RecipeType<RecipeHolder<R>> getRecipeType() {
        return recipeType;
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(
            M menu,
            RecipeHolder<R> holder,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer) {

        EncodedIngredients encoded = extractor.extract(holder, player.level());
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

        EncodingHelper.encodeProcessingRecipe(menu, encoded.inputs(), encoded.outputs());
        ((PatternRecipeHolder) menu).nep$setRecipeId(holder.id());
        return null;
    }
}
