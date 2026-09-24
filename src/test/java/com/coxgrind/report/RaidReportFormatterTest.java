package com.coxgrind.report;

import com.coxgrind.model.ComparisonTargets;
import com.coxgrind.model.CoxRaidRecord;
import com.coxgrind.model.RaidModeFilter;
import com.coxgrind.model.RaidSizeFilter;
import com.coxgrind.model.RoomSplit;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public class RaidReportFormatterTest
{
	@Test
	public void headingMatchesSoloChallengeMode()
	{
		String heading = RaidReportFormatter.heading(RaidModeFilter.CM, RaidSizeFilter.SOLO, 4);
		Assert.assertEquals("Analyzing all solo CM raids (4 raids)", heading);
	}

	@Test
	public void reportIncludesRoomTablePointsAndPurple()
	{
		CoxRaidRecord fast = raid(true, 1, 40000, 80, "");
		CoxRaidRecord recent = raid(true, 1, 50000, 60, "Twisted bow");
		recent.setTimestamp("2026-09-22T16:00:00Z");

		String report = RaidReportFormatter.format(
			Arrays.asList(fast, recent),
			RaidModeFilter.CM,
			RaidSizeFilter.SOLO,
			10
		);

		Assert.assertTrue(report.contains("Analyzing all solo CM raids (2 raids)"));
		Assert.assertTrue(report.indexOf("Purple Summary") < report.indexOf("Deaths"));
		Assert.assertFalse(report.contains("Room efficiency"));
		Assert.assertFalse(report.contains("Most common prep rooms"));
		Assert.assertTrue(report.contains("Tekton"));
		Assert.assertTrue(report.contains("Best"));
		Assert.assertTrue(report.contains("Average"));
		Assert.assertTrue(report.contains("Recent"));
		Assert.assertTrue(report.contains("Last 10"));
		Assert.assertTrue(report.contains("01:00"));
		Assert.assertTrue(report.contains("Total Points"));
		Assert.assertTrue(report.contains("PPH"));
		Assert.assertTrue(report.contains("Purple pace (this table): 1 in "));
		Assert.assertTrue(report.contains("Twisted bow"));
		Assert.assertTrue(report.contains("Current dry streak: 0 raids"));
		Assert.assertFalse(report.contains("Analyzing all solo raids ("));
	}

	@Test
	public void regularTeamRaidIsFilteredOutOfSoloCm()
	{
		CoxRaidRecord team = raid(false, 3, 20000, 70, "");
		String report = RaidReportFormatter.format(
			Collections.singletonList(team),
			RaidModeFilter.CM,
			RaidSizeFilter.SOLO,
			10
		);
		Assert.assertTrue(report.contains("(0 raids)"));
		Assert.assertTrue(report.contains("No completed raids"));
	}

	@Test
	public void dropsImpossiblyShortRoomTimes()
	{
		Assert.assertFalse(RaidReportFormatter.validTime("Tekton", 10));
		Assert.assertTrue(RaidReportFormatter.validTime("Tekton", 40));
	}

	@Test
	public void regularFullLayoutIsSeparateFromRegular()
	{
		CoxRaidRecord full = raid(false, 1, 50000, 40, "");
		for (int i = 1; i < com.coxgrind.track.RoomNames.PREP_ROOMS.size(); i++)
		{
			full.getSplits().add(new RoomSplit(com.coxgrind.track.RoomNames.PREP_ROOMS.get(i), 40));
		}
		Assert.assertTrue(full.isFullLayout());

		String regular = RaidReportFormatter.format(
			Collections.singletonList(full),
			RaidModeFilter.REGULAR,
			RaidSizeFilter.ALL,
			10
		);
		String fullReport = RaidReportFormatter.format(
			Collections.singletonList(full),
			RaidModeFilter.REGULAR_FULL,
			RaidSizeFilter.ALL,
			10
		);
		Assert.assertTrue(regular.contains("(0 raids)"));
		Assert.assertTrue(fullReport.contains("regular full raids"));
		Assert.assertTrue(fullReport.contains("(1 raids)"));
	}

	@Test
	public void reportShowsTargetColumnOnRateAndDeath()
	{
		CoxRaidRecord recent = raid(true, 1, 50000, 60, "Twisted bow");
		recent.setKc(12);
		recent.setDeaths(2);
		ReportOptions options = ReportOptions.defaults(10);
		ComparisonTargets targets = new ComparisonTargets();
		targets.put(true, "Tekton", 50);
		targets.put(true, ComparisonTargets.PPH, 90000);
		options.setTargets(targets);
		options.setRoomEfficiency(true);
		options.setCommonRooms(true);

		String report = RaidReportFormatter.format(
			Collections.singletonList(recent),
			RaidModeFilter.CM,
			RaidSizeFilter.SOLO,
			options
		);

		Assert.assertTrue(report.contains("vs Target"));
		String targetTab = RaidReportFormatter.recentVersusTarget(
			Collections.singletonList(recent),
			RaidModeFilter.CM,
			RaidSizeFilter.SOLO,
			options
		);
		Assert.assertFalse(targetTab.contains("Recent raid vs your targets"));
		Assert.assertTrue(targetTab.contains("Tekton"));
		Assert.assertTrue(report.contains("On Rate"));
		Assert.assertTrue(report.contains("Death estimate"));
		int deathsAt = report.indexOf("Total deaths");
		Assert.assertTrue(deathsAt >= 0);
		Assert.assertTrue(report.substring(deathsAt, Math.min(report.length(), deathsAt + 40)).contains("2"));
		Assert.assertTrue(report.contains("Room efficiency"));
		Assert.assertTrue(report.contains("Most common prep rooms"));
		Assert.assertTrue(report.contains("CM 12"));
		Assert.assertTrue(report.contains("+ purple"));
	}

	@Test
	public void methodChecksAdjustTargets()
	{
		CoxRaidRecord raid = raid(true, 1, 50000, 60, "");
		raid.getSplits().add(new RoomSplit("Vasa", 80));
		raid.getSplits().add(new RoomSplit("Ice demon", 120));
		raid.getSplits().add(new RoomSplit("Tightrope", 90));
		raid.getSplits().add(new RoomSplit("Vespula", 50));
		ReportOptions options = ReportOptions.defaults(10);
		ComparisonTargets targets = new ComparisonTargets();
		targets.put(true, "Vasa", 200);
		targets.put(true, "Ice demon", 100);
		targets.put(true, "Tightrope", 80);
		targets.put(true, "Vespula", 60);
		targets.put(true, "Tekton", 50);
		options.setTargets(targets);
		TargetStyle style = options.getTargetStyle();
		style.setUseTbow(false);
		style.setIceMilking(true);
		style.setIceMilkSeconds(70);
		style.setKillRope(true);
		style.setKillRopeSeconds(40);
		style.setMilkVespula(true);
		style.setMilkVespulaSeconds(10);

		String report = RaidReportFormatter.format(
			java.util.Collections.singletonList(raid),
			RaidModeFilter.CM,
			RaidSizeFilter.SOLO,
			options
		);

		Assert.assertTrue(report.contains("03:00"));
		Assert.assertTrue(report.contains("02:50"));
		Assert.assertTrue(report.contains("01:50"));
		Assert.assertTrue(report.contains("01:10"));
		Assert.assertTrue(report.contains("00:50"));
		Assert.assertTrue(report.contains("no twisted bow"));
		Assert.assertTrue(report.contains("ice milking"));
		Assert.assertTrue(report.contains("killing rope"));
		Assert.assertFalse(report.contains("03:20"));
	}

	@Test
	public void sidebarComparisonIsOneRow()
	{
		CoxRaidRecord raid = raid(true, 1, 50000, 68, "");
		raid.getSplits().add(new RoomSplit("Olm mage hand phase 1", 70));
		String text = RaidReportFormatter.recentVersusAverage(
			java.util.Collections.singletonList(raid),
			RaidModeFilter.CM,
			RaidSizeFilter.SOLO,
			ReportOptions.defaults(10)
		);
		Assert.assertEquals("mage hand p1", RaidReportFormatter.shortRoom("Olm mage hand phase 1"));
		Assert.assertEquals("Between rooms", RaidReportFormatter.shortRoom("Between room time"));
		Assert.assertTrue(text.contains("mage hand p1"));
		Assert.assertFalse(text.contains("Olm mage hand phase 1"));
		boolean found = false;
		for (String line : text.split("\n"))
		{
			if (line.startsWith("mage hand p1"))
			{
				found = line.contains("01:10");
			}
		}
		Assert.assertTrue(found);
	}

	@Test
	public void liveRaidListsSplitsAsTheyFinish()
	{
		CoxRaidRecord past = raid(true, 1, 50000, 80, "");
		CoxRaidRecord live = new CoxRaidRecord();
		live.putSplit("Tekton", 60);
		live.putSplit("Floor 1", 90);
		RaidReportFormatter.PaceComparison view = RaidReportFormatter.paceView(
			Collections.singletonList(past),
			RaidModeFilter.CM,
			RaidSizeFilter.SOLO,
			ReportOptions.defaults(10),
			live,
			45,
			true
		);
		String text = view.getText();
		Assert.assertTrue(text.contains("Tekton"));
		Assert.assertTrue(text.contains("01:00"));
		Assert.assertTrue(text.contains("Current"));
		Assert.assertTrue(text.contains("00:45"));
		Assert.assertFalse(text.contains("Floor"));
		Assert.assertFalse(text.contains("01:20"));
		Assert.assertEquals(2, view.getPoints().size());
		Assert.assertEquals("Start", view.getPoints().get(0).getLabel());
		Assert.assertEquals(0, view.getPoints().get(0).getAheadSeconds());
		Assert.assertEquals("Tekton", view.getPoints().get(1).getLabel());
		Assert.assertEquals(20, view.getPoints().get(1).getAheadSeconds());
	}

	@Test
	public void paceLineSkipsMageHandAndEndsOnThePersonalBest()
	{
		CoxRaidRecord older = new CoxRaidRecord();
		older.setChallengeMode(true);
		older.setTeamSize(1);
		older.setPersonalPoints(50000);
		older.setTotalSeconds(640);
		older.putSplit("Tekton", 60);
		older.putSplit("Olm mage hand phase 1", 40);
		older.putSplit("Olm phase 1", 90);
		older.putSplit("Olm", 480);
		older.putSplit("Raid Completed", 640);

		CoxRaidRecord recent = new CoxRaidRecord();
		recent.setChallengeMode(true);
		recent.setTeamSize(1);
		recent.setPersonalPoints(52000);
		recent.setTotalSeconds(630);
		recent.putSplit("Tekton", 60);
		recent.putSplit("Olm mage hand phase 1", 50);
		recent.putSplit("Olm phase 1", 100);
		recent.putSplit("Olm", 480);
		recent.putSplit("Raid Completed", 630);

		RaidReportFormatter.PaceComparison view = RaidReportFormatter.paceView(
			Arrays.asList(older, recent),
			RaidModeFilter.CM,
			RaidSizeFilter.SOLO,
			ReportOptions.defaults(10),
			null,
			-1,
			false
		);
		java.util.Map<String, Integer> ahead = new java.util.LinkedHashMap<>();
		for (int i = 0; i < view.getPoints().size(); i++)
		{
			RaidReportFormatter.PacePoint point = view.getPoints().get(i);
			ahead.put(point.getLabel(), point.getAheadSeconds());
		}
		Assert.assertFalse(ahead.containsKey("mage hand p1"));
		Assert.assertEquals(Integer.valueOf(-10), ahead.get("Olm phase 1"));
		Assert.assertEquals(Integer.valueOf(0), ahead.get("Olm"));
		Assert.assertEquals(Integer.valueOf(10), ahead.get("Finish"));
		Assert.assertFalse(ahead.containsKey("Between rooms"));
	}

	@Test
	public void paceLineKeepsTheAverageAsTheCenterAndThePersonalBestAsZero()
	{
		CoxRaidRecord slow = timedRaid(80, 1500);
		CoxRaidRecord personalBest = timedRaid(60, 1200);
		CoxRaidRecord recent = timedRaid(50, 1200);

		RaidReportFormatter.PaceComparison view = RaidReportFormatter.paceView(
			Arrays.asList(slow, personalBest, recent),
			RaidModeFilter.CM,
			RaidSizeFilter.SOLO,
			ReportOptions.defaults(10),
			null,
			-1,
			false
		);
		java.util.Map<String, Integer> ahead = new java.util.LinkedHashMap<>();
		for (int i = 0; i < view.getPoints().size(); i++)
		{
			RaidReportFormatter.PacePoint point = view.getPoints().get(i);
			ahead.put(point.getLabel(), point.getAheadSeconds());
		}
		Assert.assertEquals(Integer.valueOf(-150), ahead.get("Start"));
		Assert.assertEquals(Integer.valueOf(-130), ahead.get("Tekton"));
		Assert.assertEquals(Integer.valueOf(0), ahead.get("Finish"));
	}

	@Test
	public void purpleTabColorsTheRate()
	{
		CoxRaidRecord raid = raid(true, 1, 50000, 68, "Twisted bow");
		String text = RaidReportFormatter.purpleView(
			Collections.singletonList(raid),
			ReportOptions.defaults(10)
		);
		Assert.assertTrue(text.contains(ReportColor.GREEN));
		Assert.assertTrue(text.contains("Twisted bow"));
		Assert.assertTrue(text.contains("DHCB"));
		Assert.assertFalse(text.contains("Dragon hunter crossbow"));
		Assert.assertTrue(text.indexOf("Avg points") < text.indexOf("All points"));
		Assert.assertTrue(text.contains("All points  50000"));
		Assert.assertFalse(text.contains("Total points"));
		Assert.assertTrue(text.contains("  Regular   " + ReportColor.CYAN + "0"));
		Assert.assertTrue(text.contains("  CM        " + ReportColor.GOLD + "1"));
		Assert.assertTrue(text.contains("Purples: 1"));
		Assert.assertTrue(text.contains("' = every "));
		Assert.assertTrue(text.contains(ReportColor.GOLD + "@"));
		Assert.assertTrue(text.contains("next raid"));
		Assert.assertTrue(text.contains("Summary\n"));
		Assert.assertTrue(text.contains("Items\n"));
		Assert.assertTrue(text.contains("History\n"));
		Assert.assertTrue(text.contains("Tracked items\n"));
		Assert.assertFalse(text.contains("Purple Summary"));
		Assert.assertFalse(text.contains("Purple History"));
		Assert.assertFalse(text.contains("Tracked purples"));
		int averageDry = text.indexOf("Average dry streak: ");
		Assert.assertTrue(averageDry >= 0);
		Assert.assertFalse(text.substring(averageDry, text.indexOf('\n', averageDry)).contains("."));
		Assert.assertTrue(text.matches("(?s).*[+-]\\d+\\.\\d{2}.*"));
		Assert.assertFalse(text.contains(" | Purples:"));
		Assert.assertFalse(text.contains("------------------------------------------------"));

		CoxRaidRecord regular = raid(false, 1, 30000, 50, "Elder maul");
		String both = RaidReportFormatter.purpleView(Arrays.asList(raid, regular), ReportOptions.defaults(10));
		Assert.assertTrue(both.contains("Twisted bow"));
		Assert.assertTrue(both.contains("Elder maul"));
		Assert.assertTrue(both.contains("All points  80000"));
		Assert.assertTrue(both.contains("  Regular   " + ReportColor.CYAN + "1"));
		Assert.assertTrue(both.contains("  CM        " + ReportColor.GOLD + "1"));
	}

	@Test
	public void purpleBoardMatchesTheSidebarCounts()
	{
		CoxRaidRecord raid = raid(true, 1, 50000, 68, "Twisted bow");
		CoxRaidRecord regular = raid(false, 1, 30000, 50, "Elder maul");
		com.coxgrind.report.PurpleBoard board = RaidReportFormatter.purpleBoard(Arrays.asList(raid, regular));
		Assert.assertEquals(2, board.getRaids());
		Assert.assertEquals(1, board.getRegular());
		Assert.assertEquals(1, board.getChallengeMode());
		Assert.assertEquals(2, board.getActual());
		Assert.assertEquals(80000L, board.getAllPoints());
		Assert.assertEquals(com.coxgrind.report.PurpleBoard.Mark.PURPLE, board.getMarks().get(0));
		Assert.assertEquals(com.coxgrind.report.PurpleBoard.Mark.NEXT, board.getMarks().get(board.getMarks().size() - 1));
		Assert.assertEquals("Elder maul", board.getTracked().get(0).getItem());
		Assert.assertEquals("Twisted bow", board.getTracked().get(1).getItem());
		int bow = -1;
		for (int i = 0; i < board.getItems().size(); i++)
		{
			if ("Twisted bow".equals(board.getItems().get(i).getName()))
			{
				bow = board.getItems().get(i).getGot();
			}
			Assert.assertFalse(board.getItems().get(i).getName().isEmpty());
		}
		Assert.assertEquals(1, bow);
	}

	@Test
	public void petKitAndDustStayOutOfThePurpleCounts()
	{
		CoxRaidRecord raid = raid(true, 1, 50000, 68, "Twisted bow");
		raid.addExtra("Olmlet");
		raid.addExtra("Metamorphic dust");
		CoxRaidRecord dry = raid(true, 1, 40000, 70, "");
		dry.addExtra("Twisted ancestral colour kit");
		com.coxgrind.report.PurpleBoard board = RaidReportFormatter.purpleBoard(Arrays.asList(raid, dry));
		Assert.assertEquals(1, board.getActual());
		Assert.assertEquals(com.coxgrind.report.PurpleBoard.Mark.PURPLE, board.getMarks().get(0));
		Assert.assertEquals("pet", board.getSideKinds().get(0));
		Assert.assertEquals(com.coxgrind.report.PurpleBoard.Mark.DRY, board.getMarks().get(1));
		Assert.assertEquals("kit", board.getSideKinds().get(1));
		Assert.assertEquals(1, board.getSideItems().get(0).getGot());
		Assert.assertEquals("Olmlet", board.getSideItems().get(0).getName());
		Assert.assertEquals(1, board.getSideItems().get(1).getGot());
		Assert.assertEquals(1, board.getSideItems().get(2).getGot());
		Assert.assertEquals("kit", board.getTracked().get(0).getKind());
		Assert.assertEquals("Twisted bow", board.getTracked().get(1).getItem());
		Assert.assertEquals("", board.getTracked().get(1).getKind());
		Assert.assertEquals("pet", board.getTracked().get(2).getKind());
		Assert.assertEquals("dust", board.getTracked().get(3).getKind());
	}

	@Test
	public void iceOverThreeFiftyIsMilkingUntilFourThirty()
	{
		Assert.assertTrue(RaidReportFormatter.validTime("Ice demon", 230));
		Assert.assertFalse(RaidReportFormatter.validTime("Ice demon", 231));
		Assert.assertTrue(RaidReportFormatter.validTime("Ice demon", 231, true));
		Assert.assertTrue(RaidReportFormatter.validTime("Ice demon", 270, true));
		Assert.assertFalse(RaidReportFormatter.validTime("Ice demon", 271, true));

		CoxRaidRecord normal = raid(false, 1, 30000, 40, "");
		normal.setKc(1);
		normal.putSplit("Ice demon", 120);
		CoxRaidRecord milking = raid(false, 1, 30000, 40, "");
		milking.setKc(2);
		milking.putSplit("Ice demon", 240);

		String off = RaidReportFormatter.format(Arrays.asList(normal, milking), RaidModeFilter.ALL, RaidSizeFilter.ALL, 10);
		String iceOff = lineStarting(off, "Ice demon");
		Assert.assertTrue(iceOff.contains("02:00"));
		Assert.assertFalse(iceOff.contains("03:00"));
		Assert.assertTrue(off.contains("ice milking"));
		Assert.assertTrue(off.indexOf("Account Breakdown") < off.indexOf("Discarded outliers"));

		ReportOptions on = ReportOptions.defaults(10);
		on.getTargetStyle().setIceMilking(true);
		String milkingOn = RaidReportFormatter.format(Arrays.asList(normal, milking), RaidModeFilter.ALL, RaidSizeFilter.ALL, on);
		String iceOn = lineStarting(milkingOn, "Ice demon");
		Assert.assertTrue(iceOn.contains("02:00"));
		Assert.assertTrue(iceOn.contains("03:00"));
		Assert.assertFalse(milkingOn.contains("ice milking"));
	}

	@Test
	public void purpleSectionIgnoresTheTimeFilterAndColorsTheDifference()
	{
		CoxRaidRecord regular = raid(false, 1, 30000, 50, "Twisted bow");
		regular.setKc(10);
		CoxRaidRecord cm = raid(true, 1, 60000, 70, "");
		cm.setKc(4);
		String report = RaidReportFormatter.format(
			Arrays.asList(regular, cm),
			RaidModeFilter.CM,
			RaidSizeFilter.SOLO,
			10
		);
		Assert.assertTrue(report.contains("Analyzing all solo CM raids (1 raids)"));
		Assert.assertTrue(report.contains("Purple numbers use every logged raid"));
		Assert.assertTrue(report.contains("Logged raids"));
		int difference = report.indexOf("Difference");
		Assert.assertTrue(difference > 0);
		String diffLine = report.substring(difference, report.indexOf('\n', difference));
		Assert.assertTrue(diffLine.contains("\u001B["));
		Assert.assertTrue(report.contains("Account Breakdown"));
		Assert.assertTrue(report.contains("Regular logged"));
		Assert.assertTrue(report.contains("CM logged"));
		Assert.assertTrue(report.indexOf("Purple Summary") < report.indexOf("Account Breakdown"));
	}

	@Test
	public void bestSplitsUsesTheFastestValidRoom()
	{
		CoxRaidRecord slow = raid(true, 1, 60000, 80, "");
		slow.setKc(3);
		CoxRaidRecord fast = raid(true, 1, 60000, 50, "");
		fast.setKc(9);
		CoxRaidRecord paced = raid(true, 1, 61000, 60, "");
		paced.setKc(4);
		paced.setTotalSeconds(60 + 8 * 60 + 40);
		String text = RaidReportFormatter.bestSplits(
			Arrays.asList(slow, fast, paced),
			RaidModeFilter.CM,
			RaidSizeFilter.SOLO,
			ReportOptions.defaults(10)
		);
		Assert.assertFalse(text.contains("Best splits"));
		Assert.assertTrue(text.contains("Tekton"));
		String tekton = lineStarting(text, "Tekton");
		int pad = 0;
		for (int i = "Tekton".length(); i < tekton.length() && tekton.charAt(i) == ' '; i++)
		{
			pad++;
		}
		Assert.assertEquals(10, pad);
		Assert.assertTrue(text.contains(ReportColor.GOLD));
		Assert.assertTrue(text.contains("00:50"));
		Assert.assertTrue(text.contains("CM 9"));
		Assert.assertTrue(text.contains("Between rooms"));
		Assert.assertTrue(text.contains("00:40"));
		Assert.assertTrue(text.contains("Total Points"));
		Assert.assertTrue(text.contains("61000"));
		Assert.assertTrue(text.contains("PPH"));
		Assert.assertFalse(text.contains("00:80") || text.contains("01:20"));
		int preOlm = text.indexOf("Pre-Olm");
		Assert.assertTrue(preOlm >= 0);
		Assert.assertTrue(text.substring(text.indexOf('\n', preOlm) + 1).startsWith("-----------------------------"));
		int completed = text.indexOf("Raid Completed");
		Assert.assertTrue(completed >= 0);
		int rule = text.indexOf('\n', completed);
		Assert.assertTrue(text.substring(rule + 1).startsWith("-----------------------------"));
		int between = text.indexOf("Between rooms");
		Assert.assertTrue(text.substring(text.indexOf('\n', between) + 1).startsWith("-----------------------------"));
	}

	private static String lineStarting(String report, String label)
	{
		int at = report.indexOf(label);
		Assert.assertTrue(at >= 0);
		int end = report.indexOf('\n', at);
		return report.substring(at, end);
	}

	private static CoxRaidRecord timedRaid(int tektonSeconds, int totalSeconds)
	{
		CoxRaidRecord raid = new CoxRaidRecord();
		raid.setChallengeMode(true);
		raid.setTeamSize(1);
		raid.setPersonalPoints(50000);
		raid.setTotalSeconds(totalSeconds);
		raid.putSplit("Tekton", tektonSeconds);
		raid.putSplit("Olm", 8 * 60);
		raid.putSplit("Raid Completed", totalSeconds);
		return raid;
	}

	private static CoxRaidRecord raid(boolean cm, int team, int points, int tektonSeconds, String purple)
	{
		CoxRaidRecord raid = new CoxRaidRecord();
		raid.setId("id");
		raid.setPlayerName("Local Player");
		raid.setChallengeMode(cm);
		raid.setTeamSize(team);
		raid.setPersonalPoints(points);
		raid.setTeamPoints(points);
		raid.setTotalSeconds(20 * 60);
		raid.setPurple(purple);
		raid.getSplits().add(new RoomSplit("Tekton", tektonSeconds));
		raid.getSplits().add(new RoomSplit("Olm", 8 * 60));
		raid.getSplits().add(new RoomSplit("Raid Completed", 20 * 60));
		return raid;
	}
}
