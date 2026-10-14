package dev.rylex.nep.compat.oritech;

import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.NepConfigGates;
import dev.rylex.nep.NepCreativeTabs;
import dev.rylex.nep.decoder.DecoderContent;
import dev.rylex.nep.decoder.DecoderModule;
import dev.rylex.nep.decoder.EncodingModuleItem;
import dev.rylex.nep.machine.MatrixStatus;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.tooltip.TooltipLocation;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

final class NepOritechContent {
    private NepOritechContent() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nep.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nep.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nep.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Nep.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Nep.MOD_ID);

    static final DeferredItem<EncodingModuleItem> ENCODING_MODULE =
            DecoderContent.registerModule(ITEMS, DecoderModule.ORITECH);

    static final DeferredBlock<ParticleCollisionControllerBlock> CONTROLLER = BLOCKS.registerBlock(
            "particle_collision_controller", ParticleCollisionControllerBlock::new, props -> props.strength(3.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false));

    static final DeferredItem<BlockItem> CONTROLLER_ITEM = ITEMS.registerSimpleBlockItem(CONTROLLER);

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ParticleCollisionControllerBlockEntity>>
            CONTROLLER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
                    "particle_collision_controller",
                    () -> new BlockEntityType<>(
                            (pos, state) -> new ParticleCollisionControllerBlockEntity(
                                    NepOritechContent.CONTROLLER_BLOCK_ENTITY.get(), pos, state),
                            CONTROLLER.get()));

    static final DeferredHolder<MenuType<?>, MenuType<ParticleCollisionControllerMenu>> CONTROLLER_MENU =
            MENUS.register(
                    "particle_collision_controller",
                    () -> IMenuTypeExtension.create((windowId, inventory, buffer) ->
                            new ParticleCollisionControllerMenu(windowId, inventory, buffer.readBlockPos())));

    static final DeferredBlock<ParticleCollisionMatrixBlock> MATRIX = BLOCKS.registerBlock(
            "particle_collision_matrix", ParticleCollisionMatrixBlock::new, props -> props.strength(3.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(MatrixStatus.LIGHT));

    static final DeferredItem<BlockItem> MATRIX_ITEM = ITEMS.registerSimpleBlockItem(MATRIX);

    static final DeferredItem<Item> CORE = ITEMS.registerSimpleItem("particle_collision_core");

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ParticleCollisionMatrixBlockEntity>>
            MATRIX_BLOCK_ENTITY = BLOCK_ENTITIES.register(
                    "particle_collision_matrix",
                    () -> new BlockEntityType<>(
                            (pos, state) -> new ParticleCollisionMatrixBlockEntity(
                                    NepOritechContent.MATRIX_BLOCK_ENTITY.get(), pos, state),
                            MATRIX.get()));

    static final DeferredHolder<MenuType<?>, MenuType<ParticleCollisionMatrixMenu>> MATRIX_MENU = MENUS.register(
            "particle_collision_matrix",
            () -> IMenuTypeExtension.create((windowId, inventory, buffer) ->
                    new ParticleCollisionMatrixMenu(windowId, inventory, buffer.readBlockPos())));

    static final DeferredItem<AcceleratorLinkerItem> LINKER =
            ITEMS.registerItem("accelerator_linker", AcceleratorLinkerItem::new, props -> props.stacksTo(1));

    static final DeferredHolder<DataComponentType<?>, DataComponentType<List<BlockPos>>> LINKER_ACCELERATORS =
            COMPONENTS.registerComponentType(
                    "linker_accelerators", builder -> builder.persistent(BlockPos.CODEC.listOf())
                            .networkSynchronized(BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list())));

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener(NepOritechContent::onBuildCreativeTab);
        modBus.addListener(NepOritechContent::onRegisterTooltipAppenders);
        NepConfigGates.register(MATRIX_ITEM, NepConfig::oritechParticleCollisionMatrix);
        NepConfigGates.register(CORE, NepConfig::oritechParticleCollisionMatrix);
        NepConfigGates.register(CONTROLLER_ITEM, NepConfig::oritechParticleCollisionController);
        NepConfigGates.register(LINKER, NepConfig::oritechParticleCollisionController);
    }

    private static void onRegisterTooltipAppenders(RegisterTooltipAppendersEvent event) {
        event.registerAppender(TooltipLocation.POST_CUSTOM, (stack, _, _, _, _, tooltip) -> {
            if (stack.is(MATRIX_ITEM.get())) {
                tooltip.accept(Component.translatable("tooltip.nep.particle_collision_matrix")
                        .withStyle(ChatFormatting.GRAY));
            } else if (stack.is(LINKER.get())) {
                AcceleratorLinkerItem.appendTooltip(stack, tooltip);
            }
        });
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == NepCreativeTabs.MAIN.getKey()) {
            event.accept(CORE.get());
            event.accept(MATRIX_ITEM.get());
            event.accept(CONTROLLER_ITEM.get());
            event.accept(LINKER.get());
        }
    }
}
