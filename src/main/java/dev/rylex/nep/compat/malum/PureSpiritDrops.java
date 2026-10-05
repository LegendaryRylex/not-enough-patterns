package dev.rylex.nep.compat.malum;

import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class PureSpiritDrops {
    private PureSpiritDrops() {}

    private static final TagKey<EntityType<?>> SOURCES =
            TagKey.create(Registries.ENTITY_TYPE, Nep.id("pure_spirit_sources"));

    public static void appendPureSpirit(List<ItemStack> spirits, LivingEntity target, LivingEntity attacker) {
        int chance = NepConfig.malumPureSpiritDropChance();
        if (chance <= 0 || attacker == null || !target.getType().is(SOURCES)) {
            return;
        }
        if (attacker.getRandom().nextInt(100) >= chance) {
            return;
        }
        spirits.add(new ItemStack(NepMalumContent.PURE_SPIRIT.get()));
    }
}
