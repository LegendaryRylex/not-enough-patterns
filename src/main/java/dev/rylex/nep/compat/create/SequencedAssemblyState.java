package dev.rylex.nep.compat.create;

import appeng.api.stacks.GenericStack;
import dev.rylex.nep.Nep;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public record SequencedAssemblyState(
        int status,
        boolean halted,
        boolean outputBlocked,
        List<Station> stations,
        List<Making> making,
        List<GenericStack> missing,
        List<FluidStack> fluids)
        implements CustomPacketPayload {

    public static final Type<SequencedAssemblyState> TYPE = new Type<>(Nep.id("sequenced_assembly_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SequencedAssemblyState> STREAM_CODEC =
            StreamCodec.of(SequencedAssemblyState::encode, SequencedAssemblyState::decode);

    public enum Issue {
        OK,
        UNRECOGNIZED,
        WRONG_FACING,
        UNPOWERED
    }

    public record Station(StationKind kind, BlockPos pos, Issue issue, StationKind expected) {
        public boolean healthy() {
            return issue == Issue.OK;
        }
    }

    public record Making(ItemStack output, long count) {}

    @Override
    public Type<SequencedAssemblyState> type() {
        return TYPE;
    }

    static SequencedAssemblyState empty() {
        return new SequencedAssemblyState(0, false, false, List.of(), List.of(), List.of(), List.of());
    }

    boolean matches(SequencedAssemblyState other) {
        if (status != other.status
                || halted != other.halted
                || outputBlocked != other.outputBlocked
                || making.size() != other.making.size()
                || fluids.size() != other.fluids.size()
                || !stations.equals(other.stations)
                || !missing.equals(other.missing)) {
            return false;
        }
        for (int i = 0; i < making.size(); i++) {
            Making a = making.get(i);
            Making b = other.making.get(i);
            if (a.count() != b.count()
                    || a.output().getCount() != b.output().getCount()
                    || !ItemStack.isSameItemSameComponents(a.output(), b.output())) {
                return false;
            }
        }
        for (int i = 0; i < fluids.size(); i++) {
            FluidStack a = fluids.get(i);
            FluidStack b = other.fluids.get(i);
            if (a.getAmount() != b.getAmount() || !FluidStack.isSameFluidSameComponents(a, b)) {
                return false;
            }
        }
        return true;
    }

    private static void encode(RegistryFriendlyByteBuf buf, SequencedAssemblyState state) {
        buf.writeVarInt(state.status);
        buf.writeBoolean(state.halted);
        buf.writeBoolean(state.outputBlocked);
        buf.writeVarInt(state.stations.size());
        for (Station station : state.stations) {
            buf.writeVarInt(station.kind().ordinal());
            buf.writeBlockPos(station.pos());
            buf.writeVarInt(station.issue().ordinal());
            buf.writeVarInt(station.expected().ordinal());
        }
        buf.writeVarInt(state.making.size());
        for (Making entry : state.making) {
            ItemStack.STREAM_CODEC.encode(buf, entry.output());
            buf.writeVarLong(entry.count());
        }
        buf.writeVarInt(state.missing.size());
        for (GenericStack stack : state.missing) {
            GenericStack.writeBuffer(stack, buf);
        }
        buf.writeVarInt(state.fluids.size());
        for (FluidStack stack : state.fluids) {
            FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    }

    private static SequencedAssemblyState decode(RegistryFriendlyByteBuf buf) {
        int status = buf.readVarInt();
        boolean halted = buf.readBoolean();
        boolean outputBlocked = buf.readBoolean();
        int stationCount = buf.readVarInt();
        List<Station> stations = new ArrayList<>(stationCount);
        for (int i = 0; i < stationCount; i++) {
            StationKind kind = kind(buf.readVarInt());
            BlockPos pos = buf.readBlockPos();
            Issue issue = issue(buf.readVarInt());
            StationKind expected = kind(buf.readVarInt());
            stations.add(new Station(kind, pos, issue, expected));
        }
        int makingCount = buf.readVarInt();
        List<Making> making = new ArrayList<>(makingCount);
        for (int i = 0; i < makingCount; i++) {
            ItemStack output = ItemStack.STREAM_CODEC.decode(buf);
            making.add(new Making(output, buf.readVarLong()));
        }
        int missingCount = buf.readVarInt();
        List<GenericStack> missing = new ArrayList<>(missingCount);
        for (int i = 0; i < missingCount; i++) {
            GenericStack stack = GenericStack.readBuffer(buf);
            if (stack != null) {
                missing.add(stack);
            }
        }
        int fluidCount = buf.readVarInt();
        List<FluidStack> fluids = new ArrayList<>(fluidCount);
        for (int i = 0; i < fluidCount; i++) {
            fluids.add(FluidStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return new SequencedAssemblyState(status, halted, outputBlocked, stations, making, missing, fluids);
    }

    private static StationKind kind(int ordinal) {
        StationKind[] values = StationKind.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : StationKind.UNKNOWN;
    }

    private static Issue issue(int ordinal) {
        Issue[] values = Issue.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : Issue.OK;
    }
}
