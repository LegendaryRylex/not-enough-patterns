package dev.rylex.nep.compat.create;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

final class SequencedAssemblyControllerReadout {

    private SequencedAssemblyControllerReadout() {}

    static SequencedAssemblyState.Issue structuralIssueOf(StationKind kind, BlockState state) {
        if (!kind.recognized()) {
            return SequencedAssemblyState.Issue.UNRECOGNIZED;
        }
        if (kind == StationKind.DEPLOYER && !Stations.deployerFacesDown(state)) {
            return SequencedAssemblyState.Issue.WRONG_FACING;
        }
        return SequencedAssemblyState.Issue.OK;
    }

    static SequencedAssemblyState.Issue issueOf(Level level, BlockPos machine, StationKind kind, BlockState state) {
        SequencedAssemblyState.Issue structural = structuralIssueOf(kind, state);
        if (structural != SequencedAssemblyState.Issue.OK) {
            return structural;
        }
        if (Stations.isUnpowered(level, machine, kind)) {
            return SequencedAssemblyState.Issue.UNPOWERED;
        }
        return SequencedAssemblyState.Issue.OK;
    }

    static void sendLinks(
            Player player,
            @Nullable Level level,
            BlockPos controller,
            @Nullable BlockPos input,
            @Nullable BlockPos output,
            List<BlockPos> machines) {
        player.displayClientMessage(
                Component.translatable("block.nep.sequenced_assembly_controller")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(" " + format(controller)).withStyle(ChatFormatting.GRAY)),
                false);
        player.displayClientMessage(endpointLine("chat.nep.sequenced_assembly.input", input), false);
        player.displayClientMessage(endpointLine("chat.nep.sequenced_assembly.output", output), false);
        if (machines.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("chat.nep.sequenced_assembly.no_machines")
                            .withStyle(ChatFormatting.GRAY),
                    false);
            return;
        }
        for (int i = 0; i < machines.size(); i++) {
            BlockPos pos = machines.get(i);
            StationKind kind = level == null ? StationKind.UNKNOWN : Stations.detect(level.getBlockState(pos));
            player.displayClientMessage(
                    Component.translatable(
                                    "chat.nep.sequenced_assembly.step",
                                    i + 1,
                                    kind.displayName()
                                            .copy()
                                            .withStyle(kind.recognized() ? ChatFormatting.GREEN : ChatFormatting.RED),
                                    Component.literal(format(pos)).withStyle(ChatFormatting.DARK_GRAY))
                            .withStyle(ChatFormatting.GRAY),
                    false);
        }
    }

    private static Component endpointLine(String key, @Nullable BlockPos pos) {
        Component value = pos == null
                ? Component.translatable("chat.nep.sequenced_assembly.unset").withStyle(ChatFormatting.RED)
                : Component.literal(format(pos)).withStyle(ChatFormatting.WHITE);
        return Component.translatable(key, value).withStyle(ChatFormatting.GRAY);
    }

    private static String format(BlockPos pos) {
        return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }
}
