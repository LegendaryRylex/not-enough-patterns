package dev.rylex.nep.client;

import dev.rylex.nep.Nep;
import net.minecraft.resources.Identifier;

public enum MatrixCoreProfile {
    INFUSED_AWAKENING("infused_awakening", 0xA80000, 0xF18C8C);

    private static final String FOLDER = "textures/block/matrix/core/";
    private static final Identifier PLASMA = Nep.id(FOLDER + "plasma.png");

    private final Identifier texture;
    private final Identifier ring;
    private final int bodyTint;
    private final int coreTint;

    MatrixCoreProfile(String set, int bodyTint, int coreTint) {
        this.texture = Nep.id(FOLDER + set + ".png");
        this.ring = Nep.id("textures/block/matrix/" + set + "/ring.png");
        this.bodyTint = bodyTint;
        this.coreTint = coreTint;
    }

    /** The core is drawn straight through {@code RenderType}, not off the block atlas, so this is a file path. */
    public Identifier texture() {
        return texture;
    }

    public Identifier ring() {
        return ring;
    }

    public Identifier plasma() {
        return PLASMA;
    }

    public int bodyTint() {
        return bodyTint;
    }

    public int coreTint() {
        return coreTint;
    }

    public MatrixCoreMesh mesh() {
        return MatrixCoreMesh.SPHERE;
    }
}
