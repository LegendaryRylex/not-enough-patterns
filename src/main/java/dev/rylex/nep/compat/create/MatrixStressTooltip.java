package dev.rylex.nep.compat.create;

import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CKinetics;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

final class MatrixStressTooltip {
    private MatrixStressTooltip() {}

    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(NepCreateContent.MATRIX_ITEM.get()) || !StressImpact.isEnabled()) {
            return;
        }

        float minimumSpeed = SequencedAssemblyMatrixBlockEntity.minimumSpeed();
        float peakSpeed = SequencedAssemblyMatrixBlockEntity.peakSpeed();
        int minimumStress = SequencedAssemblyMatrixBlockEntity.stressDrawAt(minimumSpeed);
        int peakStress = SequencedAssemblyMatrixBlockEntity.stressDrawAt(peakSpeed);
        if (peakStress <= 0) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(Component.translatable("create.tooltip.stressImpact").withStyle(ChatFormatting.GRAY));

        StressImpact tier = tierOf(peakStress / peakSpeed);
        tooltip.add(Component.literal(TooltipHelper.makeProgressBar(3, tier.ordinal() + 1))
                .append(Component.translatable("tooltip.nep.sequenced_assembly_matrix.stress.scales"))
                .withStyle(tier.getAbsoluteColor()));
        tooltip.add(stressLine(minimumStress, minimumSpeed));
        tooltip.add(stressLine(peakStress, peakSpeed));
    }

    private static Component stressLine(int stress, float speed) {
        return Component.translatable(
                        "tooltip.nep.sequenced_assembly_matrix.stress.at",
                        String.format("%,d", stress),
                        String.format("%,d", Math.round(speed)))
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    private static StressImpact tierOf(double impact) {
        CKinetics config = AllConfigs.server().kinetics;
        if (impact >= config.highStressImpact.get()) {
            return StressImpact.HIGH;
        }
        return impact >= config.mediumStressImpact.get() ? StressImpact.MEDIUM : StressImpact.LOW;
    }
}
