package dev.rylex.nep.compat.mysticalagriculture;

import dev.rylex.nep.Nep;
import dev.rylex.nep.NepCreativeTabs;
import dev.rylex.nep.decoder.DecoderContent;
import dev.rylex.nep.decoder.DecoderModule;
import dev.rylex.nep.decoder.EncodingModuleItem;
import dev.rylex.nep.machine.MatrixStatus;
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

final class NepMysticalContent {
    private NepMysticalContent() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nep.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nep.MOD_ID);

    static final DeferredItem<EncodingModuleItem> ENCODING_MODULE =
            DecoderContent.registerModule(ITEMS, DecoderModule.MYSTICAL_AGRICULTURE);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nep.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Nep.MOD_ID);

    static final DeferredBlock<InfusedAwakeningMatrixBlock> MATRIX = BLOCKS.registerBlock(
            "infused_awakening_matrix",
            InfusedAwakeningMatrixBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(MatrixStatus.LIGHT));

    static final DeferredItem<BlockItem> MATRIX_ITEM = ITEMS.registerSimpleBlockItem(MATRIX);

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfusedAwakeningMatrixBlockEntity>>
            MATRIX_BLOCK_ENTITY = BLOCK_ENTITIES.register("infused_awakening_matrix", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new InfusedAwakeningMatrixBlockEntity(
                            NepMysticalContent.MATRIX_BLOCK_ENTITY.get(), pos, state),
                    MATRIX.get())
            .build(null));

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
