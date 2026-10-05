package dev.rylex.nep.compat.jei;

import dev.rylex.nep.machine.ManualPull;
import dev.rylex.nep.machine.ManualRequirement;
import dev.rylex.nep.menu.ManualCraftMenu;
import dev.rylex.nep.net.ManualCraftPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class ManualTransferHandler<M extends AbstractContainerMenu & ManualCraftMenu, T>
        implements IRecipeTransferHandler<M, T> {

    @FunctionalInterface
    public interface Requirements<T> {
        @Nullable
        List<ManualRequirement> of(T recipe, Level level);
    }

    private final Class<M> menuClass;
    private final MenuType<M> menuType;
    private final RecipeType<T> recipeType;
    private final IRecipeTransferHandlerHelper helper;
    private final Requirements<T> requirements;
    private final PatternTransferHandler.Identifier<T> identifier;

    public ManualTransferHandler(
            Class<M> menuClass,
            MenuType<M> menuType,
            RecipeType<T> recipeType,
            IRecipeTransferHandlerHelper helper,
            Requirements<T> requirements,
            PatternTransferHandler.Identifier<T> identifier) {
        this.menuClass = menuClass;
        this.menuType = menuType;
        this.recipeType = recipeType;
        this.helper = helper;
        this.requirements = requirements;
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

        Level level = player.level();
        List<ManualRequirement> needed = requirements.of(recipe, level);
        ResourceLocation id = identifier.id(recipe, level);
        if (needed == null || needed.isEmpty() || id == null) {
            return helper.createUserErrorWithTooltip(Component.translatable("jei.nep.manual.unsupported"));
        }

        List<ItemStack> inventory = ManualPull.snapshot(player.getInventory().items);
        int preferredSlot = player.getInventory().selected;
        int wanted = maxTransfer ? ManualPull.MAXIMUM_BATCHES : 1;
        int batches = ManualPull.affordableBatches(inventory, needed, wanted, preferredSlot);
        if (batches <= 0) {
            Component tooltip = Component.translatable("jei.nep.manual.missing_items");
            List<IRecipeSlotView> missing = missingSlots(
                    recipeSlots, needed, ManualPull.unmetRequirements(inventory, needed, 1, preferredSlot));
            return missing == null
                    ? helper.createUserErrorWithTooltip(tooltip)
                    : helper.createUserErrorForMissingSlots(tooltip, missing);
        }
        if (!doTransfer) {
            return carriedDataWarning(inventory, needed, preferredSlot);
        }
        PacketDistributor.sendToServer(new ManualCraftPayload(menu.machinePos(), id, batches));
        return null;
    }

    @Nullable
    private static IRecipeTransferError carriedDataWarning(
            List<ItemStack> inventory, List<ManualRequirement> needed, int preferredSlot) {
        List<ManualPull.Take> takes = ManualPull.plan(inventory, needed, 1, preferredSlot);
        if (takes == null) {
            return null;
        }
        for (ManualPull.Take take : takes) {
            if (needed.get(take.requirement()).carriesData()) {
                return new CarriedDataWarning(inventory.get(take.slot()).copy());
            }
        }
        return null;
    }

    @Nullable
    private static List<IRecipeSlotView> missingSlots(
            IRecipeSlotsView recipeSlots, List<ManualRequirement> needed, List<Integer> unmet) {
        if (unmet.isEmpty()) {
            return null;
        }
        List<IRecipeSlotView> views = new ArrayList<>(recipeSlots.getSlotViews(RecipeIngredientRole.INPUT));
        views.addAll(recipeSlots.getSlotViews(RecipeIngredientRole.CATALYST));

        boolean[] claimed = new boolean[views.size()];
        int[] matched = new int[needed.size()];
        for (int index = 0; index < needed.size(); index++) {
            matched[index] = claim(views, claimed, needed.get(index));
        }

        List<IRecipeSlotView> missing = new ArrayList<>(unmet.size());
        for (int index : unmet) {
            if (matched[index] >= 0) {
                missing.add(views.get(matched[index]));
            }
        }
        return missing.isEmpty() ? null : missing;
    }

    private static int claim(List<IRecipeSlotView> views, boolean[] claimed, ManualRequirement requirement) {
        for (int slot = 0; slot < views.size(); slot++) {
            if (!claimed[slot] && shows(views.get(slot), requirement)) {
                claimed[slot] = true;
                return slot;
            }
        }
        return -1;
    }

    private static boolean shows(IRecipeSlotView slot, ManualRequirement requirement) {
        return slot.getItemStacks().anyMatch(stack -> requirement.ingredient().test(stack));
    }
}
