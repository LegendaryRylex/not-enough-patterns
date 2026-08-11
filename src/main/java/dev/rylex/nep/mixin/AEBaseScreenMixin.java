package dev.rylex.nep.mixin;

import appeng.client.gui.AEBaseScreen;
import dev.rylex.nep.client.RetainedHighlight;
import dev.rylex.nep.pattern.encoding.RetainedSlotHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AEBaseScreen.class)
public abstract class AEBaseScreenMixin {

    @Inject(method = "extractSlot", at = @At("RETURN"))
    private void nep$outlineRetainedSlot(
            GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (screen.getMenu() instanceof RetainedSlotHolder holder && holder.nep$isRetainedSlot(slot)) {
            RetainedHighlight.draw(graphics, slot.x, slot.y);
        }
    }
}
