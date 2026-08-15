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

    private static final String MATRIX = "modules.mysticalagriculture.infusedAwakeningMatrix";
    private static final String INFUSION = "modules.apothic_enchanting.infusion";

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
    void aModuleTogglePinnedOffInAnOlderFileCorrectsRatherThanCrashing() {
        CommentedConfig config = CommentedConfig.inMemory();
        NepConfig.SPEC.correct(config);

        List<String> awakening = List.of("modules", "mysticalagriculture", "awakening");
        CommentedConfig submenu = config.createSubConfig();
        submenu.set("enabled", false);
        config.set(awakening, submenu);

        assertFalse(NepConfig.SPEC.isCorrect(config), "the old submenu shape was accepted as valid");

        NepConfig.SPEC.correct(config);

        assertTrue(
                NepConfig.SPEC.isCorrect(config), "a config file written before the toggle moved up did not correct");
        assertEquals(
                Boolean.TRUE,
                config.get(awakening),
                "the toggle did not come back as a plain boolean at its default, so the file is still the old shape");
    }

    @Test
    void everyFallbackMatchesTheSettingItStandsInFor() {
        Map<String, ModConfigSpec.ValueSpec> settings = settings();
        Map<String, Number> fallbacks = new LinkedHashMap<>();
        fallbacks.put(MATRIX + ".craftTicks", NepConfig.mysticalInfusedAwakeningMatrixCraftTicks());
        fallbacks.put(MATRIX + ".essenceTankCapacity", NepConfig.mysticalInfusedAwakeningMatrixTankCapacity());
        fallbacks.put(MATRIX + ".meNetworkDrain", NepConfig.mysticalInfusedAwakeningMatrixMeDrain());
        fallbacks.put(MATRIX + ".idleMeNetworkDrain", NepConfig.mysticalInfusedAwakeningMatrixIdleMeDrain());
        fallbacks.put(INFUSION + ".experiencePerBottle", NepConfig.apothicInfusionExperiencePerBottle());
        fallbacks.put(INFUSION + ".millibucketsPerExperience", NepConfig.apothicInfusionMillibucketsPerExperience());
        fallbacks.put("provider.importCardGrace", NepConfig.importCardGrace());
        fallbacks.put("machineHub.linkRange", NepConfig.machineHubLinkRange());
        fallbacks.put("machineHub.maximumLinks", NepConfig.machineHubMaximumLinks());
        fallbacks.put("machineHub.scanBudget", NepConfig.machineHubScanBudget());
        fallbacks.put("machineHub.casingDepth", NepConfig.machineHubCasingDepth());
        fallbacks.put("machineHub.meNetworkChannels", NepConfig.machineHubChannels());
        fallbacks.put("machineHub.meNetworkChannelsPerLink", NepConfig.machineHubChannelsPerLink());
        fallbacks.put(MATRIX + ".meNetworkChannels", NepConfig.mysticalInfusedAwakeningMatrixChannels());

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
}
