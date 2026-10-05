package dev.rylex.nep.compat.ars;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.rylex.nep.Nep;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

final class RitualConductorRenderer implements BlockEntityRenderer<RitualConductorBlockEntity> {

    static final ModelResourceLocation GEM = ModelResourceLocation.standalone(Nep.id("block/ritual_conductor_gem"));

    private static final float PIXEL = 1.0F / 16.0F;
    private static final float RESTING_HEIGHT = 16.0F * PIXEL;
    private static final float CONDUCTING_HEIGHT = 19.0F * PIXEL;
    private static final float BOB_RATE = 0.09F;
    private static final float BOB_REACH = 0.5F * PIXEL;
    private static final float SPIN_RATE = 2.0F;
    private static final float RESTING_TURN = 45.0F;

    /** Stands the cube on a vertex: 45 degrees about one axis, then the angle between a cube's face and body diagonals about another. */
    private static final float CORNER_ROLL = 45.0F;

    private static final float CORNER_PITCH = 35.264F;

    private static final float DIM = 0.45F;

    RitualConductorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            RitualConductorBlockEntity conductor,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        Level level = conductor.getLevel();
        if (level == null) {
            return;
        }
        boolean conducting = conductor.getBlockState().getValue(RitualConductorBlock.CONDUCTING);
        BakedModel gem = Minecraft.getInstance().getModelManager().getModel(GEM);

        pose.pushPose();
        if (conducting) {
            float time = level.getGameTime() + partialTick;
            pose.translate(0.5F, CONDUCTING_HEIGHT + Mth.sin(time * BOB_RATE) * BOB_REACH, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(time * SPIN_RATE));
            pose.mulPose(Axis.XP.rotationDegrees(CORNER_PITCH));
            pose.mulPose(Axis.ZP.rotationDegrees(CORNER_ROLL));
        } else {
            pose.translate(0.5F, RESTING_HEIGHT, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(RESTING_TURN));
        }
        pose.translate(-0.5F, -0.5F, -0.5F);

        float tone = conducting ? 1.0F : DIM;
        Minecraft.getInstance()
                .getBlockRenderer()
                .getModelRenderer()
                .renderModel(
                        pose.last(),
                        buffers.getBuffer(RenderType.solid()),
                        null,
                        gem,
                        tone,
                        tone,
                        tone,
                        conducting ? LightTexture.FULL_BRIGHT : light,
                        overlay,
                        ModelData.EMPTY,
                        RenderType.solid());
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(RitualConductorBlockEntity conductor) {
        return new AABB(conductor.getBlockPos()).expandTowards(0.0, 0.5, 0.0);
    }
}
