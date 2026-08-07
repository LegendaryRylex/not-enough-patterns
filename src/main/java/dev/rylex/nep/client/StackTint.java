package dev.rylex.nep.client;

import appeng.api.stacks.AEItemKey;
import com.mojang.blaze3d.platform.NativeImage;
import dev.rylex.nep.Nep;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The colour an arbitrary item reads as, for gauges and bars that stand in for a stack. A registered
 * {@code ItemColor} is preferred, since a mod that tints one greyscale texture per variant (Mystical
 * Agriculture's essences being the case in point) keeps its palette there; failing that the item's
 * particle sprite is averaged, which covers untinted items from any mod. Results are cached per item
 * variant and dropped on a resource reload, because the sprite behind them can change.
 */
@EventBusSubscriber(modid = Nep.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
        OptionalInt tinted = registeredTint(stack);
        return tinted.isPresent() ? tinted : averageSprite(stack);
    }

    private static OptionalInt registeredTint(ItemStack stack) {
        for (int layer = 0; layer < TINT_LAYERS; layer++) {
            int tint = Minecraft.getInstance().getItemColors().getColor(stack, layer) & RGB_MASK;
            if (tint != RGB_MASK && tint != 0) {
                return OptionalInt.of(tint);
            }
        }
        return OptionalInt.empty();
    }

    private static OptionalInt averageSprite(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            BakedModel model = minecraft.getItemRenderer().getModel(stack, minecraft.level, null, 0);
            TextureAtlasSprite sprite = model.getParticleIcon(ModelData.EMPTY);
            NativeImage image = sprite.contents().getOriginalImage();
            if (image.format() != NativeImage.Format.RGBA) {
                return OptionalInt.empty();
            }
            int width = Math.min(sprite.contents().width(), image.getWidth());
            int height = Math.min(sprite.contents().height(), image.getHeight());
            long red = 0;
            long green = 0;
            long blue = 0;
            long counted = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    if ((abgr >>> 24) < OPAQUE_ALPHA) {
                        continue;
                    }
                    red += abgr & 0xFF;
                    green += (abgr >> 8) & 0xFF;
                    blue += (abgr >> 16) & 0xFF;
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

    @SubscribeEvent
    static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> CACHE.clear());
    }
}
