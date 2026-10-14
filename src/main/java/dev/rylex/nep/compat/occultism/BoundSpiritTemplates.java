package dev.rylex.nep.compat.occultism;

import appeng.api.stacks.AEItemKey;
import dev.rylex.nep.compat.occultism.OccultismMatrixRecipes.Kind;
import dev.rylex.nep.util.EnumNames;
import dev.rylex.nep.util.ItemCounts;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;

final class BoundSpiritTemplates {
    private static final String KIND_KEY = "Kind";
    private static final String INPUT_KEY = "Input";
    private static final String RECIPE_KEY = "Recipe";
    private static final String TIER_KEY = "Tier";

    private BoundSpiritTemplates() {}

    record Template(Kind kind, AEItemKey input, ResourceLocation recipe, int tier) {}

    static ListTag save(Map<AEItemKey, Template> templates, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<AEItemKey, Template> entry : templates.entrySet()) {
            CompoundTag tag = new CompoundTag();
            ItemCounts.putKey(tag, entry.getKey(), registries);
            tag.putString(KIND_KEY, entry.getValue().kind().name());
            CompoundTag input = new CompoundTag();
            ItemCounts.putKey(input, entry.getValue().input(), registries);
            tag.put(INPUT_KEY, input);
            tag.putString(RECIPE_KEY, entry.getValue().recipe().toString());
            tag.putInt(TIER_KEY, entry.getValue().tier());
            list.add(tag);
        }
        return list;
    }

    static void load(Map<AEItemKey, Template> templates, ListTag list, HolderLookup.Provider registries) {
        templates.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            AEItemKey output = ItemCounts.key(tag, ItemCounts.LEGACY_ID_TAG, registries);
            AEItemKey input = ItemCounts.key(tag.getCompound(INPUT_KEY), ItemCounts.LEGACY_ID_TAG, registries);
            ResourceLocation recipe = ResourceLocation.tryParse(tag.getString(RECIPE_KEY));
            if (output != null && input != null && recipe != null) {
                templates.put(
                        output,
                        new Template(
                                EnumNames.byName(Kind.values(), tag.getString(KIND_KEY), Kind.SPIRIT_FIRE),
                                input,
                                recipe,
                                tag.getInt(TIER_KEY)));
            }
        }
    }
}
