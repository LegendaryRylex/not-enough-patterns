package dev.rylex.nep.hub;

import appeng.api.networking.IGrid;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import com.mojang.serialization.Codec;
import dev.rylex.nep.NepConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
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

    private record Side(BlockPos pos, @Nullable Direction face) {}

    private final List<HubLink> links = new ArrayList<>();
    private final Map<Side, BlockCapabilityCache<ResourceHandler<ItemResource>, Direction>> itemCaches =
            new HashMap<>();
    private final Map<Side, BlockCapabilityCache<ResourceHandler<FluidResource>, Direction>> fluidCaches =
            new HashMap<>();
    private final MachineHubStorage storage = new MachineHubStorage(this);

    private HubGridNode node = new HubGridNode(this);

    private HubStatus status = HubStatus.OK;
    private int returnCooldown;

    public MachineHubBlockEntity(BlockPos pos, BlockState state) {
        super(NepContent.MACHINE_HUB_BLOCK_ENTITY.get(), pos, state);
    }

    public IInWorldGridNodeHost gridNodeHost() {
        return node;
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
        return NepConfig.machineHubEnabled() && !links.isEmpty() && node.isActive();
    }

    public List<HubTarget> targets() {
        if (!routing() || !(level instanceof ServerLevel)) {
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
        node.create(level, worldPosition);
        if (returnCooldown > 0) {
            returnCooldown--;
            return;
        }
        returnCooldown = RETURN_INTERVAL - 1;
        status = returnOutputs();
    }

    private HubStatus returnOutputs() {
        IGrid grid = node.grid();
        if (grid == null || !node.connected()) {
            return HubStatus.NO_NETWORK;
        }
        if (node.missingChannel()) {
            return HubStatus.NO_CHANNEL;
        }
        if (!node.isActive()) {
            return HubStatus.OFFLINE;
        }
        List<HubTarget> targets = targets();
        boolean returns = false;
        for (HubTarget target : targets) {
            returns |= target.provides();
        }
        if (!returns) {
            return HubStatus.OK;
        }
        HubReturn.Outcome outcome =
                HubReturn.push(targets, grid.getStorageService().getInventory(), IActionSource.empty());
        return outcome.refused() ? HubStatus.FULL : HubStatus.OK;
    }

    public HubTarget resolve(HubLink link) {
        Side side = new Side(link.pos(), link.face());
        return new HubTarget(link, itemHandler(side), fluidHandler(side));
    }

    @Nullable
    private ResourceHandler<ItemResource> itemHandler(Side side) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return itemCaches
                .computeIfAbsent(
                        side,
                        at -> BlockCapabilityCache.create(
                                Capabilities.Item.BLOCK, serverLevel, at.pos(), at.face(), this::stillLinked, () -> {}))
                .getCapability();
    }

    @Nullable
    private ResourceHandler<FluidResource> fluidHandler(Side side) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return fluidCaches
                .computeIfAbsent(
                        side,
                        at -> BlockCapabilityCache.create(
                                Capabilities.Fluid.BLOCK,
                                serverLevel,
                                at.pos(),
                                at.face(),
                                this::stillLinked,
                                () -> {}))
                .getCapability();
    }

    public boolean canLink(BlockPos pos) {
        if (level == null || pos.equals(worldPosition)) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof MachineHubBlockEntity || HubScan.networkBlock(level, pos)) {
            return false;
        }
        if (HubRules.load().unlinkable(level.getBlockState(pos))) {
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
        Map<BlockPos, HubLink> existing = new HashMap<>();
        for (HubLink link : links) {
            existing.put(link.pos(), link);
        }
        List<HubLink> accepted = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        int limit = NepConfig.machineHubMaximumLinks();
        for (HubLink link : plan) {
            if (accepted.size() >= limit || !seen.add(link.pos()) || !canLink(link.pos())) {
                continue;
            }
            accepted.add(carryOver(existing.get(link.pos()), link));
        }
        setLinks(accepted);
        return accepted.size();
    }

    private static HubLink carryOver(@Nullable HubLink previous, HubLink proposed) {
        if (previous == null) {
            return proposed;
        }
        return proposed.withFace(previous.face())
                .withPriority(previous.priority())
                .withInsertFilter(previous.insertFilter())
                .withReturnFilter(previous.returnFilter());
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
        editLink(index, link -> link.withRole(link.role().next()));
    }

    public void setFace(int index, @Nullable Direction face) {
        editLink(index, link -> link.withFace(face));
    }

    public void setPriority(int index, int priority) {
        editLink(index, link -> link.withPriority(priority));
    }

    public void setInsertFilter(int index, int slot, @Nullable AEKey key) {
        editLink(index, link -> link.withInsertFilter(link.insertFilter().withKey(slot, key)));
    }

    public void setReturnFilter(int index, int slot, @Nullable AEKey key) {
        editLink(index, link -> link.withReturnFilter(link.returnFilter().withKey(slot, key)));
    }

    public void toggleInsertFilterMode(int index) {
        editLink(index, link -> link.withInsertFilter(link.insertFilter().toggled()));
    }

    public void toggleReturnFilterMode(int index) {
        editLink(index, link -> link.withReturnFilter(link.returnFilter().toggled()));
    }

    private void editLink(int index, UnaryOperator<HubLink> edit) {
        if (index < 0 || index >= links.size()) {
            return;
        }
        List<HubLink> updated = new ArrayList<>(links);
        updated.set(index, edit.apply(updated.get(index)));
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
        int before = node.channelDemand();
        links.clear();
        links.addAll(updated);
        forgetUnlinkedCaches();
        setChanged();
        int after = node.channelDemand();
        if (before == after) {
            return;
        }
        if (before == 0 || after == 0) {
            rebuildNode();
        } else {
            node.repath();
        }
    }

    private void rebuildNode() {
        if (level == null) {
            node.destroy();
            node = new HubGridNode(this);
            return;
        }
        TagValueOutput carried = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        node.save(carried);
        node.destroy();
        node = new HubGridNode(this);
        node.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), carried.buildResult()));
        node.create(level, worldPosition);
    }

    private void forgetUnlinkedCaches() {
        Set<Side> linked = new HashSet<>();
        for (HubLink link : links) {
            linked.add(new Side(link.pos(), link.face()));
        }
        itemCaches.keySet().retainAll(linked);
        fluidCaches.keySet().retainAll(linked);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        node.destroy();
        itemCaches.clear();
        fluidCaches.clear();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        node.destroy();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        node.save(output);
        output.store(LINKS_KEY, LINKS_CODEC, List.copyOf(links));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        node.load(input);
        links.clear();
        itemCaches.clear();
        fluidCaches.clear();
        input.read(LINKS_KEY, LINKS_CODEC).ifPresent(links::addAll);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        List<HubLink> plan = input.getOrDefault(NepContent.HUB_PLAN.get(), List.<HubLink>of());
        if (!plan.isEmpty() && level instanceof ServerLevel) {
            applyPlan(plan);
        }
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
