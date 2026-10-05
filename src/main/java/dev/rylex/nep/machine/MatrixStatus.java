package dev.rylex.nep.machine;

import java.util.function.ToIntFunction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public enum MatrixStatus implements StringRepresentable {
    IDLE("idle", 0),
    RUNNING("running", 7),
    STALLED("stalled", 3);

    /**
     * Shared by every Matrix block: {@code StateHolder} keys its value map by property identity, so a per-block
     * instance would not resolve.
     */
    public static final EnumProperty<MatrixStatus> PROPERTY = EnumProperty.create("status", MatrixStatus.class);

    public static final ToIntFunction<BlockState> LIGHT = state -> state.getValue(PROPERTY).light;

    private final String name;
    private final int light;

    MatrixStatus(String name, int light) {
        this.name = name;
        this.light = light;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
