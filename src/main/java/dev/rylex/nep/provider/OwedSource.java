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
            BlockPos from = providerPos.relative(side);
            Direction face = side.getOpposite();
            StackImportStrategy items =
                    StackWorldBehaviors.createImportFacade(level, from, face, type -> type == AEKeyType.items());
            StackImportStrategy fluids = new OwedFluidImport(level, from, face);
            return context -> items.transfer(context) | fluids.transfer(context);
        }

        @Override
        public void writeToNBT(ValueOutput output) {
            output.putByte("side", (byte) side.get3DDataValue());
        }
    }
}
