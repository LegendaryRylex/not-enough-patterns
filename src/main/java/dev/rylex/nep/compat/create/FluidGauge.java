package dev.rylex.nep.compat.create;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

final class FluidGauge {
    private FluidGauge() {}

    static void draw(GuiGraphics graphics, FluidStack stack, int x, int bottom, int width, int height, int capacity) {
        if (stack.isEmpty() || height <= 0 || capacity <= 0) {
            return;
        }
        int filled = Math.max(1, (int) ((long) height * Math.min(stack.getAmount(), capacity) / capacity));
        filled = Math.min(filled, height);
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(stack.getFluid());
        ResourceLocation texture = extensions.getStillTexture(stack);
        if (texture == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(texture);
        int tint = extensions.getTintColor(stack);
        float alpha = (tint >> 24 & 0xFF) / 255.0F;
        if (alpha == 0.0F) {
            alpha = 1.0F;
        }
        float red = (tint >> 16 & 0xFF) / 255.0F;
        float green = (tint >> 8 & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        int drawn = 0;
        while (drawn < filled) {
            int slice = Math.min(16, filled - drawn);
            int y = bottom - drawn - slice;
            if (slice < 16) {
                graphics.enableScissor(x, y, x + width, y + slice);
                graphics.blit(x, y + slice - 16, 0, width, 16, sprite, red, green, blue, alpha);
                graphics.disableScissor();
            } else {
                graphics.blit(x, y, 0, width, slice, sprite, red, green, blue, alpha);
            }
            drawn += slice;
        }
    }
}
