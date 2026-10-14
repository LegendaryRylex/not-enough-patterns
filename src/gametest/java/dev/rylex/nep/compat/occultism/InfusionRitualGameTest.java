package dev.rylex.nep.compat.occultism;

import com.klikli_dev.modonomicon.api.multiblock.Multiblock;
import com.klikli_dev.occultism.common.blockentity.GoldenSacrificialBowlBlockEntity;
import com.klikli_dev.occultism.common.blockentity.SacrificialBowlBlockEntity;
import com.klikli_dev.occultism.crafting.recipe.RitualRecipe;
import com.klikli_dev.occultism.registry.OccultismBlocks;
import com.klikli_dev.occultism.registry.OccultismItems;
import com.klikli_dev.occultism.registry.OccultismRecipes;
import dev.rylex.nep.Nep;
import dev.rylex.nep.NepGameTests;
import dev.rylex.nep.util.Recipes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.item.ItemResource;

public final class InfusionRitualGameTest {

    private static final BlockPos CENTRE = new BlockPos(4, 1, 4);
    private static final List<BlockPos> BOWLS = List.of(
            new BlockPos(0, 1, 0),
            new BlockPos(1, 1, 0),
            new BlockPos(7, 1, 0),
            new BlockPos(8, 1, 0),
            new BlockPos(0, 1, 8));

    private InfusionRitualGameTest() {}

    public static void register(NepGameTests.Batch batch0) {
        batch0.add(
                        "every_book_ritual_loads_and_makes_its_book",
                        InfusionRitualGameTest::everyBookRitualLoadsAndMakesItsBook)
                .add(
                        "foliot_crusher_ritual_binds_the_spirit_into_a_book",
                        2400,
                        InfusionRitualGameTest::foliotCrusherRitualBindsTheSpiritIntoABook);
    }

    private static RecipeHolder<RitualRecipe> ritual(GameTestHelper helper, String path) {
        return Recipes.allOf(helper.getLevel(), OccultismRecipes.RITUAL_TYPE.get()).stream()
                .filter(holder -> Recipes.idOf(holder).equals(Nep.id(path)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("ritual " + path + " is not loaded"));
    }

    public static void everyBookRitualLoadsAndMakesItsBook(GameTestHelper helper) {
        for (BoundSpirit.Work work : BoundSpirit.Work.values()) {
            for (BoundSpirit.Tier tier : BoundSpirit.Tier.values()) {
                String path = "ritual/infuse_" + tier.getSerializedName() + "_" + work.getSerializedName();
                ItemStack result = ritual(helper, path).value().getResult();
                helper.assertTrue(
                        new BoundSpirit(tier, work).equals(result.get(NepOccultismContent.BOUND_SPIRIT.get())),
                        path + " does not make a " + tier.getSerializedName() + " " + work.getSerializedName()
                                + " book");
            }
        }
        helper.succeed();
    }

    public static void foliotCrusherRitualBindsTheSpiritIntoABook(GameTestHelper helper) {
        RecipeHolder<RitualRecipe> recipe = ritual(helper, "ritual/infuse_foliot_crusher");
        Multiblock pentacle = recipe.value().getPentacle();
        helper.assertTrue(pentacle != null, "the foliot craft pentacle did not load");
        pentacle.place(helper.getLevel(), helper.absolutePos(CENTRE), Rotation.NONE);
        helper.assertTrue(
                helper.getBlockState(CENTRE).is(OccultismBlocks.GOLDEN_SACRIFICIAL_BOWL.get()),
                "the pentacle did not place a golden bowl at its centre");

        List<ItemStack> offerings = List.of(
                new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.COPPER_INGOT),
                new ItemStack(OccultismItems.SILVER_INGOT.get()),
                new ItemStack(OccultismItems.BOOK_OF_BINDING_EMPTY.get()));
        for (int index = 0; index < offerings.size(); index++) {
            BlockPos pos = BOWLS.get(index);
            helper.setBlock(pos, OccultismBlocks.SACRIFICIAL_BOWL.get().defaultBlockState());
            SacrificialBowlBlockEntity bowl = helper.getBlockEntity(pos, SacrificialBowlBlockEntity.class);
            bowl.itemStackHandler.set(
                    0,
                    ItemResource.of(offerings.get(index)),
                    offerings.get(index).getCount());
        }

        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setItemInHand(
                InteractionHand.MAIN_HAND, new ItemStack(OccultismItems.BOOK_OF_BINDING_BOUND_FOLIOT.get()));
        GoldenSacrificialBowlBlockEntity golden = helper.getBlockEntity(CENTRE, GoldenSacrificialBowlBlockEntity.class);
        helper.assertTrue(
                golden.activate(
                        helper.getLevel(), helper.absolutePos(CENTRE), player, InteractionHand.MAIN_HAND, Direction.UP),
                "the golden bowl did not start the infusion ritual");

        BoundSpirit expected = new BoundSpirit(BoundSpirit.Tier.FOLIOT, BoundSpirit.Work.CRUSHER);
        helper.succeedWhen(() -> {
            AABB area = new AABB(helper.absolutePos(CENTRE)).inflate(4);
            boolean dropped = helper
                    .getLevel()
                    .getEntitiesOfClass(
                            ItemEntity.class,
                            area,
                            item -> expected.equals(item.getItem().get(NepOccultismContent.BOUND_SPIRIT.get())))
                    .stream()
                    .findAny()
                    .isPresent();
            boolean held = expected.equals(
                    golden.itemStackHandler.getResource(0).toStack().get(NepOccultismContent.BOUND_SPIRIT.get()));
            helper.assertTrue(dropped || held, "the ritual has not produced the foliot crusher book yet");
        });
    }
}
