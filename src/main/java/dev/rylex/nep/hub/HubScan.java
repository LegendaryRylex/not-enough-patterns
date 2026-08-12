package dev.rylex.nep.hub;

import appeng.api.AECapabilities;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

public final class HubScan {

    private HubScan() {}

    private static final String VANILLA = "minecraft";
    private static final ItemResource PROBE = ItemResource.of(Items.STONE);

    public static List<HubLink> propose(
            Level level, BlockPos origin, int reach, int budget, int casingDepth, int limit) {
        List<HubLink> proposed = new ArrayList<>();
        Walk walk = new Walk(level, origin, reach, casingDepth);

        for (Direction side : Direction.values()) {
            walk.consider(origin.relative(side), 0, true);
        }

        int examined = 0;
        while (!walk.frontier.isEmpty() && examined < budget && proposed.size() < limit) {
            Step step = walk.frontier.poll();
            examined++;
            if (linkable(level, step.pos())) {
                proposed.add(new HubLink(step.pos(), roleFor(items(level, step.pos()))));
            }
            for (Direction side : Direction.values()) {
                walk.consider(step.pos().relative(side), step.casing(), false);
            }
        }
        return proposed;
    }

    private record Step(BlockPos pos, int casing) {}

    private static final class Walk {

        private final Level level;
        private final BlockPos origin;
        private final int reach;
        private final int casingDepth;
        private final Set<BlockPos> seen = new HashSet<>();
        private final Set<String> machineMods = new HashSet<>();
        private final Deque<Step> frontier = new ArrayDeque<>();

        private Walk(Level level, BlockPos origin, int reach, int casingDepth) {
            this.level = level;
            this.origin = origin;
            this.reach = reach;
            this.casingDepth = casingDepth;
            seen.add(origin);
        }

        private void consider(BlockPos pos, int casingSoFar, boolean touchingTheHub) {
            BlockPos at = pos.immutable();
            if (!seen.add(at) || beyond(origin, at, reach) || !level.isLoaded(at)) {
                return;
            }
            BlockState state = level.getBlockState(at);
            if (state.isAir() || state.liquid()) {
                return;
            }
            BlockEntity be = level.getBlockEntity(at);
            if (be instanceof MachineHubBlockEntity || networkBlock(level, at)) {
                return;
            }

            String mod = namespace(state);
            boolean vanilla = VANILLA.equals(mod);
            if (machinePart(level, at, be, vanilla)) {
                if (!vanilla) {
                    machineMods.add(mod);
                }
                frontier.add(new Step(at, 0));
                return;
            }
            if (vanilla) {
                return;
            }
            int casing = casingSoFar + 1;
            if (casing > casingDepth) {
                return;
            }
            if (touchingTheHub) {
                machineMods.add(mod);
            } else if (!machineMods.contains(mod)) {
                return;
            }
            frontier.add(new Step(at, casing));
        }
    }

    private static boolean machinePart(Level level, BlockPos pos, @Nullable BlockEntity be, boolean vanilla) {
        if (be == null) {
            return false;
        }
        return !vanilla || items(level, pos) != null || fluids(level, pos) != null;
    }

    private static boolean beyond(BlockPos origin, BlockPos pos, int reach) {
        int dx = Math.abs(pos.getX() - origin.getX());
        int dy = Math.abs(pos.getY() - origin.getY());
        int dz = Math.abs(pos.getZ() - origin.getZ());
        return Math.max(dx, Math.max(dy, dz)) > reach;
    }

    private static String namespace(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id == null ? VANILLA : id.getNamespace();
    }

    public static boolean networkBlock(Level level, BlockPos pos) {
        return level.getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null) != null;
    }

    public static boolean linkable(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be instanceof MachineHubBlockEntity || networkBlock(level, pos)) {
            return false;
        }
        ResourceHandler<ItemResource> items = items(level, pos);
        if (items != null && items.size() > 0) {
            return true;
        }
        ResourceHandler<FluidResource> fluids = fluids(level, pos);
        return fluids != null && fluids.size() > 0;
    }

    @Nullable
    public static ResourceHandler<ItemResource> items(Level level, BlockPos pos) {
        return level.getCapability(Capabilities.Item.BLOCK, pos, null);
    }

    @Nullable
    public static ResourceHandler<FluidResource> fluids(Level level, BlockPos pos) {
        return level.getCapability(Capabilities.Fluid.BLOCK, pos, null);
    }

    public static HubRole roleFor(@Nullable ResourceHandler<ItemResource> handler) {
        if (handler == null || handler.size() == 0) {
            return HubRole.INPUT;
        }
        try (Transaction probing = Transaction.openRoot()) {
            for (int slot = 0; slot < handler.size(); slot++) {
                ItemResource held = handler.getResource(slot);
                ItemResource probe = held.isEmpty() ? PROBE : held;
                if (handler.isValid(slot, probe) || handler.insert(slot, probe, 1, probing) > 0) {
                    return HubRole.INPUT;
                }
            }
        }
        return HubRole.OUTPUT;
    }
}
