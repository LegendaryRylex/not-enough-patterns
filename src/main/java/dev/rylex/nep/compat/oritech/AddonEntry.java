package dev.rylex.nep.compat.oritech;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.block.entity.addons.HeartOfTheMachineAddonEntity;
import rearth.oritech.util.MachineAddonController;

record AddonEntry(Identifier block, float speed, float efficiency, long capacity, long insert, int chambers) {

    static final Codec<AddonEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Identifier.CODEC.fieldOf("block").forGetter(AddonEntry::block),
                    Codec.FLOAT.optionalFieldOf("speed", 1.0F).forGetter(AddonEntry::speed),
                    Codec.FLOAT.optionalFieldOf("efficiency", 1.0F).forGetter(AddonEntry::efficiency),
                    Codec.LONG.optionalFieldOf("capacity", 0L).forGetter(AddonEntry::capacity),
                    Codec.LONG.optionalFieldOf("insert", 0L).forGetter(AddonEntry::insert),
                    Codec.INT.optionalFieldOf("chambers", 0).forGetter(AddonEntry::chambers))
            .apply(instance, AddonEntry::new));

    static AddonEntry of(MachineAddonController.AddonBlock addon) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(addon.addonBlock());
        if (addon.addonEntity() instanceof HeartOfTheMachineAddonEntity heart) {
            MachineAddonController.BaseAddonData data = heart.getBaseData();
            return new AddonEntry(
                    id,
                    data.speed(),
                    data.efficiency(),
                    data.energyBonusCapacity(),
                    data.energyBonusTransfer(),
                    data.extraChambers());
        }
        MachineAddonBlock.AddonSettings settings = addon.addonBlock().getAddonSettings();
        return new AddonEntry(
                id,
                settings.speedMultiplier(),
                settings.efficiencyMultiplier(),
                settings.addedCapacity(),
                settings.addedInsert(),
                settings.chamberCount());
    }

    Block resolved() {
        return BuiltInRegistries.BLOCK.getValue(block);
    }

    ItemStack stack() {
        return new ItemStack(resolved());
    }

    Component name() {
        return resolved().getName();
    }
}
