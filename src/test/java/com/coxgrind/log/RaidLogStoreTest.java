package com.coxgrind.log;

import com.coxgrind.model.CoxRaidRecord;
import com.coxgrind.model.RoomSplit;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class RaidLogStoreTest
{
	@Test
	public void savesOneRaidAndReplacesIt() throws Exception
	{
		Path root = Files.createTempDirectory("coxgrind-log");
		RaidLogStore store = new RaidLogStore(root);

		CoxRaidRecord raid = raid("raid-a", "");
		raid.setDeaths(2);
		store.save("acct-1", raid);

		raid.setKc(491);
		raid.setChallengeMode(true);
		raid.setPurple("Twisted bow");
		raid.setPersonalPoints(41000);
		store.save("acct-1", raid);

		CoxRaidRecord other = raid("raid-b", "Kodai insignia");
		store.save("acct-2", other);

		List<CoxRaidRecord> first = store.load("acct-1");
		Assert.assertEquals(1, first.size());
		Assert.assertEquals(491, first.get(0).getKc());
		Assert.assertTrue(first.get(0).isChallengeMode());
		Assert.assertEquals("Twisted bow", first.get(0).getPurple());
		Assert.assertEquals(41000, first.get(0).getPersonalPoints());
		Assert.assertEquals(2, first.get(0).getDeaths());
		Assert.assertEquals(60, first.get(0).secondsFor("Tekton"));

		String json = new String(Files.readAllBytes(root.resolve("account-acct-1.json")), StandardCharsets.UTF_8);
		Assert.assertFalse(json.contains("\"type\""));
		Assert.assertFalse(json.contains("playerName"));
		Assert.assertFalse(json.contains("accountHash"));
		Assert.assertFalse(json.contains("totalSeconds"));
		Assert.assertEquals(1200, first.get(0).getTotalSeconds());
		Assert.assertTrue(json.contains("{\"room\": \"Tekton\", \"seconds\": 60}"));
		Assert.assertEquals(1, store.load("acct-2").size());
		Assert.assertEquals("Kodai insignia", store.load("acct-2").get(0).getPurple());
	}

	@Test
	public void extrasRoundTripWithoutAPurple() throws Exception
	{
		Path root = Files.createTempDirectory("coxgrind-extras");
		RaidLogStore store = new RaidLogStore(root);
		CoxRaidRecord raid = raid("raid-a", "");
		raid.addExtra("Olmlet");
		raid.addExtra("Metamorphic dust");
		store.save("acct-1", raid);
		CoxRaidRecord loaded = store.load("acct-1").get(0);
		Assert.assertFalse(loaded.hasPurple());
		Assert.assertEquals(Arrays.asList("Olmlet", "Metamorphic dust"), loaded.getExtras());
		String json = new String(Files.readAllBytes(root.resolve("account-acct-1.json")), StandardCharsets.UTF_8);
		Assert.assertTrue(json.contains("\"extras\""));
		Assert.assertFalse(json.contains("\"purple\""));
	}

	@Test
	public void foldsLegacyPatchIntoOneRaid() throws Exception
	{
		Path root = Files.createTempDirectory("coxgrind-legacy");
		String legacy = "{\"type\":\"raid\",\"raid\":{\"id\":\"raid-a\",\"timestamp\":\"2026-09-22T16:38:15.932598400Z\",\"playerName\":\"Local Player\",\"accountHash\":\"acct-1\",\"challengeMode\":false,\"kc\":0,\"teamSize\":1,\"deaths\":0,\"personalPoints\":30000,\"teamPoints\":30000,\"totalSeconds\":1200,\"splits\":[{\"room\":\"Tekton\",\"seconds\":60},{\"room\":\"Raid Completed\",\"seconds\":1200}],\"purple\":\"\",\"partyPurples\":[]}}\n"
			+ "{\"type\":\"patch\",\"id\":\"raid-a\",\"kc\":214,\"challengeMode\":true,\"purple\":\"\",\"personalPoints\":30000,\"teamPoints\":30000,\"teamSize\":1,\"partyPurples\":[{\"playerName\":\"Someone Else\",\"item\":\"Elder maul\"}],\"deaths\":0}\n";
		Files.write(root.resolve("account-acct-1.jsonl"), legacy.getBytes(StandardCharsets.UTF_8));

		RaidLogStore store = new RaidLogStore(root);
		List<CoxRaidRecord> loaded = store.load("acct-1");
		Assert.assertEquals(1, loaded.size());
		Assert.assertEquals(214, loaded.get(0).getKc());
		Assert.assertTrue(loaded.get(0).isChallengeMode());
		Assert.assertEquals("2026-09-22T16:38:15Z", loaded.get(0).getTimestamp());
		Assert.assertEquals(1, loaded.get(0).getPartyPurples().size());
		Assert.assertEquals("Elder maul", loaded.get(0).getPartyPurples().get(0).getItem());
		Assert.assertFalse(Files.exists(root.resolve("account-acct-1.jsonl")));
		Assert.assertTrue(Files.exists(root.resolve("account-acct-1.json")));

		String json = new String(Files.readAllBytes(root.resolve("account-acct-1.json")), StandardCharsets.UTF_8);
		Assert.assertFalse(json.contains("Local Player"));
		Assert.assertFalse(json.contains("\"kc\": 0"));
		Assert.assertTrue(json.contains("\"kc\": 214"));
		Assert.assertTrue(json.contains("\"challengeMode\": true"));
		Assert.assertFalse(json.contains("totalSeconds"));
		Assert.assertEquals(1200, loaded.get(0).getTotalSeconds());
	}

	@Test
	public void updateKeepsYourPurpleAndOneTeammateDrop() throws Exception
	{
		Path root = Files.createTempDirectory("coxgrind-party");
		RaidLogStore store = new RaidLogStore(root);
		CoxRaidRecord raid = raid("raid-a", "Twisted bow");
		raid.addPartyPurple("Someone Else", "Elder maul");
		store.save("acct-1", raid);
		raid.addPartyPurple("Someone Else", "Elder maul");
		store.save("acct-1", raid);

		CoxRaidRecord loaded = store.load("acct-1").get(0);
		Assert.assertEquals("Twisted bow", loaded.getPurple());
		Assert.assertEquals(1, loaded.getPartyPurples().size());
		Assert.assertEquals("Someone Else", loaded.getPartyPurples().get(0).getPlayerName());
	}

	@Test
	public void missingFileIsAnEmptyLog() throws Exception
	{
		Path root = Files.createTempDirectory("coxgrind-empty");
		RaidLogStore store = new RaidLogStore(root);
		Assert.assertTrue(store.load("nobody").isEmpty());
	}

	@Test
	public void latestAccountIsTheNewestFile() throws Exception
	{
		Path root = Files.createTempDirectory("coxgrind-latest");
		RaidLogStore store = new RaidLogStore(root);
		store.save("older", raid("raid-a", ""));
		store.save("newer", raid("raid-b", ""));
		Files.setLastModifiedTime(
			root.resolve("account-older.json"),
			java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() - 60_000)
		);
		Assert.assertEquals("newer", store.latestAccountHash());
	}

	private static CoxRaidRecord raid(String id, String purple)
	{
		CoxRaidRecord raid = new CoxRaidRecord();
		raid.setId(id);
		raid.setTimestamp("2026-09-22T14:00:00.123Z");
		raid.setPlayerName("Local Player");
		raid.setAccountHash("acct-1");
		raid.setPersonalPoints(30000);
		raid.setTeamPoints(30000);
		raid.setTeamSize(1);
		raid.setTotalSeconds(1200);
		raid.setPurple(purple);
		raid.setSplits(Arrays.asList(new RoomSplit("Tekton", 60), new RoomSplit("Raid Completed", 1200)));
		return raid;
	}
}
