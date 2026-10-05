package dev.rylex.nep.compat.provider.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import com.loliball.appliedcreate.patternprovider.MechanicalCraftingPatternLogic;
import dev.rylex.nep.pattern.NepPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applied Create lays any pattern whose output has a crafting recipe out from the first corner that fits rather than nearest
 * the output crafter, so every nep pattern is handed back to AE2's own dispatch, which reaches nep's machines.
 */
@Mixin(MechanicalCraftingPatternLogic.class)
public abstract class MechanicalCraftingPatternLogicMixin extends PatternProviderLogic {

    private MechanicalCraftingPatternLogicMixin(IManagedGridNode mainNode, PatternProviderLogicHost host) {
        super(mainNode, host);
    }

    @Inject(
            method = "pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void nep$dispatchThroughAe2(
            IPatternDetails patternDetails, KeyCounter[] inputHolder, CallbackInfoReturnable<Boolean> cir) {
        if (patternDetails instanceof NepPattern) {
            cir.setReturnValue(super.pushPattern(patternDetails, inputHolder));
        }
    }
}
