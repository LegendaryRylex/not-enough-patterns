package dev.rylex.nep.machine;

import appeng.crafting.execution.CraftingCpuLogic;
import dev.rylex.nep.NepGameTests;
import java.lang.reflect.Method;
import net.minecraft.gametest.framework.GameTestHelper;

public final class PushingCpuMixinGameTest {

    private PushingCpuMixinGameTest() {}

    public static void register(NepGameTests.Batch batch) {
        batch.add(
                        "the_cpu_logic_reports_which_cpu_is_pushing",
                        PushingCpuMixinGameTest::theCpuLogicReportsWhichCpuIsPushing)
                .add("no_cpu_is_pushing_outside_a_craft", PushingCpuMixinGameTest::noCpuIsPushingOutsideACraft);
    }

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

    public static void noCpuIsPushingOutsideACraft(GameTestHelper helper) {
        helper.assertTrue(
                PushingCpuContext.current() == null,
                "a pushing cpu leaked outside a pattern push; pending crafts would be blamed on the wrong job");
        helper.succeed();
    }
}
