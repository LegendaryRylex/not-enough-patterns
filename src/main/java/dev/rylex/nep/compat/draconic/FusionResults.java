package dev.rylex.nep.compat.draconic;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.draconicevolution.api.crafting.IFusionInjector;
import com.brandon3055.draconicevolution.api.crafting.IFusionInventory;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.init.ItemData;
import dev.rylex.nep.util.RecipeCache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class FusionResults {
    private FusionResults() {}

    private static final Map<ResourceLocation, Boolean> STABLE = new ConcurrentHashMap<>();

    private record CanonicalForm(AEItemKey plain, AEItemKey produced) {}

    private static final RecipeCache<Map<Item, CanonicalForm>> CANONICAL_FORMS =
            RecipeCache.of(FusionResults::buildCanonicalForms);

    record CatalystOnly(ItemStack catalyst) implements IFusionInventory {
        @Override
        public ItemStack getCatalystStack() {
            return catalyst;
        }

        @Override
        public ItemStack getOutputStack() {
            return ItemStack.EMPTY;
        }

        @Override
        public void setCatalystStack(ItemStack stack) {}

        @Override
        public void setOutputStack(ItemStack stack) {}

        @Override
        public List<IFusionInjector> getInjectors() {
            return List.of();
        }

        @Override
        public TechLevel getMinimumTier() {
            return TechLevel.CHAOTIC;
        }

        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? catalyst : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    static void clearCache() {
        STABLE.clear();
        CANONICAL_FORMS.clear();
    }

    static AEItemKey canonical(AEItemKey key, Level level) {
        CanonicalForm form = CANONICAL_FORMS.get(level).get(key.getItem());
        return form != null && form.plain().equals(key) ? form.produced() : key;
    }

    static GenericStack canonical(GenericStack stack, Level level) {
        if (!(stack.what() instanceof AEItemKey key)) {
            return stack;
        }
        AEItemKey canonical = canonical(key, level);
        return canonical.equals(key) ? stack : new GenericStack(canonical, stack.amount());
    }

    @Nullable
    static List<GenericStack> canonical(@Nullable List<GenericStack> stacks, Level level) {
        if (stacks == null) {
            return null;
        }
        List<GenericStack> canonical = new ArrayList<>(stacks.size());
        for (GenericStack stack : stacks) {
            canonical.add(canonical(stack, level));
        }
        return List.copyOf(canonical);
    }

    private static Map<Item, CanonicalForm> buildCanonicalForms(Level level) {
        Map<Item, AEItemKey> produced = new HashMap<>();
        Set<Item> ambiguous = new HashSet<>();
        for (RecipeHolder<IFusionRecipe> holder : FusionRecipeResolver.candidates(level)) {
            if (!producesAStableResult(holder.value(), holder.id(), level)) {
                continue;
            }
            AEItemKey result = AEItemKey.of(expectedResult(holder.value(), level));
            if (result == null) {
                continue;
            }
            AEItemKey existing = produced.putIfAbsent(result.getItem(), result);
            if (existing != null && !existing.equals(result)) {
                ambiguous.add(result.getItem());
            }
        }

        Map<Item, CanonicalForm> forms = new HashMap<>();
        for (Map.Entry<Item, AEItemKey> entry : produced.entrySet()) {
            AEItemKey plain = AEItemKey.of(new ItemStack(entry.getKey()));
            if (plain == null || plain.equals(entry.getValue()) || ambiguous.contains(entry.getKey())) {
                continue;
            }
            forms.put(entry.getKey(), new CanonicalForm(plain, entry.getValue()));
        }
        return Map.copyOf(forms);
    }

    static GenericStack producedForm(IFusionRecipe recipe, Level level, GenericStack supplied) {
        if (!(supplied.what() instanceof AEItemKey key)) {
            return supplied;
        }
        AEItemKey assembled = AEItemKey.of(expectedResult(recipe, level));
        if (assembled == null || assembled.equals(key)) {
            return supplied;
        }
        AEItemKey declared = AEItemKey.of(recipe.getResultItem(level.registryAccess()));
        return declared != null && declared.equals(key) ? new GenericStack(assembled, supplied.amount()) : supplied;
    }

    static ItemStack normalize(ItemStack stack) {
        if (stack.isEmpty() || !stack.has(ItemData.PROVIDER_IDENTITY.get())) {
            return stack;
        }
        ItemStack copy = stack.copy();
        copy.remove(ItemData.PROVIDER_IDENTITY.get());
        return copy;
    }

    static ItemStack assemble(IFusionRecipe recipe, Level level, ItemStack catalyst) {
        return normalize(recipe.assemble(new CatalystOnly(catalyst), level.registryAccess()));
    }

    static ItemStack expectedResult(IFusionRecipe recipe, Level level) {
        ItemStack[] catalysts = recipe.getCatalyst().getItems();
        if (catalysts.length == 0) {
            return recipe.getResultItem(level.registryAccess());
        }
        return assemble(recipe, level, catalysts[0].copy());
    }

    static boolean producesAStableResult(IFusionRecipe recipe, ResourceLocation id, Level level) {
        return STABLE.computeIfAbsent(id, ignored -> probe(recipe, level));
    }

    private static boolean probe(IFusionRecipe recipe, Level level) {
        ItemStack[] catalysts = recipe.getCatalyst().getItems();
        if (catalysts.length == 0) {
            return false;
        }
        AEItemKey expected = AEItemKey.of(expectedResult(recipe, level));
        if (expected == null) {
            return false;
        }
        for (ItemStack catalyst : catalysts) {
            for (int attempt = 0; attempt < 2; attempt++) {
                if (!expected.equals(AEItemKey.of(assemble(recipe, level, catalyst.copy())))) {
                    return false;
                }
            }
        }
        return true;
    }
}
