package dev.rylex.nep;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LangKeysTest {

    private static final Path LANG = Paths.get("src", "main", "resources", "assets", "nep", "lang", "en_us.json");
    private static final Path SOURCE = Paths.get("src", "main", "java");

    private static final Pattern LITERAL = Pattern.compile("\"([^\"\\\\\\n]+)\"");
    private static final Pattern KEY_SHAPED = Pattern.compile("[a-z][A-Za-z]*(\\.[A-Za-z0-9_]+)+");
    private static final Pattern INHERITED_TITLE =
            Pattern.compile("draw(?:Centered)?String\\(\\s*(?:this\\.)?font,\\s*(?:this\\.)?title\\b");

    private static JsonObject lang() {
        return JsonParser.parseString(ProjectFiles.read(LANG)).getAsJsonObject();
    }

    private static List<Path> sources(String suffix) {
        try (Stream<Path> files = Files.walk(ProjectFiles.root().resolve(SOURCE))) {
            return files.filter(path -> path.getFileName().toString().endsWith(suffix))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String relative(Path source) {
        return ProjectFiles.root().relativize(source).toString().replace('\\', '/');
    }

    private static String read(Path source) {
        try {
            return Files.readString(source);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void everyScreenTitleIsALangKeyOfItsOwn() {
        List<String> derived = new ArrayList<>();
        for (Path screen : sources("Screen.java")) {
            if (INHERITED_TITLE.matcher(read(screen)).find()) {
                derived.add(relative(screen));
            }
        }
        assertTrue(
                derived.isEmpty(),
                "these screens draw the menu title, which is the block's name, so a translator cannot word the panel "
                        + "header without renaming the block; draw a gui.nep.<machine>.title key instead:\n  "
                        + String.join("\n  ", derived));
    }

    @Test
    void everyTranslationKeyInTheSourceExists() {
        JsonObject lang = lang();
        List<String> keys = lang.keySet().stream().toList();
        List<String> missing = new ArrayList<>();

        for (Path source : sources(".java")) {
            Matcher literals = LITERAL.matcher(read(source));
            while (literals.find()) {
                String candidate = literals.group(1);
                if (!candidate.contains("nep")
                        || candidate.startsWith("dev.rylex")
                        || !KEY_SHAPED.matcher(candidate).matches()) {
                    continue;
                }
                boolean known = lang.has(candidate) || keys.stream().anyMatch(key -> key.startsWith(candidate + "."));
                if (!known) {
                    missing.add(candidate + " (" + relative(source) + ")");
                }
            }
        }

        assertTrue(
                missing.isEmpty(),
                "these keys would render raw in game because en_us.json does not define them, and no translation "
                        + "can reach them:\n  " + String.join("\n  ", missing));
    }

    @Test
    void everyMachineHasItsOwnPanelTitle() {
        JsonObject lang = lang();
        List<String> untitled = new ArrayList<>();
        for (Map.Entry<String, String> machine : Map.of(
                        "gui.nep.sequenced_assembly.title", "Sequenced Assembly Controller",
                        "gui.nep.sequenced_assembly_matrix.title", "Assembly Matrix",
                        "gui.nep.fusion_matrix.title", "Injector Fusion Matrix",
                        "gui.nep.atomic_empowering_matrix.title", "Atomic Empowering Matrix",
                        "gui.nep.miniaturization_matrix.title", "Miniaturization Matrix",
                        "gui.nep.miniaturization_controller.title", "Miniaturization Controller",
                        "gui.nep.infused_awakening_matrix.title", "Infused Awakening Matrix")
                .entrySet()) {
            if (!lang.has(machine.getKey())) {
                untitled.add(machine.getValue() + " -> " + machine.getKey());
            }
        }
        assertTrue(
                untitled.isEmpty(),
                "every machine screen carries a panel title separate from its block name:\n  "
                        + String.join("\n  ", untitled));
    }
}
