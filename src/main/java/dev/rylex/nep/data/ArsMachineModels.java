package dev.rylex.nep.data;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;

final class ArsMachineModels {
    private ArsMachineModels() {}

    private static final float SIZE = 16.0F;
    private static final int GLOW = 15;

    /** The Codex sheet is {@code BookModel}'s 64 by 32 layout, so a model UV unit spans four pixels across and two down. */
    private static final float CODEX_U = 4.0F;

    private static final float CODEX_V = 2.0F;
    private static final float BOOK_TILT = 22.5F;
    private static final float GEM_MIN = 5.0F;
    private static final float GEM_MAX = 11.0F;

    private static final Set<Direction> NONE = EnumSet.noneOf(Direction.class);

    static <T extends ModelBuilder<T>> void emitLectern(ModelBuilder<T> model, boolean lit) {
        box(model, 1, 0, 1, 15, 9, 15, "#casing", "#casing", "#casing", EnumSet.of(Direction.UP), false);
        box(model, 0, 9, 0, 16, 11, 16, "#frame", "#rim", "#frame", NONE, false);
        box(model, 3, 5, 0.5F, 13, 7, 1, "#drive", "#drive", "#drive", EnumSet.of(Direction.SOUTH), false);
        box(model, 3, 2, 0.5F, 13, 4, 1, "#drive", "#drive", "#drive", EnumSet.of(Direction.SOUTH), false);
        box(model, 11, 5.5F, 0.25F, 12.5F, 6.5F, 0.5F, "#glow", "#glow", "#glow", EnumSet.of(Direction.SOUTH), lit);
        box(model, 11, 2.5F, 0.25F, 12.5F, 3.5F, 0.5F, "#glow", "#glow", "#glow", EnumSet.of(Direction.SOUTH), lit);
        box(model, 5, 11, 5, 11, 12, 11, "#lens", "#lens", "#lens", EnumSet.of(Direction.DOWN), false);
        box(model, 6.5F, 12, 6.5F, 9.5F, 12.5F, 9.5F, "#glow", "#glow", "#glow", EnumSet.of(Direction.DOWN), lit);
    }

    static <T extends ModelBuilder<T>> void emitRestingCodex(ModelBuilder<T> model) {
        book(model, 4, 13, 3, 12, 13.5F, 8, BOOK_TILT, true);
        book(model, 4, 13, 8, 12, 13.5F, 13, -BOOK_TILT, true);
        book(model, 4.5F, 13.5F, 3.5F, 11.5F, 14.2F, 8, BOOK_TILT, false);
        book(model, 4.5F, 13.5F, 8, 11.5F, 14.2F, 12.5F, -BOOK_TILT, false);
    }

    static <T extends ModelBuilder<T>> void emitConductor(ModelBuilder<T> model, boolean lit) {
        box(model, 2, 0, 2, 14, 2, 14, "#sky", "#sky", "#sky", NONE, false);
        box(model, 5, 2, 5, 11, 4, 11, "#frame", "#frame", "#frame", EnumSet.of(Direction.DOWN), false);
        box(model, 5.5F, 4, 5.5F, 10.5F, 5, 10.5F, "#band", "#band", "#band", EnumSet.of(Direction.DOWN), lit);
        box(
                model,
                6,
                5,
                6,
                10,
                13,
                10,
                "#obsidian",
                "#obsidian",
                "#obsidian",
                EnumSet.of(Direction.DOWN, Direction.UP),
                false);
        box(model, 3, 8, 3, 13, 10, 13, "#obsidian", "#obsidian", "#obsidian", NONE, false);
        box(model, 2.5F, 10, 2.5F, 13.5F, 10.5F, 13.5F, "#gold", "#gold", "#gold", NONE, false);
        box(model, 7, 2, 1, 9, 10, 3, "#gold", "#gold", "#gold", NONE, false);
        box(model, 7, 2, 13, 9, 10, 15, "#gold", "#gold", "#gold", NONE, false);
        box(model, 1, 2, 7, 3, 10, 9, "#gold", "#gold", "#gold", NONE, false);
        box(model, 13, 2, 7, 15, 10, 9, "#gold", "#gold", "#gold", NONE, false);
        box(model, 3, 13, 3, 13, 15, 13, "#obsidian", "#obsidian", "#obsidian", NONE, false);
        box(model, 2, 15, 2, 14, 16, 4, "#gold", "#gold", "#gold", NONE, false);
        box(model, 2, 15, 12, 14, 16, 14, "#gold", "#gold", "#gold", NONE, false);
        box(model, 2, 15, 4, 4, 16, 12, "#gold", "#gold", "#gold", NONE, false);
        box(model, 12, 15, 4, 14, 16, 12, "#gold", "#gold", "#gold", NONE, false);
        box(model, 4, 15, 4, 12, 15.5F, 12, "#source", "#source", "#source", EnumSet.of(Direction.DOWN), lit);
    }

    static <T extends ModelBuilder<T>> void emitGem(ModelBuilder<T> model) {
        gem(model).end();
    }

    static <T extends ModelBuilder<T>> void emitRestingGem(ModelBuilder<T> model) {
        ModelBuilder<T>.ElementBuilder element = gem(model);
        element.from(6.5F, 14.5F, 6.5F).to(9.5F, 17.5F, 9.5F);
        element.rotation().origin(8, 16, 8).axis(Direction.Axis.Y).angle(45.0F).end();
        element.end();
    }

    private static <T extends ModelBuilder<T>> ModelBuilder<T>.ElementBuilder gem(ModelBuilder<T> model) {
        ModelBuilder<T>.ElementBuilder element =
                model.element().from(6.5F, 6.5F, 6.5F).to(9.5F, 9.5F, 9.5F).shade(false);
        for (Direction direction : Direction.values()) {
            element.face(direction)
                    .texture("#gem")
                    .uvs(GEM_MIN, GEM_MIN, GEM_MAX, GEM_MAX)
                    .end();
        }
        return element;
    }

    private static <T extends ModelBuilder<T>> void book(
            ModelBuilder<T> model,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float tilt,
            boolean cover) {
        ModelBuilder<T>.ElementBuilder element =
                model.element().from(x1, y1, z1).to(x2, y2, z2);
        for (Direction direction : Direction.values()) {
            float[] uv =
                    cover ? codex(0, 0, 6, 10) : direction == Direction.UP ? codex(1, 11, 6, 19) : codex(1, 10, 6, 11);
            element.face(direction)
                    .texture("#codex")
                    .uvs(uv[0], uv[1], uv[2], uv[3])
                    .end();
        }
        element.rotation().origin(8, 13, 8).axis(Direction.Axis.X).angle(tilt).end();
        element.end();
    }

    private static float[] codex(int left, int top, int right, int bottom) {
        return new float[] {left / CODEX_U, top / CODEX_V, right / CODEX_U, bottom / CODEX_V};
    }

    private static <T extends ModelBuilder<T>> void box(
            ModelBuilder<T> model,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            String side,
            String top,
            String bottom,
            Set<Direction> hidden,
            boolean glow) {
        ModelBuilder<T>.ElementBuilder element =
                model.element().from(x1, y1, z1).to(x2, y2, z2);
        float[] from = {x1, y1, z1};
        float[] to = {x2, y2, z2};
        for (Direction direction : Direction.values()) {
            if (hidden.contains(direction)) {
                continue;
            }
            String texture =
                    switch (direction) {
                        case UP -> top;
                        case DOWN -> bottom;
                        default -> side;
                    };
            ModelBuilder<T>.ElementBuilder.FaceBuilder face =
                    element.face(direction).texture(texture);
            if (glow) {
                face.emissivity(GLOW, GLOW);
            }
            if (onSurface(direction, from, to)) {
                face.cullface(direction);
            }
            face.end();
        }
        element.end();
    }

    private static boolean onSurface(Direction direction, float[] from, float[] to) {
        int axis = direction.getAxis().ordinal();
        return direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? to[axis] == SIZE : from[axis] == 0.0F;
    }
}
