package dev.rylex.nep.client;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.serverbound.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.FakeSlot;
import appeng.parts.encoding.EncodingMode;
import dev.rylex.nep.pattern.encoding.EncodedIngredients;
import dev.rylex.nep.pattern.encoding.PatternContents;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class ProcessingEncoding {
    private ProcessingEncoding() {}

    private static final Comparator<GridInventoryEntry> ENTRY_ORDER = Comparator.comparing(
                    GridInventoryEntry::isCraftable)
            .thenComparing(ProcessingEncoding::isUndamaged)
            .thenComparing(GridInventoryEntry::getStoredAmount);

    public static void encode(PatternEncodingTermMenu menu, EncodedIngredients encoded) {
        menu.setMode(EncodingMode.PROCESSING);

        Map<AEKey, Integer> priorities = EncodingHelper.getIngredientPriorities(menu, ENTRY_ORDER);
        List<GenericStack> chosen = new ArrayList<>(encoded.inputs().size());
        for (List<GenericStack> options : encoded.inputs()) {
            chosen.add(best(priorities, options));
        }

        fill(menu.getProcessingInputSlots(), PatternContents.condenseSlots(chosen, encoded.retainedSlots()));
        fill(menu.getProcessingOutputSlots(), encoded.outputs());
    }

    @Nullable
    private static GenericStack best(Map<AEKey, Integer> priorities, List<GenericStack> options) {
        GenericStack best = null;
        int bestPriority = Integer.MIN_VALUE;
        for (GenericStack option : options) {
            int priority = priorities.getOrDefault(option.what(), Integer.MIN_VALUE);
            if (best == null || priority > bestPriority) {
                best = option;
                bestPriority = priority;
            }
        }
        return best;
    }

    private static void fill(FakeSlot[] slots, List<GenericStack> stacks) {
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = i < stacks.size() ? GenericStack.wrapInItemStack(stacks.get(i)) : ItemStack.EMPTY;
            ServerboundPacket message = new InventoryActionPacket(InventoryAction.SET_FILTER, slots[i].index, stack);
            ClientPacketDistributor.sendToServer(message);
        }
    }

    private static Boolean isUndamaged(GridInventoryEntry entry) {
        return !(entry.getWhat() instanceof AEItemKey key) || !key.isDamaged();
    }
}
