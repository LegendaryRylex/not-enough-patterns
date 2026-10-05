package dev.rylex.nep.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class GuideStructureTest {

    private static final Path RESOURCES = locate("src/main/resources", "nep/src/main/resources");
    private static final Path PAGES = RESOURCES.resolve("assets/nep/ae2guide/nep");
    private static final Path FILTERS = RESOURCES.resolve("guide_filters");
    private static final Path SOURCE = RESOURCES.resolveSibling("java");

    private static final Set<String> ALWAYS_PRESENT = Set.of(
            "nep-index.md",
            "getting-started.md",
            "import-card.md",
            "pattern-conversion.md",
            "machine-hub.md",
            "pattern-decoder.md");
    private static final String INDEX = "nep-index.md";

    private static final Pattern PARENT = Pattern.compile("(?m)^\\s*parent:\\s*(\\S+)\\s*$");
    private static final Pattern LINK = Pattern.compile("\\]\\(([^)\\s]+)\\)");
    private static final Pattern HELP_BUTTON = Pattern.compile("addHelpButton\\(\"([^\"]+)\"\\)");

    @Test
    void onlyTheAlwaysPresentPagesLiveInTheGuideRoot() {
        Set<String> atRoot = new TreeSet<>();
        for (Path page : listMarkdown(PAGES, false)) {
            atRoot.add(page.getFileName().toString());
        }
        assertEquals(
                new TreeSet<>(ALWAYS_PRESENT),
                atRoot,
                "the guide root is for the pages that exist whatever mods are loaded; everything else belongs in "
                        + "its mod's subfolder");

        List<String> missing = new ArrayList<>();
        for (String folder : modFolders()) {
            if (!Files.isRegularFile(PAGES.resolve(folder).resolve("index.md"))) {
                missing.add(folder);
            }
        }
        assertTrue(missing.isEmpty(), "mod folders without a hub page named index.md: " + missing);
    }

    @Test
    void everyNavigationParentNamesAPageThatExists() {
        List<String> violations = new ArrayList<>();
        for (Path page : listMarkdown(PAGES, true)) {
            String name = PAGES.relativize(page).toString().replace('\\', '/');
            Matcher matcher = PARENT.matcher(read(page));
            if (!matcher.find()) {
                if (!INDEX.equals(name)) {
                    violations.add(name + " has no navigation parent, so it never appears in the sidebar");
                }
                continue;
            }
            String parent = matcher.group(1);
            if (!parent.startsWith("nep/")) {
                violations.add(name + " has parent '" + parent + "'; parents are absolute and start with 'nep/'");
                continue;
            }
            if (!Files.isRegularFile(PAGES.resolve(parent.substring("nep/".length())))) {
                violations.add(name + " has parent '" + parent + "', which is not a page");
            }
        }
        assertTrue(
                violations.isEmpty(),
                "a parent that does not resolve drops the page and everything under it from the guide sidebar, with "
                        + "nothing shown in game:\n  " + String.join("\n  ", violations));
    }

    @Test
    void everyCrossPageLinkIsAbsoluteAndNamesAPageThatExists() {
        List<String> violations = new ArrayList<>();
        for (Path page : listMarkdown(PAGES, true)) {
            String name = PAGES.relativize(page).toString().replace('\\', '/');
            Matcher matcher = LINK.matcher(read(page));
            while (matcher.find()) {
                String target = matcher.group(1);
                if (target.startsWith("http://") || target.startsWith("https://") || target.startsWith("#")) {
                    continue;
                }
                if (!target.startsWith("/nep/")) {
                    violations.add(name + " links to '" + target + "'; links are absolute and start with '/nep/'");
                    continue;
                }
                String relative = target.substring("/nep/".length()).split("#", 2)[0];
                if (!Files.isRegularFile(PAGES.resolve(relative))) {
                    violations.add(name + " links to '" + target + "', which is not a page");
                }
            }
        }
        assertTrue(
                violations.isEmpty(),
                "a relative link breaks as soon as either page moves, and a link that does not resolve renders as an "
                        + "error inside the page:\n  " + String.join("\n  ", violations));
    }

    @Test
    void eachModFolderIsHiddenByItsOwnFilterPackAndNothingElseIs() {
        List<String> violations = new ArrayList<>();
        List<Path> pages = listMarkdown(PAGES, true);

        for (String folder : modFolders()) {
            Path meta = FILTERS.resolve("no_" + folder).resolve("pack.mcmeta");
            if (!Files.isRegularFile(meta)) {
                violations.add("no filter pack at guide_filters/no_" + folder + ", so those pages show without "
                        + folder + " loaded");
                continue;
            }
            List<Pattern> blocked = blockedPaths(meta);
            for (Path page : pages) {
                String resource =
                        "ae2guide/nep/" + PAGES.relativize(page).toString().replace('\\', '/');
                boolean hidden =
                        blocked.stream().anyMatch(p -> p.matcher(resource).find());
                boolean belongs = resource.startsWith("ae2guide/nep/" + folder + "/");
                if (hidden != belongs) {
                    violations.add("no_" + folder + (hidden ? " hides " : " does not hide ") + resource);
                }
            }
        }
        assertTrue(
                violations.isEmpty(),
                "guide filter packs do not line up with the page folders:\n  " + String.join("\n  ", violations));
    }

    @Test
    void everyMachineScreenOpensAPageThatExists() {
        List<String> violations = new ArrayList<>();
        List<Path> screens = javaFiles("Screen.java");
        assertTrue(screens.size() >= 8, "no machine screens were found to check, so this test proves nothing");

        for (Path screen : screens) {
            String name = screen.getFileName().toString();
            String source = read(screen);
            if (source.contains("abstract class")) {
                continue;
            }
            Matcher matcher = HELP_BUTTON.matcher(source);
            if (!matcher.find()) {
                if (source.contains("addRightButton(")) {
                    violations.add(name + " places buttons but never calls addHelpButton");
                }
                continue;
            }
            String page = matcher.group(1);
            if (!page.startsWith("nep/")) {
                violations.add(name + " opens '" + page + "'; page ids are absolute and start with 'nep/'");
            } else if (!Files.isRegularFile(PAGES.resolve(page.substring("nep/".length())))) {
                violations.add(name + " opens '" + page + "', which is not a page");
            }
            if (matcher.find()) {
                violations.add(name + " adds more than one help button");
            }
        }
        assertTrue(
                violations.isEmpty(),
                "the help button is the one control every machine carries, and a page id it cannot resolve leaves the "
                        + "player staring at a guide error:\n  " + String.join("\n  ", violations));
    }

    private static List<Path> javaFiles(String suffix) {
        try (Stream<Path> files = Files.walk(SOURCE)) {
            return files.filter(path -> path.getFileName().toString().endsWith(suffix))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Pattern> blockedPaths(Path meta) {
        JsonObject root = JsonParser.parseString(read(meta)).getAsJsonObject();
        List<Pattern> patterns = new ArrayList<>();
        for (JsonElement entry : root.getAsJsonObject("filter").getAsJsonArray("block")) {
            JsonObject block = entry.getAsJsonObject();
            if ("nep".equals(block.get("namespace").getAsString())) {
                patterns.add(Pattern.compile(block.get("path").getAsString()));
            }
        }
        return patterns;
    }

    private static Set<String> modFolders() {
        Set<String> folders = new LinkedHashSet<>();
        try (Stream<Path> entries = Files.list(PAGES)) {
            entries.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .forEach(folders::add);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return folders;
    }

    private static List<Path> listMarkdown(Path dir, boolean recursive) {
        try (Stream<Path> files = recursive ? Files.walk(dir) : Files.list(dir)) {
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
