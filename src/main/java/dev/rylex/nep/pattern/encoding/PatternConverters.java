package dev.rylex.nep.pattern.encoding;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import dev.rylex.nep.decoder.DecoderModule;
import dev.rylex.nep.util.Recipes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class PatternConverters {
    private PatternConverters() {}

    public record Claim(ItemStack stack, DecoderModule module) {}

    private record Entry<R extends Recipe<?>>(DecoderModule module, Class<R> type, PatternConverter<R> converter) {

        @Nullable
        ItemStack convert(IPatternDetails encoded, RecipeHolder<?> holder, Level level) {
            return type.isInstance(holder.value()) ? converter.convert(encoded, cast(holder), level) : null;
        }

        @SuppressWarnings("unchecked")
        private RecipeHolder<R> cast(RecipeHolder<?> holder) {
            return (RecipeHolder<R>) holder;
        }
    }

    private static final List<Entry<?>> CONVERTERS = new CopyOnWriteArrayList<>();

    private record Fallback(DecoderModule module, PatternFallback fallback) {}

    private static final List<Fallback> FALLBACKS = new CopyOnWriteArrayList<>();

    public static <R extends Recipe<?>> void register(
            DecoderModule module, Class<R> type, PatternConverter<R> converter) {
        CONVERTERS.add(new Entry<>(module, type, converter));
    }

    public static void registerFallback(DecoderModule module, PatternFallback fallback) {
        FALLBACKS.add(new Fallback(module, fallback));
    }

    @Nullable
    public static ItemStack convert(PatternOrigin origin, ItemStack encoded, Player player) {
        return stackOf(claim(origin, encoded, player.level(), player));
    }

    @Nullable
    public static ItemStack convertQuietly(PatternOrigin origin, ItemStack encoded, Level level) {
        return stackOf(claim(origin, encoded, level, null));
    }

    @Nullable
    private static ItemStack stackOf(@Nullable Claim claim) {
        return claim == null ? null : claim.stack();
    }

    @Nullable
    public static Claim claim(PatternOrigin origin, ItemStack encoded, Level level, @Nullable Player feedbackTo) {
        if (origin.recipeViewer()) {
            return null;
        }
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, level);
        if (details == null) {
            return null;
        }

        Identifier recipe = origin.recipe();
        RecipeHolder<?> holder = recipe == null ? null : Recipes.byId(level, recipe);
        if (holder != null) {
            List<Claim> converterClaims = new ArrayList<>();
            for (Entry<?> entry : CONVERTERS) {
                ItemStack converted = sanitize(entry.convert(details, holder, level));
                if (converted != null) {
                    converterClaims.add(new Claim(converted, entry.module()));
                }
            }
            if (converterClaims.size() > 1) {
                ambiguous(feedbackTo);
                return null;
            }
            if (converterClaims.size() == 1) {
                return converterClaims.get(0);
            }
        }

        List<Claim> fallbackClaims = new ArrayList<>();
        Component feedback = null;
        for (Fallback fallback : FALLBACKS) {
            PatternFallback.Result result = fallback.fallback().convert(details, level);
            if (result == null) {
                continue;
            }
            ItemStack stack = sanitize(result.stack());
            if (stack != null) {
                fallbackClaims.add(new Claim(stack, fallback.module()));
            } else if (feedback == null) {
                feedback = result.feedback();
            }
        }
        if (fallbackClaims.size() > 1) {
            ambiguous(feedbackTo);
            return null;
        }
        if (fallbackClaims.size() == 1) {
            return fallbackClaims.get(0);
        }
        if (feedback != null && feedbackTo != null) {
            feedbackTo.sendOverlayMessage(feedback);
        }
        return null;
    }

    private static void ambiguous(@Nullable Player player) {
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("nep.encoding.ambiguous"));
        }
    }

    @Nullable
    private static ItemStack sanitize(@Nullable ItemStack converted) {
        return converted == null || converted.isEmpty() ? null : converted;
    }
}
