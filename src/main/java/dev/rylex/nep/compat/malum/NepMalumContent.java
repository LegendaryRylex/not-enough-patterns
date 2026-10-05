package dev.rylex.nep.compat.malum;

import com.mojang.serialization.Codec;
import com.sammy.malum.common.item.spirit.SpiritShardItem;
import com.sammy.malum.core.systems.registry.SpiritHolder;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.NepCreativeTabs;
import dev.rylex.nep.decoder.DecoderContent;
import dev.rylex.nep.decoder.DecoderModule;
import dev.rylex.nep.decoder.EncodingModuleItem;
import dev.rylex.nep.machine.MatrixStatus;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

final class NepMalumContent {
    private NepMalumContent() {}

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Nep.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nep.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nep.MOD_ID);

    static final DeferredItem<EncodingModuleItem> ENCODING_MODULE =
            DecoderContent.registerModule(ITEMS, DecoderModule.MALUM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nep.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Nep.MOD_ID);

    static final DeferredHolder<AttachmentType<?>, AttachmentType<SpiritReclaim>> SPIRIT_RECLAIM =
            ATTACHMENTS.register("spirit_reclaim", () -> AttachmentType.builder(() -> SpiritReclaim.NONE)
                    .serialize(SpiritReclaim.CODEC, pending -> true)
                    .build());

    static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> IMPETUS_WARD =
            ATTACHMENTS.register("impetus_ward", () -> AttachmentType.builder(() -> 0L)
                    .serialize(Codec.LONG, until -> until > 0)
                    .build());

    static final DeferredBlock<FocusedSpiritMatrixBlock> MATRIX = BLOCKS.registerBlock(
            "focused_spirit_matrix",
            FocusedSpiritMatrixBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(MatrixStatus.LIGHT));

    static final DeferredItem<BlockItem> MATRIX_ITEM = ITEMS.registerSimpleBlockItem(MATRIX);

    static final DeferredItem<MatrixImpetusItem> MATRIX_IMPETUS = ITEMS.registerItem(
            "matrix_impetus",
            MatrixImpetusItem::new,
            new Item.Properties().durability(NepConfig.malumFocusedSpiritMatrixImpetusDurability()));

    static final DeferredItem<Item> FRACTURED_MATRIX_IMPETUS = ITEMS.registerSimpleItem("fractured_matrix_impetus");

    static final DeferredItem<MatrixCatalyzerItem> MATRIX_CATALYZER = ITEMS.registerItem(
            "matrix_catalyzer",
            MatrixCatalyzerItem::new,
            new Item.Properties().stacksTo(MatrixCatalyzerItem.installLimit()));

    static final DeferredItem<SpiritShardItem> RADIANT_SPIRIT = ITEMS.registerItem(
            "radiant_spirit",
            p -> new SpiritShardItem(p, SpiritHolder.getSpiritType(Nep.id("radiant"))),
            new Item.Properties());

    static final DeferredItem<SpiritShardItem> PURE_SPIRIT = ITEMS.registerItem(
            "pure_spirit",
            p -> new SpiritShardItem(p, SpiritHolder.getSpiritType(Nep.id("pure"))),
            new Item.Properties());

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FocusedSpiritMatrixBlockEntity>>
            MATRIX_BLOCK_ENTITY = BLOCK_ENTITIES.register("focused_spirit_matrix", () -> BlockEntityType.Builder.of(
                    (pos, state) ->
                            new FocusedSpiritMatrixBlockEntity(NepMalumContent.MATRIX_BLOCK_ENTITY.get(), pos, state),
                    MATRIX.get())
            .build(null));

    static final DeferredHolder<MenuType<?>, MenuType<FocusedSpiritMatrixMenu>> MATRIX_MENU = MENUS.register(
            "focused_spirit_matrix",
            () -> IMenuTypeExtension.create((windowId, inventory, buffer) ->
                    new FocusedSpiritMatrixMenu(windowId, inventory, buffer.readBlockPos())));

    static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(NepMalumContent::onBuildCreativeTab);
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == NepCreativeTabs.MAIN.getKey()) {
            event.accept(MATRIX_ITEM.get());
            event.accept(MATRIX_IMPETUS.get());
            event.accept(FRACTURED_MATRIX_IMPETUS.get());
            event.accept(MATRIX_CATALYZER.get());
            event.accept(PURE_SPIRIT.get());
            event.accept(RADIANT_SPIRIT.get());
        }
    }
}
