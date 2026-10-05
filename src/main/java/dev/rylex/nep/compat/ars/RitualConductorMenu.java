package dev.rylex.nep.compat.ars;

import dev.rylex.nep.NepConfig;
import dev.rylex.nep.menu.MachineMenu;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class RitualConductorMenu extends MachineMenu {

    public static final int BUTTON_CLEAR = 1;
    public static final int BUTTON_WEATHER = 2;
    public static final int BUTTON_DAYLIGHT = 3;
    public static final int BUTTON_COLLECT = 4;
    public static final int BUTTON_INTERVAL_DOWN = 5;
    public static final int BUTTON_INTERVAL_UP = 6;
    public static final int BUTTON_INTERVAL_DOWN_COARSE = 7;
    public static final int BUTTON_INTERVAL_UP_COARSE = 8;
    public static final int BUTTON_WEATHER_BACK = 9;
    public static final int BUTTON_DAYLIGHT_BACK = 10;
    public static final int BUTTON_SET_AUGMENT = 200;

    public static final int FINE_STEP = 5;
    public static final int COARSE_STEP = 60;

    public static final int WIDTH = 256;
    public static final int HEIGHT = 264;

    public static final int HEADER_Y = 22;

    public static final int ROW_X = 9;
    public static final int ROW_Y = 36;
    public static final int ROW_HEIGHT = 14;
    public static final int VISIBLE_ROWS = 9;
    public static final int ROW_WIDTH = 114;
    public static final int SCROLL_X = 125;
    public static final int SCROLL_WIDTH = 4;
    public static final int SCROLL_HEIGHT = VISIBLE_ROWS * ROW_HEIGHT;

    public static final int PANE_X = 138;
    public static final int PANE_RIGHT = 247;
    public static final int CARD_Y = 37;
    public static final int SETTING_Y = 62;
    public static final int SETTING_HEIGHT = 12;

    public static final int AUGMENT_LABEL_Y = 114;
    public static final int AUGMENT_X = 138;
    public static final int AUGMENT_Y = 124;
    public static final int AUGMENT_SIZE = 18;
    public static final int AUGMENT_COLUMNS = 6;

    public static final int INVENTORY_X = 47;
    public static final int INVENTORY_Y = 182;

    private static final int STATE_INTERVAL = 5;

    @Nullable
    private final RitualConductorBlockEntity conductor;

    private final BlockPos pos;
    private final Player player;

    private RitualConductorState lastState = RitualConductorState.empty();
    private int stateCooldown;
    private boolean forceState = true;

    public RitualConductorMenu(int windowId, Inventory playerInv, BlockPos pos) {
        super(NepArsContent.RITUAL_CONDUCTOR_MENU.get(), windowId);
        this.pos = pos;
        this.player = playerInv.player;
        this.conductor =
                playerInv.player.level().getBlockEntity(pos) instanceof RitualConductorBlockEntity found ? found : null;
        addPlayerInventory(playerInv, INVENTORY_X, INVENTORY_Y);
    }

    public RitualConductorState buildState() {
        if (conductor == null || conductor.getLevel() == null) {
            return RitualConductorState.empty();
        }
        Level level = conductor.getLevel();
        return new RitualConductorState(
                conductor.state(),
                Optional.ofNullable(conductor.ritual()),
                Optional.ofNullable(conductor.brazierPos()),
                conductor.augments(),
                conductor.weather(),
                conductor.daylight(),
                conductor.collectOutput(),
                conductor.runInterval(),
                conductor.secondsUntilNextRun(),
                NepConfig.arsRitualConductor(),
                level.hasNeighborSignal(pos));
    }

    public void selectRitual(@Nullable ResourceLocation selected) {
        if (conductor == null) {
            return;
        }
        conductor.setRitual(selected);
        resend();
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (conductor == null || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (stateCooldown > 0) {
            stateCooldown--;
            return;
        }
        stateCooldown = STATE_INTERVAL - 1;
        RitualConductorState current = buildState();
        if (forceState || !current.matches(lastState)) {
            forceState = false;
            lastState = current;
            PacketDistributor.sendToPlayer(serverPlayer, current);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (conductor == null) {
            return false;
        }
        switch (id) {
            case BUTTON_CLEAR -> conductor.clearConfiguration();
            case BUTTON_WEATHER -> conductor.cycleWeather();
            case BUTTON_DAYLIGHT -> conductor.cycleDaylight();
            case BUTTON_WEATHER_BACK -> conductor.cycleWeatherBack();
            case BUTTON_DAYLIGHT_BACK -> conductor.cycleDaylightBack();
            case BUTTON_COLLECT -> conductor.toggleCollectOutput();
            case BUTTON_INTERVAL_DOWN -> conductor.adjustRunInterval(-FINE_STEP);
            case BUTTON_INTERVAL_UP -> conductor.adjustRunInterval(FINE_STEP);
            case BUTTON_INTERVAL_DOWN_COARSE -> conductor.adjustRunInterval(-COARSE_STEP);
            case BUTTON_INTERVAL_UP_COARSE -> conductor.adjustRunInterval(COARSE_STEP);
            default -> {
                if (id < BUTTON_SET_AUGMENT) {
                    return false;
                }
                conductor.setAugment(id - BUTTON_SET_AUGMENT, getCarried());
            }
        }
        resend();
        return true;
    }

    private void resend() {
        stateCooldown = 0;
        forceState = true;
    }

    @Override
    public boolean stillValid(Player player) {
        return withinReach(player, conductor);
    }
}
