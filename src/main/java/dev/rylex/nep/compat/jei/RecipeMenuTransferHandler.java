package dev.rylex.nep.compat.jei;

import java.util.Optional;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public abstract class RecipeMenuTransferHandler<M extends AbstractContainerMenu, T>
        implements IRecipeTransferHandler<M, T> {

    private final Class<M> menuClass;
    private final MenuType<M> menuType;
    private final IRecipeType<T> recipeType;
    protected final IRecipeTransferHandlerHelper helper;

    protected RecipeMenuTransferHandler(
            Class<M> menuClass, MenuType<M> menuType, IRecipeType<T> recipeType, IRecipeTransferHandlerHelper helper) {
        this.menuClass = menuClass;
        this.menuType = menuType;
        this.recipeType = recipeType;
        this.helper = helper;
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
    public IRecipeType<T> getRecipeType() {
        return recipeType;
    }
}
