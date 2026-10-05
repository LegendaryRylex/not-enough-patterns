package dev.rylex.nep;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ExtendedAePatternViewsTest {

    private static final Path PATTERNS = Paths.get("src", "main", "java", "dev", "rylex", "nep", "pattern");
    private static final Path COMPAT =
            Paths.get("src", "main", "java", "dev", "rylex", "nep", "compat", "extendedae", "ExtendedAeCompat.java");

    private static final Pattern DECLARATION = Pattern.compile(
            "^public class (\\w+) (?:extends RecipePattern|implements NepPattern)\\b", Pattern.MULTILINE);

    @Test
    void everyPatternTypeHasAViewHandler() {
        String compat = ProjectFiles.read(COMPAT);
        List<String> unhandled = new ArrayList<>();
        for (Path source : patternSources()) {
            Matcher declaration = DECLARATION.matcher(read(source));
            if (declaration.find() && !compat.contains(declaration.group(1) + ".class")) {
                unhandled.add(declaration.group(1));
            }
        }
        assertTrue(
                unhandled.isEmpty(),
                "ExtendedAE keys its pattern view on the exact details class, so a pattern type missing from "
                        + "ExtendedAeCompat opens no view at all and tells the player to report a bug to ExtendedAE:\n  "
                        + String.join("\n  ", unhandled));
    }

    private static List<Path> patternSources() {
        try (Stream<Path> files = Files.list(ProjectFiles.root().resolve(PATTERNS))) {
            return files.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path source) {
        try {
            return Files.readString(source);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
