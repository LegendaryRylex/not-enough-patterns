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

class SourceCommentsTest {

    private static final List<Path> SOURCE_SETS = List.of(
            Paths.get("src", "main", "java"), Paths.get("src", "test", "java"), Paths.get("src", "gametest", "java"));

    private static final List<Allowance> ALLOWED = List.of(new Allowance(
            "src/main/java/dev/rylex/nep/client/NepGuidePacks.java", "GuideMe has no conditional frontmatter"));

    @Test
    void sourceCarriesNoCommentsBeyondTheAllowedOnes() {
        List<String> violations = new ArrayList<>();
        forEachSourceFile((relative, source) -> {
            for (Comment comment : commentsIn(source)) {
                if (isAllowed(relative, comment)) {
                    continue;
                }
                violations.add(relative + ":" + comment.line() + "  " + summarise(comment.text()));
            }
        });
        assertTrue(
                violations.isEmpty(),
                "Source carries comments. Rationale belongs in the commit message, not the file. Delete these, or "
                        + "add an ALLOWED entry if the comment documents something a reader cannot recover from the "
                        + "code:\n  " + String.join("\n  ", violations));
    }

    @Test
    void everyAllowanceStillMatchesAComment() {
        List<String> unmatched =
                new ArrayList<>(ALLOWED.stream().map(Allowance::toString).toList());
        forEachSourceFile((relative, source) -> {
            for (Comment comment : commentsIn(source)) {
                ALLOWED.stream()
                        .filter(allowance -> allowance.matches(relative, comment))
                        .map(Allowance::toString)
                        .forEach(unmatched::remove);
            }
        });
        assertTrue(
                unmatched.isEmpty(),
                "An ALLOWED entry no longer matches any comment, so it is stale and silently permits nothing. "
                        + "Remove it:\n  " + String.join("\n  ", unmatched));
    }

    private static boolean isAllowed(String relative, Comment comment) {
        return ALLOWED.stream().anyMatch(allowance -> allowance.matches(relative, comment));
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
                append(found, new Comment(line, source.substring(start, index).trim(), ownsItsLine(source, start)));
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
                found.add(new Comment(startLine, source.substring(start, index).trim(), ownsItsLine(source, start)));
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
        Path root = ProjectFiles.root();
        for (Path sourceSet : SOURCE_SETS) {
            Path directory = root.resolve(sourceSet);
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(directory)) {
                files.filter(file -> file.toString().endsWith(".java"))
                        .sorted()
                        .forEach(file ->
                                visitor.visit(root.relativize(file).toString().replace('\\', '/'), read(file)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record Comment(int line, String text, boolean ownsItsLine) {

        Comment joinedWith(Comment next) {
            return new Comment(line, text + "\n" + next.text(), ownsItsLine);
        }
    }

    private record Allowance(String file, String fragment) {

        boolean matches(String relative, Comment comment) {
            return file.equals(relative) && comment.text().contains(fragment);
        }

        @Override
        public String toString() {
            return file + "  " + fragment;
        }
    }
}
