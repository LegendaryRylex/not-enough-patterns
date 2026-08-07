package dev.rylex.nep.compat.draconic;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.brandon3055.draconicevolution.api.crafting.IFusionInjector;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

public final class FusionReclaimer {
    private FusionReclaimer() {}

    private static TileFusionCraftingCore assembling;
    private static boolean endedWhileAssembling;

    static void expect(TileFusionCraftingCore core, List<GenericStack> items, Direction direction) {
        core.setData(NepDraconicContent.FUSION_RECLAIM.get(), new FusionReclaim(items, direction));
        core.setChanged();
    }

    static void forget(TileFusionCraftingCore core) {
        core.removeData(NepDraconicContent.FUSION_RECLAIM.get());
    }

    public static void beginFusionState(TileFusionCraftingCore core) {
        assembling = core;
        endedWhileAssembling = false;
    }

    public static void endFusionState(TileFusionCraftingCore core) {
        assembling = null;
        if (endedWhileAssembling) {
            endedWhileAssembling = false;
            onCraftEnded(core);
        }
    }

    public static void onCraftEnded(TileFusionCraftingCore core) {
        if (core == assembling) {
            endedWhileAssembling = true;
            return;
        }
        Level level = core.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        FusionReclaim pending =
                core.getExistingData(NepDraconicContent.FUSION_RECLAIM.get()).orElse(null);
        if (pending == null) {
            return;
        }
        forget(core);
        ItemStack output = core.getOutputStack();
        ItemStack normalized = FusionResults.normalize(output);
        if (normalized != output) {
            core.setOutputStack(normalized);
        }
        core.setChanged();

        if (pending.isEmpty()) {
            return;
        }
        List<ItemStack> recovered = takeFromInjectors(core, pending.items());
        if (recovered.isEmpty()) {
            return;
        }
        IItemHandler target = ejectionTarget(level, core, pending.direction());
        for (ItemStack stack : recovered) {
            ItemStack leftover = target == null ? stack : ItemHandlerHelper.insertItem(target, stack, false);
            if (!leftover.isEmpty()) {
                Containers.dropItemStack(
                        level,
                        core.getBlockPos().getX() + 0.5,
                        core.getBlockPos().getY() + 1.0,
                        core.getBlockPos().getZ() + 0.5,
                        leftover);
                if (NepConfig.debugLogging()) {
                    Nep.LOGGER.info(
                            "Fusion crafting core {} could not return {} to the pattern provider and dropped it",
                            core.getBlockPos(),
                            leftover);
                }
            }
        }
    }

    private static List<ItemStack> takeFromInjectors(TileFusionCraftingCore core, List<GenericStack> wanted) {
        List<ItemStack> recovered = new ArrayList<>();
        core.updateInjectors();
        for (GenericStack want : wanted) {
            if (!(want.what() instanceof AEItemKey key)) {
                continue;
            }
            int remaining = (int) want.amount();
            for (IFusionInjector injector : core.getInjectors()) {
                if (remaining <= 0) {
                    break;
                }
                ItemStack held = injector.getInjectorStack();
                if (held.isEmpty() || !key.matches(held)) {
                    continue;
                }
                int take = Math.min(remaining, held.getCount());
                ItemStack kept = held.copy();
                kept.shrink(take);
                injector.setInjectorStack(kept);
                recovered.add(key.toStack(take));
                remaining -= take;
            }
        }
        return recovered;
    }

    @Nullable
    static IItemHandler ejectionTarget(Level level, TileFusionCraftingCore core, Direction direction) {
        return level.getCapability(
                Capabilities.ItemHandler.BLOCK, core.getBlockPos().relative(direction), direction.getOpposite());
    }
}
