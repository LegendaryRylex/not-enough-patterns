package dev.rylex.nep.compat.create;

import appeng.api.stacks.GenericStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

record DeployerReclaim(@Nullable GenericStack tool, BlockPos provider, Direction face) {

    static final DeployerReclaim NONE = new DeployerReclaim(null, BlockPos.ZERO, Direction.UP);

    static final Codec<DeployerReclaim> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                    GenericStack.CODEC.optionalFieldOf("tool").forGetter(r -> java.util.Optional.ofNullable(r.tool())),
                    BlockPos.CODEC.fieldOf("provider").forGetter(DeployerReclaim::provider),
                    Direction.CODEC.fieldOf("face").forGetter(DeployerReclaim::face))
            .apply(builder, (tool, provider, face) -> new DeployerReclaim(tool.orElse(null), provider, face)));

    boolean isEmpty() {
        return tool == null || tool.amount() <= 0;
    }
}
