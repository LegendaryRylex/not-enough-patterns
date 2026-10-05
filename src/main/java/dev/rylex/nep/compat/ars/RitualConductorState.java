package dev.rylex.nep.compat.ars;

import dev.rylex.nep.Nep;
import dev.rylex.nep.compat.ars.RitualConductorBlockEntity.ConductorState;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record RitualConductorState(
        ConductorState state,
        Optional<ResourceLocation> ritual,
        Optional<BlockPos> brazier,
        List<ItemStack> augments,
        RitualWeather weather,
        RitualDaylight daylight,
        boolean collectOutput,
        int runInterval,
        int secondsUntilNextRun,
        boolean enabled,
        boolean powered)
        implements CustomPacketPayload {

    public static final Type<RitualConductorState> TYPE = new Type<>(Nep.id("ritual_conductor_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RitualConductorState> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeByte(value.state().ordinal());
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buffer, value.ritual());
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC).encode(buffer, value.brazier());
                ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buffer, value.augments());
                RitualWeather.STREAM_CODEC.encode(buffer, value.weather());
                RitualDaylight.STREAM_CODEC.encode(buffer, value.daylight());
                buffer.writeBoolean(value.collectOutput());
                buffer.writeVarInt(value.runInterval());
                buffer.writeVarInt(value.secondsUntilNextRun());
                buffer.writeBoolean(value.enabled());
                buffer.writeBoolean(value.powered());
            },
            buffer -> new RitualConductorState(
                    ConductorState.values()[buffer.readByte()],
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buffer),
                    ByteBufCodecs.optional(BlockPos.STREAM_CODEC).decode(buffer),
                    ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(buffer),
                    RitualWeather.STREAM_CODEC.decode(buffer),
                    RitualDaylight.STREAM_CODEC.decode(buffer),
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readBoolean()));

    @Override
    public Type<RitualConductorState> type() {
        return TYPE;
    }

    public static RitualConductorState empty() {
        return new RitualConductorState(
                ConductorState.UNCONFIGURED,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                RitualWeather.ANY,
                RitualDaylight.ANY,
                true,
                0,
                0,
                true,
                false);
    }

    public boolean matches(RitualConductorState other) {
        if (state != other.state
                || !ritual.equals(other.ritual)
                || !brazier.equals(other.brazier)
                || weather != other.weather
                || daylight != other.daylight
                || collectOutput != other.collectOutput
                || runInterval != other.runInterval
                || secondsUntilNextRun != other.secondsUntilNextRun
                || enabled != other.enabled
                || powered != other.powered
                || augments.size() != other.augments.size()) {
            return false;
        }
        for (int index = 0; index < augments.size(); index++) {
            if (!ItemStack.isSameItemSameComponents(augments.get(index), other.augments.get(index))) {
                return false;
            }
        }
        return true;
    }
}
