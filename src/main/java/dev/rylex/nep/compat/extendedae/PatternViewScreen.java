package dev.rylex.nep.compat.extendedae;

import appeng.core.AppEng;
import com.glodblock.github.extendedae.client.gui.pattern.GuiPattern;
import dev.rylex.nep.client.RetainedHighlight;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class PatternViewScreen extends GuiPattern<PatternViewMenu> {

    private static final ResourceLocation BACKGROUND = AppEng.makeId("textures/guis/processing_pattern_recipe.png");

    public PatternViewScreen(PatternViewMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 251;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;
        graphics.blit(BACKGROUND, left, top, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        super.renderSlot(graphics, slot);
        if (menu.isRetained(slot)) {
            RetainedHighlight.draw(graphics, slot.x, slot.y);
        }
    }
}
