package dev.rylex.nep.client;

import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.Tooltip;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.widgets.UpgradesPanel;
import dev.rylex.nep.mixin.WidgetContainerAccessor;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/**
 * Other addons' upgrade panels share this screen column and only know their height at render time, so this one
 * settles below them every frame.
 */
public final class StackedUpgradesPanel implements ICompositeWidget {

    private static final int GAP = 4;
    private static final int EMPTY_PANEL_HEIGHT = 10;

    private final UpgradesPanel panel;
    private final WidgetContainer container;
    private Point anchor = Point.ZERO;
    private int placedTop = Integer.MIN_VALUE;

    public StackedUpgradesPanel(List<Slot> slots, Supplier<List<Component>> tooltip, WidgetContainer container) {
        this.panel = new UpgradesPanel(slots, tooltip);
        this.container = container;
    }

    @Override
    public boolean isVisible() {
        return panel.isVisible();
    }

    @Override
    public void setPosition(Point position) {
        anchor = position;
        placedTop = position.getY();
        panel.setPosition(position);
    }

    @Override
    public void setSize(int width, int height) {
        panel.setSize(width, height);
    }

    @Override
    public Rect2i getBounds() {
        return panel.getBounds();
    }

    @Override
    public void populateScreen(Consumer<AbstractWidget> addWidget, Rect2i bounds, AEBaseScreen<?> screen) {
        panel.populateScreen(addWidget, bounds, screen);
    }

    @Override
    public void updateBeforeRender() {
        settle();
        panel.updateBeforeRender();
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics guiGraphics, Rect2i bounds, Point mouse) {
        panel.drawBackgroundLayer(guiGraphics, bounds, mouse);
    }

    @Override
    public void addExclusionZones(List<Rect2i> exclusionZones, Rect2i screenBounds) {
        settle();
        panel.addExclusionZones(exclusionZones, screenBounds);
    }

    @Nullable
    @Override
    public Tooltip getTooltip(int mouseX, int mouseY) {
        return panel.getTooltip(mouseX, mouseY);
    }

    private void settle() {
        var own = panel.getBounds();
        int left = anchor.getX();
        int right = left + own.getWidth();
        int top = anchor.getY();
        for (var widget :
                ((WidgetContainerAccessor) container).nep$compositeWidgets().values()) {
            if (widget == this || !(widget instanceof UpgradesPanel other) || !other.isVisible()) {
                continue;
            }
            var bounds = other.getBounds();
            boolean sameColumn = bounds.getX() < right && bounds.getX() + bounds.getWidth() > left;
            if (sameColumn && bounds.getHeight() > EMPTY_PANEL_HEIGHT) {
                top = Math.max(top, bounds.getY() + bounds.getHeight() + GAP);
            }
        }
        if (top != placedTop) {
            placedTop = top;
            panel.setPosition(new Point(left, top));
        }
    }
}
