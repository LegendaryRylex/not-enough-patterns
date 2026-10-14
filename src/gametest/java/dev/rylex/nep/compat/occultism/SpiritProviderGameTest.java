package dev.rylex.nep.compat.occultism;

import static dev.rylex.nep.GameTestFixtures.inputsOf;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.klikli_dev.occultism.common.entity.job.SpiritJob;
import com.klikli_dev.occultism.common.entity.spirit.SpiritEntity;
import com.klikli_dev.occultism.registry.OccultismEntities;
import com.klikli_dev.occultism.registry.OccultismSpiritJobs;
import dev.rylex.nep.GameTestFixtures;
import dev.rylex.nep.Nep;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nep.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpiritProviderGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "nep_spirit_provider";

    private static final BlockPos PROVIDER = new BlockPos(4, 1, 4);
    private static final BlockPos CHEST = PROVIDER.west();
    private static final BlockPos SPIRIT = new BlockPos(6, 1, 6);

    private static final TagKey<Item> IRON_DUSTS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "dusts/iron"));

    private SpiritProviderGameTest() {}

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
            }
        }
    }

    private static SpiritEntity summonCrusher(GameTestHelper helper) {
        SpiritEntity spirit = OccultismEntities.FOLIOT.get().create(helper.getLevel());
        helper.assertTrue(spirit != null, "no foliot could be created");
        Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(SPIRIT));
        spirit.moveTo(at.x, at.y, at.z, 0, 0);
        helper.getLevel().addFreshEntity(spirit);
        spirit.setWorkAreaPosition(helper.absolutePos(PROVIDER));
        SpiritJob job = OccultismSpiritJobs.CRUSH_TIER1.get().create(spirit);
        job.init();
        spirit.setJob(job);
        return spirit;
    }

    private static ICraftingMachine machine(GameTestHelper helper) {
        return GameTestFixtures.craftingMachine(
                helper, PROVIDER, "the Spirit Provider exposed no crafting machine capability");
    }

    private static IPatternDetails crushRawIron(GameTestHelper helper) {
        ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(Items.RAW_IRON), 1)),
                List.of(new GenericStack(AEItemKey.of(dust(helper)), 2)));
        IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        helper.assertTrue(details != null, "the crushing pattern did not decode");
        return details;
    }

    private static Item dust(GameTestHelper helper) {
        return helper.getLevel()
                .registryAccess()
                .registryOrThrow(Registries.ITEM)
                .getTag(IRON_DUSTS)
                .flatMap(tag -> tag.stream().findFirst())
                .orElseThrow()
                .value();
    }

    private static int dustIn(ChestBlockEntity chest) {
        int count = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.is(IRON_DUSTS)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static String describeStall(GameTestHelper helper, SpiritEntity spirit, AABB area) {
        return "the crushed dust has not reached the pattern provider yet; spirit at "
                + spirit.blockPosition() + " alive=" + spirit.isAlive() + " hand="
                + spirit.getMainHandItem() + " job="
                + spirit.getJob().map(Object::toString).orElse("none")
                + " loot=" + spirit.canPickUpLoot() + " items="
                + helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).stream()
                        .map(item -> item.getItem() + "@" + item.blockPosition() + " tags=" + item.getTags())
                        .toList()
                + " provider=" + helper.absolutePos(PROVIDER);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 1200)
    public static void crusherSpiritCollectsTheInputAndReturnsTheDust(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(PROVIDER, NepOccultismContent.SPIRIT_PROVIDER.get().defaultBlockState());
        helper.setBlock(CHEST, Blocks.CHEST.defaultBlockState());
        SpiritEntity spirit = summonCrusher(helper);

        helper.runAfterDelay(20, () -> {
            IPatternDetails details = crushRawIron(helper);
            helper.assertTrue(
                    machine(helper).pushPattern(details, inputsOf(details), Direction.WEST),
                    "the Spirit Provider refused a crushing craft a crusher spirit can do");

            helper.succeedWhen(() -> {
                ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(CHEST);
                AABB area = new AABB(helper.absolutePos(PROVIDER)).inflate(8);
                if (dustIn(chest) < 2) {
                    helper.fail(describeStall(helper, spirit, area));
                }
                helper.assertTrue(
                        helper.getLevel()
                                .getEntitiesOfClass(ItemEntity.class, area, item -> item.getItem()
                                        .is(IRON_DUSTS))
                                .isEmpty(),
                        "the spirit dropped dust on the ground instead of handing it to the Provider");
            });
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void refusesACraftNoNearbySpiritCanDo(GameTestHelper helper) {
        helper.setBlock(PROVIDER, NepOccultismContent.SPIRIT_PROVIDER.get().defaultBlockState());
        IPatternDetails details = crushRawIron(helper);
        helper.assertFalse(
                machine(helper).pushPattern(details, inputsOf(details), Direction.WEST),
                "the Spirit Provider took a craft with no spirit around to do it");
        helper.succeed();
    }
}
