package dev.rylex.nep.compat.compactcrafting;

import dev.rylex.nep.Nep;
import dev.rylex.nep.NepCreativeTabs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

final class NepCompactCraftingContent {
    private NepCompactCraftingContent() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nep.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nep.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nep.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Nep.MOD_ID);

    static final DeferredBlock<MiniaturizationMatrixBlock> MATRIX = BLOCKS.registerBlock(
            "miniaturization_matrix",
            MiniaturizationMatrixBlock::new,
            BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops());

    static final DeferredItem<BlockItem> MATRIX_ITEM = ITEMS.registerSimpleBlockItem(MATRIX);

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MiniaturizationMatrixBlockEntity>>
            MATRIX_BLOCK_ENTITY = BLOCK_ENTITIES.register("miniaturization_matrix", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new MiniaturizationMatrixBlockEntity(
                            NepCompactCraftingContent.MATRIX_BLOCK_ENTITY.get(), pos, state),
                    MATRIX.get())
            .build(null));

    static final DeferredHolder<MenuType<?>, MenuType<MiniaturizationMatrixMenu>> MATRIX_MENU = MENUS.register(
            "miniaturization_matrix",
            () -> IMenuTypeExtension.create((windowId, inventory, buffer) ->
                    new MiniaturizationMatrixMenu(windowId, inventory, buffer.readBlockPos())));

    static final DeferredBlock<MiniaturizationControllerBlock> CONTROLLER = BLOCKS.registerBlock(
            "miniaturization_controller",
            MiniaturizationControllerBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.0F)
                    .requiresCorrectToolForDrops()
                    .isRedstoneConductor((state, level, pos) -> false));

    static final DeferredItem<BlockItem> CONTROLLER_ITEM = ITEMS.registerSimpleBlockItem(CONTROLLER);

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MiniaturizationControllerBlockEntity>>
            CONTROLLER_BLOCK_ENTITY =
                    BLOCK_ENTITIES.register("miniaturization_controller", () -> BlockEntityType.Builder.of(
                                    (pos, state) -> new MiniaturizationControllerBlockEntity(
                                            NepCompactCraftingContent.CONTROLLER_BLOCK_ENTITY.get(), pos, state),
                                    CONTROLLER.get())
                            .build(null));

    static final DeferredHolder<MenuType<?>, MenuType<MiniaturizationControllerMenu>> CONTROLLER_MENU = MENUS.register(
            "miniaturization_controller",
            () -> IMenuTypeExtension.create((windowId, inventory, buffer) ->
                    new MiniaturizationControllerMenu(windowId, inventory, buffer.readBlockPos())));

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(NepCompactCraftingContent::onBuildCreativeTab);
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == NepCreativeTabs.MAIN.getKey()) {
            event.accept(MATRIX_ITEM.get());
            event.accept(CONTROLLER_ITEM.get());
        }
    }
}
