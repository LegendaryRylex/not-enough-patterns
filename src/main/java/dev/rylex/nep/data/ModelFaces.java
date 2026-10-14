package dev.rylex.nep.data;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;

final class ModelFaces {
    static final int GLOW = 15;

    private static final float SIZE = 16.0F;

    private ModelFaces() {}

    static <T extends ModelBuilder<T>> void face(
            ModelBuilder<T>.ElementBuilder element, Direction direction, String texture, boolean glow, boolean cull) {
        ModelBuilder<T>.ElementBuilder.FaceBuilder face =
                element.face(direction).texture(texture);
        if (glow) {
            face.emissivity(GLOW, GLOW);
        }
        if (cull) {
            face.cullface(direction);
        }
        face.end();
    }

    static boolean onSurface(Direction direction, float[] from, float[] to) {
        int axis = direction.getAxis().ordinal();
        return direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? to[axis] == SIZE : from[axis] == 0.0F;
    }
}
