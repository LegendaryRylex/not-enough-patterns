package dev.rylex.nep.client;

import appeng.api.stacks.AEItemKey;
import dev.rylex.nep.Nep;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

/**
 * The colour an arbitrary item reads as, for gauges and bars that stand in for a stack. A model's own
 * resolved tint is preferred, since a mod that tints one greyscale texture per variant (Mystical
 * Agriculture's essences being the case in point) keeps its palette there; failing that the item's
 * particle sprite is averaged, which covers untinted items from any mod. Results are cached per item
 * variant and dropped on a resource reload, because the sprite behind them can change.
 */
@EventBusSubscriber(modid = Nep.MOD_ID, value = Dist.CLIENT)
public final class StackTint {

    private static final Map<AEItemKey, OptionalInt> CACHE = new HashMap<>();

    private static final int RGB_MASK = 0xFFFFFF;
    private static final int TINT_LAYERS = 2;
    private static final int OPAQUE_ALPHA = 128;

    private StackTint() {}

    public static OptionalInt of(ItemStack stack) {
        if (stack.isEmpty()) {
            return OptionalInt.empty();
        }
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return OptionalInt.empty();
        }
        return CACHE.computeIfAbsent(key, ignored -> derive(stack));
    }

    public static int of(ItemStack stack, int fallback) {
        return of(stack).orElse(fallback);
    }

    private static OptionalInt derive(ItemStack stack) {
        Probe probe = new Probe();
        try {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft
                    .getItemModelResolver()
                    .updateForTopItem(probe, stack, ItemDisplayContext.GUI, minecraft.level, null, 0);
        } catch (RuntimeException error) {
            return OptionalInt.empty();
        }
        OptionalInt tinted = modelTint(probe);
        return tinted.isPresent() ? tinted : averageSprite(probe);
    }

    private static OptionalInt modelTint(Probe probe) {
        for (ItemStackRenderState.LayerRenderState layer : probe.layers()) {
            IntList tints = layer.tintLayers();
            for (int index = 0; index < Math.min(TINT_LAYERS, tints.size()); index++) {
                int tint = tints.getInt(index) & RGB_MASK;
                if (tint != RGB_MASK && tint != 0) {
                    return OptionalInt.of(tint);
                }
            }
        }
        return OptionalInt.empty();
    }

    private static OptionalInt averageSprite(Probe probe) {
        try {
            Material.Baked particle = probe.pickParticleMaterial(RandomSource.create(0L));
            if (particle == null) {
                return OptionalInt.empty();
            }
            TextureAtlasSprite sprite = particle.sprite();
            int width = sprite.contents().width();
            int height = sprite.contents().height();
            long red = 0;
            long green = 0;
            long blue = 0;
            long counted = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = sprite.getPixelRGBA(0, x, y);
                    if ((argb >>> 24) < OPAQUE_ALPHA) {
                        continue;
                    }
                    red += (argb >> 16) & 0xFF;
                    green += (argb >> 8) & 0xFF;
                    blue += argb & 0xFF;
                    counted++;
                }
            }
            if (counted == 0) {
                return OptionalInt.empty();
            }
            return OptionalInt.of((int) (red / counted) << 16 | (int) (green / counted) << 8 | (int) (blue / counted));
        } catch (RuntimeException error) {
            return OptionalInt.empty();
        }
    }

    private static final class Probe extends ItemStackRenderState {

        private final List<ItemStackRenderState.LayerRenderState> layers = new ArrayList<>();

        @Override
        public ItemStackRenderState.LayerRenderState newLayer() {
            ItemStackRenderState.LayerRenderState layer = super.newLayer();
            layers.add(layer);
            return layer;
        }

        @Override
        public void clear() {
            super.clear();
            layers.clear();
        }

        List<ItemStackRenderState.LayerRenderState> layers() {
            return layers;
        }
    }

    @SubscribeEvent
    static void onAddReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(Nep.id("stack_tint_cache"), (ResourceManagerReloadListener) manager -> CACHE.clear());
    }
}
