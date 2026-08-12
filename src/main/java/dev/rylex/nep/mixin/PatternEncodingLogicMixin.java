package dev.rylex.nep.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.parts.encoding.EncodingMode;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;
import dev.rylex.nep.NepComponents;
import dev.rylex.nep.pattern.NepPattern;
import dev.rylex.nep.pattern.encoding.PatternContents;
import dev.rylex.nep.pattern.encoding.PatternEncodeGuard;
import dev.rylex.nep.pattern.encoding.PatternGrid;
import dev.rylex.nep.pattern.encoding.PatternOrigin;
import dev.rylex.nep.pattern.encoding.PatternRecipeHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PatternEncodingLogic.class)
public abstract class PatternEncodingLogicMixin implements PatternRecipeHolder, PatternEncodeGuard {

    @Unique
    private static final String NEP_RECIPE_TAG = "nepRecipe";

    @Unique
    private static final String NEP_RECIPE_VIEWER_TAG = "nepRecipeViewer";

    @Shadow
    @Final
    private IPatternTerminalLogicHost host;

    @Shadow
    public abstract void setMode(EncodingMode mode);

    @Shadow
    public abstract ConfigInventory getEncodedInputInv();

    @Shadow
    public abstract ConfigInventory getEncodedOutputInv();

    @Shadow
    public abstract void saveChanges();

    @Unique
    private PatternOrigin nep$origin = PatternOrigin.MANUAL;

    @Unique
    @Nullable
    private ItemStack nep$justEncoded;

    @Unique
    private int nep$version;

    @Override
    public void nep$expectEncoded(@Nullable ItemStack pattern) {
        this.nep$justEncoded = pattern == null || pattern.isEmpty() ? null : pattern.copy();
    }

    @Override
    public PatternOrigin nep$origin() {
        return nep$origin;
    }

    @Override
    public void nep$setOrigin(PatternOrigin origin) {
        this.nep$origin = origin;
        this.nep$version++;
        saveChanges();
    }

    @Override
    public int nep$encodingVersion() {
        return nep$version;
    }

    @Inject(method = "onEncodedInputChanged", at = @At("HEAD"))
    private void nep$forgetRecipeOnInputChange(CallbackInfo ci) {
        this.nep$origin = PatternOrigin.MANUAL;
        this.nep$version++;
    }

    @Inject(method = "onEncodedOutputChanged", at = @At("HEAD"))
    private void nep$forgetRecipeOnOutputChange(CallbackInfo ci) {
        this.nep$origin = PatternOrigin.MANUAL;
        this.nep$version++;
    }

    @Inject(method = "loadEncodedPattern", at = @At("HEAD"), cancellable = true)
    private void nep$keepGridOnOwnEncode(ItemStack pattern, CallbackInfo ci) {
        if (nep$justEncoded == null || !ItemStack.isSameItemSameComponents(pattern, nep$justEncoded)) {
            return;
        }
        this.nep$justEncoded = null;
        ci.cancel();
    }

    @Inject(method = "loadEncodedPattern", at = @At("RETURN"))
    private void nep$loadPatternIntoGrid(ItemStack pattern, CallbackInfo ci) {
        if (pattern.isEmpty()) {
            return;
        }
        Level level = host.getLevel();
        if (level == null) {
            return;
        }
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, level);
        if (details == null) {
            return;
        }
        PatternGrid grid = pattern.get(NepComponents.PATTERN_GRID.get());
        if (details instanceof NepPattern nepPattern) {
            setMode(EncodingMode.PROCESSING);
            if (grid != null) {
                grid.restore(getEncodedInputInv(), getEncodedOutputInv());
            } else {
                PatternGrid.fill(getEncodedInputInv(), PatternContents.condenseInputs(details));
                PatternGrid.fill(getEncodedOutputInv(), details.getOutputs());
            }
            this.nep$origin = PatternOrigin.ofRecipe(nepPattern.nepRecipeId());
            this.nep$version++;
            return;
        }
        if (grid != null) {
            grid.restore(getEncodedInputInv(), getEncodedOutputInv());
        }
        Identifier source = pattern.get(NepComponents.SOURCE_RECIPE.get());
        if (source != null) {
            this.nep$origin = PatternOrigin.ofRecipe(source);
            this.nep$version++;
        }
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void nep$writeToNBT(ValueOutput output, CallbackInfo ci) {
        output.storeNullable(NEP_RECIPE_TAG, Identifier.CODEC, nep$origin.recipe());
        if (nep$origin.recipeViewer()) {
            output.putBoolean(NEP_RECIPE_VIEWER_TAG, true);
        }
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void nep$readFromNBT(ValueInput input, CallbackInfo ci) {
        Identifier recipe = input.read(NEP_RECIPE_TAG, Identifier.CODEC).orElse(null);
        this.nep$origin = input.getBooleanOr(NEP_RECIPE_VIEWER_TAG, false)
                ? PatternOrigin.RECIPE_VIEWER
                : PatternOrigin.ofRecipe(recipe);
        this.nep$version++;
    }
}
