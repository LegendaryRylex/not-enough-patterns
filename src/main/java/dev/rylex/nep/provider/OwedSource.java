package dev.rylex.nep.provider;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.stacks.AEKeyType;
import appeng.parts.automation.StackWorldBehaviors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

public sealed interface OwedSource {

    String KIND_KEY = "kind";

    StackImportStrategy createStrategy(ServerLevel level, BlockPos providerPos);

    void writeToNBT(CompoundTag tag);

    static OwedSource readFromNBT(CompoundTag tag) {
        if (tag.contains(KIND_KEY)) {
            return new Machine(tag.getString(KIND_KEY), BlockPos.of(tag.getLong("machine")));
        }
        return new Side(Direction.from3DDataValue(tag.getByte("side")));
    }

    record Side(Direction side) implements OwedSource {

        @Override
        public StackImportStrategy createStrategy(ServerLevel level, BlockPos providerPos) {
            BlockPos from = providerPos.relative(side);
            Direction face = side.getOpposite();
            StackImportStrategy items =
                    StackWorldBehaviors.createImportFacade(level, from, face, type -> type == AEKeyType.items());
            StackImportStrategy fluids = new OwedFluidImport(level, from, face);
            return context -> items.transfer(context) | fluids.transfer(context);
        }

        @Override
        public void writeToNBT(CompoundTag tag) {
            tag.putByte("side", (byte) side.get3DDataValue());
        }
    }

    /** A machine somewhere else on the network, named by a kind a compat module registered an import route for. */
    record Machine(String kind, BlockPos pos) implements OwedSource {

        @Override
        public StackImportStrategy createStrategy(ServerLevel level, BlockPos providerPos) {
            return MachineImports.strategyFor(kind, level, pos);
        }

        @Override
        public void writeToNBT(CompoundTag tag) {
            tag.putString(KIND_KEY, kind);
            tag.putLong("machine", pos.asLong());
        }
    }
}
