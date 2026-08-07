package dev.rylex.nep.compat.create;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import dev.rylex.nep.util.RecipeCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.Nullable;

final class SequencedAssemblyResolver {
    private SequencedAssemblyResolver() {}

    record Requirements(
            int deployers,
            int spouts,
            int presses,
            int saws,
            List<Ingredient> deployerTools,
            List<StationKind> stationOrder) {}

    record ItemDemand(Ingredient ingredient, int count) {}

    record FluidDemand(FluidIngredient ingredient, int amount) {}

    record Demand(List<ItemDemand> items, List<FluidDemand> fluids) {}

    private static final RecipeCache<Map<Item, Optional<SequencedAssemblyRecipe>>> CACHE =
            RecipeCache.of(level -> Collections.synchronizedMap(new HashMap<>()));
    private static final Map<SequencedAssemblyRecipe, Requirements> REQ_CACHE =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<SequencedAssemblyRecipe, Demand> DEMAND_CACHE =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private static volatile int generation;

    static int generation() {
        return generation;
    }

    static void clearCache() {
        generation++;
        CACHE.clear();
        REQ_CACHE.clear();
        DEMAND_CACHE.clear();
    }

    private static Requirements requirementsOf(SequencedAssemblyRecipe recipe) {
        return REQ_CACHE.computeIfAbsent(recipe, SequencedAssemblyResolver::requirements);
    }

    static List<RecipeHolder<SequencedAssemblyRecipe>> candidatesFor(Level level, ItemStack target) {
        List<RecipeHolder<SequencedAssemblyRecipe>> found = new ArrayList<>();
        if (target.isEmpty()) {
            return found;
        }
        for (RecipeHolder<SequencedAssemblyRecipe> holder : all(level)) {
            if (produces(holder.value(), target)) {
                found.add(holder);
            }
        }
        return found;
    }

    static boolean produces(SequencedAssemblyRecipe recipe, ItemStack target) {
        if (recipe.resultPool.isEmpty()) {
            return false;
        }
        ItemStack primary = recipe.resultPool.get(0).getStack();
        return !primary.isEmpty() && ItemStack.isSameItemSameComponents(primary, target);
    }

    @Nullable
    static SequencedAssemblyRecipe resolveById(Level level, ResourceLocation id) {
        RecipeHolder<?> holder = level.getRecipeManager().byKey(id).orElse(null);
        return holder != null && holder.value() instanceof SequencedAssemblyRecipe recipe ? recipe : null;
    }

    @Nullable
    static SequencedAssemblyRecipe resolveByResult(Level level, ItemStack target) {
        if (target.isEmpty()) {
            return null;
        }
        return CACHE.get(level)
                .computeIfAbsent(target.getItem(), item -> Optional.ofNullable(compute(level, target)))
                .orElse(null);
    }

    @Nullable
    private static SequencedAssemblyRecipe compute(Level level, ItemStack target) {
        for (RecipeHolder<SequencedAssemblyRecipe> holder : all(level)) {
            if (produces(holder.value(), target)) {
                return holder.value();
            }
        }
        return null;
    }

    record StationCounts(int deployers, int spouts, int presses, int saws) {
        static StationCounts of(Level level, List<BlockPos> machines) {
            int deployers = 0;
            int spouts = 0;
            int presses = 0;
            int saws = 0;
            for (BlockPos pos : machines) {
                BlockState state = level.getBlockState(pos);
                switch (Stations.detect(state)) {
                    case DEPLOYER -> {
                        if (Stations.deployerFacesDown(state)) {
                            deployers++;
                        }
                    }
                    case SPOUT -> spouts++;
                    case PRESS -> presses++;
                    case SAW -> saws++;
                    default -> {}
                }
            }
            return new StationCounts(deployers, spouts, presses, saws);
        }
    }

    static List<SequencedAssemblyRecipe> recipesForLine(
            Level level, @Nullable BlockPos input, @Nullable BlockPos output, List<BlockPos> machines) {
        List<SequencedAssemblyRecipe> matches = new ArrayList<>();
        if (input == null || output == null) {
            return matches;
        }
        if (level.getBlockState(output).getBlock() != AllBlocks.DEPOT.get()) {
            return matches;
        }
        StationCounts counts = StationCounts.of(level, machines);
        for (RecipeHolder<SequencedAssemblyRecipe> holder : all(level)) {
            Requirements req = requirementsOf(holder.value());
            if (req.deployers() == counts.deployers()
                    && req.spouts() == counts.spouts()
                    && req.presses() == counts.presses()
                    && req.saws() == counts.saws()) {
                matches.add(holder.value());
            }
        }
        return matches;
    }

    static List<StationKind> expectedStations(
            Level level, List<BlockPos> machines, List<SequencedAssemblyRecipe> preferred) {
        if (machines.isEmpty()) {
            return List.of();
        }
        List<StationKind> observed = new ArrayList<>(machines.size());
        for (BlockPos pos : machines) {
            observed.add(Stations.detect(level.getBlockState(pos)));
        }
        List<StationKind> hints = StationLayouts.expected(layoutsOf(preferred), observed);
        if (StationLayouts.anyKnown(hints)) {
            return hints;
        }
        return StationLayouts.expected(allLayouts(level), observed);
    }

    private static List<List<StationKind>> layoutsOf(List<SequencedAssemblyRecipe> recipes) {
        List<List<StationKind>> layouts = new ArrayList<>(recipes.size());
        for (SequencedAssemblyRecipe recipe : recipes) {
            layouts.add(requirementsOf(recipe).stationOrder());
        }
        return layouts;
    }

    private static List<List<StationKind>> allLayouts(Level level) {
        List<List<StationKind>> layouts = new ArrayList<>();
        for (RecipeHolder<SequencedAssemblyRecipe> holder : all(level)) {
            layouts.add(requirementsOf(holder.value()).stationOrder());
        }
        return layouts;
    }

    static boolean lineHealthy(
            Level level, @Nullable BlockPos input, @Nullable BlockPos output, List<BlockPos> machines) {
        if (input == null || output == null || machines.isEmpty()) {
            return false;
        }
        if (level.getBlockState(output).getBlock() != AllBlocks.DEPOT.get()) {
            return false;
        }
        for (BlockPos pos : machines) {
            BlockState state = level.getBlockState(pos);
            StationKind kind = Stations.detect(state);
            if (!kind.recognized()) {
                return false;
            }
            if (kind == StationKind.DEPLOYER && !Stations.deployerFacesDown(state)) {
                return false;
            }
            if (Stations.isUnpowered(level, pos, kind)) {
                return false;
            }
        }
        return true;
    }

    static Demand demandOf(SequencedAssemblyRecipe recipe) {
        return DEMAND_CACHE.computeIfAbsent(recipe, SequencedAssemblyResolver::demand);
    }

    private static Demand demand(SequencedAssemblyRecipe recipe) {
        int loops = Math.max(1, recipe.getLoops());
        List<Ingredient> stepIngredients = new ArrayList<>();
        List<SizedFluidIngredient> stepFluids = new ArrayList<>();
        for (SequencedRecipe<?> step : recipe.getSequence()) {
            IAssemblyRecipe assembly = step.getAsAssemblyRecipe();
            assembly.addAssemblyIngredients(stepIngredients);
            assembly.addAssemblyFluidIngredients(stepFluids);
        }

        List<ItemDemand> items = new ArrayList<>();
        items.add(new ItemDemand(recipe.getIngredient(), 1));
        for (Ingredient ingredient : stepIngredients) {
            if (!ingredient.isEmpty()) {
                items.add(new ItemDemand(ingredient, loops));
            }
        }

        List<FluidDemand> fluids = new ArrayList<>();
        for (SizedFluidIngredient fluid : stepFluids) {
            if (!fluid.ingredient().isEmpty()) {
                fluids.add(new FluidDemand(fluid.ingredient(), fluid.amount() * loops));
            }
        }
        return new Demand(List.copyOf(items), List.copyOf(fluids));
    }

    private static Requirements requirements(SequencedAssemblyRecipe recipe) {
        List<Ingredient> orderedTools = new ArrayList<>();
        List<StationKind> order = new ArrayList<>();
        int deployers = 0;
        int spouts = 0;
        int presses = 0;
        int saws = 0;

        for (SequencedRecipe<?> sequenced : recipe.getSequence()) {
            IAssemblyRecipe assembly = sequenced.getAsAssemblyRecipe();

            Set<ItemLike> stepMachines = new LinkedHashSet<>();
            assembly.addRequiredMachines(stepMachines);
            switch (Stations.kindOf(stepMachines)) {
                case DEPLOYER -> {
                    List<Ingredient> ingredients = new ArrayList<>();
                    assembly.addAssemblyIngredients(ingredients);
                    orderedTools.add(ingredients.isEmpty() ? Ingredient.EMPTY : ingredients.get(0));
                    order.add(StationKind.DEPLOYER);
                    deployers++;
                }
                case SPOUT -> {
                    order.add(StationKind.SPOUT);
                    spouts++;
                }
                case PRESS -> {
                    order.add(StationKind.PRESS);
                    presses++;
                }
                case SAW -> {
                    order.add(StationKind.SAW);
                    saws++;
                }
                default -> {}
            }
        }
        return new Requirements(deployers, spouts, presses, saws, List.copyOf(orderedTools), List.copyOf(order));
    }

    static List<List<Ingredient>> deployerToolLayout(List<SequencedAssemblyRecipe> recipes) {
        int width = 0;
        List<List<Ingredient>> perRecipe = new ArrayList<>();
        for (SequencedAssemblyRecipe recipe : recipes) {
            List<Ingredient> tools = requirementsOf(recipe).deployerTools();
            perRecipe.add(tools);
            width = Math.max(width, tools.size());
        }
        List<List<Ingredient>> layout = new ArrayList<>();
        for (int index = 0; index < width; index++) {
            List<Ingredient> accepted = new ArrayList<>();
            for (List<Ingredient> tools : perRecipe) {
                if (index < tools.size()) {
                    accepted.add(tools.get(index));
                }
            }
            layout.add(accepted);
        }
        return layout;
    }

    private static List<RecipeHolder<SequencedAssemblyRecipe>> all(Level level) {
        return level.getRecipeManager()
                .getAllRecipesFor(AllRecipeTypes.SEQUENCED_ASSEMBLY.<RecipeWrapper, SequencedAssemblyRecipe>getType());
    }
}
