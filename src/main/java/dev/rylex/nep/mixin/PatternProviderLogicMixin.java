package dev.rylex.nep.mixin;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEBlocks;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.parts.automation.StackWorldBehaviors;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rylex.nep.NepItems;
import dev.rylex.nep.provider.OwedImportContext;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

@Mixin(PatternProviderLogic.class)
public abstract class PatternProviderLogicMixin implements IUpgradeableObject {

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
    private final Map<Direction, Map<AEItemKey, Long>> nep$owed = new EnumMap<>(Direction.class);

    @Unique
    private final Map<Direction, StackImportStrategy> nep$strategies = new EnumMap<>(Direction.class);

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
    public IUpgradeInventory getUpgrades() {
        return nep$upgrades == null ? UpgradeInventories.empty() : nep$upgrades;
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
            nep$recordOwed(pattern, ejectionDirection.getOpposite());
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
            nep$recordOwed(patternDetails, sendDirection);
        }
    }

    @Inject(method = "hasWorkToDo", at = @At("RETURN"), cancellable = true)
    private void nep$hasImportWork(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && !nep$owed.isEmpty() && nep$hasImportCard()) {
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

        var owedTag = new ListTag();
        for (var sideEntry : nep$owed.entrySet()) {
            for (var entry : sideEntry.getValue().entrySet()) {
                var entryTag = new CompoundTag();
                entryTag.putByte("side", (byte) sideEntry.getKey().get3DDataValue());
                entryTag.put(
                        "stack", GenericStack.writeTag(registries, new GenericStack(entry.getKey(), entry.getValue())));
                owedTag.add(entryTag);
            }
        }
        tag.put("nepOwed", owedTag);
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void nep$readFromNBT(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        nep$upgrades.readFromNBT(tag, "nepUpgrades", registries);

        nep$owed.clear();
        nep$strategies.clear();

        var owedTag = tag.getList("nepOwed", Tag.TAG_COMPOUND);
        for (int i = 0; i < owedTag.size(); i++) {
            var entryTag = owedTag.getCompound(i);
            var stack = GenericStack.readTag(registries, entryTag.getCompound("stack"));
            if (stack == null || stack.amount() <= 0 || !(stack.what() instanceof AEItemKey key)) {
                continue;
            }
            nep$owed.computeIfAbsent(Direction.from3DDataValue(entryTag.getByte("side")), s -> new LinkedHashMap<>())
                    .merge(key, stack.amount(), Long::sum);
        }
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
        nep$owed.clear();
        nep$strategies.clear();
    }

    @Unique
    private void nep$onUpgradesChanged() {
        if (!nep$hasImportCard()) {
            nep$owed.clear();
            nep$strategies.clear();
        }
        host.saveChanges();
    }

    @Unique
    private boolean nep$hasImportCard() {
        return nep$upgrades != null && nep$upgrades.isInstalled(NepItems.IMPORT_CARD.get());
    }

    @Unique
    private void nep$recordOwed(IPatternDetails pattern, Direction side) {
        if (!nep$hasImportCard()) {
            return;
        }

        boolean recorded = false;
        for (var output : pattern.getOutputs()) {
            if (output.amount() > 0 && output.what() instanceof AEItemKey key) {
                nep$owed.computeIfAbsent(side, s -> new LinkedHashMap<>()).merge(key, output.amount(), Long::sum);
                recorded = true;
            }
        }

        if (recorded) {
            mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
            host.saveChanges();
        }
    }

    @Unique
    private boolean nep$importOwedOutputs() {
        if (nep$owed.isEmpty() || !nep$hasImportCard() || !mainNode.isActive()) {
            return false;
        }

        var blockEntity = host.getBlockEntity();
        if (!(blockEntity.getLevel() instanceof ServerLevel level)) {
            return false;
        }

        var grid = mainNode.getGrid();
        var crafting = grid.getCraftingService();
        var storage = grid.getStorageService();
        var energy = grid.getEnergyService();

        boolean moved = false;
        var sides = nep$owed.entrySet().iterator();
        while (sides.hasNext()) {
            var sideEntry = sides.next();
            var side = sideEntry.getKey();
            var entries = sideEntry.getValue().entrySet().iterator();

            while (entries.hasNext()) {
                var entry = entries.next();
                var key = entry.getKey();

                long live = Math.min(entry.getValue(), crafting.getRequestedAmount(key));
                if (live <= 0) {
                    entries.remove();
                    continue;
                }

                long before = storage.getInventory().extract(key, Long.MAX_VALUE, Actionable.SIMULATE, actionSource);
                var context = new OwedImportContext(
                        storage, energy, actionSource, key, (int) Math.min(live, Integer.MAX_VALUE));
                nep$strategyFor(level, blockEntity.getBlockPos(), side).transfer(context);

                long after = storage.getInventory().extract(key, Long.MAX_VALUE, Actionable.SIMULATE, actionSource);
                long imported = Math.max(0, after - before);
                if (imported > 0) {
                    moved = true;
                    long remaining = entry.getValue() - imported;
                    if (remaining <= 0) {
                        entries.remove();
                    } else {
                        entry.setValue(remaining);
                    }
                }
            }

            if (sideEntry.getValue().isEmpty()) {
                nep$strategies.remove(side);
                sides.remove();
            }
        }

        if (moved) {
            host.saveChanges();
        }
        return moved;
    }

    @Unique
    private StackImportStrategy nep$strategyFor(ServerLevel level, BlockPos pos, Direction side) {
        return nep$strategies.computeIfAbsent(
                side,
                s -> StackWorldBehaviors.createImportFacade(
                        level, pos.relative(s), s.getOpposite(), type -> type == AEKeyType.items()));
    }
}
