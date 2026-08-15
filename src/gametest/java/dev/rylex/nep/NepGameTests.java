package dev.rylex.nep;

import dev.rylex.nep.compat.advancedae.AdvancedAeMixinGameTest;
import dev.rylex.nep.compat.apothic.InfusionCraftingMachineGameTest;
import dev.rylex.nep.compat.mysticalagriculture.AwakeningAltarCraftingMachineGameTest;
import dev.rylex.nep.compat.mysticalagriculture.InfusedAwakeningMatrixGameTest;
import dev.rylex.nep.compat.mysticalagriculture.InfusionAltarCraftingMachineGameTest;
import dev.rylex.nep.compat.thunderbolt.ThunderboltChannelGameTest;
import dev.rylex.nep.hub.ChannelModeGameTest;
import dev.rylex.nep.hub.MachineHubGameTest;
import dev.rylex.nep.machine.PushingCpuMixinGameTest;
import dev.rylex.nep.pattern.PatternEncodingMixinGameTest;
import dev.rylex.nep.pattern.ProcessingPatternConversionGameTest;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Nep.MOD_ID)
public final class NepGameTests {

    public static final String SMALL = "empty_5x5x5";
    public static final String LARGE = "empty_9x6x9";

    private static final int DEFAULT_TICKS = 100;

    private static final Map<Identifier, Consumer<GameTestHelper>> BODIES = new HashMap<>();

    private NepGameTests() {}

    @SubscribeEvent
    static void registerInstanceType(RegisterEvent event) {
        event.register(
                Registries.TEST_INSTANCE_TYPE, registry -> registry.register(Nep.id("named"), NepTestInstance.CODEC));
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        BODIES.clear();

        NepRecipesGameTest.register(new Batch(event, "nep_recipes", SMALL));
        ImportCardInstallGameTest.register(new Batch(event, "nep_import_card_install", SMALL));
        AddonProviderImportCardGameTest.register(new Batch(event, "nep_addon_providers", SMALL));
        ImportCardChainedCraftGameTest.register(new Batch(event, "nep_import_card_chain", LARGE));
        AppliedFluxCompatGameTest.register(new Batch(event, "nep_appflux", SMALL));
        AdvancedAeMixinGameTest.register(new Batch(event, "nep_advancedae", SMALL));
        PushingCpuMixinGameTest.register(new Batch(event, "nep_pushing_cpu", SMALL));
        PatternEncodingMixinGameTest.register(new Batch(event, "nep_pattern_encoding", SMALL));
        ProcessingPatternConversionGameTest.register(new Batch(event, "nep_pattern_conversion", SMALL));
        MachineHubGameTest.register(
                new Batch(event, "nep_machine_hub", SMALL),
                new Batch(event, "nep_machine_hub_wide", LARGE),
                new Batch(event, "nep_machine_hub_channels", SMALL),
                new Batch(event, "nep_machine_hub_adhoc", SMALL),
                new Batch(event, "nep_machine_hub_rules", SMALL));
        ChannelModeGameTest.register(new Batch(event, "nep_channel_modes", SMALL));
        ThunderboltChannelGameTest.register(new Batch(event, "nep_thunderbolt_channels", LARGE));
        InfusionCraftingMachineGameTest.register(new Batch(event, "nep_infusion", LARGE));
        InfusedAwakeningMatrixGameTest.register(new Batch(event, "nep_infused_awakening_matrix", SMALL));
        AwakeningAltarCraftingMachineGameTest.register(new Batch(event, "nep_awakening_altar", LARGE));
        InfusionAltarCraftingMachineGameTest.register(new Batch(event, "nep_infusion_altar", LARGE));
    }

    static Consumer<GameTestHelper> body(Identifier name) {
        Consumer<GameTestHelper> body = BODIES.get(name);
        if (body == null) {
            throw new IllegalStateException("no game test body registered under " + name);
        }
        return body;
    }

    public static final class Batch {

        private final RegisterGameTestsEvent event;
        private final String name;
        private final Identifier structure;
        private final Holder<TestEnvironmentDefinition<?>> environment;

        private Batch(RegisterGameTestsEvent event, String name, String structure) {
            this.event = event;
            this.name = name;
            this.structure = Nep.id(structure);
            this.environment = event.registerEnvironment(Nep.id(name));
        }

        public Batch add(String test, Consumer<GameTestHelper> body) {
            return add(test, DEFAULT_TICKS, body);
        }

        public Batch add(String test, int maxTicks, Consumer<GameTestHelper> body) {
            Identifier id = Nep.id(name + "/" + test);
            BODIES.put(id, body);
            event.registerTest(
                    id,
                    new NepTestInstance(id, new TestData<>(environment, structure, maxTicks, 0, true, Rotation.NONE)));
            return this;
        }
    }
}
