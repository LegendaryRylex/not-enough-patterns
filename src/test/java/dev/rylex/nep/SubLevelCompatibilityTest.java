package dev.rylex.nep;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SubLevelCompatibilityTest {

    private static final Path SOURCE_ROOT = ProjectFiles.root().resolve(Paths.get("src", "main", "java"));

    @Test
    void menusMeasureReachThroughSubLevels() {
        List<String> violations = new ArrayList<>();
        forEachSourceFile((relative, source) -> {
            if (source.contains("player.distanceToSqr(") || source.contains(".distanceToSqr(player")) {
                violations.add(relative.toString());
            }
        });
        assertTrue(
                violations.isEmpty(),
                "Raw distanceToSqr compares a player in world space against a block that Sable may keep in a "
                        + "sub-level plot thousands of blocks away, which closes the screen on any ship. Use "
                        + "MachineMenu.withinReach or SubLevels.distanceSqr:\n  " + String.join("\n  ", violations));
    }

    @Test
    void blockEntitiesThatDropContentsAlsoClearThem() {
        List<String> violations = new ArrayList<>();
        forEachSourceFile((relative, source) -> {
            if (!source.contains(" void dropBuffer(") && !source.contains(" void dropBuffers(")) {
                return;
            }
            if (!source.contains("clearContent()")) {
                violations.add(relative.toString());
            }
        });
        assertTrue(
                violations.isEmpty(),
                "A block entity drops its contents on removal without clearing them. Sable snapshots the block "
                        + "entity into a sub-level and then removes the original, so anything the removal drops is "
                        + "duplicated. Implement Clearable and call clearContent() from the drop:\n  "
                        + String.join("\n  ", violations));
    }

    private interface SourceVisitor {
        void visit(Path relative, String source);
    }

    private static void forEachSourceFile(SourceVisitor visitor) {
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    visitor.visit(SOURCE_ROOT.relativize(path), Files.readString(path));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
