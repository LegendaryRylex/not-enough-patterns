package dev.rylex.nep.client;

import appeng.api.stacks.AEKey;
import java.util.function.Consumer;
import net.minecraft.client.renderer.Rect2i;

public record GhostTarget(Rect2i area, Consumer<AEKey> accept) {}
