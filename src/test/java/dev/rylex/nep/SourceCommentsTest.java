package dev.rylex.nep;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SourceCommentsTest {

    @Test
    void sourceCarriesNoCommentsBeyondJavadocOnDeclarations() {
        List<String> violations = new ArrayList<>();
        forEachSourceFile((relative, source) -> {
            for (Comment comment : commentsIn(source)) {
                String fault = faultIn(source, comment);
                if (fault != null) {
                    violations.add(relative + ":" + comment.line() + "  " + fault + " -- " + summarise(comment.text()));
                }
            }
        });
        assertTrue(
                violations.isEmpty(),
                "Only javadoc on a declaration is permitted, and only for what the code cannot say itself: an "
                        + "ordering or lifecycle constraint, a fact about a foreign mod, a unit or a null case. "
                        + "Everything else belongs in the commit message. There is no allowance list.\n  "
                        + String.join("\n  ", violations));
    }

    private static String faultIn(String source, Comment comment) {
        if (comment.text().startsWith("//")) {
            return "line comment";
        }
        if (!comment.text().startsWith("/**")) {
            return "block comment";
        }
        if (!comment.ownsItsLine()) {
            return "javadoc trailing code";
        }
        if (!attachedToDeclaration(source, comment.end())) {
            return "javadoc not on a declaration";
        }
        return null;
    }

    private static boolean attachedToDeclaration(String source, int end) {
        for (String raw : source.substring(Math.min(end, source.length())).split("\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("@")) {
                continue;
            }
            return !line.startsWith("}") && !line.startsWith("/*") && !line.startsWith("//");
        }
        return false;
    }

    private static String summarise(String text) {
        String flattened = text.replaceAll("\\s+", " ").trim();
        return flattened.length() <= 72 ? flattened : flattened.substring(0, 69) + "...";
    }

    private static List<Comment> commentsIn(String source) {
        List<Comment> found = new ArrayList<>();
        int length = source.length();
        int line = 1;
        int index = 0;
        while (index < length) {
            char current = source.charAt(index);
            if (current == '\n') {
                line++;
                index++;
            } else if (source.startsWith("\"\"\"", index)) {
                index += 3;
                while (index < length && !source.startsWith("\"\"\"", index)) {
                    if (source.charAt(index) == '\n') {
                        line++;
                    }
                    index += source.charAt(index) == '\\' ? 2 : 1;
                }
                index = Math.min(length, index + 3);
            } else if (current == '"' || current == '\'') {
                index++;
                while (index < length && source.charAt(index) != current) {
                    index += source.charAt(index) == '\\' ? 2 : 1;
                }
                index++;
            } else if (source.startsWith("//", index)) {
                int start = index;
                while (index < length && source.charAt(index) != '\n') {
                    index++;
                }
                append(
                        found,
                        new Comment(line, source.substring(start, index).trim(), ownsItsLine(source, start), index));
            } else if (source.startsWith("/*", index)) {
                int start = index;
                int startLine = line;
                index += 2;
                while (index < length && !source.startsWith("*/", index)) {
                    if (source.charAt(index) == '\n') {
                        line++;
                    }
                    index++;
                }
                index = Math.min(length, index + 2);
                found.add(new Comment(
                        startLine, source.substring(start, index).trim(), ownsItsLine(source, start), index));
            } else {
                index++;
            }
        }
        return found;
    }

    private static void append(List<Comment> found, Comment comment) {
        Comment previous = found.isEmpty() ? null : found.getLast();
        boolean continues = previous != null
                && previous.ownsItsLine()
                && comment.ownsItsLine()
                && previous.text().startsWith("//")
                && previous.line() + previous.text().lines().count() == comment.line();
        if (continues) {
            found.set(found.size() - 1, previous.joinedWith(comment));
        } else {
            found.add(comment);
        }
    }

    private static boolean ownsItsLine(String source, int start) {
        for (int index = start - 1; index >= 0 && source.charAt(index) != '\n'; index--) {
            if (!Character.isWhitespace(source.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private interface SourceVisitor {
        void visit(String relative, String source);
    }

    private static void forEachSourceFile(SourceVisitor visitor) {
        Path root = projectRoot();
        Path src = root.resolve("src");
        List<Path> sourceSets = new ArrayList<>();
        try (Stream<Path> children = Files.list(src)) {
            children.map(child -> child.resolve("java"))
                    .filter(Files::isDirectory)
                    .sorted()
                    .forEach(sourceSets::add);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertTrue(!sourceSets.isEmpty(), "No java source sets found under " + src);
        for (Path sourceSet : sourceSets) {
            try (Stream<Path> files = Files.walk(sourceSet)) {
                files.filter(file -> file.toString().endsWith(".java"))
                        .sorted()
                        .forEach(file ->
                                visitor.visit(root.relativize(file).toString().replace('\\', '/'), read(file)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static Path projectRoot() {
        for (Path start : List.of(Paths.get("").toAbsolutePath(), classesRoot())) {
            for (Path candidate = start; candidate != null; candidate = candidate.getParent()) {
                if (Files.isDirectory(candidate.resolve(Paths.get("src", "main", "java")))) {
                    return candidate.normalize();
                }
            }
        }
        throw new IllegalStateException(
                "Could not locate the project root from " + Paths.get("").toAbsolutePath());
    }

    private static Path classesRoot() {
        try {
            return Paths.get(SourceCommentsTest.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI())
                    .toAbsolutePath();
        } catch (URISyntaxException | NullPointerException e) {
            return Paths.get("").toAbsolutePath();
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record Comment(int line, String text, boolean ownsItsLine, int end) {

        Comment joinedWith(Comment next) {
            return new Comment(line, text + "\n" + next.text(), ownsItsLine, next.end());
        }
    }
}
