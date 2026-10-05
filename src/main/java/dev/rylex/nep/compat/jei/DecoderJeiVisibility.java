package dev.rylex.nep.compat.jei;

import dev.rylex.nep.NepConfig;
import dev.rylex.nep.decoder.DecoderContent;
import dev.rylex.nep.decoder.EncodingModuleItem;
import dev.rylex.nep.decoder.PatternDecoding;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Hidden through JEI rather than a recipe condition because the server config is not loaded when datapacks are read. */
final class DecoderJeiVisibility {
    private DecoderJeiVisibility() {}

    static void apply(IJeiRuntime runtime) {
        List<ItemStack> hidden = new ArrayList<>();
        if (!NepConfig.requireDecoder()) {
            hidden.add(DecoderContent.PATTERN_DECODER_ITEM.toStack());
        }
        BuiltInRegistries.ITEM.stream()
                .filter(EncodingModuleItem.class::isInstance)
                .map(EncodingModuleItem.class::cast)
                .filter(module -> !PatternDecoding.required(module.module()))
                .forEach(module -> hidden.add(new ItemStack(module)));
        if (!hidden.isEmpty()) {
            runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
        }
    }
}
