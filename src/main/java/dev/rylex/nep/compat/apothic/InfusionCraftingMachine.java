package dev.rylex.nep.compat.apothic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEItems;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.machine.PushOutcome;
import dev.rylex.nep.machine.RejectLog;
import dev.rylex.nep.pattern.EnchantingPattern;
import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.table.RavenTableStats;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class InfusionCraftingMachine implements ICraftingMachine {

    private static final int MAX_MEMOIZED_REJECTS = 512;

    private static final Set<AEItemKey> UNSATISFIABLE = new HashSet<>();
    private static final RejectLog REJECT_LOG = new RejectLog();

    private final EnchantingTableBlockEntity table;

    public InfusionCraftingMachine(EnchantingTableBlockEntity table) {
        this.table = table;
    }

    static void clearCache() {
        UNSATISFIABLE.clear();
        REJECT_LOG.clear();
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        ItemStack icon = new ItemStack(Ench.Items.RAVEN_ENCHANTING_TABLE.value());
        return new PatternContainerGroup(
                AEItemKey.of(icon), Ench.Blocks.RAVEN_ENCHANTING_TABLE.value().getName(), List.of());
    }

    @Override
    public boolean acceptsPlans() {
        return NepConfig.apothicInfusion();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        Level level = table.getLevel();
        if (level == null || level.isClientSide() || !NepConfig.apothicInfusion()) {
            return false;
        }

        AEItemKey key = patternDetails.getDefinition();
        if (UNSATISFIABLE.contains(key)) {
            return false;
        }

        PushOutcome outcome = attempt(level, patternDetails, inputs, ejectionDirection);
        if (outcome.unsatisfiable() && UNSATISFIABLE.size() < MAX_MEMOIZED_REJECTS) {
            UNSATISFIABLE.add(key);
        }
        log(level, patternDetails, outcome);
        return outcome.accepted();
    }

    private PushOutcome attempt(
            Level level, IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        if (!(patternDetails instanceof EnchantingPattern)
                && patternDetails.getDefinition().getItem() != AEItems.PROCESSING_PATTERN.asItem()) {
            return PushOutcome.unsatisfiable("not an enchanting or processing pattern");
        }

        InfusionRecipeResolver.Plan plan =
                InfusionRecipeResolver.resolve(patternDetails, level, InfusionCosts.Rates.fromConfig());
        if (plan == null) {
            return PushOutcome.unsatisfiable("no infusion recipe matched the pattern");
        }

        Map<AEKey, Long> provided = new HashMap<>();
        for (KeyCounter counter : inputs) {
            for (var entry : counter) {
                if (entry.getLongValue() <= 0) {
                    return PushOutcome.unsatisfiable("an input was pushed with no amount");
                }
                provided.merge(entry.getKey(), entry.getLongValue(), Long::sum);
            }
        }

        if (!provided.equals(plan.expected())) {
            return PushOutcome.unsatisfiable(
                    "pushed inputs " + provided + " do not match the recipe's inputs " + plan.expected());
        }

        if (!(plan.result().what() instanceof AEItemKey resultKey)) {
            return PushOutcome.unsatisfiable("recipe result is not an item");
        }
        ItemStack declared = resultKey.toStack((int) plan.result().amount());

        ItemStack produced = infuse(plan);
        if (!ItemStack.isSameItemSameComponents(produced, declared) || produced.getCount() != declared.getCount()) {
            return PushOutcome.unsatisfiable("the recipe produced " + produced + " rather than the pattern's "
                    + declared + ", so its result cannot be encoded");
        }

        return PushOutcome.accepted("infusing " + declared + " at eterna " + plan.eterna() + ", quanta " + plan.quanta()
                + ", arcana " + plan.arcana() + ", " + deliver(level, produced, ejectionDirection));
    }

    private String deliver(Level level, ItemStack produced, Direction provider) {
        ItemStack remaining = insert(level, provider, produced);
        if (remaining.isEmpty()) {
            return "returned to the pattern provider";
        }
        for (Direction side : Direction.values()) {
            if (side == provider) {
                continue;
            }
            remaining = insert(level, side, remaining);
            if (remaining.isEmpty()) {
                return "put into the inventory on the " + side + " side";
            }
        }

        BlockPos pos = table.getBlockPos();
        ItemStack dropped = remaining.copy();
        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, remaining);
        return "dropped " + dropped + " above the table, with nothing around it able to take it";
    }

    private ItemStack insert(Level level, Direction side, ItemStack stack) {
        IItemHandler target = level.getCapability(
                Capabilities.ItemHandler.BLOCK, table.getBlockPos().relative(side), side.getOpposite());
        return target == null ? stack : ItemHandlerHelper.insertItem(target, stack, false);
    }

    private ItemStack infuse(InfusionRecipeResolver.Plan plan) {
        RavenTableStats stats = table.getData(RavenTableStats.TYPE);
        int eterna = stats.eterna();
        int quanta = stats.quanta();
        int arcana = stats.arcana();
        try {
            stats.set(plan.eterna(), plan.quanta(), plan.arcana());
            return plan.holder().value().assemble(plan.input().toStack(), plan.eterna(), plan.quanta(), plan.arcana());
        } finally {
            stats.set(eterna, quanta, arcana);
            table.setChanged();
        }
    }

    private void log(Level level, IPatternDetails patternDetails, PushOutcome outcome) {
        if (!NepConfig.debugLogging()) {
            return;
        }
        if (!outcome.accepted()
                && !outcome.unsatisfiable()
                && !REJECT_LOG.shouldLog(level, table.getBlockPos(), outcome.detail())) {
            return;
        }
        Nep.LOGGER.info(
                "Enchanting Table of the Raven {} {} push: {} (output={})",
                table.getBlockPos(),
                outcome.accepted() ? "accepted" : "rejected",
                outcome.detail(),
                patternDetails.getOutputs().isEmpty()
                        ? "?"
                        : patternDetails.getOutputs().get(0).toString());
    }
}
