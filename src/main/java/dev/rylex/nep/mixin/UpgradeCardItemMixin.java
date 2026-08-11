package dev.rylex.nep.mixin;

import appeng.api.parts.IPartHost;
import appeng.core.localization.PlayerMessages;
import appeng.items.materials.UpgradeCardItem;
import appeng.util.InteractionUtil;
import dev.rylex.nep.NepItems;
import dev.rylex.nep.provider.ImportUpgradeHost;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(UpgradeCardItem.class)
public abstract class UpgradeCardItemMixin {

    @Inject(method = "onItemUseFirst", at = @At("HEAD"), cancellable = true)
    private void nep$installImportCard(
            ItemStack stack, UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!stack.is(NepItems.IMPORT_CARD.get())) {
            return;
        }

        var player = context.getPlayer();
        if (player == null || !InteractionUtil.isInAlternateUseMode(player)) {
            return;
        }

        var host = nep$resolveHost(context);
        if (host == null) {
            return;
        }

        var upgrades = host.nepImportUpgrades();
        if (upgrades.size() == 0) {
            return;
        }

        var hand = context.getHand();
        var heldStack = player.getItemInHand(hand);
        int maxInstalled = upgrades.getMaxInstalled(heldStack.getItem());
        if (maxInstalled <= 0) {
            return;
        }

        if (upgrades.getInstalledUpgrades(heldStack.getItem()) >= maxInstalled) {
            player.sendOverlayMessage(PlayerMessages.MaxUpgradesOfTypeInstalled.text());
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        if (player.level().isClientSide()) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }

        player.setItemInHand(hand, upgrades.addItems(heldStack));
        cir.setReturnValue(InteractionResult.CONSUME);
    }

    @Unique
    @Nullable
    private ImportUpgradeHost nep$resolveHost(UseOnContext context) {
        var blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (blockEntity instanceof IPartHost partHost) {
            var selected = partHost.selectPartWorld(context.getClickLocation());
            return selected.part instanceof ImportUpgradeHost host ? host : null;
        }
        return blockEntity instanceof ImportUpgradeHost host ? host : null;
    }
}
