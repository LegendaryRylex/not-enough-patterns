package dev.rylex.nep.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEBlocks;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rylex.nep.NepItems;
import dev.rylex.nep.provider.ImportTrackerHost;
import dev.rylex.nep.provider.ImportUpgradeHost;
import dev.rylex.nep.provider.OwedImportTracker;
import dev.rylex.nep.provider.OwedSource;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PatternProviderLogic.class, priority = 1500)
public abstract class PatternProviderLogicMixin implements ImportUpgradeHost, ImportTrackerHost {

    @Shadow
    @Final
    private PatternProviderLogicHost host;

    @Shadow
    @Final
    private IManagedGridNode mainNode;

    @Shadow
    @Final
    private IActionSource actionSource;

    @Shadow
    private Direction sendDirection;

    @Unique
    private final OwedImportTracker nep$tracker = new OwedImportTracker();

    @Unique
    private IUpgradeInventory nep$upgrades;

    @Inject(
            method =
                    "<init>(Lappeng/api/networking/IManagedGridNode;Lappeng/helpers/patternprovider/PatternProviderLogicHost;I)V",
            at = @At("RETURN"))
    private void nep$createUpgradeInventory(
            IManagedGridNode mainNode, PatternProviderLogicHost host, int patternInventorySize, CallbackInfo ci) {
        this.nep$upgrades =
                UpgradeInventories.forMachine(AEBlocks.PATTERN_PROVIDER.block(), 1, this::nep$onUpgradesChanged);
    }

    @Override
    public IUpgradeInventory nepImportUpgrades() {
        return nep$upgrades == null ? UpgradeInventories.empty() : nep$upgrades;
    }

    @Override
    public void nepRecordOwed(IPatternDetails pattern, KeyCounter[] inputs, OwedSource source) {
        if (!nep$hasImportCard()) {
            return;
        }
        if (nep$tracker.record(pattern, inputs, source)) {
            mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
            host.saveChanges();
        }
    }

    @WrapOperation(
            method = "pushPattern",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lappeng/api/implementations/blockentities/ICraftingMachine;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;Lnet/minecraft/core/Direction;)Z"))
    private boolean nep$recordMachineDispatch(
            ICraftingMachine machine,
            IPatternDetails pattern,
            KeyCounter[] inputs,
            Direction ejectionDirection,
            Operation<Boolean> original) {
        boolean pushed = original.call(machine, pattern, inputs, ejectionDirection);
        if (pushed) {
            nepRecordOwed(pattern, inputs, new OwedSource.Side(ejectionDirection.getOpposite()));
        }
        return pushed;
    }

    @Inject(
            method = "pushPattern",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lappeng/helpers/patternprovider/PatternProviderLogic;sendStacksOut()Z"))
    private void nep$recordExternalDispatch(
            IPatternDetails patternDetails, KeyCounter[] inputHolder, CallbackInfoReturnable<Boolean> cir) {
        if (sendDirection != null) {
            nepRecordOwed(patternDetails, inputHolder, new OwedSource.Side(sendDirection));
        }
    }

    @Inject(method = "hasWorkToDo", at = @At("RETURN"), cancellable = true)
    private void nep$hasImportWork(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && !nep$tracker.isEmpty() && nep$hasImportCard()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "doWork", at = @At("RETURN"), cancellable = true)
    private void nep$doImportWork(CallbackInfoReturnable<Boolean> cir) {
        if (nep$importOwedOutputs()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void nep$writeToNBT(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        nep$upgrades.writeToNBT(tag, "nepUpgrades", registries);
        nep$tracker.writeToNBT(tag, registries);
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void nep$readFromNBT(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        nep$upgrades.readFromNBT(tag, "nepUpgrades", registries);
        nep$tracker.readFromNBT(tag, registries);
    }

    @Inject(method = "addDrops", at = @At("RETURN"))
    private void nep$addDrops(List<ItemStack> drops, CallbackInfo ci) {
        for (var stack : nep$upgrades) {
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
    }

    @Inject(method = "clearContent", at = @At("RETURN"))
    private void nep$clearContent(CallbackInfo ci) {
        nep$upgrades.clear();
        nep$tracker.clear();
    }

    @Unique
    private void nep$onUpgradesChanged() {
        if (!nep$hasImportCard()) {
            nep$tracker.clear();
        }
        host.saveChanges();
    }

    @Unique
    private boolean nep$hasImportCard() {
        return nep$upgrades != null && nep$upgrades.isInstalled(NepItems.IMPORT_CARD.get());
    }

    @Unique
    private boolean nep$importOwedOutputs() {
        if (nep$tracker.isEmpty() || !nep$hasImportCard() || !mainNode.isActive()) {
            return false;
        }

        var blockEntity = host.getBlockEntity();
        if (!(blockEntity.getLevel() instanceof ServerLevel level)) {
            return false;
        }

        var result = nep$tracker.importOwed(level, blockEntity.getBlockPos(), mainNode.getGrid(), actionSource);
        if (result.changed()) {
            host.saveChanges();
        }
        return result.moved();
    }
}
