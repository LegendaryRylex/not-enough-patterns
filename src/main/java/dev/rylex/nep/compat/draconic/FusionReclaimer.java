package dev.rylex.nep.compat.draconic;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.brandon3055.draconicevolution.api.crafting.IFusionInjector;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.CraftedOutputs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

public final class FusionReclaimer {
    private FusionReclaimer() {}

    private enum Ending {
        COMPLETED,
        CANCELLED
    }

    private static TileFusionCraftingCore assembling;

    @Nullable
    private static Ending endedWhileAssembling;

    static void expect(
            TileFusionCraftingCore core,
            List<GenericStack> retained,
            List<GenericStack> loaded,
            GenericStack catalyst,
            Direction direction) {
        core.setData(
                NepDraconicContent.FUSION_RECLAIM.get(),
                new FusionReclaim(retained, loaded, List.of(catalyst), direction));
        core.setChanged();
    }

    static void forget(TileFusionCraftingCore core) {
        core.removeData(NepDraconicContent.FUSION_RECLAIM.get());
    }

    public static void beginFusionState(TileFusionCraftingCore core) {
        assembling = core;
        endedWhileAssembling = null;
    }

    public static void endFusionState(TileFusionCraftingCore core) {
        assembling = null;
        Ending ending = endedWhileAssembling;
        endedWhileAssembling = null;
        if (ending == Ending.COMPLETED) {
            onCraftCompleted(core);
        } else if (ending == Ending.CANCELLED) {
            onCraftCancelled(core);
        }
    }

    public static void onCraftCompleted(TileFusionCraftingCore core) {
        if (core == assembling) {
            endedWhileAssembling = Ending.COMPLETED;
            return;
        }
        Level level = serverLevel(core);
        FusionReclaim pending = pending(core);
        if (level == null || pending == null) {
            return;
        }
        forget(core);

        ItemStack output = core.getOutputStack();
        ItemStack normalized = FusionResults.normalize(output);
        if (normalized != output) {
            core.setOutputStack(normalized);
        }
        core.setChanged();
        rememberCraftedForm(level, normalized);

        if (pending.isEmpty()) {
            return;
        }
        returnItems(level, core, pending.direction(), takeFromInjectors(core, pending.retained()));
    }

    private static void rememberCraftedForm(Level level, ItemStack output) {
        RecipeHolder<IFusionRecipe> holder = FusionRecipeResolver.resolveByOutputItem(level, output.getItem());
        if (holder != null) {
            CraftedOutputs.expect(
                    AEItemKey.of(output), AEItemKey.of(FusionResults.expectedResult(holder.value(), level)));
        }
    }

    public static void onCraftCancelled(TileFusionCraftingCore core) {
        if (core == assembling) {
            endedWhileAssembling = Ending.CANCELLED;
            return;
        }
        Level level = serverLevel(core);
        FusionReclaim pending = pending(core);
        if (level == null || pending == null) {
            return;
        }
        forget(core);
        core.setChanged();

        List<ItemStack> recovered = takeFromInjectors(core, pending.loaded());
        recovered.addAll(takeCatalyst(core, pending.catalyst()));
        returnItems(level, core, pending.direction(), recovered);
    }

    static boolean returnStranded(
            Level level, TileFusionCraftingCore core, IFusionInjector injector, Direction direction) {
        ItemStack stranded = injector.getInjectorStack();
        if (!handBack(level, core, stranded, direction, "an injector")) {
            return false;
        }
        injector.setInjectorStack(ItemStack.EMPTY);
        return true;
    }

    static boolean returnCatalyst(Level level, TileFusionCraftingCore core, Direction direction) {
        ItemStack stranded = core.getCatalystStack();
        if (!handBack(level, core, stranded, direction, "the catalyst slot")) {
            return false;
        }
        core.setCatalystStack(ItemStack.EMPTY);
        return true;
    }

    private static boolean handBack(
            Level level, TileFusionCraftingCore core, ItemStack stranded, Direction direction, String where) {
        if (stranded.isEmpty()) {
            return true;
        }
        IItemHandler target = ejectionTarget(level, core, direction);
        if (target == null
                || !ItemHandlerHelper.insertItem(target, stranded.copy(), true).isEmpty()) {
            return false;
        }
        ItemHandlerHelper.insertItem(target, stranded.copy(), false);
        if (NepConfig.debugLogging()) {
            Nep.LOGGER.info(
                    "Fusion crafting core {} handed {} back to the pattern provider; a craft that never ran had left"
                            + " it in {}, where it blocks every later push",
                    core.getBlockPos(),
                    stranded,
                    where);
        }
        return true;
    }

    @Nullable
    private static Level serverLevel(TileFusionCraftingCore core) {
        Level level = core.getLevel();
        return level == null || level.isClientSide() ? null : level;
    }

    @Nullable
    private static FusionReclaim pending(TileFusionCraftingCore core) {
        return core.getExistingData(NepDraconicContent.FUSION_RECLAIM.get()).orElse(null);
    }

    private static void returnItems(
            Level level, TileFusionCraftingCore core, Direction direction, List<ItemStack> recovered) {
        if (recovered.isEmpty()) {
            return;
        }
        IItemHandler target = ejectionTarget(level, core, direction);
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

    private static List<ItemStack> takeCatalyst(TileFusionCraftingCore core, List<GenericStack> wanted) {
        List<ItemStack> recovered = new ArrayList<>();
        for (GenericStack want : wanted) {
            ItemStack held = core.getCatalystStack();
            if (!(want.what() instanceof AEItemKey key) || held.isEmpty() || !key.matches(held)) {
                continue;
            }
            int take = Math.min((int) want.amount(), held.getCount());
            ItemStack kept = held.copy();
            kept.shrink(take);
            core.setCatalystStack(kept);
            recovered.add(key.toStack(take));
        }
        return recovered;
    }

    @Nullable
    static IItemHandler ejectionTarget(Level level, TileFusionCraftingCore core, Direction direction) {
        return level.getCapability(
                Capabilities.ItemHandler.BLOCK, core.getBlockPos().relative(direction), direction.getOpposite());
    }
}
