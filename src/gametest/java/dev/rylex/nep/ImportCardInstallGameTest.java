package dev.rylex.nep;

import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.parts.crafting.PatternProviderPart;
import dev.rylex.nep.provider.ImportUpgradeHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class ImportCardInstallGameTest {

    private static final BlockPos PROVIDER = new BlockPos(2, 1, 2);

    private ImportCardInstallGameTest() {}

    public static void register(NepGameTests.Batch batch) {
        batch.add(
                        "shift_clicking_a_provider_installs_the_import_card",
                        ImportCardInstallGameTest::shiftClickingAProviderInstallsTheImportCard)
                .add(
                        "shift_clicking_a_provider_rejects_unsupported_cards",
                        ImportCardInstallGameTest::shiftClickingAProviderRejectsUnsupportedCards)
                .add("a_second_import_card_is_refused", ImportCardInstallGameTest::aSecondImportCardIsRefused)
                .add(
                        "dropping_the_card_into_the_upgrade_slot_by_hand_leaves_the_same_state",
                        ImportCardInstallGameTest::droppingTheCardIntoTheUpgradeSlotByHandLeavesTheSameState)
                .add(
                        "a_fake_player_installs_the_card_exactly_once",
                        ImportCardInstallGameTest::aFakePlayerInstallsTheCardExactlyOnceLikeAnyOtherAe2Upgrade)
                .add(
                        "the_cable_mounted_provider_accepts_cards_too",
                        ImportCardInstallGameTest::theCableMountedProviderAcceptsCardsToo);
    }

    private record Interaction(ImportUpgradeHost provider, Player player) {
        ItemStack held() {
            return player.getItemInHand(InteractionHand.MAIN_HAND);
        }
    }

    private static Interaction shiftClickProvider(GameTestHelper helper, ItemStack held) {
        helper.setBlock(PROVIDER, AEBlocks.PATTERN_PROVIDER.block());

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);

        BlockPos pos = helper.absolutePos(PROVIDER);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        player.getItemInHand(InteractionHand.MAIN_HAND)
                .onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));

        PatternProviderBlockEntity be = helper.getBlockEntity(PROVIDER, PatternProviderBlockEntity.class);
        helper.assertTrue(
                be instanceof ImportUpgradeHost,
                "the pattern provider host mixin did not apply; upgrade cards cannot be installed in world");
        return new Interaction((ImportUpgradeHost) be, player);
    }

    public static void shiftClickingAProviderInstallsTheImportCard(GameTestHelper helper) {
        Interaction interaction = shiftClickProvider(helper, new ItemStack(NepItems.IMPORT_CARD.get()));

        helper.assertTrue(
                interaction.provider().nepImportUpgrades().isInstalled(NepItems.IMPORT_CARD.get()),
                "shift clicking the provider with an import card did not install it");
        helper.assertTrue(interaction.held().isEmpty(), "the import card was installed but not taken from the player");
        helper.succeed();
    }

    public static void shiftClickingAProviderRejectsUnsupportedCards(GameTestHelper helper) {
        Interaction interaction = shiftClickProvider(helper, new ItemStack(AEItems.SPEED_CARD));

        helper.assertTrue(
                interaction.provider().nepImportUpgrades().isEmpty(),
                "an unsupported card was installed on the provider");
        helper.assertTrue(interaction.held().getCount() == 1, "an unsupported card was taken from the player");
        helper.succeed();
    }

    public static void aSecondImportCardIsRefused(GameTestHelper helper) {
        Interaction first = shiftClickProvider(helper, new ItemStack(NepItems.IMPORT_CARD.get()));
        helper.assertTrue(
                first.provider().nepImportUpgrades().isInstalled(NepItems.IMPORT_CARD.get()),
                "the first card did not install");

        ItemStack second = new ItemStack(NepItems.IMPORT_CARD.get());
        first.player().setItemInHand(InteractionHand.MAIN_HAND, second);
        BlockPos pos = helper.absolutePos(PROVIDER);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        second.onItemUseFirst(new UseOnContext(first.player(), InteractionHand.MAIN_HAND, hit));

        helper.assertTrue(
                first.provider().nepImportUpgrades().getInstalledUpgrades(NepItems.IMPORT_CARD.get()) == 1,
                "a second import card was installed on a provider that already had one");
        helper.assertTrue(first.held().getCount() == 1, "a refused import card was still taken from the player");
        helper.succeed();
    }

    public static void droppingTheCardIntoTheUpgradeSlotByHandLeavesTheSameState(GameTestHelper helper) {
        helper.setBlock(PROVIDER, AEBlocks.PATTERN_PROVIDER.block());
        PatternProviderBlockEntity be = helper.getBlockEntity(PROVIDER, PatternProviderBlockEntity.class);
        helper.assertTrue(be instanceof ImportUpgradeHost, "the pattern provider host mixin did not apply");

        ImportUpgradeHost provider = (ImportUpgradeHost) be;
        ItemStack leftover = provider.nepImportUpgrades().addItems(new ItemStack(NepItems.IMPORT_CARD.get()));

        helper.assertTrue(leftover.isEmpty(), "the provider's upgrade slot refused an import card put in by hand");
        helper.assertTrue(
                provider.nepImportUpgrades().isInstalled(NepItems.IMPORT_CARD.get()),
                "a card placed in the upgrade slot by hand did not register as installed, so the slot reads"
                        + " differently depending on how the card got there");
        helper.assertTrue(
                provider.nepImportUpgrades().getInstalledUpgrades(NepItems.IMPORT_CARD.get()) == 1,
                "a hand-placed card counted as "
                        + provider.nepImportUpgrades().getInstalledUpgrades(NepItems.IMPORT_CARD.get())
                        + " installs");
        helper.succeed();
    }

    public static void aFakePlayerInstallsTheCardExactlyOnceLikeAnyOtherAe2Upgrade(GameTestHelper helper) {
        helper.setBlock(PROVIDER, AEBlocks.PATTERN_PROVIDER.block());
        PatternProviderBlockEntity be = helper.getBlockEntity(PROVIDER, PatternProviderBlockEntity.class);
        helper.assertTrue(be instanceof ImportUpgradeHost, "the pattern provider host mixin did not apply");

        FakePlayer fake = FakePlayerFactory.getMinecraft(helper.getLevel());
        fake.setShiftKeyDown(true);
        ItemStack card = new ItemStack(NepItems.IMPORT_CARD.get());
        fake.setItemInHand(InteractionHand.MAIN_HAND, card);

        BlockPos pos = helper.absolutePos(PROVIDER);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        fake.getItemInHand(InteractionHand.MAIN_HAND)
                .onItemUseFirst(new UseOnContext(fake, InteractionHand.MAIN_HAND, hit));

        helper.assertTrue(
                ((ImportUpgradeHost) be).nepImportUpgrades().getInstalledUpgrades(NepItems.IMPORT_CARD.get()) == 1,
                "a fake player could not install the card, though the install path is AE2's own upgrade card item and"
                        + " behaves the same on every other AE2 machine");
        helper.assertTrue(
                fake.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "the card was installed but not taken off the fake player, so automation could duplicate it");
        fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.succeed();
    }

    public static void theCableMountedProviderAcceptsCardsToo(GameTestHelper helper) {
        helper.assertTrue(
                ImportUpgradeHost.class.isAssignableFrom(PatternProviderPart.class),
                "the cable mounted pattern provider cannot take upgrade cards in world");
        helper.succeed();
    }
}
