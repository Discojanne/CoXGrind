package com.coxgrind.track;

import org.junit.Assert;
import org.junit.Test;

public class RaidChatTest
{
	@Test
	public void readsRoomAndFloorMessages()
	{
		Assert.assertEquals("Tekton", RaidChat.roomName("Combat room `Tekton` complete! Duration: 1:02. Personal best: 0:58"));
		Assert.assertEquals("Ice demon", RaidChat.roomName("Puzzle `Ice Demon` complete! Duration: 2:10"));
		Assert.assertEquals("Crabs", RaidChat.roomName("Puzzle `Crabs` complete! Duration: 1:20"));
		Assert.assertNull(RaidChat.roomName("Upper level complete! Duration: 4:31"));
		Assert.assertEquals("Upper", RaidChat.levelName("Upper level complete! Duration: 4:31"));
		Assert.assertEquals("Lower", RaidChat.levelName("Lower level complete! Duration: 5:02"));
		Assert.assertEquals("Upper", RaidChat.levelName("@mes_hl_mag@Upper level complete! Duration: </col><col=ff0000>6:19</col>"));
		Assert.assertEquals("Middle", RaidChat.levelName("@mes_hl_mag@Middle level complete! Duration: </col><col=ff0000>13:31</col>"));
		Assert.assertEquals("Lower", RaidChat.levelName("@mes_hl_mag@Lower level complete! Duration: </col><col=ff0000>20:06</col>"));
	}

	@Test
	public void readsStartCompleteAndKillCount()
	{
		Assert.assertTrue(RaidChat.isRaidStart("The raid has begun!"));
		Assert.assertTrue(RaidChat.isRaidComplete("Congratulations - your raid is complete!"));
		Assert.assertTrue(RaidChat.isPlayerDeath("Oh dear, you are dead!"));
		Assert.assertFalse(RaidChat.isPlayerDeath("You died."));
		RaidChat.KillCount regular = RaidChat.killCount("Your completed Chambers of Xeric count is: 491.");
		Assert.assertNotNull(regular);
		Assert.assertEquals(491, regular.getKc());
		Assert.assertFalse(regular.isChallengeMode());

		RaidChat.KillCount cm = RaidChat.killCount("Your completed Chambers of Xeric Challenge Mode count is: 12.");
		Assert.assertNotNull(cm);
		Assert.assertEquals(12, cm.getKc());
		Assert.assertTrue(cm.isChallengeMode());
	}

	@Test
	public void readsOnlyPersonalPurples()
	{
		Assert.assertEquals("Twisted bow", ownItem("Special loot: Twisted bow", "Local Player"));
		Assert.assertEquals("Dinh's bulwark", ownItem("Local Player received special loot: Dinh's bulwark", "Local Player"));
		Assert.assertNull(ownItem("Someone Else received special loot: Twisted bow", "Local Player"));
		Assert.assertNull(ownItem("Special loot: Metamorphic dust", "Local Player"));
		Assert.assertNull(ownItem("Special loot: Olmlet", "Local Player"));
	}

	private static String ownItem(String message, String name)
	{
		RaidChat.LootDrop drop = RaidChat.lootDrop(message);
		if (drop == null || !drop.isOwnDrop(name))
		{
			return null;
		}
		return drop.getItem();
	}

	@Test
	public void keepsATeammatePurple()
	{
		RaidChat.LootDrop drop = RaidChat.lootDrop("Someone Else received special loot: Twisted bow");
		Assert.assertNotNull(drop);
		Assert.assertEquals("Someone Else", drop.getPlayerName());
		Assert.assertEquals("Twisted bow", drop.getItem());
		Assert.assertFalse(drop.isOwnDrop("Local Player"));

		RaidChat.LootDrop own = RaidChat.lootDrop("Special loot: Dragon claws");
		Assert.assertNotNull(own);
		Assert.assertTrue(own.isOwnDrop("Local Player"));
	}

	@Test
	public void readsTheLiveFriendsChatPurpleList()
	{
		String header = "<col=ef20ff>Special loot:</col>";
		Assert.assertTrue(RaidChat.isPurpleHeader(header));
		Assert.assertNull(RaidChat.lootDrop(header));

		String row = "<col=ef20ff>Local\u00A0Player -</col> <col=ff0000>Dexterous prayer scroll</col>";
		RaidChat.LootDrop drop = RaidChat.purpleRecipient(row);
		Assert.assertNotNull(drop);
		Assert.assertEquals("Local Player", drop.getPlayerName());
		Assert.assertEquals("Dexterous prayer scroll", drop.getItem());
		Assert.assertTrue(drop.isOwnDrop("Local Player"));

		RaidChat.LootDrop teammate = RaidChat.purpleRecipient("Someone Else - Twisted bow");
		Assert.assertNotNull(teammate);
		Assert.assertEquals("Twisted bow", teammate.getItem());
		Assert.assertFalse(teammate.isOwnDrop("Local Player"));
		Assert.assertNull(RaidChat.purpleRecipient("Congratulations - your raid is complete!"));
		Assert.assertNull(RaidChat.purpleRecipient("Someone Else - Olmlet"));
	}

	@Test
	public void readsYourValuableDropAndIgnoresTheClanBroadcast()
	{
		String valuable = "<col=ef1020>Valuable drop: Dexterous prayer scroll (15,020,290 coins)</col>";
		RaidChat.LootDrop drop = RaidChat.lootDrop(valuable);
		Assert.assertNotNull(drop);
		Assert.assertEquals("Dexterous prayer scroll", drop.getItem());
		Assert.assertTrue(drop.isOwnDrop("Local Player"));
		Assert.assertNull(RaidChat.lootDrop("Valuable drop: Metamorphic dust (1,000 coins)"));
		Assert.assertNull(RaidChat.lootDrop("Local Player received special loot from a raid: Dexterous prayer scroll (15,020,290 coins)."));
	}

	@Test
	public void readsPetKitAndDustApartFromPurples()
	{
		Assert.assertNull(RaidChat.lootDrop("Special loot: Olmlet"));
		Assert.assertEquals("Olmlet", RaidChat.sideReward("Special loot: Olmlet").getItem());
		Assert.assertEquals("pet", RaidChat.sideKind("Olmlet"));
		Assert.assertEquals("Metamorphic dust", RaidChat.sideReward("Valuable drop: Metamorphic dust (1,000 coins)").getItem());
		Assert.assertEquals("dust", RaidChat.sideKind("Metamorphic dust"));
		Assert.assertEquals("Twisted ancestral colour kit",
			RaidChat.sideReward("Local Player received special loot: Twisted ancestral colour kit").getItem());
		Assert.assertEquals("kit", RaidChat.sideKind("Twisted ancestral colour kit"));
		Assert.assertTrue(RaidChat.sideReward("Special loot: Olmlet").isOwnDrop("Local Player"));
		Assert.assertFalse(RaidChat.sideReward("Someone Else received special loot: Olmlet").isOwnDrop("Local Player"));

		RaidChat.LootDrop row = RaidChat.sideRecipient("<col=ef20ff>Local Player -</col> <col=ff0000>Olmlet</col>");
		Assert.assertNotNull(row);
		Assert.assertEquals("Olmlet", row.getItem());
		Assert.assertNull(RaidChat.purpleRecipient("Local Player - Olmlet"));
		Assert.assertNull(RaidChat.sideReward("Special loot: Twisted bow"));
	}
}
