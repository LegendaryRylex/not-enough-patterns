package dev.rylex.nep.guide;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModuleStatusTest {

    private static final Path RESOURCES = locate("src/main/resources", "nep/src/main/resources");
    private static final Path RECIPES = RESOURCES.resolve("data/nep/recipe/module_status");
    private static final Path PAGES = RESOURCES.resolve("assets/nep/ae2guide/nep");

    private static final Pattern TAG = Pattern.compile("<Recipe id=\"nep:module_status/([a-z_]+)\"\\s*/>");
    private static final Pattern MODULE_FIELD = Pattern.compile("\"module\"\\s*:\\s*\"([a-z_]+)\"");

    private static final List<String> MODULES = List.of(
            "create", "mechanical_crafting", "deploying", "filling", "sequenced_assembly", "sequenced_assembly_matrix");

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
            String source = read(recipe);
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
            Matcher matcher = TAG.matcher(read(page));
            while (matcher.find()) {
                String module = matcher.group(1);
                referenced.add(module);
                if (!Files.isRegularFile(RECIPES.resolve(module + ".json"))) {
                    violations.add(page.getFileName() + " references missing 'nep:module_status/" + module + "'");
                }
            }
        }
        assertTrue(violations.isEmpty(), "Broken module status tags:\n  " + String.join("\n  ", violations));
        assertTrue(referenced.contains("sequenced_assembly_matrix"), "No page carries a status banner");
    }

    private static List<Path> listFiles(Path dir, String suffix) {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.toString().endsWith(suffix)).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path locate(String... candidates) {
        String projectDir = System.getProperty("nep.projectDir");
        for (String candidate : candidates) {
            if (projectDir != null) {
                Path anchored = Paths.get(projectDir).resolve(candidate);
                if (Files.isDirectory(anchored)) {
                    return anchored.toAbsolutePath().normalize();
                }
            }
            Path path = Paths.get(candidate);
            if (Files.isDirectory(path)) {
                return path.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException(
                "Could not locate src/main/resources from " + Paths.get("").toAbsolutePath());
    }
}
