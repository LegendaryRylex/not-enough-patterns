package dev.rylex.nep.compat.malum;

import com.sammy.malum.common.block.curiosities.spirit_crucible.SpiritCrucibleCoreBlockEntity;
import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.block.curiosities.totem.TotemPoleBlock;
import com.sammy.malum.core.systems.spirit.type.SpiritLike;
import com.sammy.malum.registry.common.block.MalumBlocks;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import dev.rylex.nep.Nep;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RiteGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_rites";

    private static final BlockPos BASE = new BlockPos(4, 1, 4);
    private static final BlockPos CRUCIBLE = new BlockPos(4, 1, 7);

    private static final List<SpiritLike> PRESERVATION_POLES = List.of(
            MalumSpiritTypes.ELDRITCH_SPIRIT,
            MalumSpiritTypes.ARCANE_SPIRIT,
            MalumSpiritTypes.SACRED_SPIRIT,
            MalumSpiritTypes.SACRED_SPIRIT,
            NepSpirits.PURE);

    private RiteGameTest() {}

    private static TotemBaseBlockEntity lightRunewoodTotem(GameTestHelper helper, List<SpiritLike> spirits) {
        helper.setBlock(BASE, MalumBlocks.RUNEWOOD_TOTEM_BASE.get().defaultBlockState());
        TotemPoleBlock<?> pole = (TotemPoleBlock<?>) MalumBlocks.RUNEWOOD_TOTEM_POLE.get();
        for (int i = 0; i < spirits.size(); i++) {
            helper.setBlock(
                    BASE.above(i + 1), TotemPoleBlock.createTotemPoleState(pole, Direction.NORTH, spirits.get(i)));
        }
        TotemBaseBlockEntity base = helper.getBlockEntity(BASE) instanceof TotemBaseBlockEntity be ? be : null;
        helper.assertTrue(base != null, "the totem base has no block entity");
        base.setState(helper.getLevel(), TotemBaseBlockEntity.TotemBaseState.ASSEMBLING);
        return base;
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aPureCarvedTotemLightsTheRiteOfPreservation(GameTestHelper helper) {
        TotemBaseBlockEntity base = lightRunewoodTotem(helper, PRESERVATION_POLES);
        helper.succeedWhen(() -> {
            helper.assertTrue(
                    base.getState() == TotemBaseBlockEntity.TotemBaseState.ACTIVE,
                    "the totem is " + base.getState() + " rather than active");
            helper.assertTrue(
                    base.getRite() == NepRites.RITE_OF_PRESERVATION.get(),
                    "the totem lit " + base.getRite() + " rather than the Rite of Preservation");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 600)
    public static void theRiteOfPreservationWardsACrucibleInRange(GameTestHelper helper) {
        helper.setBlock(CRUCIBLE, MalumBlocks.SPIRIT_CRUCIBLE.get().defaultBlockState());
        helper.setBlock(
                CRUCIBLE.above(), MalumBlocks.SPIRIT_CRUCIBLE_COMPONENT.get().defaultBlockState());
        SpiritCrucibleCoreBlockEntity crucible =
                helper.getBlockEntity(CRUCIBLE) instanceof SpiritCrucibleCoreBlockEntity be ? be : null;
        helper.assertTrue(crucible != null, "the Spirit Crucible has no core block entity");

        lightRunewoodTotem(helper, PRESERVATION_POLES);
        helper.succeedWhen(
                () -> helper.assertTrue(ImpetusWard.shields(crucible), "the rite never warded the crucible"));
    }
}
