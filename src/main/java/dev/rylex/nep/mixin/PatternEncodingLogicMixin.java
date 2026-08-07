package dev.rylex.nep.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.parts.encoding.EncodingMode;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;
import dev.rylex.nep.pattern.NepPattern;
import dev.rylex.nep.pattern.encoding.PatternContents;
import dev.rylex.nep.pattern.encoding.PatternRecipeHolder;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PatternEncodingLogic.class)
public abstract class PatternEncodingLogicMixin implements PatternRecipeHolder {

    @Unique
    private static final String NEP_RECIPE_TAG = "nepRecipe";

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
    @Nullable
    private ResourceLocation nep$recipe;

    @Unique
    private int nep$version;

    @Override
    @Nullable
    public ResourceLocation nep$recipeId() {
        return nep$recipe;
    }

    @Override
    public void nep$setRecipeId(@Nullable ResourceLocation recipe) {
        this.nep$recipe = recipe;
        this.nep$version++;
        saveChanges();
    }

    @Override
    public int nep$encodingVersion() {
        return nep$version;
    }

    @Inject(method = "onEncodedInputChanged", at = @At("HEAD"))
    private void nep$forgetRecipeOnInputChange(CallbackInfo ci) {
        this.nep$recipe = null;
        this.nep$version++;
    }

    @Inject(method = "onEncodedOutputChanged", at = @At("HEAD"))
    private void nep$forgetRecipeOnOutputChange(CallbackInfo ci) {
        this.nep$recipe = null;
        this.nep$version++;
    }

    @Inject(method = "loadEncodedPattern", at = @At("HEAD"))
    private void nep$loadPatternIntoGrid(ItemStack pattern, CallbackInfo ci) {
        if (pattern.isEmpty()) {
            return;
        }
        Level level = host.getLevel();
        if (level == null) {
            return;
        }
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, level);
        if (!(details instanceof NepPattern nepPattern)) {
            return;
        }
        setMode(EncodingMode.PROCESSING);
        nep$fill(getEncodedInputInv(), PatternContents.condenseInputs(details));
        nep$fill(getEncodedOutputInv(), details.getOutputs());
        this.nep$recipe = nepPattern.nepRecipeId();
        this.nep$version++;
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void nep$writeToNBT(CompoundTag data, HolderLookup.Provider registries, CallbackInfo ci) {
        if (nep$recipe != null) {
            data.putString(NEP_RECIPE_TAG, nep$recipe.toString());
        }
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void nep$readFromNBT(CompoundTag data, HolderLookup.Provider registries, CallbackInfo ci) {
        this.nep$recipe = data.contains(NEP_RECIPE_TAG, Tag.TAG_STRING)
                ? ResourceLocation.tryParse(data.getString(NEP_RECIPE_TAG))
                : null;
        this.nep$version++;
    }

    @Unique
    private static void nep$fill(ConfigInventory inv, List<GenericStack> stacks) {
        inv.beginBatch();
        try {
            for (int i = 0; i < inv.size(); i++) {
                inv.setStack(i, i < stacks.size() ? stacks.get(i) : null);
            }
        } finally {
            inv.endBatch();
        }
    }
}
