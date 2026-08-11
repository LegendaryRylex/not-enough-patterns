package dev.rylex.nep.hub;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import com.mojang.serialization.Codec;
import dev.rylex.nep.NepConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

public class MachineHubBlockEntity extends BlockEntity implements MenuProvider {

    private static final Codec<List<HubLink>> LINKS_CODEC = HubLink.CODEC.listOf();
    private static final String LINKS_KEY = "links";

    private static final int RETURN_INTERVAL = 10;

    private final List<HubLink> links = new ArrayList<>();
    private final Map<BlockPos, BlockCapabilityCache<ResourceHandler<ItemResource>, Direction>> itemCaches =
            new HashMap<>();
    private final Map<BlockPos, BlockCapabilityCache<ResourceHandler<FluidResource>, Direction>> fluidCaches =
            new HashMap<>();
    private final MachineHubStorage storage = new MachineHubStorage(this);

    private HubStatus status = HubStatus.OK;
    private int returnCooldown;

    public MachineHubBlockEntity(BlockPos pos, BlockState state) {
        super(NepContent.MACHINE_HUB_BLOCK_ENTITY.get(), pos, state);
    }

    public MachineHubStorage storage() {
        return storage;
    }

    public HubStatus status() {
        return status;
    }

    public List<HubLink> links() {
        return List.copyOf(links);
    }

    public boolean routing() {
        return NepConfig.machineHubEnabled() && !links.isEmpty();
    }

    public List<HubTarget> targets() {
        if (!NepConfig.machineHubEnabled() || !(level instanceof ServerLevel)) {
            return List.of();
        }
        List<HubTarget> resolved = new ArrayList<>(links.size());
        for (HubLink link : links) {
            HubTarget target = resolve(link);
            if (!target.isEmpty()) {
                resolved.add(target);
            }
        }
        return resolved;
    }

    public void serverTick(ServerLevel level) {
        if (returnCooldown > 0) {
            returnCooldown--;
            return;
        }
        returnCooldown = RETURN_INTERVAL - 1;
        status = returnOutputs(level);
    }

    private HubStatus returnOutputs(ServerLevel level) {
        List<HubTarget> targets = targets();
        boolean returns = false;
        for (HubTarget target : targets) {
            returns |= !target.accepts();
        }
        if (!returns) {
            return HubStatus.OK;
        }
        IGridNode node = HubNetwork.adjacentNode(level, worldPosition);
        if (node == null) {
            return HubStatus.NO_NETWORK;
        }
        IGrid grid = node.getGrid();
        if (!node.isActive() || grid == null) {
            return HubStatus.OFFLINE;
        }
        HubReturn.Outcome outcome =
                HubReturn.push(targets, grid.getStorageService().getInventory(), IActionSource.empty());
        return outcome.refused() ? HubStatus.FULL : HubStatus.OK;
    }

    public HubTarget resolve(HubLink link) {
        return new HubTarget(link.pos(), link.role(), itemHandler(link.pos()), fluidHandler(link.pos()));
    }

    @Nullable
    private ResourceHandler<ItemResource> itemHandler(BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return itemCaches
                .computeIfAbsent(
                        pos,
                        at -> BlockCapabilityCache.create(
                                Capabilities.Item.BLOCK, serverLevel, at, null, this::stillLinked, () -> {}))
                .getCapability();
    }

    @Nullable
    private ResourceHandler<FluidResource> fluidHandler(BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return fluidCaches
                .computeIfAbsent(
                        pos,
                        at -> BlockCapabilityCache.create(
                                Capabilities.Fluid.BLOCK, serverLevel, at, null, this::stillLinked, () -> {}))
                .getCapability();
    }

    public boolean canLink(BlockPos pos) {
        if (level == null || pos.equals(worldPosition)) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof MachineHubBlockEntity || HubScan.networkBlock(level, pos)) {
            return false;
        }
        return withinRange(pos);
    }

    public boolean withinRange(BlockPos pos) {
        int range = NepConfig.machineHubLinkRange();
        int dx = Math.abs(pos.getX() - worldPosition.getX());
        int dy = Math.abs(pos.getY() - worldPosition.getY());
        int dz = Math.abs(pos.getZ() - worldPosition.getZ());
        return Math.max(dx, Math.max(dy, dz)) <= range;
    }

    public int applyPlan(List<HubLink> plan) {
        List<HubLink> accepted = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        int limit = NepConfig.machineHubMaximumLinks();
        for (HubLink link : plan) {
            if (accepted.size() >= limit || !seen.add(link.pos()) || !canLink(link.pos())) {
                continue;
            }
            accepted.add(link);
        }
        setLinks(accepted);
        return accepted.size();
    }

    public List<HubLink> scan() {
        if (level == null) {
            return List.of();
        }
        return HubScan.propose(
                level,
                worldPosition,
                NepConfig.machineHubLinkRange(),
                NepConfig.machineHubScanBudget(),
                NepConfig.machineHubCasingDepth(),
                NepConfig.machineHubMaximumLinks());
    }

    public void clearLinks() {
        setLinks(List.of());
    }

    public void cycleRole(int index) {
        if (index < 0 || index >= links.size()) {
            return;
        }
        List<HubLink> updated = new ArrayList<>(links);
        updated.set(index, updated.get(index).withRole(updated.get(index).role().next()));
        setLinks(updated);
    }

    public void removeLink(int index) {
        if (index < 0 || index >= links.size()) {
            return;
        }
        List<HubLink> updated = new ArrayList<>(links);
        updated.remove(index);
        setLinks(updated);
    }

    private boolean stillLinked() {
        return !isRemoved();
    }

    private void setLinks(List<HubLink> updated) {
        links.clear();
        links.addAll(updated);
        forgetUnlinkedCaches();
        setChanged();
    }

    private void forgetUnlinkedCaches() {
        Set<BlockPos> linked = new HashSet<>();
        for (HubLink link : links) {
            linked.add(link.pos());
        }
        itemCaches.keySet().retainAll(linked);
        fluidCaches.keySet().retainAll(linked);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        itemCaches.clear();
        fluidCaches.clear();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(LINKS_KEY, LINKS_CODEC, List.copyOf(links));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        links.clear();
        itemCaches.clear();
        fluidCaches.clear();
        input.read(LINKS_KEY, LINKS_CODEC).ifPresent(links::addAll);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new MachineHubMenu(windowId, inventory, worldPosition);
    }

    @Nullable
    public static MachineHubBlockEntity at(@Nullable Level level, BlockPos pos) {
        return level != null && level.getBlockEntity(pos) instanceof MachineHubBlockEntity hub ? hub : null;
    }
}
