package dev.rylex.nep.compat.mysticalagriculture;

import dev.rylex.nep.Nep;
import dev.rylex.nep.NepCreativeTabs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

final class NepMysticalContent {
    private NepMysticalContent() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nep.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nep.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nep.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Nep.MOD_ID);

    static final DeferredBlock<InfusedAwakeningMatrixBlock> MATRIX = BLOCKS.registerBlock(
            "infused_awakening_matrix", InfusedAwakeningMatrixBlock::new, props -> props.strength(3.0F)
                    .requiresCorrectToolForDrops());

    static final DeferredItem<BlockItem> MATRIX_ITEM = ITEMS.registerSimpleBlockItem(MATRIX);

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfusedAwakeningMatrixBlockEntity>>
            MATRIX_BLOCK_ENTITY = BLOCK_ENTITIES.register(
                    "infused_awakening_matrix",
                    () -> new BlockEntityType<>(
                            (pos, state) -> new InfusedAwakeningMatrixBlockEntity(
                                    NepMysticalContent.MATRIX_BLOCK_ENTITY.get(), pos, state),
                            MATRIX.get()));

    static final DeferredHolder<MenuType<?>, MenuType<InfusedAwakeningMatrixMenu>> MATRIX_MENU = MENUS.register(
            "infused_awakening_matrix",
            () -> IMenuTypeExtension.create((windowId, inventory, buffer) ->
                    new InfusedAwakeningMatrixMenu(windowId, inventory, buffer.readBlockPos())));

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(NepMysticalContent::onBuildCreativeTab);
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == NepCreativeTabs.MAIN.getKey()) {
            event.accept(MATRIX_ITEM.get());
        }
    }
}
