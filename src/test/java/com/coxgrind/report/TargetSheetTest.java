package com.coxgrind.report;

import com.coxgrind.CoxGrindConfig;
import com.coxgrind.model.ComparisonTargets;
import com.coxgrind.model.CoxRaidRecord;
import com.coxgrind.model.RaidFilter;
import com.coxgrind.model.RaidModeFilter;
import com.coxgrind.model.RaidSizeFilter;
import com.coxgrind.model.TargetSheet;
import java.util.Collections;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class TargetSheetTest
{
	@Test
	public void soloAndTeamUseDifferentSheets()
	{
		ComparisonTargets targets = new ComparisonTargets();
		targets.put(TargetSheet.CM_SOLO, "Tekton", 70);
		targets.put(TargetSheet.CM_TEAM, "Tekton", 90);
		targets.put(TargetSheet.REGULAR_SOLO, "Tekton", 40);
		targets.put(TargetSheet.REGULAR_TEAM, "Tekton", 50);
		ReportOptions options = ReportOptions.defaults(10);
		options.setTargets(targets);

		Assert.assertEquals(Integer.valueOf(70), time(options, RaidModeFilter.CM, RaidSizeFilter.SOLO));
		Assert.assertEquals(Integer.valueOf(90), time(options, RaidModeFilter.CM, RaidSizeFilter.TEAM));
		Assert.assertEquals(Integer.valueOf(40), time(options, RaidModeFilter.REGULAR, RaidSizeFilter.SOLO));
		Assert.assertEquals(Integer.valueOf(50), time(options, RaidModeFilter.REGULAR_FULL, RaidSizeFilter.TEAM));
		Assert.assertEquals(Integer.valueOf(90), time(options, RaidModeFilter.CM, RaidSizeFilter.of(3)));
		Assert.assertEquals("Analyzing all 3-man CM raids (1 raids)", RaidReportFormatter.heading(RaidModeFilter.CM, RaidSizeFilter.of(3), 1));
		Assert.assertEquals(RaidSizeFilter.TEAM, RaidSizeFilter.fromKey("TEAM"));
		CoxRaidRecord three = new CoxRaidRecord();
		three.setTeamSize(3);
		CoxRaidRecord mass = new CoxRaidRecord();
		mass.setTeamSize(28);
		Assert.assertTrue(RaidFilter.matches(three, RaidModeFilter.ALL, RaidSizeFilter.of(3)));
		Assert.assertFalse(RaidFilter.matches(mass, RaidModeFilter.ALL, RaidSizeFilter.of(3)));
		Assert.assertTrue(RaidFilter.matches(mass, RaidModeFilter.ALL, RaidSizeFilter.TEAM));
		Assert.assertTrue(options.comparisonSheet(RaidModeFilter.CM, RaidSizeFilter.ALL).isEmpty());
		Assert.assertTrue(options.comparisonSheet(RaidModeFilter.ALL, RaidSizeFilter.SOLO).isEmpty());
	}

	@Test
	public void sectionTitlesMapToSheets()
	{
		Assert.assertEquals(TargetSheet.CM_SOLO, TargetSheet.fromTitle("CM solo"));
		Assert.assertEquals(TargetSheet.CM_TEAM, TargetSheet.fromTitle("<html>+ CM team</html>"));
		Assert.assertEquals(TargetSheet.REGULAR_SOLO, TargetSheet.fromTitle("Regular solo"));
		Assert.assertNull(TargetSheet.fromTitle("Strategies"));
		Assert.assertNull(TargetSheet.fromTitle("Death: CM team"));
		Assert.assertNull(TargetSheet.fromTitle("Death: CM solo"));
	}

	@Test
	public void cmSoloDefaultsAddUpToTheRaidTime()
	{
		Assert.assertEquals("25:30", TargetTotals.text(new CoxGrindConfig() {}, TargetSheet.CM_SOLO));
		Assert.assertEquals("", TargetTotals.text(new CoxGrindConfig() {}, TargetSheet.REGULAR_SOLO));
	}

	@Test
	public void onePhaseTimeIsReusedAndTheLastPhaseHasNoMageHand()
	{
		ComparisonTargets targets = new ComparisonTargets();
		targets.put(TargetSheet.CM_SOLO, "Olm phase", 114);
		targets.put(TargetSheet.CM_SOLO, "Olm mage hand", 56);
		targets.put(TargetSheet.CM_SOLO, "Olm head", 66);
		ReportOptions options = ReportOptions.defaults(10);
		options.setTargets(targets);
		Map<String, Integer> sheet = options.comparisonSheet(RaidModeFilter.CM, RaidSizeFilter.SOLO);
		Assert.assertEquals(Integer.valueOf(468), sheet.get("Olm"));

		CoxRaidRecord raid = new CoxRaidRecord();
		raid.setTeamSize(28);
		Map<String, Integer> scaled = TargetStyle.expandFor(sheet, Collections.singletonList(raid));
		Assert.assertEquals(Integer.valueOf(114), scaled.get("Olm phase 6"));
		Assert.assertEquals(Integer.valueOf(56), scaled.get("Olm mage hand phase 5"));
		Assert.assertNull(scaled.get("Olm mage hand phase 6"));
		Assert.assertEquals(Integer.valueOf(900), scaled.get("Olm"));
	}

	@Test
	public void resetKeysStayOnTheirOwnSheet()
	{
		String[] solo = TargetSettings.keysForReset("resetCmSolo");
		Assert.assertEquals("cmTekton", solo[0]);
		Assert.assertEquals("cmPph", solo[solo.length - 1]);
		Assert.assertNull(TargetSettings.keysForReset("cmTekton"));
		String[] team = TargetSettings.keysForReset("resetCmTeam");
		Assert.assertEquals("cmTeamTekton", team[0]);
		Assert.assertTrue(TargetSettings.isTotalKey("cmSoloTotal"));
		Assert.assertFalse(TargetSettings.isTotalKey("cmTekton"));
	}

	private static Integer time(ReportOptions options, RaidModeFilter mode, RaidSizeFilter size)
	{
		Map<String, Integer> sheet = options.comparisonSheet(mode, size);
		return sheet.get("Tekton");
	}
}
