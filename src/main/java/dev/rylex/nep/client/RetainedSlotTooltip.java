package dev.rylex.nep.client;

import appeng.client.gui.AEBaseScreen;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.RetainedInputs;
import dev.rylex.nep.pattern.encoding.RetainedSlotHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = Nep.MOD_ID, value = Dist.CLIENT)
public final class RetainedSlotTooltip {
    private RetainedSlotTooltip() {}

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        if (!(Minecraft.getInstance().screen instanceof AEBaseScreen<?> screen)
                || !(screen.getMenu() instanceof RetainedSlotHolder holder)) {
            return;
        }
        Slot slot = screen.getSlotUnderMouse();
        if (!holder.nep$isRetainedSlot(slot)
                || !ItemStack.isSameItemSameComponents(slot.getItem(), event.getItemStack())) {
            return;
        }
        event.getToolTip().add(RetainedInputs.slotNote());
    }
}
