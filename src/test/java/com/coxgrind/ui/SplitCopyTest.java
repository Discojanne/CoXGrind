package com.coxgrind.ui;

import com.coxgrind.report.RaidReportFormatter;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class SplitCopyTest
{
	@Test
	public void discordPasteDropsDiffsAndGroupsTheList()
	{
		String text = ActiveTimes.discordSplits(Arrays.asList(
			row("Tekton", "01:08", "-00:02", -2, false),
			row("Pre-Olm", "16:40", "+00:10", 10, false),
			row("mage hand p1", "00:56", "00:00", 0, false),
			row("Olm phase 1", "01:53", "-00:04", -4, false),
			row("Olm", "08:13", "-00:20", -20, false),
			row("Raid Completed", "28:34", "-01:00", -60, false),
			row("Between rooms", "00:56", "--", null, false),
			row("Total Points", "64226", "+120", 120, true),
			row("PPH", "135000", "+500", 500, true)
		), "CM 221");

		Assert.assertEquals(
			"```\n"
				+ "CM 221\n"
				+ "Tekton            1:08\n"
				+ "Pre-Olm          16:40\n"
				+ "\n"
				+ "Mage P1           0:56\n"
				+ "Olm P1            1:53\n"
				+ "Olm               8:13\n"
				+ "\n"
				+ "Raid Completed   28:34\n"
				+ "Between rooms     0:56\n"
				+ "Total Points     64226\n"
				+ "PPH             135000\n"
				+ "```",
			text
		);
	}

	@Test
	public void bestSplitsKeepEachKillCount()
	{
		String text = ActiveTimes.discordSplits(Arrays.asList(
			new RaidReportFormatter.TimeRow("Tekton", "01:08", "CM 214", null, false, false, null),
			new RaidReportFormatter.TimeRow("Crabs", "00:52", "KC 90", null, false, false, null)
		), "Splits");

		Assert.assertEquals(
			"```\n"
				+ "Splits\n"
				+ "Tekton  1:08  CM 214\n"
				+ "Crabs   0:52  KC 90\n"
				+ "```",
			text
		);
	}

	@Test
	public void openSplitIsCopiedWithoutAComparison()
	{
		String text = ActiveTimes.discordSplits(Arrays.asList(
			row("Tekton", "01:08", "-00:02", -2, false),
			new RaidReportFormatter.TimeRow("Current", "04:12", "", null, false, true, null)
		), "In progress");

		Assert.assertEquals(
			"```\n"
				+ "In progress\n"
				+ "Tekton   1:08\n"
				+ "Current  4:12\n"
				+ "```",
			text
		);
	}

	@Test
	public void emptyListCopiesNothing()
	{
		Assert.assertEquals("", ActiveTimes.discordSplits(Arrays.asList(), ""));
		Assert.assertEquals("", ActiveTimes.discordSplits(null, "CM 1"));
	}

	private static RaidReportFormatter.TimeRow row(String label, String value, String diff, Integer delta, boolean points)
	{
		return new RaidReportFormatter.TimeRow(label, value, diff, delta, points, false, null);
	}
}
