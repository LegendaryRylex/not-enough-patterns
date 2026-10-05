package dev.rylex.nep.compat.ars;

import appeng.api.stacks.AEItemKey;
import com.hollingsworth.arsnouveau.api.registry.RitualRegistry;
import com.hollingsworth.arsnouveau.api.ritual.AbstractRitual;
import com.hollingsworth.arsnouveau.common.items.RitualTablet;
import java.util.Comparator;
import java.util.List;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class ArsRituals {
    private ArsRituals() {}

    static final int MAX_AUGMENTS = 12;

    static List<ResourceLocation> sorted() {
        return RitualRegistry.getRitualMap().keySet().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    static ItemStack tabletStack(@Nullable ResourceLocation ritual) {
        if (ritual == null) {
            return ItemStack.EMPTY;
        }
        RitualTablet tablet = RitualRegistry.getRitualItemMap().get(ritual);
        return tablet == null ? ItemStack.EMPTY : new ItemStack(tablet);
    }

    static Component displayName(ResourceLocation ritual) {
        ItemStack tablet = tabletStack(ritual);
        return tablet.isEmpty() ? Component.literal(ritual.toString()) : tablet.getHoverName();
    }

    static Component shortName(ResourceLocation ritual) {
        String key = "item." + ritual.getNamespace() + "." + ritual.getPath();
        return Language.getInstance().has(key) ? Component.translatable(key) : displayName(ritual);
    }

    @Nullable
    static Component description(ResourceLocation ritual) {
        AbstractRitual known = RitualRegistry.getRitual(ritual);
        return known == null ? null : Component.translatable(known.getDescriptionKey());
    }

    static int sourceCost(ResourceLocation ritual) {
        AbstractRitual known = RitualRegistry.getRitual(ritual);
        return known == null ? 0 : known.getSourceCost();
    }

    @Nullable
    static ResourceLocation ritualOf(ItemStack stack) {
        if (!(stack.getItem() instanceof RitualTablet tablet) || tablet.ritual == null) {
            return null;
        }
        return tablet.ritual.getRegistryName();
    }

    @Nullable
    static AEItemKey tabletKey(@Nullable ResourceLocation ritual) {
        if (ritual == null) {
            return null;
        }
        RitualTablet tablet = RitualRegistry.getRitualItemMap().get(ritual);
        return tablet == null ? null : AEItemKey.of(new ItemStack(tablet));
    }

    static boolean known(@Nullable ResourceLocation ritual) {
        return ritual != null && RitualRegistry.getRitualMap().containsKey(ritual);
    }
}
