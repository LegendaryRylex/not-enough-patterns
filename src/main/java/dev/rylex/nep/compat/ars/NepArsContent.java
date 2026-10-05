package dev.rylex.nep.compat.ars;

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

final class NepArsContent {
    private NepArsContent() {}

    private static final int CONDUCTING_LIGHT = 7;

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nep.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nep.MOD_ID);

    static final DeferredItem<EncodingModuleItem> ENCODING_MODULE =
            DecoderContent.registerModule(ITEMS, DecoderModule.ARS_NOUVEAU);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nep.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Nep.MOD_ID);

    static final DeferredBlock<ArcaneLecternBlock> ARCANE_LECTERN = BLOCKS.registerBlock(
            "arcane_lectern",
            ArcaneLecternBlock::new,
            BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops());

    static final DeferredItem<BlockItem> ARCANE_LECTERN_ITEM = ITEMS.registerSimpleBlockItem(ARCANE_LECTERN);

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneLecternBlockEntity>>
            ARCANE_LECTERN_BLOCK_ENTITY = BLOCK_ENTITIES.register("arcane_lectern", () -> BlockEntityType.Builder.of(
                    (pos, state) ->
                            new ArcaneLecternBlockEntity(NepArsContent.ARCANE_LECTERN_BLOCK_ENTITY.get(), pos, state),
                    ARCANE_LECTERN.get())
            .build(null));

    static final DeferredBlock<RitualConductorBlock> RITUAL_CONDUCTOR = BLOCKS.registerBlock(
            "ritual_conductor",
            RitualConductorBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(RitualConductorBlock.CONDUCTING) ? CONDUCTING_LIGHT : 0));

    static final DeferredItem<BlockItem> RITUAL_CONDUCTOR_ITEM = ITEMS.registerSimpleBlockItem(RITUAL_CONDUCTOR);

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RitualConductorBlockEntity>>
            RITUAL_CONDUCTOR_BLOCK_ENTITY =
                    BLOCK_ENTITIES.register("ritual_conductor", () -> BlockEntityType.Builder.of(
                                    (pos, state) -> new RitualConductorBlockEntity(
                                            NepArsContent.RITUAL_CONDUCTOR_BLOCK_ENTITY.get(), pos, state),
                                    RITUAL_CONDUCTOR.get())
                            .build(null));

    static final DeferredHolder<MenuType<?>, MenuType<RitualConductorMenu>> RITUAL_CONDUCTOR_MENU = MENUS.register(
            "ritual_conductor",
            () -> IMenuTypeExtension.create((windowId, inventory, buffer) ->
                    new RitualConductorMenu(windowId, inventory, buffer.readBlockPos())));

    static final DeferredBlock<ArcaneEnchantingMatrixBlock> MATRIX = BLOCKS.registerBlock(
            "arcane_enchanting_matrix",
            ArcaneEnchantingMatrixBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(MatrixStatus.LIGHT));

    static final DeferredItem<BlockItem> MATRIX_ITEM = ITEMS.registerSimpleBlockItem(MATRIX);

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneEnchantingMatrixBlockEntity>>
            MATRIX_BLOCK_ENTITY = BLOCK_ENTITIES.register("arcane_enchanting_matrix", () -> BlockEntityType.Builder.of(
                    (pos, state) ->
                            new ArcaneEnchantingMatrixBlockEntity(NepArsContent.MATRIX_BLOCK_ENTITY.get(), pos, state),
                    MATRIX.get())
            .build(null));

    static final DeferredHolder<MenuType<?>, MenuType<ArcaneEnchantingMatrixMenu>> MATRIX_MENU = MENUS.register(
            "arcane_enchanting_matrix",
            () -> IMenuTypeExtension.create((windowId, inventory, buffer) ->
                    new ArcaneEnchantingMatrixMenu(windowId, inventory, buffer.readBlockPos())));

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(NepArsContent::onBuildCreativeTab);
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == NepCreativeTabs.MAIN.getKey()) {
            event.accept(ARCANE_LECTERN_ITEM.get());
            event.accept(RITUAL_CONDUCTOR_ITEM.get());
            event.accept(MATRIX_ITEM.get());
        }
    }
}
