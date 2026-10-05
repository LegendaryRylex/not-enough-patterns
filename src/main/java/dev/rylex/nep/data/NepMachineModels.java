package dev.rylex.nep.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;

final class NepMachineModels {
    private NepMachineModels() {}

    private static final float SIZE = 16.0F;
    private static final float CENTRE = 8.0F;
    private static final int GLOW = 15;
    private static final float DIAGONAL = 45.0F;
    private static final float CYL_FLAT = 0.41F;
    private static final float CYL_CORNER = 0.72F;

    /** Insets each overlapping box of a cylinder so their end faces never share a plane. */
    private static final float STAGGER = 0.05F;

    private static final float[] JACK_COLUMNS = {3.0F, 6.75F, 10.5F};
    private static final float[] JACK_ROWS = {3.5F, 7.0F, 10.5F};
    private static final float[] WHEELS = {2.375F, 5.375F, 8.375F, 11.375F};
    private static final float WHEEL_WIDTH = 2.25F;
    private static final float WHEEL_RADIUS = 3.75F;
    private static final float LAMP_FLOOR = 3.0F;
    private static final float LAMP_SPAN = 10.0F;
    private static final float LAMP_GAP = 1.0F;
    private static final float CORE_LOW = 7.25F;
    private static final float CORE_FLOOR = 8.5F;
    private static final float CORE_SIZE = 2.5F;
    private static final float COG_FACE = 15.0F;
    private static final float COG_DISC = 3.5F;
    private static final float COG_TIP = 5.0F;
    private static final float COG_TOOTH = 0.85F;
    private static final float COG_HUB = 1.25F;
    private static final float COG_DEPTH = 0.75F;

    record Box(float x1, float y1, float z1, float x2, float y2, float z2, String texture, Direction.Axis diagonal) {

        Box turned() {
            Direction.Axis axis = diagonal == Direction.Axis.Z
                    ? Direction.Axis.X
                    : diagonal == Direction.Axis.X ? Direction.Axis.Z : diagonal;
            return new Box(SIZE - z2, y1, x1, SIZE - z1, y2, x2, texture, axis);
        }
    }

    static List<Box> patchPanel() {
        List<Box> boxes = new ArrayList<>();
        boxes.add(box(0, 0, 0, 16, 2, 16, "frame"));
        boxes.add(box(1, 2, 1, 15, 14, 15, "plate"));
        boxes.addAll(sides(List.of(box(0, 2, 14, 2, 14, 16, "rim"))));
        List<Box> jacks = new ArrayList<>();
        for (float x : JACK_COLUMNS) {
            for (float y : JACK_ROWS) {
                jacks.add(box(x, y, 15, x + 2.5F, y + 2.5F, 15.4F, "frame"));
                jacks.add(box(x + 0.75F, y + 0.75F, 15.4F, x + 1.75F, y + 1.75F, 15.6F, "contact"));
            }
        }
        boxes.addAll(sides(jacks));
        boxes.addAll(capRing(14, 16, 3, "frame"));
        boxes.add(box(3, 14, 3, 13, 15, 13, "rim"));
        boxes.add(box(5, 15, 5, 11, 15.4F, 11, "dark"));
        boxes.add(box(6.5F, 15.4F, 6.5F, 9.5F, 15.8F, 9.5F, "core"));
        return boxes;
    }

    static List<Box> combinationLock(int modules) {
        List<Box> boxes = new ArrayList<>();
        boxes.add(box(0, 0, 0, 16, 2, 12, "frame"));
        boxes.add(box(0, 14, 0, 16, 16, 12, "frame"));
        boxes.add(box(0, 2, 0, 16, 14, 1.5F, "frame"));
        boxes.add(box(1, 2, 1.5F, 15, 14, 12, "plate"));
        int perSide = Math.ceilDiv(modules, 2);
        float height = (LAMP_SPAN - LAMP_GAP * (perSide - 1)) / perSide;
        for (int module = 0; module < modules; module++) {
            float y = LAMP_FLOOR + (perSide - 1 - module % perSide) * (height + LAMP_GAP);
            float x = module < perSide ? 0.5F : 15;
            boxes.add(box(x, y, 3, x + 0.5F, y + height, 10, moduleLamp(module)));
        }
        boxes.add(box(0, 12, 12, 16, 16, 16, "frame"));
        boxes.add(box(0, 0, 12, 16, 4, 16, "frame"));
        boxes.add(box(0, 4, 12, 2, 12, 15.5F, "frame"));
        boxes.add(box(14, 4, 12, 16, 12, 15.5F, "frame"));
        boxes.add(box(0.5F, 7, 15.5F, 1.5F, 9, 16, "contact"));
        boxes.add(box(14.5F, 7, 15.5F, 15.5F, 9, 16, "contact"));
        boxes.add(box(2, 4, 12, 14, 12, 12.5F, "backlight"));
        boxes.addAll(cylinderX(2, 14, CENTRE, 12, 1.25F, "rim"));
        for (float x : WHEELS) {
            boxes.addAll(cylinderX(x, x + WHEEL_WIDTH, CENTRE, 12, WHEEL_RADIUS, "dial"));
        }
        return turn(turn(boxes));
    }

    static String moduleLamp(int module) {
        return "module" + module;
    }

    static List<Box> miniatureField() {
        List<Box> boxes = new ArrayList<>();
        boxes.add(box(0, 0, 0, 16, 3, 16, "metal"));
        boxes.addAll(capRing(3, 4, 1.5F, "frame"));
        boxes.add(box(1.5F, 3, 1.5F, 14.5F, 3.5F, 14.5F, "dark"));
        boxes.addAll(
                sides(List.of(box(7, 4, 14, 9, 6, 15.5F, "frame"), box(7.5F, 6, 14.25F, 8.5F, 6.5F, 15.25F, "glow"))));
        boxes.add(box(
                CORE_LOW,
                CORE_FLOOR,
                CORE_LOW,
                CORE_LOW + CORE_SIZE,
                CORE_FLOOR + CORE_SIZE,
                CORE_LOW + CORE_SIZE,
                "glow"));
        boxes.add(box(3.25F, 4.5F, 3.25F, 13.75F, 15, 13.75F, "field"));
        return boxes;
    }

    static List<Box> brassCasing() {
        List<Box> boxes = new ArrayList<>();
        boxes.add(box(0, 0, 0, 16, 2, 16, "casing"));
        boxes.add(box(0, 14, 0, 16, 16, 16, "casing"));
        boxes.add(box(0, 2, 0, 2, 14, 2, "casing"));
        boxes.add(box(14, 2, 0, 16, 14, 2, "casing"));
        boxes.add(box(0, 2, 14, 2, 14, 16, "casing"));
        boxes.add(box(14, 2, 14, 16, 14, 16, "casing"));
        boxes.add(box(1, 2, 1, 15, 14, 15, "panel"));
        return boxes;
    }

    /** One cogwheel on the south face, centred on the block's Z axis so a renderer can spin it in place. */
    static List<Box> cog() {
        float front = COG_FACE + COG_DEPTH;
        List<Box> boxes = new ArrayList<>();
        boxes.add(box(
                CENTRE - COG_DISC, CENTRE - COG_DISC, COG_FACE, CENTRE + COG_DISC, CENTRE + COG_DISC, front, "cog"));
        boxes.add(diagonal(box(
                CENTRE - COG_DISC,
                CENTRE - COG_DISC,
                COG_FACE,
                CENTRE + COG_DISC,
                CENTRE + COG_DISC,
                front - STAGGER,
                "cog")));
        List<Box> teeth = List.of(
                box(
                        CENTRE - COG_TOOTH,
                        CENTRE - COG_TIP,
                        COG_FACE,
                        CENTRE + COG_TOOTH,
                        CENTRE + COG_TIP,
                        front - 2 * STAGGER,
                        "cog"),
                box(
                        CENTRE - COG_TIP,
                        CENTRE - COG_TOOTH,
                        COG_FACE,
                        CENTRE + COG_TIP,
                        CENTRE + COG_TOOTH,
                        front - 3 * STAGGER,
                        "cog"));
        boxes.addAll(teeth);
        teeth.stream()
                .map(NepMachineModels::diagonal)
                .map(NepMachineModels::recessed)
                .forEach(boxes::add);
        boxes.add(box(CENTRE - COG_HUB, CENTRE - COG_HUB, front, CENTRE + COG_HUB, CENTRE + COG_HUB, SIZE, "hub"));
        return boxes;
    }

    static List<Box> cogs() {
        return sides(cog());
    }

    static <T extends ModelBuilder<T>> void emit(ModelBuilder<T> model, List<Box> boxes, Set<String> glowing) {
        for (Box box : boxes) {
            float[] from = {box.x1(), box.y1(), box.z1()};
            float[] to = {box.x2(), box.y2(), box.z2()};
            ModelBuilder<T>.ElementBuilder element =
                    model.element().from(from[0], from[1], from[2]).to(to[0], to[1], to[2]);
            if (box.diagonal() != null) {
                element.rotation()
                        .angle(DIAGONAL)
                        .axis(box.diagonal())
                        .origin(CENTRE, CENTRE, CENTRE)
                        .end();
            }
            for (Direction direction : Direction.values()) {
                ModelBuilder<T>.ElementBuilder.FaceBuilder face =
                        element.face(direction).texture("#" + box.texture());
                if (glowing.contains(box.texture())) {
                    face.emissivity(GLOW, GLOW);
                }
                if (box.diagonal() == null && onSurface(direction, from, to)) {
                    face.cullface(direction);
                }
                face.end();
            }
            element.end();
        }
    }

    private static Box box(float x1, float y1, float z1, float x2, float y2, float z2, String texture) {
        return new Box(x1, y1, z1, x2, y2, z2, texture, null);
    }

    private static Box diagonal(Box box) {
        return new Box(box.x1(), box.y1(), box.z1(), box.x2(), box.y2(), box.z2(), box.texture(), Direction.Axis.Z);
    }

    private static Box recessed(Box box) {
        return new Box(
                box.x1(),
                box.y1(),
                box.z1(),
                box.x2(),
                box.y2(),
                box.z2() - 2 * STAGGER,
                box.texture(),
                box.diagonal());
    }

    private static List<Box> turn(List<Box> boxes) {
        return boxes.stream().map(Box::turned).toList();
    }

    private static List<Box> sides(List<Box> boxes) {
        List<Box> out = new ArrayList<>();
        List<Box> current = boxes;
        for (int side = 0; side < 4; side++) {
            out.addAll(current);
            current = turn(current);
        }
        return out;
    }

    private static List<Box> capRing(float y1, float y2, float width, String texture) {
        return List.of(
                box(0, y1, 0, SIZE, y2, width, texture),
                box(0, y1, SIZE - width, SIZE, y2, SIZE, texture),
                box(0, y1, width, width, y2, SIZE - width, texture),
                box(SIZE - width, y1, width, SIZE, y2, SIZE - width, texture));
    }

    private static List<Box> cylinderX(float x1, float x2, float cy, float cz, float radius, String texture) {
        float flat = radius * CYL_FLAT;
        float corner = radius * CYL_CORNER;
        return List.of(
                box(x1, cy - radius, cz - flat, x2, cy + radius, cz + flat, texture),
                box(x1 + STAGGER, cy - flat, cz - radius, x2 - STAGGER, cy + flat, cz + radius, texture),
                box(x1 + 2 * STAGGER, cy - corner, cz - corner, x2 - 2 * STAGGER, cy + corner, cz + corner, texture));
    }

    private static boolean onSurface(Direction direction, float[] from, float[] to) {
        int axis = direction.getAxis().ordinal();
        return direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? to[axis] == SIZE : from[axis] == 0.0F;
    }
}
