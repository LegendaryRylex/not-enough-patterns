package dev.rylex.nep.compat.create;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import guideme.compiler.tags.RecipeTypeMappingSupplier.RecipeTypeMappings;
import guideme.document.block.LytBlock;
import guideme.document.block.LytHBox;
import guideme.document.block.LytSlotGrid;
import guideme.document.block.recipes.LytStandardRecipeBox;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.Nullable;

public final class CreateGuideRecipes {
    private CreateGuideRecipes() {}

    public static void collect(RecipeTypeMappings mappings) {
        processing(mappings, AllRecipeTypes.CRUSHING, AllBlocks.CRUSHING_WHEEL.get(), "create.recipe.crushing");
        processing(mappings, AllRecipeTypes.MILLING, AllBlocks.MILLSTONE.get(), "create.recipe.milling");
        processing(mappings, AllRecipeTypes.CUTTING, AllBlocks.MECHANICAL_SAW.get(), "create.recipe.sawing");
        processing(mappings, AllRecipeTypes.PRESSING, AllBlocks.MECHANICAL_PRESS.get(), "create.recipe.pressing");
        processing(mappings, AllRecipeTypes.MIXING, AllBlocks.MECHANICAL_MIXER.get(), "create.recipe.mixing");
        processing(mappings, AllRecipeTypes.COMPACTING, AllBlocks.MECHANICAL_PRESS.get(), "create.recipe.packing");
        processing(mappings, AllRecipeTypes.BASIN, AllBlocks.BASIN.get(), null);
        processing(mappings, AllRecipeTypes.SPLASHING, AllBlocks.ENCASED_FAN.get(), "create.recipe.fan_washing");
        processing(mappings, AllRecipeTypes.HAUNTING, AllBlocks.ENCASED_FAN.get(), "create.recipe.fan_haunting");
        processing(mappings, AllRecipeTypes.DEPLOYING, AllBlocks.DEPLOYER.get(), "create.recipe.deploying");
        processing(
                mappings, AllRecipeTypes.ITEM_APPLICATION, AllBlocks.DEPLOYER.get(), "create.recipe.item_application");
        processing(mappings, AllRecipeTypes.FILLING, AllBlocks.SPOUT.get(), "create.recipe.spout_filling");
        processing(mappings, AllRecipeTypes.EMPTYING, AllBlocks.ITEM_DRAIN.get(), "create.recipe.draining");
        processing(
                mappings,
                AllRecipeTypes.SANDPAPER_POLISHING,
                AllItems.SAND_PAPER.get(),
                "create.recipe.sandpaper_polishing");
        processing(mappings, AllRecipeTypes.CONVERSION, AllBlocks.DEPOT.get(), "create.recipe.mystery_conversion");

        mappings.add(
                AllRecipeTypes.MECHANICAL_CRAFTING.<CraftingInput, MechanicalCraftingRecipe>getType(),
                CreateGuideRecipes::mechanicalCrafting);
        mappings.add(
                AllRecipeTypes.SEQUENCED_ASSEMBLY.<RecipeWrapper, SequencedAssemblyRecipe>getType(),
                CreateGuideRecipes::sequencedAssembly);
    }

    private static <I extends RecipeInput, R extends ProcessingRecipe<I, ?>> void processing(
            RecipeTypeMappings mappings, AllRecipeTypes type, ItemLike icon, String titleKey) {
        RecipeType<R> recipeType = type.getType();
        mappings.add(recipeType, holder -> processingBox(holder, icon, titleKey));
    }

    private static LytBlock processingBox(
            RecipeHolder<? extends ProcessingRecipe<?, ?>> holder, ItemLike icon, String titleKey) {
        ProcessingRecipe<?, ?> recipe = holder.value();

        List<Ingredient> inputs = new ArrayList<>(recipe.getIngredients());
        for (SizedFluidIngredient fluid : recipe.getFluidIngredients()) {
            Ingredient asBucket = bucketOf(fluid);
            if (asBucket != null) {
                inputs.add(asBucket);
            }
        }

        List<ItemStack> outputs = new ArrayList<>();
        for (ProcessingOutput output : recipe.getRollableResults()) {
            ItemStack stack = output.getStack();
            if (!stack.isEmpty()) {
                outputs.add(stack);
            }
        }
        for (FluidStack fluid : recipe.getFluidResults()) {
            ItemStack bucket = new ItemStack(fluid.getFluid().getBucket());
            if (!bucket.isEmpty()) {
                outputs.add(bucket);
            }
        }

        LytStandardRecipeBox.Builder builder =
                LytStandardRecipeBox.builder().icon(icon).title(title(titleKey, icon));
        if (!inputs.isEmpty()) {
            builder.input(LytSlotGrid.row(inputs, false));
        }
        if (!outputs.isEmpty()) {
            builder.output(LytSlotGrid.rowFromStacks(outputs, false));
        }
        return builder.build(holder);
    }

    private static LytBlock mechanicalCrafting(RecipeHolder<MechanicalCraftingRecipe> holder) {
        MechanicalCraftingRecipe recipe = holder.value();
        int width = recipe.getWidth();
        int height = recipe.getHeight();
        LytSlotGrid grid = new LytSlotGrid(width, height);
        List<Ingredient> ingredients = recipe.getIngredients();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (index >= ingredients.size()) {
                    continue;
                }
                Ingredient ingredient = ingredients.get(index);
                if (!ingredient.isEmpty()) {
                    grid.setIngredient(x, y, ingredient);
                }
            }
        }
        return LytStandardRecipeBox.builder()
                .icon(AllBlocks.MECHANICAL_CRAFTER.get())
                .title(title("create.recipe.mechanical_crafting", AllBlocks.MECHANICAL_CRAFTER.get()))
                .input(grid)
                .outputFromResultOf(holder)
                .build(holder);
    }

    private static LytBlock sequencedAssembly(RecipeHolder<SequencedAssemblyRecipe> holder) {
        SequencedAssemblyRecipe recipe = holder.value();

        LytHBox steps = new LytHBox();
        for (SequencedRecipe<?> step : recipe.getSequence()) {
            List<Ingredient> row = new ArrayList<>();
            Set<ItemLike> stations = new LinkedHashSet<>();
            step.getAsAssemblyRecipe().addRequiredMachines(stations);
            for (ItemLike station : stations) {
                row.add(Ingredient.of(station));
            }
            step.getAsAssemblyRecipe().addAssemblyIngredients(row);
            for (SizedFluidIngredient fluid : fluidsOf(step)) {
                Ingredient asBucket = bucketOf(fluid);
                if (asBucket != null) {
                    row.add(asBucket);
                }
            }
            if (!row.isEmpty()) {
                steps.append(LytSlotGrid.column(row, false));
            }
        }

        ItemStack result = recipe.resultPool.isEmpty()
                ? ItemStack.EMPTY
                : recipe.resultPool.getFirst().getStack();

        LytStandardRecipeBox.Builder builder = LytStandardRecipeBox.builder()
                .icon(AllBlocks.DEPOT.get())
                .title(sequencedAssemblyTitle(recipe))
                .input(recipe.getIngredient())
                .addBottom(steps);
        if (!result.isEmpty()) {
            builder.output(result);
        }
        return builder.build(holder);
    }

    private static List<SizedFluidIngredient> fluidsOf(SequencedRecipe<?> step) {
        List<SizedFluidIngredient> fluids = new ArrayList<>();
        step.getAsAssemblyRecipe().addAssemblyFluidIngredients(fluids);
        return fluids;
    }

    private static String sequencedAssemblyTitle(SequencedAssemblyRecipe recipe) {
        String base = title("create.recipe.sequenced_assembly", AllBlocks.DEPOT.get());
        int loops = recipe.getLoops();
        return loops > 1
                ? Component.translatable("nep.guide.loops", base, loops).getString()
                : base;
    }

    private static String title(@Nullable String key, ItemLike fallback) {
        if (key != null) {
            String translated = Component.translatable(key).getString();
            if (!translated.equals(key)) {
                return translated;
            }
        }
        return new ItemStack(fallback).getHoverName().getString();
    }

    @Nullable
    private static Ingredient bucketOf(SizedFluidIngredient fluid) {
        FluidStack[] stacks = fluid.getFluids();
        if (stacks.length == 0) {
            return null;
        }
        Item bucket = stacks[0].getFluid().getBucket();
        return bucket == null || bucket == Items.AIR ? null : Ingredient.of(bucket);
    }
}
