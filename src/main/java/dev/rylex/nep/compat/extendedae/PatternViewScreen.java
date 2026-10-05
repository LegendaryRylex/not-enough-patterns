package dev.rylex.nep.compat.extendedae;

import appeng.core.AppEng;
import com.glodblock.github.extendedae.client.gui.pattern.GuiPattern;
import dev.rylex.nep.client.RetainedHighlight;
import dev.rylex.nep.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class PatternViewScreen extends GuiPattern<PatternViewMenu> {

    private static final Identifier BACKGROUND = AppEng.makeId("textures/guis/processing_pattern_recipe.png");

    public PatternViewScreen(PatternViewMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        ((AbstractContainerScreenAccessor) this).nep$setImageHeight(251);
    }

    @Override
    protected void extractMenuBackground(GuiGraphicsExtractor graphics) {
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        super.extractSlot(graphics, slot, mouseX, mouseY);
        if (menu.isRetained(slot)) {
            RetainedHighlight.draw(graphics, slot.x, slot.y);
        }
    }
}
