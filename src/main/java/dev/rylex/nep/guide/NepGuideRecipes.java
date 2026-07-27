package dev.rylex.nep.guide;

import dev.rylex.nep.NepConfig;
import dev.rylex.nep.compat.create.CreateGuideRecipes;
import guideme.color.SymbolicColor;
import guideme.compiler.tags.RecipeTypeMappingSupplier;
import guideme.document.block.LytBlock;
import guideme.document.block.LytParagraph;
import guideme.document.block.LytVBox;
import guideme.document.flow.LytFlowSpan;
import guideme.document.flow.LytFlowText;
import guideme.style.TextStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;

public final class NepGuideRecipes implements RecipeTypeMappingSupplier {

    @Override
    public void collect(RecipeTypeMappings mappings) {
        mappings.add(NepRecipes.MODULE_STATUS.get(), NepGuideRecipes::moduleStatus);
        if (ModList.get().isLoaded("create")) {
            CreateGuideRecipes.collect(mappings);
        }
    }

    private static LytBlock moduleStatus(RecipeHolder<ModuleStatusRecipe> holder) {
        if (!NepConfig.loaded() || !NepModules.disabled(holder.value().module())) {
            return new LytVBox();
        }

        LytFlowSpan span = new LytFlowSpan();
        span.setStyle(TextStyle.builder().color(SymbolicColor.RED).bold(true).build());
        span.append(LytFlowText.of(
                Component.translatable("nep.guide.module_disabled").getString()));

        LytParagraph paragraph = new LytParagraph();
        paragraph.append(span);
        return paragraph;
    }
}
