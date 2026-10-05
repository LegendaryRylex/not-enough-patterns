package dev.rylex.nep.provider;

import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.util.prioritylist.IPartitionList;
import java.util.List;

/** Operations are counted in the owed key's own units, so a budget of 1000 means 1000 mB rather than 1000 buckets. */
public final class OwedImportContext implements StackTransferContext {

    private final IPartitionList filter = new IPartitionList() {
        @Override
        public boolean isListed(AEKey input) {
            return owed.equals(input);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Iterable<AEKey> getItems() {
            return List.of(owed);
        }
    };

    private final IStorageService internalStorage;
    private final IEnergySource energySource;
    private final IActionSource actionSource;
    private final AEKey owed;
    private final int budget;
    private int operationsRemaining;

    public OwedImportContext(
            IStorageService internalStorage,
            IEnergySource energySource,
            IActionSource actionSource,
            AEKey owed,
            int budget) {
        this.internalStorage = internalStorage;
        this.energySource = energySource;
        this.actionSource = actionSource;
        this.owed = owed;
        this.budget = budget;
        this.operationsRemaining = budget;
    }

    @Override
    public IStorageService getInternalStorage() {
        return internalStorage;
    }

    @Override
    public IEnergySource getEnergySource() {
        return energySource;
    }

    @Override
    public IActionSource getActionSource() {
        return actionSource;
    }

    @Override
    public int getOperationsRemaining() {
        return operationsRemaining;
    }

    @Override
    public void setOperationsRemaining(int operationsRemaining) {
        this.operationsRemaining = operationsRemaining;
    }

    @Override
    public boolean hasOperationsLeft() {
        return operationsRemaining > 0;
    }

    @Override
    public boolean hasDoneWork() {
        return operationsRemaining < budget;
    }

    public long moved() {
        return budget - operationsRemaining;
    }

    @Override
    public boolean isKeyTypeEnabled(AEKeyType space) {
        return space == owed.getType();
    }

    @Override
    public boolean isInFilter(AEKey key) {
        return owed.equals(key);
    }

    @Override
    public IPartitionList getFilter() {
        return filter;
    }

    @Override
    public void setInverted(boolean inverted) {}

    @Override
    public boolean isInverted() {
        return false;
    }

    @Override
    public boolean canInsert(AEItemKey what, long amount) {
        return owed.equals(what)
                && internalStorage.getInventory().insert(what, amount, Actionable.SIMULATE, actionSource) > 0;
    }

    @Override
    public void reduceOperationsRemaining(long inserted) {
        operationsRemaining -= (int) Math.min(inserted, operationsRemaining);
    }
}
