package dev.rylex.nep.client;

import dev.rylex.nep.Nep;
import net.minecraft.resources.ResourceLocation;

public enum MatrixCoreProfile {
    ARCANE_ENCHANTING("arcane_enchanting", 0xC822A8, 0xFFB3EE),
    EMPOWERING("empowering", 0x7B02EF, 0xDEA6FF),
    FOCUSED_SPIRIT("focused_spirit", 0x005CFA, 0xA2B6FF),
    FUSION("fusion", 0xE95508, 0xFFCEA8),
    INFUSED_AWAKENING("infused_awakening", 0xA80000, 0xF18C8C),
    MINIATURIZATION("miniaturization", 0x08BAE9, 0xA8E4FF),
    SEQUENCED_ASSEMBLER("sequenced_assembler", 0xC07A31, 0xF6E7B1);

    private static final String FOLDER = "textures/block/matrix/core/";
    private static final ResourceLocation PLASMA = Nep.id(FOLDER + "plasma.png");

    private final ResourceLocation texture;
    private final ResourceLocation ring;
    private final int bodyTint;
    private final int coreTint;

    MatrixCoreProfile(String set, int bodyTint, int coreTint) {
        this.texture = Nep.id(FOLDER + set + ".png");
        this.ring = Nep.id("textures/block/matrix/" + set + "/ring.png");
        this.bodyTint = bodyTint;
        this.coreTint = coreTint;
    }

    /** The core is drawn straight through {@code RenderType}, not off the block atlas, so this is a file path. */
    public ResourceLocation texture() {
        return texture;
    }

    public ResourceLocation ring() {
        return ring;
    }

    public ResourceLocation plasma() {
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
