package dev.rylex.nep.compat.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.rylex.nep.client.MatrixCoreProfile;
import dev.rylex.nep.client.MatrixCoreRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

final class SequencedAssemblyMatrixRenderer extends MatrixCoreRenderer<SequencedAssemblyMatrixBlockEntity> {

    private final PartialModel stubs;

    SequencedAssemblyMatrixRenderer(PartialModel stubs) {
        super(MatrixCoreProfile.SEQUENCED_ASSEMBLER);
        this.stubs = stubs;
    }

    @Override
    public void render(
            SequencedAssemblyMatrixBlockEntity matrix,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        super.render(matrix, partialTick, pose, buffers, packedLight, packedOverlay);
        if (matrix.getLevel() == null) {
            return;
        }
        SuperByteBuffer shafts = KineticBlockEntityRenderer.standardKineticRotationTransform(
                CachedBuffers.partial(stubs, matrix.getBlockState()), matrix, packedLight);
        switch (KineticBlockEntityRenderer.getRotationAxisOf(matrix)) {
            case X -> shafts.rotateZCenteredDegrees(90.0F);
            case Z -> shafts.rotateXCenteredDegrees(90.0F);
            case Y -> {}
        }
        shafts.renderInto(pose, buffers.getBuffer(RenderType.cutoutMipped()));
    }
}
