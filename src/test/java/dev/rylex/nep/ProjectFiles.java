package dev.rylex.nep;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public final class ProjectFiles {

    private ProjectFiles() {}

    public static Path root() {
        String projectDir = System.getProperty("nep.projectDir");
        if (projectDir != null && Files.isDirectory(Paths.get(projectDir, "src", "main"))) {
            return Paths.get(projectDir).toAbsolutePath().normalize();
        }
        for (Path candidate : List.of(Paths.get(""), Paths.get("nep"))) {
            if (Files.isDirectory(candidate.resolve(Paths.get("src", "main")))) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException(
                "Could not locate the project root from " + Paths.get("").toAbsolutePath());
    }

    public static Path resources() {
        return root().resolve(Paths.get("src", "main", "resources"));
    }

    public static String read(Path relative) {
        try {
            return Files.readString(root().resolve(relative));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<Path> walk(Path dir, String suffix) {
        try (Stream<Path> files = Files.walk(root().resolve(dir))) {
            return files.filter(path -> path.getFileName().toString().endsWith(suffix))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<Path> list(Path dir, String suffix) {
        try (Stream<Path> files = Files.list(root().resolve(dir))) {
            return files.filter(path -> path.getFileName().toString().endsWith(suffix))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
