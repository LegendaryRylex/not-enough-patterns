package dev.rylex.nep.compat.ars;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

final class CatalystShelf {

    static final int SLOTS = 9;

    private static final String ITEMS_KEY = "Items";
    private static final String OWNERS_KEY = "Owners";

    private final ItemStackHandler items;
    private final ResourceLocation[] owners = new ResourceLocation[SLOTS];

    CatalystShelf(Runnable onChanged) {
        this.items = new ItemStackHandler(SLOTS) {
            @Override
            protected void onContentsChanged(int slot) {
                onChanged.run();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return false;
            }
        };
    }

    ItemStackHandler items() {
        return items;
    }

    boolean holds(ResourceLocation recipe) {
        for (ResourceLocation owner : owners) {
            if (recipe.equals(owner)) {
                return true;
            }
        }
        return false;
    }

    int freeSlots() {
        int free = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            if (owners[slot] == null && items.getStackInSlot(slot).isEmpty()) {
                free++;
            }
        }
        return free;
    }

    Set<ResourceLocation> heldRecipes() {
        Set<ResourceLocation> held = new LinkedHashSet<>();
        for (ResourceLocation owner : owners) {
            if (owner != null) {
                held.add(owner);
            }
        }
        return held;
    }

    void shelve(ResourceLocation recipe, List<ItemStack> set) {
        int next = 0;
        for (ItemStack stack : set) {
            while (next < SLOTS
                    && (owners[next] != null || !items.getStackInSlot(next).isEmpty())) {
                next++;
            }
            if (next >= SLOTS) {
                return;
            }
            owners[next] = recipe;
            items.setStackInSlot(next, stack.copy());
        }
    }

    List<ItemStack> release(ResourceLocation recipe) {
        List<ItemStack> released = new ArrayList<>();
        for (int slot = 0; slot < SLOTS; slot++) {
            if (recipe.equals(owners[slot])) {
                owners[slot] = null;
                ItemStack stack = items.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    released.add(stack);
                    items.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
        }
        return released;
    }

    void restore(ResourceLocation recipe, List<ItemStack> leftovers) {
        List<ItemStack> kept =
                leftovers.stream().filter(stack -> !stack.isEmpty()).toList();
        if (!kept.isEmpty()) {
            shelve(recipe, kept);
        }
    }

    List<ItemStack> clear() {
        List<ItemStack> all = new ArrayList<>();
        for (int slot = 0; slot < SLOTS; slot++) {
            owners[slot] = null;
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                all.add(stack);
                items.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        return all;
    }

    @Nullable
    static int[] assign(List<Ingredient> needs, List<ItemStack> offered) {
        int[] picks = new int[needs.size()];
        boolean[] used = new boolean[offered.size()];
        for (int need = 0; need < needs.size(); need++) {
            picks[need] = -1;
            for (int option = 0; option < offered.size(); option++) {
                if (!used[option] && needs.get(need).test(offered.get(option))) {
                    used[option] = true;
                    picks[need] = option;
                    break;
                }
            }
            if (picks[need] < 0) {
                return null;
            }
        }
        return picks;
    }

    CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put(ITEMS_KEY, items.serializeNBT(registries));
        ListTag list = new ListTag();
        for (ResourceLocation owner : owners) {
            list.add(StringTag.valueOf(owner == null ? "" : owner.toString()));
        }
        tag.put(OWNERS_KEY, list);
        return tag;
    }

    void load(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag sized = tag.getCompound(ITEMS_KEY).copy();
        sized.putInt("Size", SLOTS);
        items.deserializeNBT(registries, sized);
        ListTag list = tag.getList(OWNERS_KEY, Tag.TAG_STRING);
        for (int slot = 0; slot < SLOTS; slot++) {
            String owner = slot < list.size() ? list.getString(slot) : "";
            owners[slot] = owner.isEmpty() ? null : ResourceLocation.tryParse(owner);
        }
    }
}
