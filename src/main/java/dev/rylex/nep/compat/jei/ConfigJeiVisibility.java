package dev.rylex.nep.compat.jei;

import dev.rylex.nep.NepConfigGates;
import java.util.List;
import java.util.Set;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Hidden through JEI rather than a recipe condition because the server config is not loaded when datapacks are read. */
final class ConfigJeiVisibility {
    private ConfigJeiVisibility() {}

    @Nullable
    private static IJeiRuntime runtime;

    private static Set<Item> hidden = Set.of();
    private static boolean listening;

    static void attach(IJeiRuntime jei) {
        runtime = jei;
        hidden = Set.of();
        if (!listening) {
            listening = true;
            NepConfigGates.onChange(() -> Minecraft.getInstance().execute(ConfigJeiVisibility::refresh));
        }
        refresh();
    }

    static void detach() {
        runtime = null;
    }

    private static void refresh() {
        if (runtime == null) {
            return;
        }
        Set<Item> disabled = Set.copyOf(NepConfigGates.disabledItems());
        List<ItemStack> shown = hidden.stream()
                .filter(item -> !disabled.contains(item))
                .map(ItemStack::new)
                .toList();
        List<ItemStack> removed = disabled.stream()
                .filter(item -> !hidden.contains(item))
                .map(ItemStack::new)
                .toList();
        IIngredientManager ingredients = runtime.getIngredientManager();
        if (!shown.isEmpty()) {
            ingredients.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, shown);
        }
        if (!removed.isEmpty()) {
            ingredients.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, removed);
        }
        hidden = disabled;
    }
}
