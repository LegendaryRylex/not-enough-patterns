package dev.rylex.nep.guide;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rylex.nep.ProjectFiles;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class GuideConfigValuesTest {

    private static final Path PAGES = ProjectFiles.resources().resolve("assets/nep/ae2guide/nep");

    private static final Pattern USE = Pattern.compile("<nep:ConfigValue\\s+name=\"([^\"]*)\"\\s*/>");
    private static final Pattern ANY_USE = Pattern.compile("<nep:ConfigValue[^>]*>");

    @Test
    void everyConfigValueTagNamesASettingThatExists() {
        List<String> violations = new ArrayList<>();
        for (Path page : markdown()) {
            String name = PAGES.relativize(page).toString().replace('\\', '/');
            String source = ProjectFiles.read(page);

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
            Matcher matcher = USE.matcher(ProjectFiles.read(page));
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
        return ProjectFiles.walk(PAGES, ".md");
    }
}
