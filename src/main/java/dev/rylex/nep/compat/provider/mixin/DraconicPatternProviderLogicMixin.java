package dev.rylex.nep.compat.provider.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import com.ae2draconicfusion.autocrafter.ae2.DraconicPatternProviderLogic;
import com.ae2draconicfusion.autocrafter.ae2.FusionBusAccess;
import dev.rylex.nep.provider.ImportTrackerHost;
import dev.rylex.nep.provider.MachineImports;
import dev.rylex.nep.provider.OwedSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Draconic provider overrides AE2's pushPattern outright, so nep's own dispatch hook never sees the craft and the
 * Import Card is never told what the fusion core owes it.
 */
@Mixin(DraconicPatternProviderLogic.class)
public abstract class DraconicPatternProviderLogicMixin {

    /** Matches the range the provider itself searches when it routes a craft. */
    private static final int CORE_SEARCH_RANGE = 8;

    @Shadow
    @Final
    private PatternProviderLogicHost host;

    @Inject(
            method = "pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z",
            at = @At("RETURN"),
            remap = false)
    private void nep$recordFusionDispatch(
            IPatternDetails pattern, KeyCounter[] inputs, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !(this instanceof ImportTrackerHost tracker)) {
            return;
        }
        BlockEntity blockEntity = host.getBlockEntity();
        if (!(blockEntity.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos core = FusionBusAccess.resolveFusionCoreInRange(level, blockEntity.getBlockPos(), CORE_SEARCH_RANGE)
                .orElse(null);
        if (core != null) {
            tracker.nepRecordOwed(pattern, inputs, new OwedSource.Machine(MachineImports.DRACONIC_FUSION_CORE, core));
        }
    }
}
