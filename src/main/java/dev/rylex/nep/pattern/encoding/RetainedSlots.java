package dev.rylex.nep.pattern.encoding;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.parts.encoding.EncodingMode;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;
import dev.rylex.nep.pattern.RecipePattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class RetainedSlots {
    private RetainedSlots() {}

    public static List<Integer> compute(PatternEncodingLogic logic, PatternOrigin origin, Level level) {
        if (logic.getMode() != EncodingMode.PROCESSING) {
            return List.of();
        }
        ConfigInventory inputs = logic.getEncodedInputInv();
        ItemStack encoded = encode(inputs, logic.getEncodedOutputInv());
        if (encoded == null) {
            return List.of();
        }
        ItemStack converted = PatternConverters.convertQuietly(origin, encoded, level);
        if (converted == null || !(PatternDetailsHelper.decodePattern(converted, level) instanceof RecipePattern p)) {
            return List.of();
        }
        if (p.retained().isEmpty()) {
            return List.of();
        }
        List<GenericStack> slots = new ArrayList<>(inputs.size());
        for (int slot = 0; slot < inputs.size(); slot++) {
            slots.add(inputs.getStack(slot));
        }
        return slotsOf(slots, p.retained());
    }

    static List<Integer> slotsOf(List<GenericStack> slots, List<GenericStack> retained) {
        Map<AEKey, Long> kept = new LinkedHashMap<>();
        for (GenericStack stack : retained) {
            kept.merge(stack.what(), stack.amount(), Long::sum);
        }
        List<Integer> marked = new ArrayList<>();
        for (Map.Entry<AEKey, Long> entry : kept.entrySet()) {
            marked.addAll(cover(slots, entry.getKey(), entry.getValue()));
        }
        Collections.sort(marked);
        return List.copyOf(marked);
    }

    private static List<Integer> cover(List<GenericStack> slots, AEKey what, long amount) {
        for (int slot = slots.size() - 1; slot >= 0; slot--) {
            GenericStack stack = slots.get(slot);
            if (stack != null && stack.what().equals(what) && stack.amount() == amount) {
                return List.of(slot);
            }
        }
        List<Integer> picked = new ArrayList<>();
        long left = amount;
        for (int slot = slots.size() - 1; slot >= 0 && left > 0; slot--) {
            GenericStack stack = slots.get(slot);
            if (stack == null || !stack.what().equals(what) || stack.amount() > left) {
                continue;
            }
            picked.add(slot);
            left -= stack.amount();
        }
        return left == 0 ? picked : List.of();
    }

    @Nullable
    private static ItemStack encode(ConfigInventory inputs, ConfigInventory outputs) {
        List<GenericStack> sparseInputs = new ArrayList<>(inputs.size());
        boolean anyInput = false;
        for (int slot = 0; slot < inputs.size(); slot++) {
            GenericStack stack = inputs.getStack(slot);
            sparseInputs.add(stack);
            anyInput |= stack != null;
        }
        if (!anyInput) {
            return null;
        }
        List<GenericStack> sparseOutputs = new ArrayList<>(outputs.size());
        for (int slot = 0; slot < outputs.size(); slot++) {
            sparseOutputs.add(outputs.getStack(slot));
        }
        if (sparseOutputs.isEmpty() || sparseOutputs.get(0) == null) {
            return null;
        }
        try {
            return PatternDetailsHelper.encodeProcessingPattern(sparseInputs, sparseOutputs);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
