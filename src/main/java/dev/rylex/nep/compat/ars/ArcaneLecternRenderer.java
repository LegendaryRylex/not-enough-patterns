package dev.rylex.nep.compat.ars;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.rylex.nep.Nep;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

final class ArcaneLecternRenderer implements BlockEntityRenderer<ArcaneLecternBlockEntity> {

    private static final Material CODEX = new Material(TextureAtlas.LOCATION_BLOCKS, Nep.id("block/ars/codex"));

    private static final float PIXEL = 1.0F / 16.0F;
    private static final float RESTING_HEIGHT = 13.6F * PIXEL;
    private static final float READING_HEIGHT = 15.0F * PIXEL;
    private static final float BOB_RATE = 0.1F;
    private static final float BOB_REACH = 0.01F;

    /** Tilt of the spine off the vertical: 90 lays the book flat, the vanilla enchanting table uses 80. */
    private static final float FLAT = 90.0F;

    private static final float READING_TILT = 67.5F;
    private static final float CLOSED_ROLL = 90.0F;

    /** Half the depth of a closed {@link BookModel}, measured from its spine. */
    private static final float CLOSED_HALF_DEPTH = 3.0F * PIXEL;

    private final BookModel book;

    ArcaneLecternRenderer(BlockEntityRendererProvider.Context context) {
        this.book = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void render(
            ArcaneLecternBlockEntity lectern,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        float open = Mth.lerp(partialTick, lectern.oOpen, lectern.open);
        float time = lectern.time + partialTick;
        float closed = 1.0F - open;
        Direction facing = lectern.getBlockState().getValue(ArcaneLecternBlock.FACING);

        pose.pushPose();
        float bob = Mth.sin(time * BOB_RATE) * BOB_REACH * open;
        pose.translate(0.5F, Mth.lerp(open, RESTING_HEIGHT, READING_HEIGHT) + bob, 0.5F);
        pose.mulPose(Axis.YP.rotation((float) -Mth.atan2(facing.getStepZ(), facing.getStepX())));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(open, FLAT, READING_TILT)));
        pose.mulPose(Axis.YP.rotationDegrees(CLOSED_ROLL * closed));
        pose.translate(-CLOSED_HALF_DEPTH * closed, 0.0F, 0.0F);

        float flip = Mth.lerp(partialTick, lectern.oFlip, lectern.flip);
        float right = Mth.clamp(Mth.frac(flip + 0.25F) * 1.6F - 0.3F, 0.0F, 1.0F);
        float left = Mth.clamp(Mth.frac(flip + 0.75F) * 1.6F - 0.3F, 0.0F, 1.0F);
        book.setupAnim(time, right, left, open);
        book.render(pose, CODEX.buffer(buffers, RenderType::entitySolid), light, overlay, -1);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ArcaneLecternBlockEntity lectern) {
        return new AABB(lectern.getBlockPos()).expandTowards(0.0, 0.25, 0.0);
    }
}
