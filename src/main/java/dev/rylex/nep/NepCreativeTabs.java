package dev.rylex.nep;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NepCreativeTabs {
    private NepCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Nep.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.nep"))
                    .icon(() -> NepItems.SEQUENCED_ASSEMBLY_PATTERN.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(NepItems.IMPORT_CARD.get());
                        output.accept(NepItems.MATRIX_CIRCUITRY.get());
                    })
                    .build());
}
