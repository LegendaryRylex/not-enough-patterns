package dev.rylex.nep.mixin;

import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.encoding.EncodingMode;
import appeng.parts.encoding.PatternEncodingLogic;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.rylex.nep.net.PatternRecipePayload;
import dev.rylex.nep.pattern.encoding.PatternConverters;
import dev.rylex.nep.pattern.encoding.PatternRecipeHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PatternEncodingTermMenu.class)
public abstract class PatternEncodingTermMenuMixin implements PatternRecipeHolder {

    @Shadow
    @Final
    private PatternEncodingLogic encodingLogic;

    @Override
    @Nullable
    public ResourceLocation nep$recipeId() {
        return ((PatternRecipeHolder) encodingLogic).nep$recipeId();
    }

    @Override
    public void nep$setRecipeId(@Nullable ResourceLocation recipe) {
        PatternEncodingTermMenu menu = (PatternEncodingTermMenu) (Object) this;
        if (menu.getPlayer().level().isClientSide()) {
            PacketDistributor.sendToServer(PatternRecipePayload.of(recipe));
        } else {
            ((PatternRecipeHolder) encodingLogic).nep$setRecipeId(recipe);
        }
    }

    @ModifyExpressionValue(
            method = "encode",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lappeng/menu/me/items/PatternEncodingTermMenu;encodePattern()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack nep$convertEncodedPattern(@Nullable ItemStack encoded) {
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
        ItemStack converted = PatternConverters.convert(nep$recipeId(), encoded, player);
        return converted == null ? encoded : converted;
    }
}
