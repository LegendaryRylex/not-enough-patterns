package dev.rylex.nep.pattern.encoding;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class PatternConverters {
    private PatternConverters() {}

    private record Entry<R extends Recipe<?>>(Class<R> type, PatternConverter<R> converter) {

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
    private static final List<PatternFallback> FALLBACKS = new CopyOnWriteArrayList<>();

    public static <R extends Recipe<?>> void register(Class<R> type, PatternConverter<R> converter) {
        CONVERTERS.add(new Entry<>(type, converter));
    }

    public static void registerFallback(PatternFallback fallback) {
        FALLBACKS.add(fallback);
    }

    @Nullable
    public static ItemStack convert(@Nullable ResourceLocation recipe, ItemStack encoded, Player player) {
        Level level = player.level();
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, level);
        if (details == null) {
            return null;
        }

        RecipeHolder<?> holder =
                recipe == null ? null : level.getRecipeManager().byKey(recipe).orElse(null);
        if (holder != null) {
            List<ItemStack> converterClaims = new ArrayList<>();
            for (Entry<?> entry : CONVERTERS) {
                ItemStack converted = sanitize(entry.convert(details, holder, level));
                if (converted != null) {
                    converterClaims.add(converted);
                }
            }
            if (converterClaims.size() > 1) {
                ambiguous(player);
                return null;
            }
            if (converterClaims.size() == 1) {
                return converterClaims.get(0);
            }
        }

        List<ItemStack> fallbackClaims = new ArrayList<>();
        Component feedback = null;
        for (PatternFallback fallback : FALLBACKS) {
            PatternFallback.Result result = fallback.convert(details, level);
            if (result == null) {
                continue;
            }
            ItemStack stack = sanitize(result.stack());
            if (stack != null) {
                fallbackClaims.add(stack);
            } else if (feedback == null) {
                feedback = result.feedback();
            }
        }
        if (fallbackClaims.size() > 1) {
            ambiguous(player);
            return null;
        }
        if (fallbackClaims.size() == 1) {
            return fallbackClaims.get(0);
        }
        if (feedback != null) {
            player.displayClientMessage(feedback, true);
        }
        return null;
    }

    private static void ambiguous(Player player) {
        player.displayClientMessage(Component.translatable("nep.encoding.ambiguous"), true);
    }

    @Nullable
    private static ItemStack sanitize(@Nullable ItemStack converted) {
        return converted == null || converted.isEmpty() ? null : converted;
    }
}
