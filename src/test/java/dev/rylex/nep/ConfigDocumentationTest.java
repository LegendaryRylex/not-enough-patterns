package dev.rylex.nep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

class ConfigDocumentationTest {

    private static final Path LANG = Paths.get("src", "main", "resources", "assets", "nep", "lang", "en_us.json");
    private static final Path CONFIG_SOURCE = Paths.get("src", "main", "java", "dev", "rylex", "nep", "NepConfig.java");

    private static final String UPGRADES = "modules.draconicevolution.fusionMatrix.upgrades";
    private static final String MATRIX = "modules.actuallyadditions.atomicEmpoweringMatrix";

    private static Map<String, ModConfigSpec.ValueSpec> settings() {
        Map<String, ModConfigSpec.ValueSpec> found = new LinkedHashMap<>();
        collect(NepConfig.SPEC.getSpec(), "", found);
        return found;
    }

    private static void collect(UnmodifiableConfig config, String prefix, Map<String, ModConfigSpec.ValueSpec> out) {
        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
            String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getRawValue();
            if (value instanceof UnmodifiableConfig nested) {
                collect(nested, path, out);
            } else if (value instanceof ModConfigSpec.ValueSpec spec) {
                out.put(path, spec);
            }
        }
    }

    private static JsonObject lang() {
        return JsonParser.parseString(ProjectFiles.read(LANG)).getAsJsonObject();
    }

    @Test
    void everySettingHasANameTheConfigScreenCanShow() {
        JsonObject lang = lang();
        List<String> missing = new ArrayList<>();
        settings().forEach((path, spec) -> {
            String key = spec.getTranslationKey();
            if (key == null || key.isBlank()) {
                missing.add(path + " has no translation key at all");
            } else if (!lang.has(key)) {
                missing.add(path + " -> " + key);
            }
        });
        assertTrue(
                missing.isEmpty(),
                "config settings would render as a raw translation key in the in-game config screen:\n  "
                        + String.join("\n  ", missing));
    }

    @Test
    void everySettingHasADescriptionTheConfigScreenCanShow() {
        JsonObject lang = lang();
        List<String> missing = new ArrayList<>();
        settings().forEach((path, spec) -> {
            String key = spec.getTranslationKey();
            if (key != null && !key.isBlank() && !lang.has(key + ".tooltip")) {
                missing.add(path + " -> " + key + ".tooltip");
            }
        });
        assertTrue(
                missing.isEmpty(),
                "config settings would fall back to their untranslatable English comment as the tooltip in the "
                        + "in-game config screen, and would show no range line:\n  " + String.join("\n  ", missing));
    }

    @Test
    void noDescriptionRestatesTheRangeTheConfigScreenAppendsItself() {
        JsonObject lang = lang();
        List<String> duplicated = new ArrayList<>();
        settings().forEach((path, spec) -> {
            String key = spec.getTranslationKey();
            if (key == null || !lang.has(key + ".tooltip")) {
                return;
            }
            String tooltip = lang.get(key + ".tooltip").getAsString();
            if (tooltip.contains("Range: ") || tooltip.contains("Default: ")) {
                duplicated.add(path);
            }
        });
        assertTrue(
                duplicated.isEmpty(),
                "a description repeats the Default/Range trailer that ModConfigSpec appends to the comment; the "
                        + "config screen adds a translated range line of its own, so this would show twice and would "
                        + "silently go stale when the default changes:\n  " + String.join("\n  ", duplicated));
    }

    @Test
    void theUpgradeCoresSubcategoryCarriesItsWholeSetOfSettings() {
        List<String> upgrades = new ArrayList<>(settings().keySet().stream()
                .filter(path -> path.startsWith(UPGRADES + "."))
                .toList());

        assertEquals(
                List.of(
                        UPGRADES + ".maxCores",
                        UPGRADES + ".coreSwapGrace",
                        UPGRADES + ".wyvernCapacity",
                        UPGRADES + ".draconicCapacity",
                        UPGRADES + ".draconicCraftTimeReductionPercent",
                        UPGRADES + ".chaoticCapacity",
                        UPGRADES + ".chaoticEnergyCostReductionPercent",
                        UPGRADES + ".chaoticMinimumCraftTicks"),
                upgrades,
                "the Upgrade Cores subcategory does not hold the eight settings the guide documents");
    }

    @Test
    void theTierCeilingsStayInsideWhatAnFeCableCanRead() {
        for (String path :
                List.of(UPGRADES + ".wyvernCapacity", UPGRADES + ".draconicCapacity", UPGRADES + ".chaoticCapacity")) {
            ModConfigSpec.Range<?> range = settings().get(path).getRange();
            assertEquals(
                    (long) Integer.MAX_VALUE,
                    ((Number) range.getMax()).longValue(),
                    path + " may be set past the 32 bit line, where an FE cable reads the buffer as permanently full");
        }
    }

    @Test
    void aValueFromAnOlderBuildAboveTheCeilingIsCorrectedRatherThanCrashing() {
        CommentedConfig config = CommentedConfig.inMemory();
        NepConfig.SPEC.correct(config);
        assertTrue(NepConfig.SPEC.isCorrect(config), "a freshly defaulted config was not accepted by its own spec");

        List<String> path =
                List.of(UPGRADES.split("\\.")[0], "draconicevolution", "fusionMatrix", "upgrades", "chaoticCapacity");
        config.set(path, 4_000_000_000L);
        assertFalse(NepConfig.SPEC.isCorrect(config), "a capacity past the ceiling was accepted as valid");

        NepConfig.SPEC.correct(config);

        assertTrue(
                NepConfig.SPEC.isCorrect(config),
                "an out-of-range capacity from an older config file did not correct to something loadable");
        assertTrue(
                ((Number) config.get(path)).longValue() <= Integer.MAX_VALUE,
                "the corrected capacity is still above the ceiling");
    }

    @Test
    void aModuleTogglePinnedOffInAnOlderFileCorrectsRatherThanCrashing() {
        CommentedConfig config = CommentedConfig.inMemory();
        NepConfig.SPEC.correct(config);

        List<String> filling = List.of("modules", "create", "filling");
        CommentedConfig submenu = config.createSubConfig();
        submenu.set("enabled", false);
        config.set(filling, submenu);

        assertFalse(NepConfig.SPEC.isCorrect(config), "the old submenu shape was accepted as valid");

        NepConfig.SPEC.correct(config);

        assertTrue(
                NepConfig.SPEC.isCorrect(config), "a config file written before the toggle moved up did not correct");
        assertEquals(
                Boolean.TRUE,
                config.get(filling),
                "the toggle did not come back as a plain boolean at its default, so the file is still the old shape");
    }

    @Test
    void everyFallbackMatchesTheSettingItStandsInFor() {
        Map<String, ModConfigSpec.ValueSpec> settings = settings();
        Map<String, Number> fallbacks = new LinkedHashMap<>();
        fallbacks.put(
                "modules.draconicevolution.fusionMatrix.energyCapacity", NepConfig.draconicFusionMatrixCapacity());
        fallbacks.put("modules.draconicevolution.fusionMatrix.chargeRate", NepConfig.draconicFusionMatrixChargeRate());
        fallbacks.put("modules.draconicevolution.fusionMatrix.craftTicks", NepConfig.draconicFusionMatrixCraftTicks());
        fallbacks.put("modules.draconicevolution.fusionMatrix.meNetworkDrain", NepConfig.draconicFusionMatrixMeDrain());
        fallbacks.put(
                "modules.draconicevolution.fusionMatrix.idleMeNetworkDrain",
                NepConfig.draconicFusionMatrixIdleMeDrain());
        fallbacks.put(UPGRADES + ".maxCores", NepConfig.draconicFusionMatrixMaxCores());
        fallbacks.put(UPGRADES + ".coreSwapGrace", NepConfig.draconicFusionMatrixCoreSwapGrace());
        fallbacks.put(UPGRADES + ".wyvernCapacity", NepConfig.draconicFusionMatrixWyvernCapacity());
        fallbacks.put(UPGRADES + ".draconicCapacity", NepConfig.draconicFusionMatrixDraconicCapacity());
        fallbacks.put(
                UPGRADES + ".draconicCraftTimeReductionPercent",
                NepConfig.draconicFusionMatrixDraconicCraftTimeReductionPercent());
        fallbacks.put(UPGRADES + ".chaoticCapacity", NepConfig.draconicFusionMatrixChaoticCapacity());
        fallbacks.put(
                UPGRADES + ".chaoticEnergyCostReductionPercent",
                NepConfig.draconicFusionMatrixChaoticEnergyCostReductionPercent());
        fallbacks.put(UPGRADES + ".chaoticMinimumCraftTicks", NepConfig.draconicFusionMatrixChaoticMinimumCraftTicks());
        fallbacks.put(MATRIX + ".energyCostPercent", NepConfig.actuallyAdditionsMatrixEnergyCostPercent());
        fallbacks.put(MATRIX + ".chargeRate", NepConfig.actuallyAdditionsMatrixChargeRate());
        fallbacks.put(MATRIX + ".energyCapacity", NepConfig.actuallyAdditionsMatrixCapacity());
        fallbacks.put(MATRIX + ".empoweringCraftTicks", NepConfig.actuallyAdditionsMatrixEmpoweringCraftTicks());
        fallbacks.put(
                MATRIX + ".atomicReconstructionCraftTicks",
                NepConfig.actuallyAdditionsMatrixAtomicReconstructionCraftTicks());
        fallbacks.put(MATRIX + ".meNetworkDrain", NepConfig.actuallyAdditionsMatrixMeDrain());
        fallbacks.put(MATRIX + ".idleMeNetworkDrain", NepConfig.actuallyAdditionsMatrixIdleMeDrain());
        fallbacks.put(
                "modules.compactcrafting.miniaturizationController.blocksPerTick",
                NepConfig.compactCraftingControllerBlocksPerTick());
        fallbacks.put("provider.importCardGrace", NepConfig.importCardGrace());
        fallbacks.put("machineHub.linkRange", NepConfig.machineHubLinkRange());
        fallbacks.put("machineHub.maximumLinks", NepConfig.machineHubMaximumLinks());
        fallbacks.put("machineHub.scanBudget", NepConfig.machineHubScanBudget());
        fallbacks.put("machineHub.casingDepth", NepConfig.machineHubCasingDepth());
        fallbacks.put("modules.create.sequencedAssembly.haltGrace", NepConfig.createSequencedAssemblyHaltGrace());
        fallbacks.put("modules.create.sequencedAssembly.reclaimGrace", NepConfig.createSequencedAssemblyReclaimGrace());

        List<String> drift = new ArrayList<>();
        fallbacks.forEach((path, fallback) -> {
            ModConfigSpec.ValueSpec spec = settings.get(path);
            assertTrue(spec != null, "no config setting at " + path);
            long declared = ((Number) spec.getDefault()).longValue();
            if (declared != fallback.longValue()) {
                drift.add(
                        path + ": the setting defaults to " + declared + " but the accessor falls back to " + fallback);
            }
        });
        assertTrue(
                drift.isEmpty(),
                "an accessor's fallback disagrees with the default it stands in for, so behaviour changes depending "
                        + "on whether the config has loaded yet:\n  " + String.join("\n  ", drift));
    }

    @Test
    void theDrainClampWarningNamesTheSettingsAsTheyAreNamedNow() {
        String source = ProjectFiles.read(CONFIG_SOURCE);

        assertTrue(
                source.contains("idleMeNetworkDrain ({} AE/t) is above meNetworkDrain"),
                "the clamp warning no longer names the drain settings as the config does");
        assertFalse(
                source.contains("powerUsage") || source.contains("idlePowerUsage"),
                "the old powerUsage names are still referenced; those keys were removed");
    }
}
