package dev.rylex.nep.compat.occultism;

import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.NepConfigGates;
import dev.rylex.nep.NepCreativeTabs;
import dev.rylex.nep.decoder.DecoderContent;
import dev.rylex.nep.decoder.DecoderModule;
import dev.rylex.nep.decoder.EncodingModuleItem;
import dev.rylex.nep.machine.MatrixStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
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

final class NepOccultismContent {
    private NepOccultismContent() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nep.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nep.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nep.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Nep.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Nep.MOD_ID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Nep.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Nep.MOD_ID);

    static final DeferredItem<EncodingModuleItem> ENCODING_MODULE =
            DecoderContent.registerModule(ITEMS, DecoderModule.OCCULTISM);

    static final DeferredHolder<RecipeType<?>, RecipeType<SacrificeStandInRecipe>> STAND_IN =
            RECIPE_TYPES.register("sacrifice_stand_in", name -> RecipeType.simple(name));

    static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SacrificeStandInRecipe>> STAND_IN_SERIALIZER =
            RECIPE_SERIALIZERS.register(
                    "sacrifice_stand_in",
                    name -> new RecipeSerializer<>(SacrificeStandInRecipe.CODEC, SacrificeStandInRecipe.STREAM_CODEC));

    static final DeferredHolder<DataComponentType<?>, DataComponentType<BoundSpirit>> BOUND_SPIRIT =
            COMPONENTS.registerComponentType("bound_spirit", builder -> builder.persistent(BoundSpirit.CODEC)
                    .networkSynchronized(BoundSpirit.STREAM_CODEC));

    static final DeferredItem<SpiritInfusedBookItem> SPIRIT_INFUSED_BOOK =
            ITEMS.registerItem("spirit_infused_book", SpiritInfusedBookItem::new, props -> props.stacksTo(1));

    static final DeferredBlock<SpiritProviderBlock> SPIRIT_PROVIDER =
            BLOCKS.registerBlock("spirit_provider", SpiritProviderBlock::new, props -> props.strength(3.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion());

    static final DeferredItem<BlockItem> SPIRIT_PROVIDER_ITEM = ITEMS.registerSimpleBlockItem(SPIRIT_PROVIDER);

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpiritProviderBlockEntity>>
            SPIRIT_PROVIDER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
                    "spirit_provider",
                    () -> new BlockEntityType<>(
                            (pos, state) -> new SpiritProviderBlockEntity(
                                    NepOccultismContent.SPIRIT_PROVIDER_BLOCK_ENTITY.get(), pos, state),
                            SPIRIT_PROVIDER.get()));

    static final DeferredBlock<BoundSpiritMatrixBlock> MATRIX =
            BLOCKS.registerBlock("bound_spirit_matrix", BoundSpiritMatrixBlock::new, props -> props.strength(3.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(MatrixStatus.LIGHT));

    static final DeferredItem<BlockItem> MATRIX_ITEM = ITEMS.registerSimpleBlockItem(MATRIX);

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoundSpiritMatrixBlockEntity>> MATRIX_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "bound_spirit_matrix",
                    () -> new BlockEntityType<>(
                            (pos, state) -> new BoundSpiritMatrixBlockEntity(
                                    NepOccultismContent.MATRIX_BLOCK_ENTITY.get(), pos, state),
                            MATRIX.get()));

    static final DeferredHolder<MenuType<?>, MenuType<BoundSpiritMatrixMenu>> MATRIX_MENU = MENUS.register(
            "bound_spirit_matrix",
            () -> IMenuTypeExtension.create((windowId, inventory, buffer) ->
                    new BoundSpiritMatrixMenu(windowId, inventory, buffer.readBlockPos())));

    static final DeferredBlock<RitualControllerBlock> CONTROLLER =
            BLOCKS.registerBlock("ritual_controller", RitualControllerBlock::new, props -> props.strength(3.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false));

    static final DeferredItem<BlockItem> CONTROLLER_ITEM = ITEMS.registerSimpleBlockItem(CONTROLLER);

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RitualControllerBlockEntity>>
            CONTROLLER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
                    "ritual_controller",
                    () -> new BlockEntityType<>(
                            (pos, state) -> new RitualControllerBlockEntity(
                                    NepOccultismContent.CONTROLLER_BLOCK_ENTITY.get(), pos, state),
                            CONTROLLER.get()));

    static final DeferredHolder<MenuType<?>, MenuType<RitualControllerMenu>> CONTROLLER_MENU = MENUS.register(
            "ritual_controller",
            () -> IMenuTypeExtension.create((windowId, inventory, buffer) ->
                    new RitualControllerMenu(windowId, inventory, buffer.readBlockPos())));

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        MENUS.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        modBus.addListener(NepOccultismContent::onBuildCreativeTab);
        modBus.addListener(NepOccultismContent::onRegisterTooltipAppenders);
        NepConfigGates.register(SPIRIT_PROVIDER_ITEM, NepConfig::occultismSpiritProvider);
        NepConfigGates.register(MATRIX_ITEM, NepConfig::occultismMatrix);
        NepConfigGates.register(SPIRIT_INFUSED_BOOK, NepConfig::occultismOverride);
        NepConfigGates.register(CONTROLLER_ITEM, NepConfig::occultismRitualController);
    }

    private static void onRegisterTooltipAppenders(RegisterTooltipAppendersEvent event) {
        event.registerAppender(TooltipLocation.POST_CUSTOM, (stack, _, _, _, _, tooltip) -> {
            if (stack.is(MATRIX_ITEM.get())) {
                tooltip.accept(Component.translatable("tooltip.nep.bound_spirit_matrix")
                        .withStyle(ChatFormatting.GRAY));
            } else if (stack.is(SPIRIT_INFUSED_BOOK.get())) {
                SpiritInfusedBookItem.appendTooltip(stack, tooltip);
            }
        });
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == NepCreativeTabs.MAIN.getKey()) {
            event.accept(MATRIX_ITEM.get());
            event.accept(SPIRIT_PROVIDER_ITEM.get());
            event.accept(CONTROLLER_ITEM.get());
            for (BoundSpirit.Work work : BoundSpirit.Work.values()) {
                for (BoundSpirit.Tier tier : BoundSpirit.Tier.values()) {
                    event.accept(SpiritInfusedBookItem.of(new BoundSpirit(tier, work)));
                }
            }
        }
    }
}
