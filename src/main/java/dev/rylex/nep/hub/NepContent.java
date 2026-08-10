package dev.rylex.nep.hub;

import appeng.api.AECapabilities;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepCreativeTabs;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NepContent {
    private NepContent() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nep.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nep.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nep.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Nep.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Nep.MOD_ID);

    public static final DeferredBlock<MachineHubBlock> MACHINE_HUB = BLOCKS.registerBlock(
            "machine_hub",
            MachineHubBlock::new,
            BlockBehaviour.Properties.of().strength(2.0F).requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> MACHINE_HUB_ITEM = ITEMS.registerSimpleBlockItem(MACHINE_HUB);

    public static final DeferredItem<HubLinkerItem> HUB_LINKER =
            ITEMS.registerItem("hub_linker", HubLinkerItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineHubBlockEntity>>
            MACHINE_HUB_BLOCK_ENTITY = BLOCK_ENTITIES.register(
                    "machine_hub", () -> BlockEntityType.Builder.of(MachineHubBlockEntity::new, MACHINE_HUB.get())
                            .build(null));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<HubLink>>> HUB_PLAN =
            COMPONENTS.registerComponentType("hub_plan", builder -> builder.persistent(HubLink.CODEC.listOf())
                    .networkSynchronized(HubLink.STREAM_CODEC.apply(ByteBufCodecs.list())));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HubRole>> HUB_MODE =
            COMPONENTS.registerComponentType(
                    "hub_mode", builder -> builder.persistent(HubRole.CODEC).networkSynchronized(HubRole.STREAM_CODEC));

    public static final DeferredHolder<MenuType<?>, MenuType<MachineHubMenu>> MACHINE_HUB_MENU = MENUS.register(
            "machine_hub",
            () -> IMenuTypeExtension.create(
                    (windowId, inventory, buffer) -> new MachineHubMenu(windowId, inventory, buffer.readBlockPos())));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(NepContent::registerCapabilities);
        modBus.addListener(NepContent::onBuildCreativeTab);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.ME_STORAGE, MACHINE_HUB_BLOCK_ENTITY.get(), (be, side) -> be.storage());
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == NepCreativeTabs.MAIN.getKey()) {
            event.accept(MACHINE_HUB_ITEM.get());
            event.accept(HUB_LINKER.get());
        }
    }
}
