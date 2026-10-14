package dev.rylex.nep.compat.occultism;

import appeng.api.stacks.AEItemKey;
import dev.rylex.nep.compat.occultism.OccultismMatrixRecipes.Kind;
import dev.rylex.nep.util.EnumNames;
import dev.rylex.nep.util.ItemCounts;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

final class BoundSpiritTemplates {
    private static final String KIND_KEY = "Kind";
    private static final String INPUT_KEY = "Input";
    private static final String RECIPE_KEY = "Recipe";
    private static final String TIER_KEY = "Tier";

    private BoundSpiritTemplates() {}

    record Template(Kind kind, AEItemKey input, Identifier recipe, int tier) {}

    static void save(Map<AEItemKey, Template> templates, ValueOutput output, String key) {
        ValueOutput.ValueOutputList list = output.childrenList(key);
        for (Map.Entry<AEItemKey, Template> entry : templates.entrySet()) {
            ValueOutput entryOutput = list.addChild();
            ItemCounts.putKey(entryOutput, entry.getKey());
            entryOutput.putString(KIND_KEY, entry.getValue().kind().name());
            ItemCounts.putKey(entryOutput.child(INPUT_KEY), entry.getValue().input());
            entryOutput.putString(RECIPE_KEY, entry.getValue().recipe().toString());
            entryOutput.putInt(TIER_KEY, entry.getValue().tier());
        }
    }

    static void load(Map<AEItemKey, Template> templates, ValueInput input, String key) {
        templates.clear();
        for (ValueInput entryInput : input.childrenListOrEmpty(key)) {
            AEItemKey output = ItemCounts.key(entryInput);
            AEItemKey templateInput =
                    entryInput.child(INPUT_KEY).map(ItemCounts::key).orElse(null);
            Identifier recipe =
                    entryInput.getString(RECIPE_KEY).map(Identifier::tryParse).orElse(null);
            if (output != null && templateInput != null && recipe != null) {
                templates.put(
                        output,
                        new Template(
                                EnumNames.byName(Kind.values(), entryInput.getStringOr(KIND_KEY, ""), Kind.SPIRIT_FIRE),
                                templateInput,
                                recipe,
                                entryInput.getIntOr(TIER_KEY, 0)));
            }
        }
    }
}
