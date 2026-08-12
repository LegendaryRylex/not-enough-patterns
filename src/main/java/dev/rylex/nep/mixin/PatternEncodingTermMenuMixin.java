package dev.rylex.nep.mixin;

import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.FakeSlot;
import appeng.parts.encoding.EncodingMode;
import appeng.parts.encoding.PatternEncodingLogic;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.rylex.nep.NepComponents;
import dev.rylex.nep.net.PatternRecipePayload;
import dev.rylex.nep.net.RetainedSlotsPayload;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternEncodeGuard;
import dev.rylex.nep.pattern.encoding.PatternGrid;
import dev.rylex.nep.pattern.encoding.PatternOrigin;
import dev.rylex.nep.pattern.encoding.PatternRecipeHolder;
import dev.rylex.nep.pattern.encoding.RetainedSlotHolder;
import dev.rylex.nep.pattern.encoding.RetainedSlots;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PatternEncodingTermMenu.class)
public abstract class PatternEncodingTermMenuMixin implements PatternRecipeHolder, RetainedSlotHolder {

    @Shadow
    @Final
    private PatternEncodingLogic encodingLogic;

    @Shadow
    @Final
    private FakeSlot[] processingInputSlots;

    @Unique
    private List<Integer> nep$retainedSlots = List.of();

    @Unique
    private int nep$syncedVersion = Integer.MIN_VALUE;

    @Override
    public PatternOrigin nep$origin() {
        return ((PatternRecipeHolder) encodingLogic).nep$origin();
    }

    @Override
    public void nep$setOrigin(PatternOrigin origin) {
        PatternEncodingTermMenu menu = (PatternEncodingTermMenu) (Object) this;
        if (menu.getPlayer().level().isClientSide()) {
            ClientPacketDistributor.sendToServer(new PatternRecipePayload(origin));
        } else {
            ((PatternRecipeHolder) encodingLogic).nep$setOrigin(origin);
        }
    }

    @Override
    public void nep$setRetainedSlots(List<Integer> slots) {
        this.nep$retainedSlots = List.copyOf(slots);
    }

    @Override
    public boolean nep$isRetainedSlot(@Nullable Slot slot) {
        if (slot == null || nep$retainedSlots.isEmpty()) {
            return false;
        }
        for (int i = 0; i < processingInputSlots.length; i++) {
            if (processingInputSlots[i] == slot) {
                return nep$retainedSlots.contains(i);
            }
        }
        return false;
    }

    @Inject(method = "broadcastChanges", at = @At("RETURN"))
    private void nep$syncRetainedSlots(CallbackInfo ci) {
        PatternEncodingTermMenu menu = (PatternEncodingTermMenu) (Object) this;
        if (!(menu.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        int version = ((PatternRecipeHolder) encodingLogic).nep$encodingVersion();
        if (version == nep$syncedVersion) {
            return;
        }
        nep$syncedVersion = version;
        List<Integer> slots = RetainedSlots.compute(encodingLogic, nep$origin(), player.level());
        if (slots.equals(nep$retainedSlots)) {
            return;
        }
        nep$retainedSlots = slots;
        PacketDistributor.sendToPlayer(player, new RetainedSlotsPayload(slots));
    }

    @ModifyExpressionValue(
            method = "encode",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lappeng/menu/me/items/PatternEncodingTermMenu;encodePattern()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack nep$convertEncodedPattern(@Nullable ItemStack encoded) {
        ItemStack result = nep$encodeResult(encoded);
        ((PatternEncodeGuard) encodingLogic).nep$expectEncoded(result);
        return result;
    }

    @Inject(method = "encode", at = @At("RETURN"))
    private void nep$forgetEncodeResult(CallbackInfo ci) {
        ((PatternEncodeGuard) encodingLogic).nep$expectEncoded(null);
    }

    @Unique
    @Nullable
    private ItemStack nep$encodeResult(@Nullable ItemStack encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return encoded;
        }
        PatternEncodingTermMenu menu = (PatternEncodingTermMenu) (Object) this;
        if (encodingLogic.getMode() != EncodingMode.PROCESSING) {
            return encoded;
        }
        Player player = menu.getPlayer();
        if (player.level().isClientSide()) {
            return encoded;
        }
        ItemStack converted = PatternConverters.convert(nep$origin(), encoded, player);
        if (converted == null) {
            return encoded;
        }
        PatternGrid grid = PatternGrid.capture(encodingLogic.getEncodedInputInv(), encodingLogic.getEncodedOutputInv());
        if (!grid.isEmpty()) {
            converted.set(NepComponents.PATTERN_GRID.get(), grid);
        }
        return converted;
    }
}
