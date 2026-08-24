package dev.rylex.nep.guide;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class GuideConfigValuesTest {

    private static final Path PAGES =
            locate("src/main/resources", "nep/src/main/resources").resolve("assets/nep/ae2guide/nep");

    private static final Pattern USE = Pattern.compile("<nep:ConfigValue\s+name=\"([^\"]*)\"\s*/>");
    private static final Pattern ANY_USE = Pattern.compile("<nep:ConfigValue[^>]*>");

    @Test
    void everyConfigValueTagNamesASettingThatExists() {
        List<String> violations = new ArrayList<>();
        for (Path page : markdown()) {
            String name = PAGES.relativize(page).toString().replace('\\', '/');
            String source = read(page);

            Matcher wellFormed = USE.matcher(source);
            long matches = wellFormed.results().count();
            long tags = ANY_USE.matcher(source).results().count();
            if (matches != tags) {
                violations.add(name + " has a nep:ConfigValue tag that is not a self-closing name=\"...\"");
            }

            wellFormed.reset();
            while (wellFormed.find()) {
                String setting = wellFormed.group(1);
                if (!NepConfigValues.names().contains(setting)) {
                    violations.add(name + " reads '" + setting + "', which no setting goes by");
                }
            }
        }
        assertTrue(
                violations.isEmpty(),
                "a nep:ConfigValue that does not resolve renders as an error in the middle of the sentence it sits "
                        + "in:\n  " + String.join("\n  ", violations));
    }

    @Test
    void everySettingOfferedToTheGuideIsReadByAPage() {
        Set<String> used = new TreeSet<>();
        for (Path page : markdown()) {
            Matcher matcher = USE.matcher(read(page));
            while (matcher.find()) {
                used.add(matcher.group(1));
            }
        }
        Set<String> unused = new TreeSet<>(NepConfigValues.names());
        unused.removeAll(used);
        assertTrue(
                unused.isEmpty(),
                "these settings are offered to the guide but no page reads them, so nothing proves they still "
                        + "format:\n  " + String.join("\n  ", unused));
    }

    @Test
    void everySettingOfferedToTheGuideFormats() {
        List<String> violations = new ArrayList<>();
        for (String setting : NepConfigValues.names()) {
            String value = NepConfigValues.format(setting);
            if (value == null || value.isBlank()) {
                violations.add(setting);
            }
        }
        assertTrue(violations.isEmpty(), "settings that format to nothing: " + violations);
    }

    private static List<Path> markdown() {
        try (Stream<Path> files = Files.walk(PAGES)) {
            return files.filter(path -> path.toString().endsWith(".md"))
                    .sorted()
                    .toList();
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
