package dev.rylex.nep.compat.occultism;

import com.klikli_dev.occultism.common.entity.job.event.ItemProcessingJobEvent;
import com.klikli_dev.occultism.common.entity.spirit.SpiritEntity;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class SpiritProviders {
    private SpiritProviders() {}

    private static final Map<Level, Set<SpiritProviderBlockEntity>> LOADED = new WeakHashMap<>();

    static void add(SpiritProviderBlockEntity provider) {
        Level level = provider.getLevel();
        if (level != null && !level.isClientSide()) {
            LOADED.computeIfAbsent(level, key -> Collections.newSetFromMap(new WeakHashMap<>()))
                    .add(provider);
        }
    }

    static void remove(SpiritProviderBlockEntity provider) {
        Set<SpiritProviderBlockEntity> providers = LOADED.get(provider.getLevel());
        if (providers != null) {
            providers.remove(provider);
        }
    }

    /** Fires for every crusher, crystallizer, smelter and trader result before Occultism drops or deposits it. */
    static void onJobResult(ItemProcessingJobEvent event) {
        ItemStack result = event.getResult();
        if (result.isEmpty() || !(event.getEntity() instanceof SpiritEntity spirit)) {
            return;
        }
        Set<SpiritProviderBlockEntity> providers = LOADED.get(spirit.level());
        if (providers == null || providers.isEmpty()) {
            return;
        }
        ItemStack input = event.getInput();
        for (SpiritProviderBlockEntity provider : List.copyOf(providers)) {
            if (!provider.isRemoved()
                    && provider.claims(input)
                    && WorkingSpirits.covers(spirit, provider.getBlockPos())) {
                provider.capture(input, result);
                event.setResult(ItemStack.EMPTY);
                return;
            }
        }
    }
}
