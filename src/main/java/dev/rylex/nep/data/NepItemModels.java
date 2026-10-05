package dev.rylex.nep.data;

import dev.rylex.nep.Nep;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class NepItemModels extends ItemModelProvider {

    private static final Map<String, String> BORROWED_TEXTURES =
            Map.of("empowered_matrix_circuitry", "matrix_circuitry");

    public NepItemModels(PackOutput output, ExistingFileHelper existing) {
        super(output, Nep.MOD_ID, existing);
    }

    @Override
    protected void registerModels() {
        BuiltInRegistries.ITEM.entrySet().stream()
                .filter(entry -> Nep.MOD_ID.equals(entry.getKey().location().getNamespace()))
                .filter(entry -> !(entry.getValue() instanceof BlockItem))
                .map(entry -> entry.getKey().location().getPath())
                .sorted()
                .forEach(this::flatItem);
    }

    private void flatItem(String path) {
        singleTexture(
                path, mcLoc("item/generated"), "layer0", Nep.id("item/" + BORROWED_TEXTURES.getOrDefault(path, path)));
    }
}
