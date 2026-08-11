package dev.rylex.nep.pattern.encoding;

import appeng.api.crafting.IPatternDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface PatternFallback {

    @Nullable
    Result convert(IPatternDetails encoded, Level level);

    record Result(@Nullable ItemStack stack, @Nullable Component feedback) {

        public static Result of(ItemStack stack) {
            return new Result(stack, null);
        }

        public static Result feedback(Component feedback) {
            return new Result(null, feedback);
        }
    }
}
