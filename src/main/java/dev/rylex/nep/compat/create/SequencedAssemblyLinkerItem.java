package dev.rylex.nep.compat.create;

import dev.rylex.nep.NepConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SequencedAssemblyLinkerItem extends Item {

    public SequencedAssemblyLinkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide || player == null) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().immutable();
        boolean sneaking = player.isShiftKeyDown();
        BlockEntity clicked = level.getBlockEntity(pos);

        if (clicked instanceof SequencedAssemblyControllerBlockEntity controller) {
            if (sneaking) {
                controller.clearLinks();
                message(
                        player,
                        Component.translatable("chat.nep.linker.cleared_controller")
                                .withStyle(ChatFormatting.YELLOW),
                        false);
            } else {
                syncTo(player, stack, controller);
            }
            return InteractionResult.SUCCESS;
        }

        if (sneaking) {
            cycleMode(player, stack);
            return InteractionResult.SUCCESS;
        }

        if (unlink(player, stack, pos)) {
            return InteractionResult.SUCCESS;
        }

        switch (mode(stack)) {
            case INPUT -> {
                stack.set(NepCreateContent.LINKER_INPUT.get(), pos);
                message(
                        player,
                        Component.translatable("chat.nep.linker.input_set", format(pos))
                                .withStyle(ChatFormatting.GREEN),
                        false);
            }
            case OUTPUT -> {
                stack.set(NepCreateContent.LINKER_OUTPUT.get(), pos);
                message(
                        player,
                        Component.translatable("chat.nep.linker.output_set", format(pos))
                                .withStyle(ChatFormatting.GREEN),
                        false);
            }
            case MACHINE -> {
                List<BlockPos> updated = new ArrayList<>(machines(stack));
                updated.add(pos);
                stack.set(NepCreateContent.LINKER_MACHINES.get(), List.copyOf(updated));
                StationKind kind = Stations.detect(level.getBlockState(pos));
                message(
                        player,
                        Component.translatable(
                                        "chat.nep.sequenced_assembly.step",
                                        updated.size(),
                                        kind.displayName()
                                                .copy()
                                                .withStyle(
                                                        kind.recognized() ? ChatFormatting.GREEN : ChatFormatting.RED),
                                        Component.literal(format(pos)).withStyle(ChatFormatting.DARK_GRAY))
                                .withStyle(ChatFormatting.GRAY),
                        false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (player.isShiftKeyDown()) {
            stack.remove(NepCreateContent.LINKER_INPUT.get());
            stack.remove(NepCreateContent.LINKER_OUTPUT.get());
            stack.remove(NepCreateContent.LINKER_MACHINES.get());
            message(
                    player,
                    Component.translatable("chat.nep.linker.cleared_plan").withStyle(ChatFormatting.YELLOW),
                    false);
        } else {
            cycleMode(player, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos in = input(stack);
        BlockPos out = output(stack);
        tooltip.add(entry(
                "tooltip.nep.linker.mode", mode(stack).displayName().copy().withStyle(ChatFormatting.WHITE)));
        tooltip.add(entry("tooltip.nep.linker.input", in == null ? unset() : position(in)));
        tooltip.add(entry("tooltip.nep.linker.output", out == null ? unset() : position(out)));
        tooltip.add(entry(
                "tooltip.nep.linker.machines",
                Component.literal(String.valueOf(machines(stack).size())).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.translatable("tooltip.nep.linker.usage.set").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.nep.linker.usage.sync").withStyle(ChatFormatting.DARK_GRAY));
    }

    private void syncTo(Player player, ItemStack stack, SequencedAssemblyControllerBlockEntity controller) {
        BlockPos in = input(stack);
        BlockPos out = output(stack);
        List<BlockPos> machines = machines(stack);
        if (in == null && out == null && machines.isEmpty()) {
            message(player, Component.translatable("chat.nep.linker.empty").withStyle(ChatFormatting.RED), true);
            return;
        }
        BlockPos origin = controller.getBlockPos();
        int range = NepConfig.createSequencedAssemblyLinkRange();
        List<Component> tooFar = new ArrayList<>();
        if (outOfRange(origin, in, range)) {
            tooFar.add(Component.translatable("chat.nep.linker.target.input", format(in)));
        }
        if (outOfRange(origin, out, range)) {
            tooFar.add(Component.translatable("chat.nep.linker.target.output", format(out)));
        }
        for (int i = 0; i < machines.size(); i++) {
            if (outOfRange(origin, machines.get(i), range)) {
                tooFar.add(Component.translatable("chat.nep.linker.target.step", i + 1, format(machines.get(i))));
            }
        }
        if (!tooFar.isEmpty()) {
            MutableComponent list = Component.empty();
            for (int i = 0; i < tooFar.size(); i++) {
                if (i > 0) {
                    list.append(", ");
                }
                list.append(tooFar.get(i));
            }
            message(
                    player,
                    Component.translatable("chat.nep.linker.too_far", range, list)
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }
        controller.applyPlan(in, out, machines);
        message(player, Component.translatable("chat.nep.linker.synced").withStyle(ChatFormatting.GREEN), false);
        controller.sendStatus(player);
    }

    private static boolean unlink(Player player, ItemStack stack, BlockPos pos) {
        if (pos.equals(input(stack))) {
            stack.remove(NepCreateContent.LINKER_INPUT.get());
            message(
                    player,
                    Component.translatable("chat.nep.linker.input_unlinked", format(pos))
                            .withStyle(ChatFormatting.YELLOW),
                    false);
            return true;
        }
        if (pos.equals(output(stack))) {
            stack.remove(NepCreateContent.LINKER_OUTPUT.get());
            message(
                    player,
                    Component.translatable("chat.nep.linker.output_unlinked", format(pos))
                            .withStyle(ChatFormatting.YELLOW),
                    false);
            return true;
        }
        List<BlockPos> machines = machines(stack);
        int index = machines.lastIndexOf(pos);
        if (index < 0) {
            return false;
        }
        List<BlockPos> updated = new ArrayList<>(machines);
        updated.remove(index);
        if (updated.isEmpty()) {
            stack.remove(NepCreateContent.LINKER_MACHINES.get());
        } else {
            stack.set(NepCreateContent.LINKER_MACHINES.get(), List.copyOf(updated));
        }
        message(
                player,
                Component.translatable("chat.nep.linker.machine_unlinked", format(pos), updated.size())
                        .withStyle(ChatFormatting.YELLOW),
                false);
        return true;
    }

    private void cycleMode(Player player, ItemStack stack) {
        LinkerMode next = mode(stack).next();
        stack.set(NepCreateContent.LINKER_MODE.get(), next);
        message(player, entry("chat.nep.linker.mode_set", next.displayName().copy()), true);
    }

    @Nullable
    private static BlockPos input(ItemStack stack) {
        return stack.get(NepCreateContent.LINKER_INPUT.get());
    }

    @Nullable
    private static BlockPos output(ItemStack stack) {
        return stack.get(NepCreateContent.LINKER_OUTPUT.get());
    }

    private static List<BlockPos> machines(ItemStack stack) {
        return stack.getOrDefault(NepCreateContent.LINKER_MACHINES.get(), List.of());
    }

    private static LinkerMode mode(ItemStack stack) {
        return stack.getOrDefault(NepCreateContent.LINKER_MODE.get(), LinkerMode.INPUT);
    }

    private static Component entry(String key, Component value) {
        return Component.translatable(key, value.copy().withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.GRAY);
    }

    private static Component position(BlockPos pos) {
        return Component.literal(format(pos)).withStyle(ChatFormatting.WHITE);
    }

    private static Component unset() {
        return Component.translatable("chat.nep.sequenced_assembly.unset").withStyle(ChatFormatting.DARK_GRAY);
    }

    private static void message(Player player, Component text, boolean actionBar) {
        player.displayClientMessage(text, actionBar);
    }

    private static boolean outOfRange(BlockPos origin, @Nullable BlockPos target, int range) {
        if (target == null) {
            return false;
        }
        int dx = Math.abs(target.getX() - origin.getX());
        int dy = Math.abs(target.getY() - origin.getY());
        int dz = Math.abs(target.getZ() - origin.getZ());
        return Math.max(dx, Math.max(dy, dz)) > range;
    }

    private static String format(BlockPos pos) {
        return Component.translatable("gui.nep.coords", pos.getX(), pos.getY(), pos.getZ())
                .getString();
    }
}
