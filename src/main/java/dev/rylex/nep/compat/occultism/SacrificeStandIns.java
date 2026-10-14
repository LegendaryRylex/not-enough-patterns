package dev.rylex.nep.compat.occultism;

import com.klikli_dev.occultism.common.item.tool.SoulGemItem;
import com.klikli_dev.occultism.registry.OccultismItems;
import com.mojang.serialization.MapCodec;
import dev.rylex.nep.NepConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class SacrificeStandIns {
    private SacrificeStandIns() {}

    private static final MapCodec<EntityType<?>> ENTITY_ID =
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("id");

    private static final int GEM_EXAMPLES = 3;

    static List<RecipeHolder<SacrificeStandInRecipe>> recipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(NepOccultismContent.STAND_IN.get());
    }

    static boolean accepts(Level level, TagKey<EntityType<?>> sacrifice, ItemStack stack) {
        if (stack.isEmpty() || !NepConfig.occultismSacrificeStandIns()) {
            return false;
        }
        for (RecipeHolder<SacrificeStandInRecipe> holder : recipes(level)) {
            SacrificeStandInRecipe recipe = holder.value();
            if (recipe.sacrifice().equals(sacrifice) && recipe.standIn().test(stack)) {
                return !(stack.getItem() instanceof SoulGemItem) || holds(stack, sacrifice);
            }
        }
        return false;
    }

    static boolean holds(ItemStack gem, TagKey<EntityType<?>> sacrifice) {
        EntityType<?> held = heldType(gem);
        return held != null && held.is(sacrifice);
    }

    @Nullable
    private static EntityType<?> heldType(ItemStack gem) {
        CustomData data = gem.get(DataComponents.ENTITY_DATA);
        return data == null ? null : data.read(ENTITY_ID).result().orElse(null);
    }

    static ItemStack leftover(ItemStack spent) {
        if (!(spent.getItem() instanceof SoulGemItem) || spent.is(OccultismItems.FRAGILE_SOUL_GEM_ITEM.get())) {
            return ItemStack.EMPTY;
        }
        ItemStack empty = spent.copyWithCount(1);
        empty.remove(DataComponents.ENTITY_DATA);
        return empty;
    }

    static List<ItemStack> examples(Level level, TagKey<EntityType<?>> sacrifice) {
        List<ItemStack> examples = new ArrayList<>();
        if (!NepConfig.occultismSacrificeStandIns()) {
            return examples;
        }
        for (RecipeHolder<SacrificeStandInRecipe> holder : recipes(level)) {
            if (!holder.value().sacrifice().equals(sacrifice)) {
                continue;
            }
            for (ItemStack option : holder.value().standIn().getItems()) {
                if (option.getItem() instanceof SoulGemItem) {
                    examples.addAll(filledGems(option, sacrifice));
                } else {
                    examples.add(option.copyWithCount(1));
                }
            }
        }
        return examples;
    }

    private static List<ItemStack> filledGems(ItemStack gem, TagKey<EntityType<?>> sacrifice) {
        List<ItemStack> gems = new ArrayList<>();
        for (Holder<EntityType<?>> type : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(sacrifice)) {
            if (gems.size() >= GEM_EXAMPLES) {
                break;
            }
            CompoundTag entity = new CompoundTag();
            entity.putString(
                    "id", BuiltInRegistries.ENTITY_TYPE.getKey(type.value()).toString());
            ItemStack filled = gem.copyWithCount(1);
            filled.set(DataComponents.ENTITY_DATA, CustomData.of(entity));
            gems.add(filled);
        }
        return gems;
    }
}
