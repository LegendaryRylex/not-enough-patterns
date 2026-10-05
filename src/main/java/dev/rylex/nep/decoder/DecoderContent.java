package dev.rylex.nep.decoder;

import appeng.api.AECapabilities;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import dev.rylex.nep.NepCreativeTabs;
import java.util.Comparator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DecoderContent {
    private DecoderContent() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nep.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nep.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nep.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Nep.MOD_ID);

    public static final DeferredBlock<PatternDecoderBlock> PATTERN_DECODER =
            BLOCKS.registerBlock("pattern_decoder", PatternDecoderBlock::new, props -> props.strength(2.0F)
                    .requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> PATTERN_DECODER_ITEM = ITEMS.registerSimpleBlockItem(PATTERN_DECODER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PatternDecoderBlockEntity>>
            PATTERN_DECODER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
                    "pattern_decoder",
                    () -> new BlockEntityType<>(PatternDecoderBlockEntity::new, PATTERN_DECODER.get()));

    public static final DeferredHolder<MenuType<?>, MenuType<PatternDecoderMenu>> PATTERN_DECODER_MENU = MENUS.register(
            "pattern_decoder",
            () -> IMenuTypeExtension.create((windowId, inventory, buffer) ->
                    new PatternDecoderMenu(windowId, inventory, buffer.readBlockPos())));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(DecoderContent::registerCapabilities);
        modBus.addListener(DecoderContent::onBuildCreativeTab);
    }

    public static DeferredItem<EncodingModuleItem> registerModule(DeferredRegister.Items items, DecoderModule module) {
        return items.registerItem(module.itemPath(), properties -> new EncodingModuleItem(properties, module));
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                PATTERN_DECODER_BLOCK_ENTITY.get(),
                (be, side) -> be.gridNodeHost());
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != NepCreativeTabs.MAIN.getKey() || !NepConfig.requireDecoder()) {
            return;
        }
        event.accept(PATTERN_DECODER_ITEM.get());
        BuiltInRegistries.ITEM.stream()
                .filter(EncodingModuleItem.class::isInstance)
                .map(EncodingModuleItem.class::cast)
                .filter(module -> PatternDecoding.required(module.module()))
                .sorted(Comparator.comparing(EncodingModuleItem::module))
                .forEach(event::accept);
    }
}
