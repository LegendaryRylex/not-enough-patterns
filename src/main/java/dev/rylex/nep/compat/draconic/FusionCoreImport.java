package dev.rylex.nep.compat.draconic;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** A fusion core hands its result to whoever right-clicks it, so there is no inventory an import facade could read. */
final class FusionCoreImport implements StackImportStrategy {

    private final ServerLevel level;
    private final BlockPos pos;

    private FusionCoreImport(ServerLevel level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    static StackImportStrategy create(ServerLevel level, BlockPos pos) {
        return new FusionCoreImport(level, pos);
    }

    @Override
    public boolean transfer(StackTransferContext context) {
        if (!context.isKeyTypeEnabled(AEKeyType.items())
                || !(level.getBlockEntity(pos) instanceof TileFusionCraftingCore core)) {
            return false;
        }
        ItemStack output = FusionResults.normalize(core.getOutputStack());
        AEItemKey key = AEItemKey.of(output);
        if (key == null || !context.isInFilter(key)) {
            return false;
        }
        int wanted = Math.min(context.getOperationsRemaining(), output.getCount());
        if (wanted <= 0) {
            return false;
        }
        long inserted = context.getInternalStorage()
                .getInventory()
                .insert(key, wanted, Actionable.MODULATE, context.getActionSource());
        if (inserted <= 0) {
            return false;
        }
        core.setOutputStack(output.copyWithCount(output.getCount() - (int) inserted));
        core.setChanged();
        context.reduceOperationsRemaining(inserted);
        return true;
    }
}
