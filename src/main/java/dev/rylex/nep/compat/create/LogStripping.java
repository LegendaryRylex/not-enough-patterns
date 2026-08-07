package dev.rylex.nep.compat.create;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerRecipeSearchEvent;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.util.RecipeCache;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.Nullable;

final class LogStripping {
    private LogStripping() {}

    private static final int SEARCH_PRIORITY = 25;

    private record Table(
            Map<Item, RecipeHolder<DeployerApplicationRecipe>> byLog,
            Map<ResourceLocation, RecipeHolder<DeployerApplicationRecipe>> byId) {}

    private static final RecipeCache<Table> TABLE = RecipeCache.of(LogStripping::build);

    static void clearCache() {
        TABLE.clear();
    }

    static List<RecipeHolder<? extends ItemApplicationRecipe>> recipes(Level level) {
        return List.copyOf(deployerRecipes(level));
    }

    static List<RecipeHolder<DeployerApplicationRecipe>> deployerRecipes(Level level) {
        return NepConfig.createDeployingLogStripping()
                ? List.copyOf(table(level).byLog().values())
                : List.of();
    }

    @Nullable
    static RecipeHolder<DeployerApplicationRecipe> byId(ResourceLocation id, Level level) {
        return NepConfig.createDeployingLogStripping() ? table(level).byId().get(id) : null;
    }

    @Nullable
    static RecipeHolder<DeployerApplicationRecipe> substitute(
            RecipeHolder<? extends ItemApplicationRecipe> displayed, Level level) {
        if (!NepConfig.createDeployingLogStripping()
                || level.getRecipeManager().byKey(displayed.id()).isPresent()) {
            return null;
        }
        for (ItemStack candidate : displayed.value().getProcessedItem().getItems()) {
            RecipeHolder<DeployerApplicationRecipe> stripping =
                    table(level).byLog().get(candidate.getItem());
            if (stripping != null) {
                return stripping;
            }
        }
        return null;
    }

    static void onDeployerRecipeSearch(DeployerRecipeSearchEvent event) {
        Level level = event.getBlockEntity().getLevel();
        if (level != null && NepConfig.createDeployingLogStripping()) {
            event.addRecipe(() -> find(event.getInventory(), level), SEARCH_PRIORITY);
        }
    }

    static Optional<RecipeHolder<DeployerApplicationRecipe>> find(RecipeWrapper inventory, Level level) {
        RecipeHolder<DeployerApplicationRecipe> holder =
                table(level).byLog().get(inventory.getItem(0).getItem());
        return holder != null && holder.value().getRequiredHeldItem().test(inventory.getItem(1))
                ? Optional.of(holder)
                : Optional.empty();
    }

    private static Table table(Level level) {
        return TABLE.get(level);
    }

    private static Table build(Level level) {
        Map<Item, RecipeHolder<DeployerApplicationRecipe>> byLog = new LinkedHashMap<>();
        Map<ResourceLocation, RecipeHolder<DeployerApplicationRecipe>> byId = new LinkedHashMap<>();
        Ingredient axes = Ingredient.of(ItemTags.AXES);
        List<RecipeHolder<? extends ItemApplicationRecipe>> existing = applicationRecipes(level);

        for (Holder<Item> log : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.LOGS)) {
            Item item = log.value();
            Item stripped = strippedForm(item);
            if (stripped == null || strippedByExistingRecipe(item, existing)) {
                continue;
            }
            ResourceLocation id = idFor(item);
            ItemApplicationRecipe.Builder<DeployerApplicationRecipe> builder =
                    new ItemApplicationRecipe.Builder<>(DeployerApplicationRecipe::new, id);
            DeployerApplicationRecipe recipe = builder.require(item)
                    .require(axes)
                    .output(stripped)
                    .toolNotConsumed()
                    .build();
            RecipeHolder<DeployerApplicationRecipe> holder = new RecipeHolder<>(id, recipe);
            byLog.put(item, holder);
            byId.put(id, holder);
        }
        return new Table(Map.copyOf(byLog), Map.copyOf(byId));
    }

    private static List<RecipeHolder<? extends ItemApplicationRecipe>> applicationRecipes(Level level) {
        List<RecipeHolder<? extends ItemApplicationRecipe>> all = new ArrayList<>();
        all.addAll(level.getRecipeManager()
                .getAllRecipesFor(AllRecipeTypes.DEPLOYING.<RecipeWrapper, DeployerApplicationRecipe>getType()));
        all.addAll(level.getRecipeManager()
                .getAllRecipesFor(AllRecipeTypes.ITEM_APPLICATION.<RecipeWrapper, ManualApplicationRecipe>getType()));
        return all;
    }

    private static boolean strippedByExistingRecipe(
            Item log, List<RecipeHolder<? extends ItemApplicationRecipe>> existing) {
        ItemStack staged = new ItemStack(log);
        for (RecipeHolder<? extends ItemApplicationRecipe> holder : existing) {
            ItemApplicationRecipe recipe = holder.value();
            if (recipe.getProcessedItem().test(staged) && acceptsAnAxe(recipe.getRequiredHeldItem())) {
                return true;
            }
        }
        return false;
    }

    private static boolean acceptsAnAxe(Ingredient tool) {
        for (Holder<Item> axe : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.AXES)) {
            if (tool.test(new ItemStack(axe.value()))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static Item strippedForm(Item log) {
        if (!(log instanceof BlockItem blockItem)) {
            return null;
        }
        BlockState stripped = AxeItem.getAxeStrippingState(blockItem.getBlock().defaultBlockState());
        if (stripped == null) {
            return null;
        }
        Item result = stripped.getBlock().asItem();
        return result == Items.AIR || result == log ? null : result;
    }

    private static ResourceLocation idFor(Item log) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(log);
        String path = key.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)
                ? key.getPath()
                : key.getNamespace() + "/" + key.getPath();
        return Nep.id("create/log_stripping/" + path);
    }
}
