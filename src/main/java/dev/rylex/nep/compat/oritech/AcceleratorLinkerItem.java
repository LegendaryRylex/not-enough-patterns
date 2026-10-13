package dev.rylex.nep.compat.oritech;

import dev.rylex.nep.NepConfig;
import dev.rylex.nep.util.SubLevels;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import rearth.oritech.block.entity.accelerator.AcceleratorControllerBlockEntity;

public class AcceleratorLinkerItem extends Item {

    public AcceleratorLinkerItem(Properties properties) {
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
        BlockEntity clicked = level.getBlockEntity(pos);

        if (clicked instanceof ParticleCollisionControllerBlockEntity controller) {
            if (player.isShiftKeyDown()) {
                message(
                        player,
                        controller.clearLinks()
                                ? Component.translatable("chat.nep.accelerator_linker.cleared_controller")
                                        .withStyle(ChatFormatting.YELLOW)
                                : Component.translatable("chat.nep.accelerator_linker.busy")
                                        .withStyle(ChatFormatting.RED));
            } else {
                syncTo(player, stack, controller);
            }
            return InteractionResult.SUCCESS;
        }

        if (!(clicked instanceof AcceleratorControllerBlockEntity)) {
            message(
                    player,
                    Component.translatable("chat.nep.accelerator_linker.not_an_accelerator", format(pos))
                            .withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }

        List<BlockPos> linked = new ArrayList<>(accelerators(stack));
        if (linked.remove(pos)) {
            save(stack, linked);
            message(
                    player,
                    Component.translatable("chat.nep.accelerator_linker.unlinked", format(pos), linked.size())
                            .withStyle(ChatFormatting.YELLOW));
            return InteractionResult.SUCCESS;
        }
        if (linked.size() >= ParticleCollisionControllerBlockEntity.LINKS) {
            message(
                    player,
                    Component.translatable(
                                    "chat.nep.accelerator_linker.full", ParticleCollisionControllerBlockEntity.LINKS)
                            .withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        linked.add(pos);
        save(stack, linked);
        message(
                player,
                Component.translatable(
                                linked.size() == 1
                                        ? "chat.nep.accelerator_linker.linked_fast"
                                        : "chat.nep.accelerator_linker.linked_slow",
                                format(pos))
                        .withStyle(ChatFormatting.GREEN));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOn(context);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player.isShiftKeyDown()) {
            stack.remove(NepOritechContent.LINKER_ACCELERATORS.get());
            message(
                    player,
                    Component.translatable("chat.nep.accelerator_linker.cleared_plan")
                            .withStyle(ChatFormatting.YELLOW));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<BlockPos> linked = accelerators(stack);
        for (int i = 0; i < ParticleCollisionControllerBlockEntity.LINKS; i++) {
            Component value = i < linked.size()
                    ? Component.literal(format(linked.get(i))).withStyle(ChatFormatting.WHITE)
                    : Component.translatable("tooltip.nep.accelerator_linker.unset")
                            .withStyle(ChatFormatting.DARK_GRAY);
            tooltip.add(Component.translatable(
                            i == 0 ? "tooltip.nep.accelerator_linker.fast" : "tooltip.nep.accelerator_linker.slow",
                            value)
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.nep.accelerator_linker.usage.set")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.nep.accelerator_linker.usage.sync")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void syncTo(Player player, ItemStack stack, ParticleCollisionControllerBlockEntity controller) {
        List<BlockPos> linked = accelerators(stack);
        if (linked.size() < ParticleCollisionControllerBlockEntity.LINKS) {
            message(
                    player,
                    Component.translatable("chat.nep.accelerator_linker.incomplete")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        BlockPos origin = controller.getBlockPos();
        Level level = controller.getLevel();
        int range = NepConfig.oritechControllerLinkRange();
        for (BlockPos target : linked) {
            if (!SubLevels.sameSubLevel(level, origin, target)) {
                message(
                        player,
                        Component.translatable("chat.nep.accelerator_linker.different_sub_level", format(target))
                                .withStyle(ChatFormatting.RED));
                return;
            }
            if (outOfRange(origin, target, range)) {
                message(
                        player,
                        Component.translatable("chat.nep.accelerator_linker.too_far", range, format(target))
                                .withStyle(ChatFormatting.RED));
                return;
            }
        }
        if (!controller.applyLinks(linked)) {
            message(
                    player,
                    Component.translatable("chat.nep.accelerator_linker.busy").withStyle(ChatFormatting.RED));
            return;
        }
        message(
                player,
                Component.translatable("chat.nep.accelerator_linker.synced", controller.topSpeed())
                        .withStyle(ChatFormatting.GREEN));
    }

    private static List<BlockPos> accelerators(ItemStack stack) {
        return stack.getOrDefault(NepOritechContent.LINKER_ACCELERATORS.get(), List.of());
    }

    private static void save(ItemStack stack, List<BlockPos> linked) {
        if (linked.isEmpty()) {
            stack.remove(NepOritechContent.LINKER_ACCELERATORS.get());
        } else {
            stack.set(NepOritechContent.LINKER_ACCELERATORS.get(), List.copyOf(linked));
        }
    }

    private static void message(Player player, Component text) {
        player.displayClientMessage(text, false);
    }

    private static boolean outOfRange(BlockPos origin, BlockPos target, int range) {
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
