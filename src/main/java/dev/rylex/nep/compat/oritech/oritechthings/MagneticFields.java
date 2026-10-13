package dev.rylex.nep.compat.oritech.oritechthings;

import com.lumengrid.oritechthings.api.MagneticFieldController;
import com.lumengrid.oritechthings.entity.custom.AcceleratorMagneticFieldBlockEntity;
import com.lumengrid.oritechthings.main.ConfigLoader;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

final class MagneticFields {
    private MagneticFields() {}

    /** Mirrors the field's own gate: oritechthings only consults the first linked field, and a redstone signal switches it off. */
    static boolean holds(Level level, BlockEntity accelerator) {
        if (!(accelerator instanceof MagneticFieldController controller)
                || !ConfigLoader.getInstance().magneticFieldSettings.enabled()) {
            return false;
        }
        List<BlockPos> fields = controller.getLinkedMagneticFields();
        return !fields.isEmpty()
                && level.isLoaded(fields.getFirst())
                && level.getBlockEntity(fields.getFirst()) instanceof AcceleratorMagneticFieldBlockEntity field
                && field.receivedRedstoneSignal() <= 0
                && field.energyStorage.getAmountAsLong() > 0;
    }
}
