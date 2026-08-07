package dev.rylex.nep.compat.draconic;

import dev.rylex.nep.Nep;
import dev.rylex.nep.NepCreativeTabs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
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

final class NepDraconicContent {
    private NepDraconicContent() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nep.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nep.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nep.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Nep.MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Nep.MOD_ID);

    static final DeferredHolder<AttachmentType<?>, AttachmentType<FusionReclaim>> FUSION_RECLAIM =
            ATTACHMENTS.register("fusion_reclaim", () -> AttachmentType.builder(() -> FusionReclaim.NONE)
                    .serialize(FusionReclaim.CODEC, pending -> true)
                    .build());

    static final DeferredBlock<FusionMatrixBlock> MATRIX = BLOCKS.registerBlock(
            "fusion_matrix",
            FusionMatrixBlock::new,
            BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops());

    static final DeferredItem<BlockItem> MATRIX_ITEM = ITEMS.registerSimpleBlockItem(MATRIX);

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionMatrixBlockEntity>> MATRIX_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("fusion_matrix", () -> BlockEntityType.Builder.of(
                            (pos, state) -> new FusionMatrixBlockEntity(
                                    NepDraconicContent.MATRIX_BLOCK_ENTITY.get(), pos, state),
                            MATRIX.get())
                    .build(null));

    static final DeferredHolder<MenuType<?>, MenuType<FusionMatrixMenu>> MATRIX_MENU = MENUS.register(
            "fusion_matrix",
            () -> IMenuTypeExtension.create(
                    (windowId, inventory, buffer) -> new FusionMatrixMenu(windowId, inventory, buffer.readBlockPos())));

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        ATTACHMENTS.register(modBus);
        modBus.addListener(NepDraconicContent::onBuildCreativeTab);
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == NepCreativeTabs.MAIN.getKey()) {
            event.accept(MATRIX_ITEM.get());
        }
    }
}
