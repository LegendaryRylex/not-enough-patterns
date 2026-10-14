package dev.rylex.nep.compat.occultism;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.util.ItemCounts;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

final class RitualControllerPersistence {
    private static final String STACKS_KEY = "Stacks";
    private static final String TEMPLATE_RECIPE_KEY = "Recipe";
    private static final String PLACED_POS_KEY = "Pos";
    private static final String PLACED_ITEM_KEY = "Item";

    private RitualControllerPersistence() {}

    record Template(
            List<AEKey> keys, List<Long> counts, @Nullable Identifier recipe) {}

    record Placed(BlockPos bowl, ItemStack stack) {}

    static void saveTemplates(Map<AEItemKey, Template> templates, ValueOutput output, String key) {
        ValueOutput.ValueOutputList list = output.childrenList(key);
        for (Map.Entry<AEItemKey, Template> entry : templates.entrySet()) {
            ValueOutput entryOutput = list.addChild();
            ItemCounts.putKey(entryOutput, entry.getKey());
            ValueOutput.TypedOutputList<GenericStack> stacks = entryOutput.list(STACKS_KEY, GenericStack.CODEC);
            for (int i = 0; i < entry.getValue().keys().size(); i++) {
                stacks.add(new GenericStack(
                        entry.getValue().keys().get(i),
                        entry.getValue().counts().get(i)));
            }
            if (entry.getValue().recipe() != null) {
                entryOutput.putString(
                        TEMPLATE_RECIPE_KEY, entry.getValue().recipe().toString());
            }
        }
    }

    static void loadTemplates(Map<AEItemKey, Template> templates, ValueInput input, String key) {
        templates.clear();
        for (ValueInput entryInput : input.childrenListOrEmpty(key)) {
            AEItemKey item = ItemCounts.key(entryInput);
            if (item == null) {
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
                        item,
                        new Template(
                                List.copyOf(keys),
                                List.copyOf(counts),
                                entryInput
                                        .getString(TEMPLATE_RECIPE_KEY)
                                        .map(Identifier::tryParse)
                                        .orElse(null)));
            }
        }
    }

    static void savePlaced(List<Placed> placed, ValueOutput output, String key) {
        ValueOutput.ValueOutputList list = output.childrenList(key);
        for (Placed entry : placed) {
            ValueOutput placedOutput = list.addChild();
            placedOutput.putLong(PLACED_POS_KEY, entry.bowl().asLong());
            placedOutput.store(PLACED_ITEM_KEY, ItemStack.CODEC, entry.stack());
        }
    }

    static void loadPlaced(List<Placed> placed, ValueInput input, String key) {
        placed.clear();
        for (ValueInput placedInput : input.childrenListOrEmpty(key)) {
            BlockPos pos = BlockPos.of(placedInput.getLongOr(PLACED_POS_KEY, 0L));
            placedInput.read(PLACED_ITEM_KEY, ItemStack.CODEC).ifPresent(stack -> placed.add(new Placed(pos, stack)));
        }
    }
}
