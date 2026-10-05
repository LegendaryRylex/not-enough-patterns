package dev.rylex.nep.hub;

import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.tooltip.TooltipLocation;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;

@EventBusSubscriber(modid = Nep.MOD_ID)
public class HubLinkerItem extends Item {

    public HubLinkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide() || player == null) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().immutable();
        boolean sneaking = player.isShiftKeyDown();

        MachineHubBlockEntity hub = MachineHubBlockEntity.at(level, pos);
        if (hub != null) {
            if (sneaking) {
                hub.clearLinks();
                message(player, Component.translatable("chat.nep.hub_linker.cleared_hub"), ChatFormatting.YELLOW);
            } else {
                syncTo(player, stack, hub);
            }
            return InteractionResult.SUCCESS;
        }

        if (sneaking) {
            cycleMode(player, stack);
            return InteractionResult.SUCCESS;
        }

        List<HubLink> plan = new ArrayList<>(plan(stack));
        int existing = indexOf(plan, pos);
        if (existing >= 0) {
            plan.remove(existing);
            savePlan(stack, plan);
            message(
                    player,
                    Component.translatable("chat.nep.hub_linker.unlinked", format(pos), plan.size()),
                    ChatFormatting.YELLOW);
            return InteractionResult.SUCCESS;
        }

        int limit = NepConfig.machineHubMaximumLinks();
        if (plan.size() >= limit) {
            message(player, Component.translatable("chat.nep.hub_linker.full", limit), ChatFormatting.RED);
            return InteractionResult.SUCCESS;
        }
        if (HubScan.networkBlock(level, pos)) {
            message(
                    player,
                    Component.translatable("chat.nep.hub_linker.network_block", format(pos)),
                    ChatFormatting.RED);
            return InteractionResult.SUCCESS;
        }
        if (HubRules.load().unlinkable(level.getBlockState(pos))) {
            message(player, Component.translatable("chat.nep.hub_linker.unlinkable", format(pos)), ChatFormatting.RED);
            return InteractionResult.SUCCESS;
        }
        if (!HubScan.linkable(level, pos)) {
            message(
                    player,
                    Component.translatable("chat.nep.hub_linker.no_inventory", format(pos)),
                    ChatFormatting.RED);
            return InteractionResult.SUCCESS;
        }

        HubRole role = mode(stack);
        plan.add(new HubLink(pos, role));
        savePlan(stack, plan);
        message(
                player,
                Component.translatable(
                        "chat.nep.hub_linker.linked",
                        format(pos),
                        role.displayName().copy().withStyle(ChatFormatting.WHITE),
                        plan.size()),
                ChatFormatting.GREEN);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOn(context);
    }

    @Override
    public boolean canDestroyBlock(ItemStack stack, BlockState state, Level level, BlockPos pos, LivingEntity user) {
        return false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            stack.remove(NepContent.HUB_PLAN.get());
            message(player, Component.translatable("chat.nep.hub_linker.cleared_plan"), ChatFormatting.YELLOW);
        } else {
            cycleMode(player, stack);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @SubscribeEvent
    static void registerTooltipAppenders(RegisterTooltipAppendersEvent event) {
        event.registerAppender(
                TooltipLocation.POST_CUSTOM, (stack, _, _, _, _, tooltip) -> appendLinkerTooltip(stack, tooltip));
    }

    private static void appendLinkerTooltip(ItemStack stack, Consumer<Component> tooltip) {
        if (!(stack.getItem() instanceof HubLinkerItem)) {
            return;
        }
        List<HubLink> plan = plan(stack);
        tooltip.accept(entry(
                "tooltip.nep.hub_linker.mode", mode(stack).displayName().copy().withStyle(ChatFormatting.WHITE)));
        tooltip.accept(entry(
                "tooltip.nep.hub_linker.links",
                Component.literal(String.valueOf(plan.size())).withStyle(ChatFormatting.WHITE)));
        for (HubLink link : plan) {
            tooltip.accept(Component.translatable(
                            "tooltip.nep.hub_linker.link", link.role().glyph(), format(link.pos()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.accept(
                Component.translatable("tooltip.nep.hub_linker.usage.set").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(
                Component.translatable("tooltip.nep.hub_linker.usage.sync").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void syncTo(Player player, ItemStack stack, MachineHubBlockEntity hub) {
        List<HubLink> plan = plan(stack);
        if (plan.isEmpty()) {
            message(player, Component.translatable("chat.nep.hub_linker.empty"), ChatFormatting.RED);
            return;
        }
        List<Component> rejected = new ArrayList<>();
        for (HubLink link : plan) {
            if (!hub.canLink(link.pos())) {
                rejected.add(Component.literal(format(link.pos())));
            }
        }
        if (!rejected.isEmpty()) {
            message(
                    player,
                    Component.translatable(
                            "chat.nep.hub_linker.rejected", NepConfig.machineHubLinkRange(), join(rejected)),
                    ChatFormatting.RED);
            return;
        }
        int linked = hub.applyPlan(plan);
        message(player, Component.translatable("chat.nep.hub_linker.synced", linked), ChatFormatting.GREEN);
    }

    private static void cycleMode(Player player, ItemStack stack) {
        HubRole next = mode(stack).next();
        stack.set(NepContent.HUB_MODE.get(), next);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                    entry(
                            "chat.nep.hub_linker.mode_set",
                            next.displayName().copy().withStyle(ChatFormatting.WHITE)),
                    true);
        }
    }

    private static void savePlan(ItemStack stack, List<HubLink> plan) {
        if (plan.isEmpty()) {
            stack.remove(NepContent.HUB_PLAN.get());
        } else {
            stack.set(NepContent.HUB_PLAN.get(), List.copyOf(plan));
        }
    }

    private static int indexOf(List<HubLink> plan, BlockPos pos) {
        for (int i = 0; i < plan.size(); i++) {
            if (plan.get(i).pos().equals(pos)) {
                return i;
            }
        }
        return -1;
    }

    public static List<HubLink> plan(ItemStack stack) {
        return stack.getOrDefault(NepContent.HUB_PLAN.get(), List.of());
    }

    private static HubRole mode(ItemStack stack) {
        return stack.getOrDefault(NepContent.HUB_MODE.get(), HubRole.INPUT);
    }

    private static Component entry(String key, Component value) {
        return Component.translatable(key, value).withStyle(ChatFormatting.GRAY);
    }

    private static void message(Player player, Component text, ChatFormatting colour) {
        player.sendSystemMessage(text.copy().withStyle(colour));
    }

    private static Component join(List<Component> parts) {
        MutableComponent joined = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                joined.append(", ");
            }
            joined.append(parts.get(i));
        }
        return joined;
    }

    private static String format(BlockPos pos) {
        return Component.translatable("gui.nep.coords", pos.getX(), pos.getY(), pos.getZ())
                .getString();
    }
}
