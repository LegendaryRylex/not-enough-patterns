package dev.rylex.nep.compat.malum;

import com.sammy.malum.common.recipe.SpiritFocusingRecipe;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import com.sammy.malum.config.CommonConfig;
import com.sammy.malum.core.handlers.hiding.HiddenTagHandler;
import com.sammy.malum.core.systems.recipe.SpiritIngredient;
import com.sammy.malum.registry.common.MalumTags;
import dev.rylex.nep.Nep;
import dev.rylex.nep.util.RecipeCache;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

final class VoidSecretsTooltip {
    private VoidSecretsTooltip() {}

    private static final List<TagKey<Item>> A_REVELATION_REVEALS =
            List.of(MalumTags.ItemTags.HIDDEN_UNTIL_VOID, MalumTags.ItemTags.HIDDEN_UNTIL_BLACK_CRYSTAL);

    private static final RecipeCache<Map<Item, List<List<ItemStack>>>> GATES = RecipeCache.of(VoidSecretsTooltip::scan);

    static void warm() {
        Level level = Minecraft.getInstance().level;
        if (level != null && gatingIsInEffect()) {
            GATES.get(level);
        }
    }

    static void onTooltip(ItemTooltipEvent event) {
        Level level = Minecraft.getInstance().level;
        if (level == null || !gatingIsInEffect()) {
            return;
        }
        List<List<ItemStack>> recipes =
                GATES.get(level).get(event.getItemStack().getItem());
        if (recipes == null || !recipes.stream().allMatch(VoidSecretsTooltip::outOfSight)) {
            return;
        }
        List<Component> lines = event.getToolTip();
        lines.add(
                Math.min(1, lines.size()),
                Component.translatable("tooltip.nep.void_secrets")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    private static boolean gatingIsInEffect() {
        return CommonConfig.HIDE_RECIPES.getConfigValue();
    }

    private static boolean outOfSight(List<ItemStack> gate) {
        return gate.stream().anyMatch(HiddenTagHandler::isHiddenItem);
    }

    private static Map<Item, List<List<ItemStack>>> scan(Level level) {
        HolderLookup.Provider registries = level.registryAccess();
        Map<Item, List<List<ItemStack>>> byResult = new HashMap<>();
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            ItemStack result = result(recipe, registries);
            if (!result.isEmpty()) {
                byResult.computeIfAbsent(result.getItem(), item -> new ArrayList<>())
                        .add(gate(recipe, result));
            }
        }
        Map<Item, List<List<ItemStack>>> gated = new HashMap<>();
        byResult.forEach((item, gates) -> {
            if (isOurs(item) && gates.stream().noneMatch(List::isEmpty)) {
                gated.put(item, List.copyOf(gates));
            }
        });
        return Map.copyOf(gated);
    }

    private static List<ItemStack> gate(Recipe<?> recipe, ItemStack result) {
        return Stream.concat(Stream.of(result), ingredients(recipe).filter(VoidSecretsTooltip::hidesWhatConsumesIt))
                .filter(VoidSecretsTooltip::revelationGated)
                .toList();
    }

    private static ItemStack result(Recipe<?> recipe, HolderLookup.Provider registries) {
        if (recipe instanceof SpiritInfusionRecipe infusion) {
            return infusion.result;
        }
        if (recipe instanceof SpiritFocusingRecipe focusing) {
            return focusing.output;
        }
        return recipe.getResultItem(registries);
    }

    private static Stream<ItemStack> ingredients(Recipe<?> recipe) {
        if (recipe instanceof SpiritInfusionRecipe infusion) {
            return Stream.concat(
                    Stream.concat(
                            items(infusion.input.ingredient()),
                            infusion.extraInputs.stream().flatMap(extra -> items(extra.ingredient()))),
                    spirits(infusion.spirits));
        }
        if (recipe instanceof SpiritFocusingRecipe focusing) {
            return Stream.concat(items(focusing.input), spirits(focusing.spirits));
        }
        return recipe.getIngredients().stream().flatMap(VoidSecretsTooltip::items);
    }

    private static boolean isOurs(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Nep.MOD_ID);
    }

    private static boolean revelationGated(ItemStack stack) {
        return A_REVELATION_REVEALS.stream().anyMatch(stack::is);
    }

    private static boolean hidesWhatConsumesIt(ItemStack stack) {
        return !stack.is(MalumTags.ItemTags.HIDDEN_AS_RESULT_ONLY);
    }

    private static Stream<ItemStack> items(Ingredient ingredient) {
        return Arrays.stream(ingredient.getItems());
    }

    private static Stream<ItemStack> spirits(List<SpiritIngredient> spirits) {
        return spirits.stream().map(SpiritIngredient::asItemStack);
    }
}
