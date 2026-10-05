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

class CompatIsolationTest {

    private static final Path SOURCE_ROOT = locateSourceRoot();
    private static final Path COMPAT_DIR = Paths.get("dev", "rylex", "nep", "compat");

    private static final List<ForeignRule> FOREIGN_RULES = List.of(
            new ForeignRule(
                    Paths.get("dev", "rylex", "nep", "compat", "create"),
                    List.of("com.simibubi.create", "net.createmod")),
            new ForeignRule(Paths.get("dev", "rylex", "nep", "compat", "create"), List.of("snownee.jade")),
            new ForeignRule(
                    Paths.get("dev", "rylex", "nep", "compat", "create", "newage"), List.of("org.antarcticgardens")),
            new ForeignRule(Paths.get("dev", "rylex", "nep", "compat", "ae2wtlib"), List.of("de.mari_023")),
            new ForeignRule(
                    Paths.get("dev", "rylex", "nep", "compat", "draconic"), List.of("com.brandon3055", "codechicken")),
            new ForeignRule(Paths.get("dev", "rylex", "nep", "compat", "apothic"), List.of("dev.shadowsoffire")),
            new ForeignRule(Paths.get("dev", "rylex", "nep", "compat", "compactcrafting"), List.of("dev.compactmods")),
            new ForeignRule(
                    Paths.get("dev", "rylex", "nep", "compat", "actuallyadditions"),
                    List.of("de.ellpeck.actuallyadditions")),
            new ForeignRule(
                    Paths.get("dev", "rylex", "nep", "compat", "mysticalagriculture"),
                    List.of("com.blakebr0.mysticalagriculture", "com.blakebr0.cucumber")),
            new ForeignRule(
                    Paths.get("dev", "rylex", "nep", "compat", "advancedae"), List.of("net.pedroksl.advanced_ae")),
            new ForeignRule(
                    Paths.get("dev", "rylex", "nep", "compat", "malum"),
                    List.of("com.sammy.malum", "team.lodestar.lodestone")),
            new ForeignRule(Paths.get("dev", "rylex", "nep", "compat", "ars"), List.of("com.hollingsworth.arsnouveau")),
            new ForeignRule(Paths.get("dev", "rylex", "nep", "compat", "ars", "arseng"), List.of("gripe._90.arseng")),
            new ForeignRule(Paths.get("dev", "rylex", "nep", "compat", "extendedae"), List.of("com.glodblock.github")),
            new ForeignRule(Paths.get("dev", "rylex", "nep", "compat", "emi"), List.of("dev.emi")),
            new ForeignRule(
                    Paths.get("dev", "rylex", "nep", "compat", "thunderbolt"), List.of("com.moakiee.thunderbolt")),
            new ForeignRule(Paths.get("dev", "rylex", "nep", "compat", "ae2lt"), List.of("com.moakiee.ae2lt")),
            new ForeignRule(Paths.get("dev", "rylex", "nep", "compat", "sable"), List.of("dev.ryanhcode")),
            new ForeignRule(
                    Paths.get("dev", "rylex", "nep", "compat", "provider"),
                    List.of("com.loliball.appliedcreate", "com.ae2draconicfusion")),
            new ForeignRule(COMPAT_DIR, List.of("mezz.jei")));

    private static final List<String> COMPAT_ENTRY_POINTS = List.of(
            "dev.rylex.nep.compat.create.CreateCompat",
            "dev.rylex.nep.compat.create.CreateGuideRecipes",
            "dev.rylex.nep.compat.draconic.DraconicCompat",
            "dev.rylex.nep.compat.apothic.ApothicCompat",
            "dev.rylex.nep.compat.compactcrafting.CompactCraftingCompat",
            "dev.rylex.nep.compat.actuallyadditions.ActuallyAdditionsCompat",
            "dev.rylex.nep.compat.mysticalagriculture.MysticalAgricultureCompat",
            "dev.rylex.nep.compat.malum.MalumCompat",
            "dev.rylex.nep.compat.ars.ArsCompat",
            "dev.rylex.nep.compat.extendedae.ExtendedAeCompat",
            "dev.rylex.nep.compat.sable.SableSubLevels");

    @Test
    void foreignClassesStayInsideTheirCompatPackage() {
        List<String> violations = new ArrayList<>();
        forEachSourceFile((relative, source) -> {
            for (ForeignRule rule : FOREIGN_RULES) {
                if (relative.startsWith(rule.allowedDir())) {
                    continue;
                }
                for (String prefix : rule.forbiddenPrefixes()) {
                    if (source.contains(prefix)) {
                        violations.add(relative + " references '" + prefix + "'");
                    }
                }
            }
        });
        assertTrue(
                violations.isEmpty(),
                "Foreign mod classes leaked outside their compat package (breaks lazy loading; crashes when the "
                        + "integration is absent):\n  " + String.join("\n  ", violations));
    }

    @Test
    void coreReachesCompatOnlyThroughGuardedEntryPoints() {
        List<String> violations = new ArrayList<>();
        forEachSourceFile((relative, source) -> {
            if (relative.startsWith(COMPAT_DIR)) {
                return;
            }
            source.lines()
                    .map(String::trim)
                    .filter(line -> line.startsWith("import dev.rylex.nep.compat."))
                    .map(line -> line.substring("import ".length(), line.indexOf(';'))
                            .trim())
                    .filter(fqn -> !COMPAT_ENTRY_POINTS.contains(fqn))
                    .forEach(fqn -> violations.add(relative + " imports '" + fqn + "'"));
        });
        assertTrue(
                violations.isEmpty(),
                "Core code names a compat class that is not a guarded entry point (pulls the compat package in "
                        + "eagerly). Add it to COMPAT_ENTRY_POINTS only if it is guarded by ModList.isLoaded:\n  "
                        + String.join("\n  ", violations));
    }

    private interface SourceVisitor {
        void visit(Path relative, String source);
    }

    private static void forEachSourceFile(SourceVisitor visitor) {
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(file -> {
                Path relative = SOURCE_ROOT.relativize(file);
                visitor.visit(relative, read(file));
            });
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

    private static Path locateSourceRoot() {
        String projectDir = System.getProperty("nep.projectDir");
        if (projectDir != null) {
            Path anchored = Paths.get(projectDir, "src", "main", "java");
            if (Files.isDirectory(anchored)) {
                return anchored.toAbsolutePath().normalize();
            }
        }
        for (Path candidate : List.of(Paths.get("src", "main", "java"), Paths.get("nep", "src", "main", "java"))) {
            if (Files.isDirectory(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException(
                "Could not locate src/main/java from " + Paths.get("").toAbsolutePath());
    }

    private record ForeignRule(Path allowedDir, List<String> forbiddenPrefixes) {}
}
