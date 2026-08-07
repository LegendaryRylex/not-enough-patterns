package dev.rylex.nep.compat.apothic;

import appeng.api.stacks.AEFluidKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

final class ExperienceFluids {
    private ExperienceFluids() {}

    static List<AEFluidKey> all() {
        List<AEFluidKey> keys = new ArrayList<>(1);
        for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(Tags.Fluids.EXPERIENCE)) {
            Fluid fluid = holder.value();
            if (fluid.defaultFluidState().isSource()) {
                keys.add(AEFluidKey.of(fluid));
            }
        }
        return keys;
    }

    @Nullable
    static AEFluidKey preferred() {
        List<AEFluidKey> keys = all();
        return keys.isEmpty() ? null : keys.get(0);
    }

    static boolean isExperience(AEFluidKey key) {
        return key.getFluid().defaultFluidState().is(Tags.Fluids.EXPERIENCE);
    }
}
