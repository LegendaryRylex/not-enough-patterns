package dev.rylex.nep.compat.occultism;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import dev.rylex.nep.machine.StackColumns;
import dev.rylex.nep.util.ItemCounts;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class RitualControllerPersistence {
    private static final String STACKS_KEY = "Stacks";
    private static final String TEMPLATE_RECIPE_KEY = "Recipe";
    private static final String PLACED_POS_KEY = "Pos";
    private static final String PLACED_ITEM_KEY = "Item";

    private RitualControllerPersistence() {}

    record Template(
            List<AEKey> keys, List<Long> counts, @Nullable ResourceLocation recipe) {}

    record Placed(BlockPos bowl, ItemStack stack) {}

    static ListTag saveTemplates(Map<AEItemKey, Template> templates, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<AEItemKey, Template> entry : templates.entrySet()) {
            CompoundTag tag = new CompoundTag();
            ItemCounts.putKey(tag, entry.getKey(), registries);
            tag.put(
                    STACKS_KEY,
                    new StackColumns(entry.getValue().keys(), entry.getValue().counts()).write(registries));
            if (entry.getValue().recipe() != null) {
                tag.putString(TEMPLATE_RECIPE_KEY, entry.getValue().recipe().toString());
            }
            list.add(tag);
        }
        return list;
    }

    static void loadTemplates(Map<AEItemKey, Template> templates, ListTag list, HolderLookup.Provider registries) {
        templates.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            AEItemKey item = ItemCounts.key(tag, ItemCounts.LEGACY_ID_TAG, registries);
            if (item == null) {
                continue;
            }
            StackColumns columns = StackColumns.read(tag, STACKS_KEY, registries);
            if (!columns.keys().isEmpty()) {
                templates.put(
                        item,
                        new Template(
                                columns.keys(),
                                columns.counts(),
                                ResourceLocation.tryParse(tag.getString(TEMPLATE_RECIPE_KEY))));
            }
        }
    }

    static ListTag savePlaced(List<Placed> placed, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Placed entry : placed) {
            CompoundTag tag = new CompoundTag();
            tag.putLong(PLACED_POS_KEY, entry.bowl().asLong());
            tag.put(PLACED_ITEM_KEY, entry.stack().save(registries));
            list.add(tag);
        }
        return list;
    }

    static void loadPlaced(List<Placed> placed, ListTag list, HolderLookup.Provider registries) {
        placed.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            BlockPos pos = BlockPos.of(tag.getLong(PLACED_POS_KEY));
            ItemStack.parse(registries, tag.getCompound(PLACED_ITEM_KEY))
                    .ifPresent(stack -> placed.add(new Placed(pos, stack)));
        }
    }
}
