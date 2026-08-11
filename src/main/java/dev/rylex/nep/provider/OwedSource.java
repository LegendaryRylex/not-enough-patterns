package dev.rylex.nep.provider;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.stacks.AEKeyType;
import appeng.parts.automation.StackWorldBehaviors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public sealed interface OwedSource {

    StackImportStrategy createStrategy(ServerLevel level, BlockPos providerPos);

    void writeToNBT(ValueOutput output);

    static OwedSource readFromNBT(ValueInput input) {
        return new Side(Direction.from3DDataValue(input.getByteOr("side", (byte) 0)));
    }

    record Side(Direction side) implements OwedSource {

        @Override
        public StackImportStrategy createStrategy(ServerLevel level, BlockPos providerPos) {
            return StackWorldBehaviors.createImportFacade(
                    level, providerPos.relative(side), side.getOpposite(), type -> type == AEKeyType.items());
        }

        @Override
        public void writeToNBT(ValueOutput output) {
            output.putByte("side", (byte) side.get3DDataValue());
        }
    }
}
