package dev.rylex.nep.guide;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rylex.nep.ProjectFiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ModuleStatusTest {

    private static final Path RESOURCES = ProjectFiles.resources();
    private static final Path RECIPES = RESOURCES.resolve("data/nep/recipe/module_status");
    private static final Path PAGES = RESOURCES.resolve("assets/nep/ae2guide/nep");

    private static final Pattern TAG = Pattern.compile("<Recipe id=\"nep:module_status/([a-z_]+)\"\\s*/>");
    private static final Pattern MODULE_FIELD = Pattern.compile("\"module\"\\s*:\\s*\"([a-z_]+)\"");

    private static final List<String> MODULES = List.of(
            "create",
            "mechanical_crafting",
            "deploying",
            "filling",
            "sequenced_assembly",
            "sequenced_assembly_matrix",
            "draconicevolution",
            "fusion_crafting",
            "fusion_matrix",
            "compactcrafting",
            "miniaturization_matrix",
            "miniaturization_controller",
            "actuallyadditions",
            "empowering",
            "atomic_reconstruction",
            "atomic_empowering_matrix",
            "mysticalagriculture",
            "infusion_altar",
            "awakening_altar",
            "infused_awakening_matrix",
            "apothic_enchanting",
            "enchantment_infusion",
            "malum",
            "spirit_infusion",
            "spirit_focusing",
            "runeworking",
            "focused_spirit_matrix",
            "ars_nouveau",
            "enchanting_apparatus",
            "imbuement_chamber",
            "arcane_lectern",
            "ritual_conductor",
            "arcane_enchanting_matrix",
            "occultism",
            "spirit_provider",
            "bound_spirit_matrix",
            "ritual_controller",
            "oritech",
            "particle_collision",
            "particle_collision_matrix",
            "particle_collision_controller");

    @Test
    void everyModuleHasAStatusRecipe() {
        for (String module : MODULES) {
            assertTrue(
                    Files.isRegularFile(RECIPES.resolve(module + ".json")),
                    "Missing status recipe for module '" + module + "'; guide pages cannot reference it");
        }
    }

    @Test
    void everyStatusRecipeNamesAKnownModule() {
        List<String> violations = new ArrayList<>();
        for (Path recipe : listFiles(RECIPES, ".json")) {
            String source = ProjectFiles.read(recipe);
            Matcher matcher = MODULE_FIELD.matcher(source);
            if (!matcher.find()) {
                violations.add(recipe.getFileName() + " has no 'module' field");
                continue;
            }
            if (!MODULES.contains(matcher.group(1))) {
                violations.add(recipe.getFileName() + " names unknown module '" + matcher.group(1) + "'");
            }
            String expected = recipe.getFileName().toString().replace(".json", "");
            if (!expected.equals(matcher.group(1))) {
                violations.add(recipe.getFileName() + " names module '" + matcher.group(1) + "'");
            }
        }
        assertTrue(violations.isEmpty(), "Broken module status recipes:\n  " + String.join("\n  ", violations));
    }

    @Test
    void guidePagesReferenceExistingStatusRecipes() {
        List<String> violations = new ArrayList<>();
        Set<String> referenced = new LinkedHashSet<>();
        for (Path page : listFiles(PAGES, ".md")) {
            Matcher matcher = TAG.matcher(ProjectFiles.read(page));
            while (matcher.find()) {
                String module = matcher.group(1);
                referenced.add(module);
                if (!Files.isRegularFile(RECIPES.resolve(module + ".json"))) {
                    violations.add(PAGES.relativize(page) + " references missing 'nep:module_status/" + module + "'");
                }
            }
        }
        assertTrue(violations.isEmpty(), "Broken module status tags:\n  " + String.join("\n  ", violations));
        assertTrue(referenced.contains("sequenced_assembly_matrix"), "No page carries a status banner");
    }

    private static List<Path> listFiles(Path dir, String suffix) {
        return ProjectFiles.walk(dir, suffix);
    }
}
