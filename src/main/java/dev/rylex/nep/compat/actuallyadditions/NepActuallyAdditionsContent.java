package dev.rylex.nep.compat.actuallyadditions;

import dev.rylex.nep.Nep;
import dev.rylex.nep.NepCreativeTabs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

final class NepActuallyAdditionsContent {
    private NepActuallyAdditionsContent() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nep.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nep.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nep.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Nep.MOD_ID);

    static final DeferredBlock<AtomicEmpoweringMatrixBlock> MATRIX = BLOCKS.registerBlock(
            "atomic_empowering_matrix",
            AtomicEmpoweringMatrixBlock::new,
            BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops());

    static final DeferredItem<BlockItem> MATRIX_ITEM = ITEMS.registerSimpleBlockItem(MATRIX);

    static final DeferredItem<Item> EMPOWERED_MATRIX_CIRCUITRY = ITEMS.registerSimpleItem(
            "empowered_matrix_circuitry",
            new Item.Properties().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AtomicEmpoweringMatrixBlockEntity>>
            MATRIX_BLOCK_ENTITY = BLOCK_ENTITIES.register("atomic_empowering_matrix", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new AtomicEmpoweringMatrixBlockEntity(
                            NepActuallyAdditionsContent.MATRIX_BLOCK_ENTITY.get(), pos, state),
                    MATRIX.get())
            .build(null));

    static final DeferredHolder<MenuType<?>, MenuType<AtomicEmpoweringMatrixMenu>> MATRIX_MENU = MENUS.register(
            "atomic_empowering_matrix",
            () -> IMenuTypeExtension.create((windowId, inventory, buffer) ->
                    new AtomicEmpoweringMatrixMenu(windowId, inventory, buffer.readBlockPos())));

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(NepActuallyAdditionsContent::onBuildCreativeTab);
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == NepCreativeTabs.MAIN.getKey()) {
            event.accept(EMPOWERED_MATRIX_CIRCUITRY.get());
            event.accept(MATRIX_ITEM.get());
        }
    }
}
