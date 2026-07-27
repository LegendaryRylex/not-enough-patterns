package dev.rylex.nep.compat.create;

import appeng.api.stacks.GenericStack;
import dev.rylex.nep.Nep;
import dev.rylex.nep.util.MissingStacks;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin("create")
public final class NepJadePlugin implements IWailaPlugin {

    private static final String ROOT = "nep";
    private static final int ENTRY_LIMIT = 4;

    private static final ControllerProvider CONTROLLER = new ControllerProvider();
    private static final MatrixProvider MATRIX = new MatrixProvider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(CONTROLLER, SequencedAssemblyControllerBlockEntity.class);
        registration.registerBlockDataProvider(MATRIX, SequencedAssemblyMatrixBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(CONTROLLER, SequencedAssemblyControllerBlock.class);
        registration.registerBlockComponent(MATRIX, SequencedAssemblyMatrixBlock.class);
    }

    private static final class ControllerProvider
            implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {

        private static final ResourceLocation UID = Nep.id("sequenced_assembly_controller");

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof SequencedAssemblyControllerBlockEntity be)) {
                return;
            }
            SequencedAssemblyState state = be.buildState();
            CompoundTag nep = new CompoundTag();
            nep.putInt("status", state.status());
            nep.putBoolean("halted", state.halted());
            nep.putBoolean("blocked", state.outputBlocked());
            ListTag making = new ListTag();
            for (SequencedAssemblyState.Making entry : state.making()) {
                if (making.size() >= ENTRY_LIMIT) {
                    break;
                }
                making.add(makingTag(entry.output().getItem(), entry.count()));
            }
            nep.put("making", making);
            nep.putInt("makingTotal", state.making().size());
            ListTag missing = new ListTag();
            for (GenericStack stack : state.missing()) {
                if (missing.size() >= ENTRY_LIMIT) {
                    break;
                }
                missing.add(GenericStack.writeTag(accessor.getLevel().registryAccess(), stack));
            }
            nep.put("missing", missing);
            data.put(ROOT, nep);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!accessor.getServerData().contains(ROOT)) {
                return;
            }
            CompoundTag nep = accessor.getServerData().getCompound(ROOT);
            boolean halted = nep.getBoolean("halted");
            boolean blocked = nep.getBoolean("blocked");
            int status = nep.getInt("status");
            ListTag making = nep.getList("making", Tag.TAG_COMPOUND);
            if (blocked) {
                tooltip.add(line("jade.nep.controller.output_blocked", ChatFormatting.RED));
            } else if (halted) {
                tooltip.add(line("jade.nep.controller.halted", ChatFormatting.RED));
            } else if (status == 2) {
                tooltip.add(line("jade.nep.controller.problem", ChatFormatting.YELLOW));
            } else if (status == 0) {
                tooltip.add(line("jade.nep.controller.unlinked", ChatFormatting.GRAY));
            } else if (making.isEmpty()) {
                tooltip.add(line("jade.nep.controller.idle", ChatFormatting.GRAY));
            } else {
                tooltip.add(line("jade.nep.controller.running", ChatFormatting.GREEN));
            }
            appendMaking(tooltip, "jade.nep.making", making, nep.getInt("makingTotal"));
            if (halted) {
                appendMissingStacks(
                        tooltip,
                        nep.getList("missing", Tag.TAG_COMPOUND),
                        accessor.getLevel().registryAccess());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private static final class MatrixProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {

        private static final ResourceLocation UID = Nep.id("sequenced_assembly_matrix");

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof SequencedAssemblyMatrixBlockEntity be)) {
                return;
            }
            CompoundTag nep = new CompoundTag();
            nep.putInt("flags", be.statusFlags());
            nep.putInt("stress", be.stressDraw());
            nep.putFloat("progress", be.craftProgress());
            List<ItemStack> makingNow = be.makingNow();
            ListTag making = new ListTag();
            for (ItemStack stack : makingNow) {
                if (making.size() >= ENTRY_LIMIT) {
                    break;
                }
                making.add(makingTag(stack.getItem(), stack.getCount()));
            }
            nep.put("making", making);
            nep.putInt("makingTotal", makingNow.size());
            ListTag missing = new ListTag();
            for (GenericStack stack : be.missingInputs()) {
                if (missing.size() >= ENTRY_LIMIT) {
                    break;
                }
                missing.add(GenericStack.writeTag(accessor.getLevel().registryAccess(), stack));
            }
            nep.put("missing", missing);
            data.put(ROOT, nep);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!accessor.getServerData().contains(ROOT)) {
                return;
            }
            CompoundTag nep = accessor.getServerData().getCompound(ROOT);
            int flags = nep.getInt("flags");
            boolean powered = (flags & SequencedAssemblyMatrixBlockEntity.FLAG_POWERED) != 0;
            boolean rotating = (flags & SequencedAssemblyMatrixBlockEntity.FLAG_ROTATING) != 0;
            boolean overstressed = (flags & SequencedAssemblyMatrixBlockEntity.FLAG_OVERSTRESSED) != 0;
            boolean fastEnough = (flags & SequencedAssemblyMatrixBlockEntity.FLAG_FAST_ENOUGH) != 0;
            boolean blocked = (flags & SequencedAssemblyMatrixBlockEntity.FLAG_OUTPUT_BLOCKED) != 0;
            boolean starved = (flags & SequencedAssemblyMatrixBlockEntity.FLAG_STARVED) != 0;
            ListTag making = nep.getList("making", Tag.TAG_COMPOUND);
            if (overstressed) {
                tooltip.add(line("jade.nep.matrix.overstressed", ChatFormatting.RED));
            } else if (!rotating) {
                tooltip.add(line("jade.nep.matrix.no_rotation", ChatFormatting.GRAY));
            } else if (!fastEnough) {
                tooltip.add(line("jade.nep.matrix.too_slow", ChatFormatting.YELLOW));
            } else if (!powered) {
                tooltip.add(line("jade.nep.matrix.unpowered", ChatFormatting.GRAY));
            } else if (blocked) {
                tooltip.add(line("jade.nep.matrix.output_blocked", ChatFormatting.RED));
            } else if (starved) {
                tooltip.add(line("jade.nep.matrix.starved", ChatFormatting.RED));
            } else if (!making.isEmpty()) {
                int percent = Mth.clamp(Mth.floor(nep.getFloat("progress") * 100.0F), 0, 100);
                tooltip.add(Component.translatable("jade.nep.matrix.assembling", percent)
                        .withStyle(ChatFormatting.GREEN));
            } else {
                tooltip.add(line("jade.nep.matrix.idle", ChatFormatting.GRAY));
            }
            int stress = nep.getInt("stress");
            if (stress > 0) {
                tooltip.add(
                        Component.translatable("jade.nep.matrix.stress", stress).withStyle(ChatFormatting.DARK_GRAY));
            }
            appendMaking(tooltip, "jade.nep.matrix.making", making, nep.getInt("makingTotal"));
            if (starved) {
                appendMissingStacks(
                        tooltip,
                        nep.getList("missing", Tag.TAG_COMPOUND),
                        accessor.getLevel().registryAccess());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private static void appendMaking(ITooltip tooltip, String label, ListTag making, int total) {
        if (making.isEmpty()) {
            return;
        }
        tooltip.add(line(label, ChatFormatting.GRAY));
        for (int i = 0; i < making.size(); i++) {
            CompoundTag entry = making.getCompound(i);
            tooltip.add(Component.translatable(
                            "jade.nep.making.entry", itemName(entry.getString("id")), entry.getLong("count"))
                    .withStyle(ChatFormatting.WHITE));
        }
        int hidden = total - making.size();
        if (hidden > 0) {
            tooltip.add(Component.translatable("jade.nep.more", hidden).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void appendMissingStacks(ITooltip tooltip, ListTag missing, HolderLookup.Provider registries) {
        if (missing.isEmpty()) {
            return;
        }
        tooltip.add(line("jade.nep.missing", ChatFormatting.GRAY));
        for (int i = 0; i < missing.size(); i++) {
            GenericStack stack = GenericStack.readTag(registries, missing.getCompound(i));
            if (stack == null) {
                continue;
            }
            tooltip.add(MissingStacks.describe(stack).withStyle(ChatFormatting.RED));
        }
    }

    private static CompoundTag makingTag(Item item, long count) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", itemId(item));
        tag.putLong("count", count);
        return tag;
    }

    private static String itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static Component itemName(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return new ItemStack(item).getHoverName();
    }

    private static Component line(String key, ChatFormatting color) {
        return Component.translatable(key).withStyle(color);
    }
}
