package dev.rylex.nep.compat.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

final class SequencedAssemblyControllerRenderer implements BlockEntityRenderer<SequencedAssemblyControllerBlockEntity> {

    private static final float DEGREES_PER_TICK = 4.0F;
    private static final long TURN_TICKS = 90L;

    private final PartialModel cog;

    SequencedAssemblyControllerRenderer(PartialModel cog) {
        this.cog = cog;
    }

    @Override
    public void render(
            SequencedAssemblyControllerBlockEntity controller,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        Level level = controller.getLevel();
        BlockState state = controller.getBlockState();
        if (level == null
                || !state.getOptionalValue(SequencedAssemblyControllerBlock.WORKING)
                        .orElse(false)) {
            return;
        }
        float angle = (level.getGameTime() % TURN_TICKS + partialTick) * DEGREES_PER_TICK;
        VertexConsumer consumer = buffers.getBuffer(RenderType.solid());
        for (Direction side : Direction.Plane.HORIZONTAL) {
            CachedBuffers.partial(cog, state)
                    .center()
                    .rotateYDegrees(-side.toYRot())
                    .rotateZDegrees(side.getAxis() == Direction.Axis.Z ? -angle : angle)
                    .uncenter()
                    .light(LevelRenderer.getLightColor(
                            level, controller.getBlockPos().relative(side)))
                    .renderInto(pose, consumer);
        }
    }
}
