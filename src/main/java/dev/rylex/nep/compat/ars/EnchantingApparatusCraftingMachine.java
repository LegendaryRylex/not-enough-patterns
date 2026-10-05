package dev.rylex.nep.compat.ars;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEItems;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.hollingsworth.arsnouveau.common.block.ArcaneCore;
import com.hollingsworth.arsnouveau.common.block.tile.ArcaneCoreTile;
import com.hollingsworth.arsnouveau.common.block.tile.ArcanePedestalTile;
import com.hollingsworth.arsnouveau.common.block.tile.EnchantingApparatusTile;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.CraftedOutputs;
import dev.rylex.nep.machine.PushOutcome;
import dev.rylex.nep.machine.RejectLog;
import dev.rylex.nep.pattern.ApparatusPattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public class EnchantingApparatusCraftingMachine implements ICraftingMachine {

    private static final int MAX_MEMOIZED_REJECTS = 512;
    private static final int SOURCE_RANGE = 10;

    private static final Set<AEItemKey> UNSATISFIABLE = new HashSet<>();
    private static final RejectLog REJECT_LOG = new RejectLog();

    private final EnchantingApparatusTile apparatus;

    public EnchantingApparatusCraftingMachine(EnchantingApparatusTile apparatus) {
        this.apparatus = apparatus;
    }

    static void clearCache() {
        UNSATISFIABLE.clear();
        REJECT_LOG.clear();
    }

    @Nullable
    static EnchantingApparatusCraftingMachine forCore(ArcaneCoreTile core) {
        EnchantingApparatusTile apparatus = apparatusFor(core);
        return apparatus == null ? null : new EnchantingApparatusCraftingMachine(apparatus);
    }

    @Nullable
    static EnchantingApparatusTile apparatusFor(ArcaneCoreTile core) {
        Level level = core.getLevel();
        if (level == null) {
            return null;
        }
        BlockPos corePos = core.getBlockPos();
        for (Direction direction : Direction.values()) {
            BlockPos apparatusPos = corePos.relative(direction);
            BlockState state = level.getBlockState(apparatusPos);
            if (!state.hasProperty(BlockStateProperties.FACING)
                    || state.getValue(BlockStateProperties.FACING) != direction) {
                continue;
            }
            if (level.getBlockEntity(apparatusPos) instanceof EnchantingApparatusTile apparatus) {
                return apparatus;
            }
        }
        return null;
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        ItemStack icon = new ItemStack(BlockRegistry.ENCHANTING_APP_BLOCK.get());
        return new PatternContainerGroup(
                AEItemKey.of(icon), BlockRegistry.ENCHANTING_APP_BLOCK.get().getName(), List.of());
    }

    @Override
    public boolean acceptsPlans() {
        return NepConfig.arsEnchantingApparatus();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = apparatus.getLevel();
        if (level == null || level.isClientSide() || !NepConfig.arsEnchantingApparatus()) {
            return false;
        }

        AEItemKey key = patternDetails.getDefinition();
        if (UNSATISFIABLE.contains(key)) {
            return false;
        }

        PushOutcome outcome = attempt(level, patternDetails, inputs);
        if (outcome.unsatisfiable() && UNSATISFIABLE.size() < MAX_MEMOIZED_REJECTS) {
            UNSATISFIABLE.add(key);
        }
        log(level, patternDetails, outcome);
        return outcome.accepted();
    }

    private PushOutcome attempt(Level level, IPatternDetails patternDetails, KeyCounter[] inputs) {
        if (!(patternDetails instanceof ApparatusPattern)
                && patternDetails.getDefinition().getItem() != AEItems.PROCESSING_PATTERN.asItem()) {
            return PushOutcome.unsatisfiable("not an apparatus or processing pattern");
        }

        ArsRecipeResolver.ApparatusPlan plan = ArsRecipeResolver.resolve(patternDetails, level);
        if (plan == null) {
            return !(patternDetails instanceof ApparatusPattern)
                            && ArsRecipeResolver.apparatusMatchCount(patternDetails, level) > 1
                    ? PushOutcome.unsatisfiable(
                            "more than one enchanting apparatus recipe fits the pattern; encode it from JEI instead")
                    : PushOutcome.unsatisfiable("no enchanting apparatus recipe matched the pattern");
        }

        Map<AEItemKey, Long> provided = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return PushOutcome.unsatisfiable("the enchanting apparatus takes items only");
                }
                provided.merge(itemKey, entry.getLongValue(), Long::sum);
            }
        }
        GenericStack declared = plan.result();
        if (!provided.equals(plan.expectedItems())) {
            ArsRecipeResolver.ApparatusPlan reassigned = ArsRecipeResolver.planFromProvided(plan, provided, level);
            if (reassigned == null) {
                return PushOutcome.unsatisfiable(
                        "pushed items " + provided + " do not match the recipe's inputs " + plan.expectedItems());
            }
            plan = reassigned;
        }

        PushOutcome misbuilt = misbuilt(level);
        if (misbuilt != null) {
            return misbuilt;
        }
        if (apparatus.isCrafting) {
            return PushOutcome.retry("the apparatus is already crafting");
        }
        if (!apparatus.getStack().isEmpty()) {
            return PushOutcome.retry("the apparatus still holds a finished item");
        }

        List<ArcanePedestalTile> pedestals = new ArrayList<>();
        for (BlockPos pos : apparatus.pedestalList()) {
            if (level.getBlockEntity(pos) instanceof ArcanePedestalTile pedestal) {
                if (!pedestal.getStack().isEmpty()) {
                    return PushOutcome.retry("pedestal at " + pos + " is already holding an item");
                }
                pedestals.add(pedestal);
            }
        }
        if (pedestals.size() < plan.pedestals().size()) {
            return PushOutcome.retry(
                    "recipe needs " + plan.pedestals().size() + " arcane pedestals, found " + pedestals.size());
        }

        if (plan.sourceCost() > 0
                && !SourceUtil.hasSourceNearby(apparatus.getBlockPos(), level, SOURCE_RANGE, plan.sourceCost())) {
            return PushOutcome.retry("not enough source nearby for " + plan.sourceCost());
        }

        PushOutcome committed = commit(plan, pedestals);
        if (committed.accepted()
                && plan.result().what() instanceof AEItemKey crafted
                && declared.what() instanceof AEItemKey promised) {
            CraftedOutputs.expect(crafted, promised);
        }
        return committed;
    }

    @Nullable
    private PushOutcome misbuilt(Level level) {
        BlockState state = apparatus.getBlockState();
        if (!state.hasProperty(BlockStateProperties.FACING)) {
            return PushOutcome.retry("the apparatus has no facing");
        }
        Direction facing = state.getValue(BlockStateProperties.FACING);
        BlockState core = level.getBlockState(apparatus.getBlockPos().relative(facing.getOpposite()));
        if (!(core.getBlock() instanceof ArcaneCore)) {
            return PushOutcome.retry("the apparatus has no Arcane Core behind it");
        }
        if (core.getValue(BlockStateProperties.FACING).getAxis() != facing.getAxis()) {
            return PushOutcome.retry("the Arcane Core behind the apparatus faces the wrong way");
        }
        return null;
    }

    private PushOutcome commit(ArsRecipeResolver.ApparatusPlan plan, List<ArcanePedestalTile> pedestals) {
        for (int slot = 0; slot < plan.pedestals().size(); slot++) {
            pedestals
                    .get(slot)
                    .setStack(ArsRecipeResolver.toStack(plan.pedestals().get(slot)));
        }

        ItemStack reagent = ArsRecipeResolver.toStack(plan.reagent());
        apparatus.setStack(reagent);
        if (!apparatus.attemptCraft(reagent, null)) {
            rollback(plan, pedestals);
            return PushOutcome.retry("the apparatus refused the craft");
        }
        apparatus.setChanged();
        return PushOutcome.accepted("enchanting " + plan.result());
    }

    private void rollback(ArsRecipeResolver.ApparatusPlan plan, List<ArcanePedestalTile> pedestals) {
        apparatus.setStack(ItemStack.EMPTY);
        for (int slot = 0; slot < plan.pedestals().size(); slot++) {
            pedestals.get(slot).setStack(ItemStack.EMPTY);
        }
    }

    private void log(Level level, IPatternDetails patternDetails, PushOutcome outcome) {
        if (!NepConfig.debugLogging()) {
            return;
        }
        if (!outcome.accepted()
                && !outcome.unsatisfiable()
                && !REJECT_LOG.shouldLog(level, apparatus.getBlockPos(), outcome.detail())) {
            return;
        }
        Nep.LOGGER.info(
                "Enchanting Apparatus {} {} push: {} (output={})",
                apparatus.getBlockPos(),
                outcome.accepted() ? "accepted" : "rejected",
                outcome.detail(),
                patternDetails.getOutputs().isEmpty()
                        ? "?"
                        : patternDetails.getOutputs().get(0).toString());
    }
}
