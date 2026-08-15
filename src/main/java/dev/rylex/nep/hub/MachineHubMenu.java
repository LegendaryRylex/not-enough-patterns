package dev.rylex.nep.hub;

import dev.rylex.nep.NepConfig;
import dev.rylex.nep.menu.MachineMenu;
import dev.rylex.nep.net.HubLinkEditPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class MachineHubMenu extends MachineMenu {

    public static final int BUTTON_SCAN = 1;
    public static final int BUTTON_CLEAR = 2;
    public static final int BUTTON_ROLE = 100;
    public static final int BUTTON_REMOVE = 200;

    public static final int WIDTH = 216;
    public static final int HEIGHT = 306;
    public static final int SHEET_HEIGHT = 320;
    public static final int PLAYER_INVENTORY_X = 27;
    public static final int PLAYER_INVENTORY_Y = 223;
    public static final int PLAYER_SLOT_X = PLAYER_INVENTORY_X + 1;
    public static final int PLAYER_SLOT_Y = PLAYER_INVENTORY_Y + 1;
    public static final int ROW_X = 9;
    public static final int ROW_Y = 34;
    public static final int ROW_HEIGHT = 14;
    public static final int ROW_WIDTH = 190;
    public static final int VISIBLE_ROWS = 12;
    public static final int SCROLL_X = 201;
    public static final int SCROLL_WIDTH = 6;
    public static final int SCROLL_HEIGHT = VISIBLE_ROWS * ROW_HEIGHT;

    private static final int STATE_INTERVAL = 5;

    @Nullable
    private final MachineHubBlockEntity hub;

    private final BlockPos pos;
    private final Player player;

    private MachineHubState lastState = MachineHubState.empty();
    private int stateCooldown;
    private boolean forceState = true;

    public MachineHubMenu(int windowId, Inventory playerInv, BlockPos pos) {
        super(NepContent.MACHINE_HUB_MENU.get(), windowId);
        this.pos = pos;
        this.player = playerInv.player;
        this.hub = MachineHubBlockEntity.at(playerInv.player.level(), pos);
        addPlayerInventory(playerInv, PLAYER_SLOT_X, PLAYER_SLOT_Y);
    }

    public BlockPos hubPos() {
        return pos;
    }

    public MachineHubState buildState() {
        if (hub == null || hub.getLevel() == null) {
            return MachineHubState.empty();
        }
        Level level = hub.getLevel();
        List<MachineHubState.Entry> entries = new ArrayList<>();
        for (HubLink link : hub.links()) {
            HubTarget target = hub.resolve(link);
            boolean items = target.items() != null;
            boolean fluids = target.fluids() != null;
            MachineHubState.Issue issue;
            if (!hub.withinRange(link.pos())) {
                issue = MachineHubState.Issue.OUT_OF_RANGE;
            } else if (!items && !fluids) {
                issue = MachineHubState.Issue.NO_INVENTORY;
            } else {
                issue = MachineHubState.Issue.OK;
            }
            entries.add(new MachineHubState.Entry(
                    link.pos(),
                    link.role(),
                    icon(level, link.pos()),
                    issue,
                    items,
                    fluids,
                    link.face(),
                    link.priority(),
                    link.insertFilter(),
                    link.returnFilter()));
        }
        return new MachineHubState(entries, NepConfig.machineHubEnabled(), hub.status());
    }

    private static ItemStack icon(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return ItemStack.EMPTY;
        }
        return level.getBlockState(pos).getBlock().asItem().getDefaultInstance();
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (hub == null || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (stateCooldown > 0) {
            stateCooldown--;
            return;
        }
        stateCooldown = STATE_INTERVAL - 1;
        MachineHubState current = buildState();
        if (forceState || !current.matches(lastState)) {
            forceState = false;
            lastState = current;
            PacketDistributor.sendToPlayer(serverPlayer, current);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (hub == null) {
            return false;
        }
        if (id == BUTTON_SCAN) {
            hub.applyPlan(hub.scan());
            resend();
            return true;
        }
        if (id == BUTTON_CLEAR) {
            hub.clearLinks();
            resend();
            return true;
        }
        if (id >= BUTTON_REMOVE) {
            hub.removeLink(id - BUTTON_REMOVE);
            resend();
            return true;
        }
        if (id >= BUTTON_ROLE) {
            hub.cycleRole(id - BUTTON_ROLE);
            resend();
            return true;
        }
        return false;
    }

    public void applyEdit(Player editor, HubLinkEditPayload payload) {
        if (hub == null || !stillValid(editor)) {
            return;
        }
        int index = payload.index();
        switch (payload.action()) {
            case SET_FACE -> hub.setFace(index, HubLink.faceByOrdinal(payload.value()));
            case SET_PRIORITY -> hub.setPriority(index, payload.value());
            case SET_INSERT_FILTER -> hub.setInsertFilter(index, payload.value(), payload.key());
            case SET_RETURN_FILTER -> hub.setReturnFilter(index, payload.value(), payload.key());
            case TOGGLE_INSERT_MODE -> hub.toggleInsertFilterMode(index);
            case TOGGLE_RETURN_MODE -> hub.toggleReturnFilterMode(index);
        }
        resend();
    }

    private void resend() {
        stateCooldown = 0;
        forceState = true;
    }

    @Override
    public boolean stillValid(Player player) {
        return withinReach(player, hub);
    }
}
