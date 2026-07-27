package dev.rylex.nep.compat.create;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.pattern.GridPos;
import dev.rylex.nep.pattern.MechanicalCraftingPattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

final class PatternEncodingHandler {
    private PatternEncodingHandler() {}

    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!NepConfig.createMechanicalCrafting()) {
            return;
        }
        if (event.getEntity() instanceof FakePlayer || !event.getEntity().isShiftKeyDown()) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (!held.is(AEItems.BLANK_PATTERN.asItem())) {
            return;
        }
        Level level = event.getLevel();
        if (!(level.getBlockEntity(event.getPos()) instanceof MechanicalCrafterBlockEntity crafter)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        if (level.isClientSide()) {
            return;
        }

        Player player = event.getEntity();
        CrafterChains.Chain chain = CrafterChains.of(crafter);
        if (chain == null) {
            message(player, "invalid_chain");
            return;
        }

        List<MechanicalCrafterBlockEntity> filled = new ArrayList<>();
        for (MechanicalCrafterBlockEntity c : chain.crafters()) {
            if (CrafterChains.isCovered(c)) {
                continue;
            }
            if (!c.craftingItemPresent()) {
                message(player, "incomplete_grid");
                return;
            }
            filled.add(c);
        }
        if (filled.isEmpty()) {
            message(player, "incomplete_grid");
            return;
        }

        ItemStack result = tryCraft(level, chain);
        if (result == null || result.isEmpty()) {
            message(player, "no_recipe");
            return;
        }

        List<GridPos> filledPositions =
                filled.stream().map(chain.positions()::get).toList();
        GridPos.Bounds bounds = GridPos.bounds(filledPositions);
        List<GenericStack> cells =
                new ArrayList<>(Collections.nCopies(bounds.width() * bounds.height(), (GenericStack) null));
        for (int i = 0; i < filled.size(); i++) {
            ItemStack ingredient = filled.get(i).getInventory().getItem(0);
            cells.set(bounds.rowMajorIndex(filledPositions.get(i)), new GenericStack(AEItemKey.of(ingredient), 1));
        }

        ItemStack pattern = MechanicalCraftingPattern.encode(
                bounds.width(), bounds.height(), cells, GenericStack.fromItemStack(result));
        held.shrink(1);
        player.getInventory().placeItemBackInInventory(pattern);
        message(player, "success", result.getHoverName());
    }

    @Nullable
    private static ItemStack tryCraft(Level level, CrafterChains.Chain chain) {
        Map<MechanicalCrafterBlockEntity, GroupedItems> grids = new HashMap<>();
        for (MechanicalCrafterBlockEntity c : chain.crafters()) {
            grids.put(c, new GroupedItems(c.getInventory().getItem(0).copy()));
        }
        List<MechanicalCrafterBlockEntity> deepestFirst = new ArrayList<>(chain.crafters());
        deepestFirst.sort(Comparator.comparing(chain.depths()::get).reversed());
        for (MechanicalCrafterBlockEntity c : deepestFirst) {
            MechanicalCrafterBlockEntity target = RecipeGridHandler.getTargetingCrafter(c);
            if (target != null) {
                grids.get(c).mergeOnto(grids.get(target), CrafterChains.pointing(c));
            }
        }
        return RecipeGridHandler.tryToApplyRecipe(level, grids.get(chain.output()));
    }

    private static void message(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("nep.encoding." + key, args), true);
    }
}
