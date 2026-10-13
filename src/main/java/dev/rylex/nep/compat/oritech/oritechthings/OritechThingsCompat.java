package dev.rylex.nep.compat.oritech.oritechthings;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class OritechThingsCompat {
    private OritechThingsCompat() {}

    private static final boolean LOADED = ModList.get().isLoaded("oritechthings");

    /** True while an oritechthings Magnetic Field linked to this accelerator can carry its particle through any bend. */
    public static boolean fieldHolds(Level level, @Nullable BlockEntity accelerator) {
        return LOADED && accelerator != null && MagneticFields.holds(level, accelerator);
    }
}
