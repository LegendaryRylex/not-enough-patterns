package dev.rylex.nep.provider;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.stacks.AEKeyType;
import appeng.parts.automation.StackWorldBehaviors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

public sealed interface OwedSource {

    StackImportStrategy createStrategy(ServerLevel level, BlockPos providerPos);

    void writeToNBT(CompoundTag tag);

    static OwedSource readFromNBT(CompoundTag tag) {
        return new Side(Direction.from3DDataValue(tag.getByte("side")));
    }

    record Side(Direction side) implements OwedSource {

        @Override
        public StackImportStrategy createStrategy(ServerLevel level, BlockPos providerPos) {
            return StackWorldBehaviors.createImportFacade(
                    level, providerPos.relative(side), side.getOpposite(), type -> type == AEKeyType.items());
        }

        @Override
        public void writeToNBT(CompoundTag tag) {
            tag.putByte("side", (byte) side.get3DDataValue());
        }
    }
}
