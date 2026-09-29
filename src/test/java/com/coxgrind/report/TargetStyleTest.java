package com.coxgrind.report;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class TargetStyleTest
{
	@Test
	public void untickedGearAddsTimeAndMoneyRunAddsWhenTicked()
	{
		TargetStyle style = new TargetStyle();
		style.setUseTbow(false);
		style.setSlayerHelm(false);
		style.setLockpick(false);
		style.setAxe(false);
		style.setSalve(false);
		style.setPreVeng(false);
		style.setVespPotSkip(false);
		style.setCrabTank(false);
		style.setKillRope(true);
		style.setMilkVespula(true);
		style.setIceMilking(true);
		style.setIceMilkSeconds(70);
		style.setOverThieve(true);

		Map<String, Integer> sheet = new LinkedHashMap<>();
		sheet.put("Tekton", 70);
		sheet.put("Crabs", 56);
		sheet.put("Shamans", 63);
		sheet.put("Vanguards", 132);
		sheet.put("Tightrope", 47);
		sheet.put("Vasa", 70);
		sheet.put("Muttadiles", 85);
		sheet.put("Olm head", 66);
		sheet.put("Thieving", 75);
		sheet.put("Ice demon", 144);
		sheet.put("Mystics", 100);
		sheet.put("Vespula", 57);

		Map<String, Integer> out = style.apply(sheet);
		Assert.assertEquals(Integer.valueOf(74), out.get("Tekton"));
		Assert.assertEquals(Integer.valueOf(60), out.get("Crabs"));
		Assert.assertEquals(Integer.valueOf(78), out.get("Shamans"));
		Assert.assertEquals(Integer.valueOf(134), out.get("Vanguards"));
		Assert.assertEquals(Integer.valueOf(47 + 8 + 6 + 59), out.get("Tightrope"));
		Assert.assertEquals(Integer.valueOf(100), out.get("Vasa"));
		Assert.assertEquals(Integer.valueOf(95), out.get("Muttadiles"));
		Assert.assertEquals(Integer.valueOf(86), out.get("Olm head"));
		Assert.assertEquals(Integer.valueOf(86), out.get("Thieving"));
		Assert.assertEquals(Integer.valueOf(219), out.get("Ice demon"));
		Assert.assertEquals(Integer.valueOf(105), out.get("Mystics"));
		Assert.assertEquals(Integer.valueOf(75), out.get("Vespula"));
		String note = style.note();
		Assert.assertTrue(note.contains("no twisted bow"));
		Assert.assertTrue(note.contains("no slayer helm"));
		Assert.assertTrue(note.contains("no lockpick"));
		Assert.assertTrue(note.contains("no axe"));
		Assert.assertTrue(note.contains("no salve"));
		Assert.assertTrue(note.contains("no pre-veng"));
		Assert.assertTrue(note.contains("no vesp pot skip"));
		Assert.assertTrue(note.contains("no crab tank"));
		Assert.assertTrue(note.contains("killing rope"));
		Assert.assertTrue(note.contains("vesp milk"));
		Assert.assertTrue(note.contains("ice milking"));
		Assert.assertTrue(note.contains("over-thieve"));
	}

	@Test
	public void tickedDefaultsLeaveTargetsAndRopeNeedsKillRope()
	{
		TargetStyle style = new TargetStyle();
		Map<String, Integer> sheet = new LinkedHashMap<>();
		sheet.put("Shamans", 63);
		sheet.put("Tekton", 70);
		sheet.put("Crabs", 56);
		sheet.put("Tightrope", 47);
		Map<String, Integer> out = style.apply(sheet);
		Assert.assertEquals(Integer.valueOf(63), out.get("Shamans"));
		Assert.assertEquals(Integer.valueOf(70), out.get("Tekton"));
		Assert.assertEquals(Integer.valueOf(56), out.get("Crabs"));
		Assert.assertEquals(Integer.valueOf(47), out.get("Tightrope"));
		Assert.assertEquals("", style.note());

		style.setUseTbow(false);
		out = style.apply(sheet);
		Assert.assertEquals(Integer.valueOf(69), out.get("Shamans"));
		Assert.assertEquals(Integer.valueOf(47), out.get("Tightrope"));
	}

	@Test
	public void sippingPoolsAndBankingAddToBetweenRooms()
	{
		TargetStyle style = new TargetStyle();
		Map<String, Integer> sheet = new LinkedHashMap<>();
		sheet.put("Between room time", 56);
		Map<String, Integer> plain = style.apply(sheet);
		Assert.assertEquals(Integer.valueOf(56), plain.get("Between room time"));
		Assert.assertEquals("", style.note());

		style.setSippingPools(true);
		style.setBankAfterRope(true);
		Map<String, Integer> out = style.apply(sheet);
		Assert.assertEquals(Integer.valueOf(73), out.get("Between room time"));
		Assert.assertTrue(style.note().contains("sipping pools"));
		Assert.assertTrue(style.note().contains("banking after rope"));
	}
}
