package dev.rylex.nep.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

class OverstackedItemHandlerTest {

    private static final int DEEP = 1_088;

    private static HolderLookup.Provider registries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    private static OverstackedItemHandler deepHandler(int slots) {
        return new OverstackedItemHandler(slots) {
            @Override
            public int getSlotLimit(int slot) {
                return DEEP;
            }
        };
    }

    @Test
    void aSlotPastTheVanillaStackCapSurvivesASaveAndLoad() {
        OverstackedItemHandler handler = deepHandler(3);
        ItemStack tagged = new ItemStack(Items.STONE, 184);
        CompoundTag custom = new CompoundTag();
        custom.putBoolean("nepTest", true);
        tagged.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        handler.setStackInSlot(1, tagged);
        handler.setStackInSlot(2, new ItemStack(Items.DIRT, DEEP));

        CompoundTag saved = handler.serializeNBT(registries());
        OverstackedItemHandler restored = deepHandler(3);
        restored.deserializeNBT(registries(), saved);

        assertTrue(restored.getStackInSlot(0).isEmpty());
        assertTrue(ItemStack.isSameItemSameComponents(tagged, restored.getStackInSlot(1)));
        assertEquals(184, restored.getStackInSlot(1).getCount());
        assertEquals(DEEP, restored.getStackInSlot(2).getCount());
    }

    @Test
    void aBufferWrittenByTheStockHandlerStillLoads() {
        ItemStackHandler stock = new ItemStackHandler(2);
        stock.setStackInSlot(1, new ItemStack(Items.STONE, 64));

        OverstackedItemHandler restored = deepHandler(2);
        restored.deserializeNBT(registries(), stock.serializeNBT(registries()));

        assertEquals(64, restored.getStackInSlot(1).getCount());
    }

    @Test
    void aStackWithinTheVanillaCapIsWrittenInTheStockFormat() {
        CompoundTag saved = OverstackedItemHandler.saveStack(new ItemStack(Items.STONE, 64), registries());

        assertFalse(saved.contains("Amount"));
        assertEquals(64, ItemStack.parse(registries(), saved).orElseThrow().getCount());
    }

    @Test
    void aSlotOutsideTheHandlerIsIgnoredOnLoad() {
        OverstackedItemHandler handler = deepHandler(4);
        handler.setStackInSlot(3, new ItemStack(Items.STONE, 200));
        CompoundTag saved = handler.serializeNBT(registries());
        ListTag items = saved.getList("Items", Tag.TAG_COMPOUND);
        items.getCompound(0).putInt("Slot", 9);

        OverstackedItemHandler restored = deepHandler(4);
        restored.deserializeNBT(registries(), saved);

        for (int slot = 0; slot < restored.getSlots(); slot++) {
            assertTrue(restored.getStackInSlot(slot).isEmpty());
        }
    }
}
