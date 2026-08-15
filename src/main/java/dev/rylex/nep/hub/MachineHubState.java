package dev.rylex.nep.hub;

import dev.rylex.nep.Nep;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record MachineHubState(List<Entry> entries, boolean enabled, HubStatus status) implements CustomPacketPayload {

    public static final Type<MachineHubState> TYPE = new Type<>(Nep.id("machine_hub_state"));

    public enum Issue {
        OK,
        NO_INVENTORY,
        OUT_OF_RANGE
    }

    public record Entry(
            BlockPos pos,
            HubRole role,
            ItemStack icon,
            Issue issue,
            boolean items,
            boolean fluids,
            @Nullable Direction face,
            int priority,
            HubFilter insertFilter,
            HubFilter returnFilter) {

        static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.of(
                (buffer, entry) -> {
                    BlockPos.STREAM_CODEC.encode(buffer, entry.pos());
                    HubRole.STREAM_CODEC.encode(buffer, entry.role());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, entry.icon());
                    buffer.writeByte(entry.issue().ordinal());
                    buffer.writeBoolean(entry.items());
                    buffer.writeBoolean(entry.fluids());
                    buffer.writeByte(entry.face() == null ? -1 : entry.face().ordinal());
                    buffer.writeVarInt(entry.priority());
                    HubFilter.STREAM_CODEC.encode(buffer, entry.insertFilter());
                    HubFilter.STREAM_CODEC.encode(buffer, entry.returnFilter());
                },
                buffer -> new Entry(
                        BlockPos.STREAM_CODEC.decode(buffer),
                        HubRole.STREAM_CODEC.decode(buffer),
                        ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                        Issue.values()[buffer.readByte()],
                        buffer.readBoolean(),
                        buffer.readBoolean(),
                        HubLink.faceByOrdinal(buffer.readByte()),
                        buffer.readVarInt(),
                        HubFilter.STREAM_CODEC.decode(buffer),
                        HubFilter.STREAM_CODEC.decode(buffer)));

        public boolean healthy() {
            return issue == Issue.OK;
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineHubState> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            MachineHubState::entries,
            ByteBufCodecs.BOOL,
            MachineHubState::enabled,
            HubStatus.STREAM_CODEC,
            MachineHubState::status,
            MachineHubState::new);

    @Override
    public Type<MachineHubState> type() {
        return TYPE;
    }

    public static MachineHubState empty() {
        return new MachineHubState(List.of(), true, HubStatus.OK);
    }

    public boolean matches(MachineHubState other) {
        if (enabled != other.enabled || status != other.status || entries.size() != other.entries.size()) {
            return false;
        }
        for (int i = 0; i < entries.size(); i++) {
            Entry mine = entries.get(i);
            Entry theirs = other.entries.get(i);
            if (!mine.pos().equals(theirs.pos())
                    || mine.role() != theirs.role()
                    || mine.issue() != theirs.issue()
                    || mine.items() != theirs.items()
                    || mine.fluids() != theirs.fluids()
                    || mine.face() != theirs.face()
                    || mine.priority() != theirs.priority()
                    || !mine.insertFilter().equals(theirs.insertFilter())
                    || !mine.returnFilter().equals(theirs.returnFilter())
                    || !ItemStack.isSameItemSameComponents(mine.icon(), theirs.icon())) {
                return false;
            }
        }
        return true;
    }
}
