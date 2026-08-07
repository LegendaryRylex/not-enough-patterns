package dev.rylex.nep.machine;

import appeng.crafting.execution.CraftingCpuLogic;
import dev.rylex.nep.Nep;
import java.lang.reflect.Method;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PushingCpuMixinGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "nep_pushing_cpu";

    private PushingCpuMixinGameTest() {}

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void theCpuLogicReportsWhichCpuIsPushing(GameTestHelper helper) {
        boolean patched = false;
        for (Method method : CraftingCpuLogic.class.getDeclaredMethods()) {
            if (method.getName().contains("nep$markPushingCpu")) {
                patched = true;
                break;
            }
        }
        helper.assertTrue(
                patched,
                "the crafting cpu logic mixin did not apply; clearing pending recipes would leave the network job stuck");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void noCpuIsPushingOutsideACraft(GameTestHelper helper) {
        helper.assertTrue(
                PushingCpuContext.current() == null,
                "a pushing cpu leaked outside a pattern push; pending crafts would be blamed on the wrong job");
        helper.succeed();
    }
}
