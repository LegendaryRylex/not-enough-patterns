package dev.rylex.nep.compat.extendedae;

import dev.rylex.nep.Nep;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

final class NepExtendedAeContent {
    private NepExtendedAeContent() {}

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Nep.MOD_ID);

    static final DeferredHolder<MenuType<?>, MenuType<PatternViewMenu>> PATTERN_VIEW =
            MENUS.register(PatternViewMenu.ID.getPath(), () -> PatternViewMenu.TYPE);

    static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
