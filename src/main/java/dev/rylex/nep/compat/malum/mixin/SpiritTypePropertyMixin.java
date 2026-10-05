package dev.rylex.nep.compat.malum.mixin;

import com.google.common.collect.ImmutableSet;
import com.sammy.malum.core.systems.registry.SpiritHolder;
import com.sammy.malum.core.systems.spirit.SpiritTypeProperty;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import dev.rylex.nep.compat.malum.NepTotemSpirits;
import java.util.Collection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpiritTypeProperty.class)
public abstract class SpiritTypePropertyMixin {

    @Shadow
    @Final
    @Mutable
    private ImmutableSet<String> values;

    @Inject(method = "<init>(Ljava/lang/String;Ljava/util/Collection;)V", at = @At("RETURN"))
    private void nep$carveNepSpirits(
            String name, Collection<SpiritHolder<SpiritArcanaType>> validSpirits, CallbackInfo ci) {
        if (!"spirit".equals(name)) {
            return;
        }
        this.values = ImmutableSet.<String>builder()
                .addAll(this.values)
                .add(NepTotemSpirits.PURE_PATH)
                .build();
    }
}
