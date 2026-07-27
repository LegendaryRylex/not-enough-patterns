package dev.rylex.nep.compat.create;

import dev.rylex.nep.Nep;
import dev.rylex.nep.NepCreativeTabs;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

final class NepCreateContent {
    private NepCreateContent() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nep.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nep.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nep.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Nep.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Nep.MOD_ID);

    static final DeferredBlock<SequencedAssemblyControllerBlock> CONTROLLER = BLOCKS.registerBlock(
            "sequenced_assembly_controller",
            SequencedAssemblyControllerBlock::new,
            BlockBehaviour.Properties.of().strength(2.0F).requiresCorrectToolForDrops());

    static final DeferredItem<BlockItem> CONTROLLER_ITEM = ITEMS.registerSimpleBlockItem(CONTROLLER);

    static final DeferredItem<SequencedAssemblyLinkerItem> LINKER = ITEMS.registerItem(
            "sequenced_assembly_linker", SequencedAssemblyLinkerItem::new, new Item.Properties().stacksTo(1));

    static final DeferredBlock<SequencedAssemblyMatrixBlock> MATRIX = BLOCKS.registerBlock(
            "sequenced_assembly_matrix",
            SequencedAssemblyMatrixBlock::new,
            BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops());

    static final DeferredItem<BlockItem> MATRIX_ITEM = ITEMS.registerSimpleBlockItem(MATRIX);

    static final DeferredItem<Item> HARDENED_OBSIDIAN_PLATE = ITEMS.registerSimpleItem("hardened_obsidian_plate");

    static final DeferredItem<Item> INCOMPLETE_HARDENED_OBSIDIAN_PLATE =
            ITEMS.registerSimpleItem("incomplete_hardened_obsidian_plate");

    static final DeferredItem<Item> INCOMPLETE_MATRIX_CIRCUITRY =
            ITEMS.registerSimpleItem("incomplete_matrix_circuitry");

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SequencedAssemblyControllerBlockEntity>>
            CONTROLLER_BLOCK_ENTITY =
                    BLOCK_ENTITIES.register("sequenced_assembly_controller", () -> BlockEntityType.Builder.of(
                                    SequencedAssemblyControllerBlockEntity::new, CONTROLLER.get())
                            .build(null));

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SequencedAssemblyMatrixBlockEntity>>
            MATRIX_BLOCK_ENTITY = BLOCK_ENTITIES.register("sequenced_assembly_matrix", () -> BlockEntityType.Builder.of(
                    NepCreateContent::createMatrix, MATRIX.get())
            .build(null));

    private static SequencedAssemblyMatrixBlockEntity createMatrix(BlockPos pos, BlockState state) {
        return new SequencedAssemblyMatrixBlockEntity(MATRIX_BLOCK_ENTITY.get(), pos, state);
    }

    static final DeferredHolder<DataComponentType<?>, DataComponentType<LinkerMode>> LINKER_MODE =
            COMPONENTS.registerComponentType("linker_mode", builder -> builder.persistent(LinkerMode.CODEC)
                    .networkSynchronized(LinkerMode.STREAM_CODEC));

    static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> LINKER_INPUT =
            COMPONENTS.registerComponentType("linker_input", builder -> builder.persistent(BlockPos.CODEC)
                    .networkSynchronized(BlockPos.STREAM_CODEC));

    static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> LINKER_OUTPUT =
            COMPONENTS.registerComponentType("linker_output", builder -> builder.persistent(BlockPos.CODEC)
                    .networkSynchronized(BlockPos.STREAM_CODEC));

    static final DeferredHolder<DataComponentType<?>, DataComponentType<List<BlockPos>>> LINKER_MACHINES =
            COMPONENTS.registerComponentType("linker_machines", builder -> builder.persistent(BlockPos.CODEC.listOf())
                    .networkSynchronized(BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list())));

    static final DeferredHolder<MenuType<?>, MenuType<SequencedAssemblyControllerMenu>> CONTROLLER_MENU =
            MENUS.register(
                    "sequenced_assembly_controller",
                    () -> IMenuTypeExtension.create((windowId, inventory, buffer) ->
                            new SequencedAssemblyControllerMenu(windowId, inventory, buffer.readBlockPos())));

    static final DeferredHolder<MenuType<?>, MenuType<SequencedAssemblyMatrixMenu>> MATRIX_MENU = MENUS.register(
            "sequenced_assembly_matrix",
            () -> IMenuTypeExtension.create((windowId, inventory, buffer) ->
                    new SequencedAssemblyMatrixMenu(windowId, inventory, buffer.readBlockPos())));

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(NepCreateContent::onBuildCreativeTab);
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == NepCreativeTabs.MAIN.getKey()) {
            event.accept(CONTROLLER_ITEM.get());
            event.accept(MATRIX_ITEM.get());
            event.accept(LINKER.get());
            event.accept(HARDENED_OBSIDIAN_PLATE.get());
        }
    }
}
