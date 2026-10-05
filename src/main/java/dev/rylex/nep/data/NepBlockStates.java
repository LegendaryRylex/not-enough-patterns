package dev.rylex.nep.data;

import dev.rylex.nep.Nep;
import dev.rylex.nep.decoder.DecoderModule;
import dev.rylex.nep.machine.MatrixStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.CompositeModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class NepBlockStates extends BlockStateProvider {

    private static final String STATUS = "status";
    private static final String AXIS = "axis";
    private static final String IDLE = "idle";
    private static final String RENDER_TYPE = "minecraft:cutout_mipped";
    private static final ResourceLocation SHAFT_SIDE = ResourceLocation.fromNamespaceAndPath("create", "block/axis");
    private static final ResourceLocation SHAFT_END = ResourceLocation.fromNamespaceAndPath("create", "block/axis_top");

    private static final List<Matrix> MATRICES = List.of(
            new Matrix("sequenced_assembly_matrix", "sequenced_assembler", true),
            new Matrix("fusion_matrix", "fusion", false),
            new Matrix("atomic_empowering_matrix", "empowering", false),
            new Matrix("infused_awakening_matrix", "infused_awakening", false),
            new Matrix("miniaturization_matrix", "miniaturization", false),
            new Matrix("focused_spirit_matrix", "focused_spirit", false),
            new Matrix("arcane_enchanting_matrix", "arcane_enchanting", false));

    private static final String ARCANE_LECTERN = "arcane_lectern";
    private static final String RITUAL_CONDUCTOR = "ritual_conductor";
    private static final String FACING = "facing";
    private static final String ONLINE = "online";
    private static final String CONDUCTING = "conducting";

    private static final String MACHINE_HUB = "machine_hub";
    private static final String PATTERN_DECODER = "pattern_decoder";
    private static final String MINIATURIZATION_CONTROLLER = "miniaturization_controller";
    private static final String ASSEMBLY_CONTROLLER = "sequenced_assembly_controller";
    private static final String WORKING = "working";
    private static final String ASSEMBLY_WORKING = "on";
    private static final String HALTED = "halted";
    private static final String ASSEMBLY_COG = "block/machine/assembly_cog";
    private static final String TRANSLUCENT = "minecraft:translucent";
    private static final String SOLID = "minecraft:solid";
    private static final String FIELD = "field";
    private static final List<String> MINIATURIZATION_STATES = List.of("idle", "running", "stalled");
    private static final List<String> ASSEMBLY_STATES = List.of("off", "on", "halted");

    public NepBlockStates(PackOutput output, ExistingFileHelper existing) {
        super(output, Nep.MOD_ID, existing);
    }

    @Override
    protected void registerStatesAndModels() {
        Cages plain = new Cages(cage("block/matrix/cage", false, false), cage("block/matrix/cage_core", true, false));
        Cages ported = new Cages(
                cage("block/matrix/cage_ported", false, true), cage("block/matrix/cage_ported_core", true, true));
        portStub();
        MATRICES.forEach(matrix -> matrix(matrix, matrix.ported() ? ported : plain));
        machineHub();
        patternDecoder();
        miniaturizationController();
        assemblyController();
        arcaneLectern();
        ritualConductor();
    }

    private void arcaneLectern() {
        ModelFile online = lecternModel(ARCANE_LECTERN + "_online", true);
        ModelFile offline = lecternModel(ARCANE_LECTERN, false);
        getVariantBuilder(block(ARCANE_LECTERN)).forAllStates(state -> ConfiguredModel.builder()
                .modelFile("true".equals(value(state, ONLINE)) ? online : offline)
                .rotationY(readerRotationY(state))
                .build());
        ItemModelBuilder item = lecternTextures(itemModels().getBuilder(ARCANE_LECTERN), true)
                .parent(itemModels().getExistingFile(mcLoc("block/block")))
                .texture("codex", ars("codex"));
        ArsMachineModels.emitLectern(item, true);
        ArsMachineModels.emitRestingCodex(item);
    }

    private ModelFile lecternModel(String name, boolean online) {
        BlockModelBuilder model = lecternTextures(models().getBuilder(name), online)
                .parent(models().getExistingFile(mcLoc("block/block")));
        ArsMachineModels.emitLectern(model, online);
        return model;
    }

    private static <T extends ModelBuilder<T>> T lecternTextures(T model, boolean online) {
        return model.texture("particle", ars("casing"))
                .texture("casing", ars("casing"))
                .texture("frame", ars("frame"))
                .texture("rim", ars("rim"))
                .texture("drive", ars("drive"))
                .texture("lens", ars("lens"))
                .texture("glow", ars(online ? "fluix_on" : "fluix_off"));
    }

    private void ritualConductor() {
        ModelFile conducting = conductorModel(RITUAL_CONDUCTOR + "_conducting", true);
        ModelFile idle = conductorModel(RITUAL_CONDUCTOR, false);
        getVariantBuilder(block(RITUAL_CONDUCTOR)).forAllStates(state -> ConfiguredModel.builder()
                .modelFile("true".equals(value(state, CONDUCTING)) ? conducting : idle)
                .build());
        ArsMachineModels.emitGem(models().getBuilder(RITUAL_CONDUCTOR + "_gem").texture("gem", ars("gem")));
        ItemModelBuilder item = conductorTextures(itemModels().getBuilder(RITUAL_CONDUCTOR), false)
                .parent(itemModels().getExistingFile(mcLoc("block/block")))
                .texture("gem", ars("gem"));
        ArsMachineModels.emitConductor(item, false);
        ArsMachineModels.emitRestingGem(item);
    }

    private ModelFile conductorModel(String name, boolean conducting) {
        BlockModelBuilder model = conductorTextures(models().getBuilder(name), conducting)
                .parent(models().getExistingFile(mcLoc("block/block")));
        ArsMachineModels.emitConductor(model, conducting);
        return model;
    }

    private static <T extends ModelBuilder<T>> T conductorTextures(T model, boolean conducting) {
        return model.texture("particle", ars("obsidian"))
                .texture("sky", ars("sky"))
                .texture("frame", ars("frame"))
                .texture("obsidian", ars("obsidian"))
                .texture("gold", ars("gold"))
                .texture("band", ars(conducting ? "fluix_on" : "fluix_off"))
                .texture("source", ars(conducting ? "source_on" : "source_off"));
    }

    private static int readerRotationY(BlockState state) {
        String facing = value(state, FACING);
        Direction direction = facing == null ? Direction.NORTH : Direction.byName(facing);
        return ((int) direction.toYRot() + 180) % 360;
    }

    private static ResourceLocation ars(String texture) {
        return Nep.id("block/ars/" + texture);
    }

    private void machineHub() {
        BlockModelBuilder model = nepTextures(models().getBuilder(MACHINE_HUB))
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", machine("nep/plate"));
        NepMachineModels.emit(model, NepMachineModels.patchPanel(), Set.of("contact", "core"));
        simpleBlock(block(MACHINE_HUB), model);
        itemModels().withExistingParent(MACHINE_HUB, model.getLocation());
    }

    private void patternDecoder() {
        BlockModelBuilder model = nepTextures(models().getBuilder(PATTERN_DECODER))
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", machine("nep/plate"))
                .texture("dial", machine("nep/dial"))
                .texture("backlight", machine("nep/backlight"));
        DecoderModule[] modules = DecoderModule.values();
        for (DecoderModule module : modules) {
            model.texture(NepMachineModels.moduleLamp(module.ordinal()), machine("nep/module/" + module.itemPath()));
        }
        NepMachineModels.emit(model, NepMachineModels.combinationLock(modules.length), Set.of("contact", "backlight"));
        getVariantBuilder(block(PATTERN_DECODER)).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(model)
                .rotationY(readerRotationY(state))
                .build());
        itemModels().withExistingParent(PATTERN_DECODER, model.getLocation());
    }

    private static <T extends ModelBuilder<T>> T nepTextures(T model) {
        return model.texture("frame", machine("nep/frame"))
                .texture("rim", machine("nep/rim"))
                .texture("plate", machine("nep/plate"))
                .texture("dark", machine("nep/dark"))
                .texture("contact", machine("nep/contact"))
                .texture("core", machine("nep/core"));
    }

    private void miniaturizationController() {
        Block block = block(MINIATURIZATION_CONTROLLER);
        List<NepMachineModels.Box> boxes = NepMachineModels.miniatureField();
        for (String status : MINIATURIZATION_STATES) {
            models().getBuilder(MINIATURIZATION_CONTROLLER + "_" + status)
                    .parent(models().getExistingFile(mcLoc("block/block")))
                    .texture("particle", machine("compact/metal"))
                    .customLoader(CompositeModelBuilder::begin)
                    .child("body", miniaturizationPart(status, boxes, false))
                    .child(FIELD, miniaturizationPart(status, boxes, true))
                    .end();
        }
        getVariantBuilder(block)
                .forAllStatesExcept(
                        state -> ConfiguredModel.builder()
                                .modelFile(models().getExistingFile(
                                                Nep.id("block/" + MINIATURIZATION_CONTROLLER + "_" + statusOf(state))))
                                .build(),
                        besidesStatus(block));
        itemModels()
                .withExistingParent(
                        MINIATURIZATION_CONTROLLER, Nep.id("block/" + MINIATURIZATION_CONTROLLER + "_running"));
    }

    private BlockModelBuilder miniaturizationPart(String status, List<NepMachineModels.Box> boxes, boolean field) {
        BlockModelBuilder part = models().nested()
                .renderType(field ? TRANSLUCENT : SOLID)
                .texture("particle", machine("compact/metal"))
                .texture("metal", machine("compact/metal"))
                .texture("frame", machine("compact/frame"))
                .texture("dark", machine("compact/dark"))
                .texture("glow", machine("compact/glow_" + status))
                .texture(FIELD, machine("compact/field_" + status));
        NepMachineModels.emit(
                part,
                boxes.stream()
                        .filter(box -> FIELD.equals(box.texture()) == field)
                        .toList(),
                Set.of("glow", FIELD));
        return part;
    }

    private void assemblyController() {
        Block block = block(ASSEMBLY_CONTROLLER);
        for (String status : ASSEMBLY_STATES) {
            BlockModelBuilder model = casingTextures(models().getBuilder(ASSEMBLY_CONTROLLER + "_" + status), status);
            List<NepMachineModels.Box> boxes = new ArrayList<>(NepMachineModels.brassCasing());
            boxes.addAll(NepMachineModels.cogs());
            NepMachineModels.emit(model, boxes, Set.of());
        }
        BlockModelBuilder working =
                casingTextures(models().getBuilder(ASSEMBLY_CONTROLLER + "_" + WORKING), ASSEMBLY_WORKING);
        NepMachineModels.emit(working, NepMachineModels.brassCasing(), Set.of());
        BlockModelBuilder cog = models().getBuilder(ASSEMBLY_COG)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", machine("create/cog"))
                .texture("cog", machine("create/cog"))
                .texture("hub", machine("create/hub"));
        NepMachineModels.emit(cog, NepMachineModels.cog(), Set.of());
        getVariantBuilder(block)
                .forAllStatesExcept(
                        state -> ConfiguredModel.builder()
                                .modelFile(models().getExistingFile(Nep.id("block/" + ASSEMBLY_CONTROLLER + "_"
                                        + ("true".equals(value(state, WORKING)) ? WORKING : statusOf(state)))))
                                .build(),
                        property(block, FACING));
        itemModels()
                .withExistingParent(
                        ASSEMBLY_CONTROLLER, Nep.id("block/" + ASSEMBLY_CONTROLLER + "_" + ASSEMBLY_WORKING));
    }

    private BlockModelBuilder casingTextures(BlockModelBuilder model, String status) {
        return model.parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", machine("create/casing"))
                .texture("casing", machine("create/casing"))
                .texture("panel", machine(HALTED.equals(status) ? "create/panel_halted" : "create/panel"))
                .texture("cog", machine("create/cog"))
                .texture("hub", machine("create/hub"));
    }

    private static Property<?> property(Block block, String name) {
        Property<?> found = block.getStateDefinition().getProperty(name);
        if (found == null) {
            throw new IllegalStateException(BuiltInRegistries.BLOCK.getKey(block) + " has no " + name + " property");
        }
        return found;
    }

    private static ResourceLocation machine(String texture) {
        return Nep.id("block/machine/" + texture);
    }

    private static Property<?>[] besidesStatus(Block block) {
        return block.getStateDefinition().getProperties().stream()
                .filter(property -> !STATUS.equals(property.getName()))
                .toArray(Property<?>[]::new);
    }

    private void matrix(Matrix matrix, Cages cages) {
        ModelFile frame = skin(models().getBuilder(matrix.block()), matrix, cages.block());
        getVariantBuilder(block(matrix.block()))
                .forAllStatesExcept(
                        state -> ConfiguredModel.builder()
                                .modelFile(frame)
                                .rotationX(pillarRotationX(state))
                                .rotationY(pillarRotationY(state))
                                .build(),
                        MatrixStatus.PROPERTY);
        skin(itemModels().getBuilder(matrix.block()), matrix, cages.item()).texture("core", matrix.core());
    }

    private ModelFile cage(String name, boolean core, boolean ported) {
        BlockModelBuilder model = models().getBuilder(name)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .renderType(RENDER_TYPE);
        MatrixCage.emitCage(model, "#corner", "#strut", "#ring", ported);
        if (core) {
            MatrixCage.emitStaticCore(model, "#core");
            if (ported) {
                MatrixCage.emitPortStubs(model, "#shaft_side", "#shaft_end");
                model.texture("shaft_side", SHAFT_SIDE).texture("shaft_end", SHAFT_END);
            }
        }
        return model;
    }

    private void portStub() {
        BlockModelBuilder model = models().getBuilder("block/matrix/port_stub")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .renderType(RENDER_TYPE)
                .texture("particle", SHAFT_SIDE)
                .texture("shaft_side", SHAFT_SIDE)
                .texture("shaft_end", SHAFT_END);
        MatrixCage.emitPortStubs(model, "#shaft_side", "#shaft_end");
    }

    private static <T extends ModelBuilder<T>> T skin(T model, Matrix matrix, ModelFile cage) {
        return model.parent(cage)
                .texture("particle", matrix.border())
                .texture("corner", matrix.corner())
                .texture("strut", matrix.strut())
                .texture("ring", matrix.ring());
    }

    private static String statusOf(BlockState state) {
        String status = value(state, STATUS);
        return status == null ? IDLE : status;
    }

    private static int pillarRotationX(BlockState state) {
        String axis = value(state, AXIS);
        return axis == null || "y".equals(axis) ? 0 : 90;
    }

    private static int pillarRotationY(BlockState state) {
        return "x".equals(value(state, AXIS)) ? 90 : 0;
    }

    private static Block block(String path) {
        return BuiltInRegistries.BLOCK
                .getOptional(Nep.id(path))
                .orElseThrow(() -> new IllegalStateException("nep:" + path + " is not registered; datagen needs the "
                        + "full mod classpath, so run it without -Pskip_mods"));
    }

    @Nullable
    private static String value(BlockState state, String property) {
        Property<?> found = state.getBlock().getStateDefinition().getProperty(property);
        return found == null ? null : name(state, found);
    }

    private static <T extends Comparable<T>> String name(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private record Cages(ModelFile block, ModelFile item) {}

    private record Matrix(String block, String set, boolean ported) {

        ResourceLocation border() {
            return Nep.id("block/matrix/" + set + "/border");
        }

        ResourceLocation corner() {
            return Nep.id("block/matrix/" + set + "/corner");
        }

        ResourceLocation strut() {
            return Nep.id("block/matrix/" + set + "/strut");
        }

        ResourceLocation ring() {
            return Nep.id("block/matrix/" + set + "/ring");
        }

        ResourceLocation core() {
            return Nep.id("block/matrix/core/" + set);
        }
    }
}
