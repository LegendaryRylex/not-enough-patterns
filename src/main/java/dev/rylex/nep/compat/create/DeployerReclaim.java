package dev.rylex.nep.compat.create;

import appeng.api.stacks.GenericStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

record DeployerReclaim(
        @Nullable GenericStack tool, @Nullable GenericStack expected, BlockPos provider, Direction face) {

    static final DeployerReclaim NONE = new DeployerReclaim(null, null, BlockPos.ZERO, Direction.UP);

    static final Codec<DeployerReclaim> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                    GenericStack.CODEC.optionalFieldOf("tool").forGetter(r -> Optional.ofNullable(r.tool())),
                    GenericStack.CODEC.optionalFieldOf("expected").forGetter(r -> Optional.ofNullable(r.expected())),
                    BlockPos.CODEC.fieldOf("provider").forGetter(DeployerReclaim::provider),
                    Direction.CODEC.fieldOf("face").forGetter(DeployerReclaim::face))
            .apply(
                    builder,
                    (tool, expected, provider, face) ->
                            new DeployerReclaim(tool.orElse(null), expected.orElse(null), provider, face)));

    boolean isEmpty() {
        return tool == null || tool.amount() <= 0;
    }

    /** True when what comes back is not the key that was lent. */
    boolean wears() {
        return expected != null;
    }
}
