package dev.rylex.nep.data;

import dev.rylex.nep.Nep;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class NepItemModels extends ItemModelProvider {

    private static final Map<String, String> BORROWED_TEXTURES =
            Map.of("empowered_matrix_circuitry", "nep:item/matrix_circuitry");

    private static final String SPIRIT_BOOK = "spirit_infused_book";
    private static final List<String> SPIRIT_BOOK_TIERS = List.of("foliot", "djinni", "afrit", "marid");
    private static final List<String> SPIRIT_BOOK_WORKS = List.of("crusher", "crystallizer");
    private static final ResourceLocation SPIRIT_BOOK_PROPERTY = Nep.id("bound_spirit");

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
        if (SPIRIT_BOOK.equals(path)) {
            spiritBook();
            return;
        }
        String texture = BORROWED_TEXTURES.get(path);
        singleTexture(
                path,
                mcLoc("item/generated"),
                "layer0",
                texture == null ? Nep.id("item/" + path) : ResourceLocation.parse(texture));
    }

    /** Each override's predicate value is the {@code nep:bound_spirit} item property, tier-major from 1. */
    private void spiritBook() {
        ItemModelBuilder base =
                singleTexture(SPIRIT_BOOK, mcLoc("item/generated"), "layer0", Nep.id("item/" + SPIRIT_BOOK));
        int index = 0;
        for (String tier : SPIRIT_BOOK_TIERS) {
            for (String work : SPIRIT_BOOK_WORKS) {
                String variant = SPIRIT_BOOK + "_" + tier + "_" + work;
                ModelFile model = singleTexture(variant, mcLoc("item/generated"), "layer0", Nep.id("item/" + variant));
                base.override()
                        .predicate(SPIRIT_BOOK_PROPERTY, ++index)
                        .model(model)
                        .end();
            }
        }
    }
}
