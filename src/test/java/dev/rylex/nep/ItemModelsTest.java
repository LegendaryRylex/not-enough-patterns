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
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrap.class)
class ItemModelsTest {

    private static final Path ITEMS = Paths.get("src", "main", "resources", "assets", "nep", "items");
    private static final Path MODELS = Paths.get("src", "main", "resources", "assets", "nep", "models");

    private static final List<String> COMPAT_GATED = List.of("infused_awakening_matrix");

    private static List<String> expectedItems() {
        List<String> paths = new ArrayList<>(COMPAT_GATED);
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (id.getNamespace().equals(Nep.MOD_ID) && !paths.contains(id.getPath())) {
                paths.add(id.getPath());
            }
        }
        return paths;
    }

    private static List<String> definitions() {
        try (Stream<Path> files = Files.walk(ProjectFiles.root().resolve(ITEMS))) {
            return files.filter(Files::isRegularFile)
                    .map(file -> file.getFileName().toString().replace(".json", ""))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JsonObject definition(String path) {
        return JsonParser.parseString(ProjectFiles.read(ITEMS.resolve(path + ".json")))
                .getAsJsonObject();
    }

    @Test
    void everyItemHasAClientItemModelDefinition() {
        List<String> expected = expectedItems();
        assertTrue(
                expected.contains("import_card"),
                "the item registry holds no nep items, so this test proves nothing; MinecraftBootstrap or the mod"
                        + " registration changed");

        List<String> missing = new ArrayList<>();
        for (String path : expected) {
            if (!Files.isRegularFile(ProjectFiles.root().resolve(ITEMS).resolve(path + ".json"))) {
                missing.add(path);
            }
        }

        assertTrue(
                missing.isEmpty(),
                "an item with no assets/nep/items entry renders as the missing-model cube everywhere it appears, and"
                        + " the only sign is one WARN line at resource load: " + missing);
    }

    @Test
    void everyItemModelDefinitionNamesAModelThatExists() {
        List<String> broken = new ArrayList<>();
        for (String path : definitions()) {
            JsonObject model = definition(path).getAsJsonObject("model");
            if (model == null || !model.has("type") || !model.has("model")) {
                broken.add(path + " is not a model definition");
                continue;
            }
            Identifier referenced = Identifier.parse(model.get("model").getAsString());
            Path target = ProjectFiles.root().resolve(MODELS).resolve(referenced.getPath() + ".json");
            if (!referenced.getNamespace().equals(Nep.MOD_ID) || !Files.isRegularFile(target)) {
                broken.add(path + " -> " + referenced);
            }
        }

        assertTrue(broken.isEmpty(), "an item model definition names a model nep does not ship: " + broken);
    }

    @Test
    void noItemModelDefinitionIsLeftBehindByADeletedItem() {
        List<String> expected = expectedItems();
        List<String> orphans =
                definitions().stream().filter(path -> !expected.contains(path)).toList();

        assertTrue(orphans.isEmpty(), "an item model definition names an item nep no longer registers: " + orphans);
    }
}
