package dev.rylex.nep.compat.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

final class AssemblyLinkVisualizer {
    private AssemblyLinkVisualizer() {}

    private static final float[] INPUT = {0.30F, 0.62F, 1.00F};
    private static final float[] OUTPUT = {1.00F, 0.55F, 0.12F};
    private static final float[] MACHINE_OK = {0.32F, 0.90F, 0.38F};
    private static final float[] MACHINE_BAD = {0.95F, 0.26F, 0.26F};
    private static final float[] BEAM = {0.88F, 0.90F, 1.00F};
    private static final float[] CONTROLLER = {1.00F, 0.82F, 0.28F};
    private static final float[] UNLINK = {0.95F, 0.26F, 0.26F};

    private static final float BOX_ALPHA = 0.85F;
    private static final float BEAM_ALPHA = 0.60F;
    private static final float PULSE_TICKS = 40.0F;

    static void init() {
        NeoForge.EVENT_BUS.addListener(AssemblyLinkVisualizer::onRenderLevelStage);
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) {
            return;
        }

        ItemStack linker = findLinker(player);
        SequencedAssemblyControllerBlockEntity controller = selectController(mc, level, linker);
        if (controller == null && linker == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        if (controller != null) {
            renderController(pose, lines, level, controller);
        } else {
            renderLinkerPlan(pose, lines, level, linker);
        }

        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    @Nullable
    private static SequencedAssemblyControllerBlockEntity selectController(
            Minecraft mc, Level level, @Nullable ItemStack linker) {
        if (mc.screen instanceof SequencedAssemblyControllerScreen screen) {
            return controllerAt(level, screen.getMenu().controllerPos());
        }
        if (linker != null) {
            return null;
        }
        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            return controllerAt(level, hit.getBlockPos());
        }
        return null;
    }

    @Nullable
    private static SequencedAssemblyControllerBlockEntity controllerAt(Level level, @Nullable BlockPos pos) {
        return pos != null && level.getBlockEntity(pos) instanceof SequencedAssemblyControllerBlockEntity be
                ? be
                : null;
    }

    private static void renderController(
            PoseStack pose, VertexConsumer lines, Level level, SequencedAssemblyControllerBlockEntity controller) {
        BlockPos hub = controller.getBlockPos();
        Vec3 center = Vec3.atCenterOf(hub);
        box(pose, lines, hub, 0.03F, CONTROLLER, pulse(level));

        BlockPos input = controller.linkedInput();
        if (input != null) {
            box(pose, lines, input, 0.02F, INPUT, BOX_ALPHA);
            beam(pose, lines, center, Vec3.atCenterOf(input), INPUT, BEAM_ALPHA);
        }
        for (BlockPos machine : controller.linkedMachines()) {
            float[] color = Stations.detect(level.getBlockState(machine)).recognized() ? MACHINE_OK : MACHINE_BAD;
            box(pose, lines, machine, 0.02F, color, BOX_ALPHA);
            beam(pose, lines, center, Vec3.atCenterOf(machine), color, BEAM_ALPHA);
        }
        BlockPos output = controller.linkedOutput();
        if (output != null) {
            box(pose, lines, output, 0.02F, OUTPUT, BOX_ALPHA);
            beam(pose, lines, center, Vec3.atCenterOf(output), OUTPUT, BEAM_ALPHA);
        }
    }

    private static void renderLinkerPlan(PoseStack pose, VertexConsumer lines, Level level, ItemStack linker) {
        BlockPos input = linker.get(NepCreateContent.LINKER_INPUT.get());
        BlockPos output = linker.get(NepCreateContent.LINKER_OUTPUT.get());
        List<BlockPos> machines = linker.getOrDefault(NepCreateContent.LINKER_MACHINES.get(), List.of());
        LinkerMode mode = linker.getOrDefault(NepCreateContent.LINKER_MODE.get(), LinkerMode.INPUT);

        List<BlockPos> chain = new ArrayList<>();
        if (input != null) {
            box(pose, lines, input, 0.02F, INPUT, BOX_ALPHA);
            chain.add(input);
        }
        for (BlockPos machine : machines) {
            float[] color = Stations.detect(level.getBlockState(machine)).recognized() ? MACHINE_OK : MACHINE_BAD;
            box(pose, lines, machine, 0.02F, color, BOX_ALPHA);
            chain.add(machine);
        }
        if (output != null) {
            box(pose, lines, output, 0.02F, OUTPUT, BOX_ALPHA);
            chain.add(output);
        }

        for (int i = 0; i + 1 < chain.size(); i++) {
            beam(pose, lines, Vec3.atCenterOf(chain.get(i)), Vec3.atCenterOf(chain.get(i + 1)), BEAM, BEAM_ALPHA);
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos hovered = hit.getBlockPos();
            boolean linked = hovered.equals(input) || hovered.equals(output) || machines.contains(hovered);
            box(pose, lines, hovered, 0.03F, linked ? UNLINK : modeColor(mode), pulse(level));
        }
    }

    @Nullable
    private static ItemStack findLinker(LocalPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() == NepCreateContent.LINKER.get()) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() == NepCreateContent.LINKER.get()) {
            return off;
        }
        return null;
    }

    private static float pulse(Level level) {
        float phase = (level.getGameTime() % (long) PULSE_TICKS) / PULSE_TICKS;
        return 0.55F + 0.35F * (float) Math.sin(phase * 2.0F * Math.PI);
    }

    private static float[] modeColor(LinkerMode mode) {
        return switch (mode) {
            case INPUT -> INPUT;
            case OUTPUT -> OUTPUT;
            case MACHINE -> MACHINE_OK;
        };
    }

    private static void box(
            PoseStack pose, VertexConsumer lines, BlockPos pos, float inflate, float[] rgb, float alpha) {
        LevelRenderer.renderLineBox(
                pose,
                lines,
                pos.getX() - inflate,
                pos.getY() - inflate,
                pos.getZ() - inflate,
                pos.getX() + 1.0 + inflate,
                pos.getY() + 1.0 + inflate,
                pos.getZ() + 1.0 + inflate,
                rgb[0],
                rgb[1],
                rgb[2],
                alpha);
    }

    private static void beam(PoseStack pose, VertexConsumer lines, Vec3 from, Vec3 to, float[] rgb, float alpha) {
        float dx = (float) (to.x - from.x);
        float dy = (float) (to.y - from.y);
        float dz = (float) (to.z - from.z);
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0e-4F) {
            return;
        }
        dx /= length;
        dy /= length;
        dz /= length;
        PoseStack.Pose last = pose.last();
        Matrix4f matrix = last.pose();
        lines.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z)
                .setColor(rgb[0], rgb[1], rgb[2], alpha)
                .setNormal(last, dx, dy, dz);
        lines.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z)
                .setColor(rgb[0], rgb[1], rgb[2], alpha)
                .setNormal(last, dx, dy, dz);
    }
}
