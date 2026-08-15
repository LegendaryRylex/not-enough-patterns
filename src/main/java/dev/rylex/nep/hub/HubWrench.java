package dev.rylex.nep.hub;

import dev.rylex.nep.Nep;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Nep.MOD_ID)
public final class HubWrench {

    private HubWrench() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        ItemStack tool = event.getItemStack();
        if (player.isSpectator() || !player.isSecondaryUseActive() || !tool.is(Tags.Items.TOOLS_WRENCH)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (MachineHubBlockEntity.at(level, pos) == null || !player.mayBuild() || !level.mayInteract(player, pos)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(dismantle(level, pos, player, tool));
    }

    public static InteractionResult dismantle(Level level, BlockPos pos, Player player, ItemStack tool) {
        MachineHubBlockEntity hub = MachineHubBlockEntity.at(level, pos);
        if (hub == null) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            List<HubLink> links = hub.links();
            for (ItemStack drop : Block.getDrops(level.getBlockState(pos), serverLevel, pos, hub, player, tool)) {
                if (!links.isEmpty() && drop.is(NepContent.MACHINE_HUB_ITEM.get())) {
                    drop.set(NepContent.HUB_PLAN.get(), links);
                }
                player.getInventory().placeItemBackInInventory(drop);
            }
            if (!links.isEmpty() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(kept(links.size()).withStyle(ChatFormatting.GREEN), true);
            }
        }
        level.playSound(player, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.7F, 1.0F);
        level.removeBlock(pos, false);
        return InteractionResult.SUCCESS;
    }

    private static MutableComponent kept(int links) {
        return links == 1
                ? Component.translatable("chat.nep.machine_hub.dismantled")
                : Component.translatable("chat.nep.machine_hub.dismantled.plural", links);
    }
}
