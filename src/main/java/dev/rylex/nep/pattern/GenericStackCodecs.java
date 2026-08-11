package dev.rylex.nep.pattern;

import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import dev.rylex.nep.Nep;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

final class GenericStackCodecs {

    private GenericStackCodecs() {}

    static final Codec<GenericStack> FAULT_TOLERANT = GenericStack.CODEC.mapResult(new Codec.ResultFunction<>() {
        @Override
        public <T> DataResult<Pair<GenericStack, T>> apply(
                DynamicOps<T> ops, T input, DataResult<Pair<GenericStack, T>> result) {
            if (!(result instanceof DataResult.Error<Pair<GenericStack, T>> error)) {
                return result;
            }
            Nep.LOGGER.error("Failed to deserialize pattern stack {}: {}", input, error.message());
            ItemStack placeholder = AEItems.MISSING_CONTENT.stack();
            if (Dynamic.convert(ops, NbtOps.INSTANCE, input) instanceof CompoundTag original) {
                placeholder.set(AEComponents.MISSING_CONTENT_ITEMSTACK_DATA, CustomData.of(original));
            }
            placeholder.set(AEComponents.MISSING_CONTENT_ERROR, error.message());
            return DataResult.success(
                    Pair.of(new GenericStack(AEItemKey.of(placeholder), 1), input), Lifecycle.stable());
        }

        @Override
        public <T> DataResult<T> coApply(DynamicOps<T> ops, GenericStack input, DataResult<T> result) {
            if (input.what() instanceof AEItemKey key && key.is(AEItems.MISSING_CONTENT)) {
                CustomData original = key.get(AEComponents.MISSING_CONTENT_ITEMSTACK_DATA);
                if (original != null) {
                    return DataResult.success(
                            Dynamic.convert(NbtOps.INSTANCE, ops, original.copyTag()), result.lifecycle());
                }
            }
            return result;
        }
    });
}
