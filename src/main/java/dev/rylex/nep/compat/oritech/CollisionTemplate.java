package dev.rylex.nep.compat.oritech;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.machine.DemandTally;
import dev.rylex.nep.machine.StackColumns;
import dev.rylex.nep.util.ItemCounts;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

record CollisionTemplate(StackColumns stacks, @Nullable Identifier recipe) {
    private static final String STACKS_KEY = "Stacks";
    private static final String RECIPE_KEY = "Recipe";

    static CollisionTemplate capture(Map<AEItemKey, Long> items, Identifier recipe) {
        List<AEKey> keys = new ArrayList<>(items.size());
        List<Long> counts = new ArrayList<>(items.size());
        for (Map.Entry<AEItemKey, Long> entry : items.entrySet()) {
            keys.add(entry.getKey());
            counts.add(entry.getValue());
        }
        return new CollisionTemplate(new StackColumns(List.copyOf(keys), List.copyOf(counts)), recipe);
    }

    void addDemand(DemandTally tally, long crafts) {
        List<AEKey> keys = stacks.keys();
        List<Long> counts = stacks.counts();
        for (int i = 0; i < keys.size(); i++) {
            tally.add(keys.get(i), counts.get(i) * crafts);
        }
    }

    long demandFor(AEKey key, long crafts) {
        int index = stacks.keys().indexOf(key);
        return index >= 0 ? stacks.counts().get(index) * crafts : 0;
    }

    static void save(Map<AEItemKey, CollisionTemplate> templates, ValueOutput output, String key) {
        ValueOutput.ValueOutputList list = output.childrenList(key);
        for (Map.Entry<AEItemKey, CollisionTemplate> entry : templates.entrySet()) {
            ValueOutput entryOutput = list.addChild();
            ItemCounts.putKey(entryOutput, entry.getKey());
            ValueOutput.TypedOutputList<GenericStack> stacks = entryOutput.list(STACKS_KEY, GenericStack.CODEC);
            StackColumns columns = entry.getValue().stacks();
            for (int i = 0; i < columns.keys().size(); i++) {
                stacks.add(
                        new GenericStack(columns.keys().get(i), columns.counts().get(i)));
            }
            if (entry.getValue().recipe() != null) {
                entryOutput.putString(RECIPE_KEY, entry.getValue().recipe().toString());
            }
        }
    }

    static void load(
            Map<AEItemKey, CollisionTemplate> templates, ValueInput input, String key, boolean recipeRequired) {
        templates.clear();
        for (ValueInput entryInput : input.childrenListOrEmpty(key)) {
            AEItemKey item = ItemCounts.key(entryInput);
            Identifier recipe =
                    entryInput.getString(RECIPE_KEY).map(Identifier::tryParse).orElse(null);
            if (item == null || (recipeRequired && recipe == null)) {
                continue;
            }
            List<AEKey> keys = new ArrayList<>();
            List<Long> counts = new ArrayList<>();
            for (GenericStack stack : entryInput.listOrEmpty(STACKS_KEY, GenericStack.CODEC)) {
                keys.add(stack.what());
                counts.add(stack.amount());
            }
            if (!keys.isEmpty()) {
                templates.put(
                        item, new CollisionTemplate(new StackColumns(List.copyOf(keys), List.copyOf(counts)), recipe));
            }
        }
    }
}
