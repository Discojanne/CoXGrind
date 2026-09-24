package com.coxgrind;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("coxgrind")
public interface CoxGrindConfig extends Config
{
	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "lastRaids",
		name = "Last N raids",
		description = "How many recent raids to average in the Last N column of the report.",
		position = 0
	)
	default int lastRaids()
	{
		return 10;
	}

	@Range(min = 0, max = 10000)
	@ConfigItem(
		keyName = "reportRaids",
		name = "Raids in report",
		description = "0 uses every raid in the filter. Any other number uses only that many newest raids.",
		position = 1
	)
	default int reportRaids()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "deathFullRegular",
		name = "Death: full regular",
		description = "A solo full-layout regular raid under this many personal points counts as a death.",
		position = 2
	)
	default int deathFullRegular()
	{
		return 48000;
	}

	@ConfigItem(
		keyName = "deathRegular",
		name = "Death: regular",
		description = "A solo regular raid that is not a full layout counts as a death under this many personal points.",
		position = 3
	)
	default int deathRegular()
	{
		return 29000;
	}

	@ConfigItem(
		keyName = "deathCmSolo",
		name = "Death: CM solo",
		description = "A solo Challenge Mode raid under this many personal points counts as a death.",
		position = 4
	)
	default int deathCmSolo()
	{
		return 60000;
	}

	@ConfigItem(
		keyName = "deathCmTeam",
		name = "Death: CM team",
		description = "A team Challenge Mode raid under this many personal points counts as a death.",
		position = 5
	)
	default int deathCmTeam()
	{
		return 40000;
	}

	@ConfigItem(
		keyName = "showPurpleSummary",
		name = "Purple summary",
		description = "Show the purple summary, item table, history, and the death sections at the bottom of the full report.",
		position = 6
	)
	default boolean showPurpleSummary()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTrackedPurples",
		name = "Tracked purples",
		description = "List your purples, newest first.",
		position = 7
	)
	default boolean showTrackedPurples()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showRoomEfficiency",
		name = "Room efficiency",
		description = "Show average points per hour for each prep room.",
		position = 8
	)
	default boolean showRoomEfficiency()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showCommonRooms",
		name = "Common rooms",
		description = "Show which prep rooms you see most, and how often a raid has 5 or 6 rooms.",
		position = 9
	)
	default boolean showCommonRooms()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showOutliers",
		name = "Discarded splits",
		description = "List splits left out of the averages.",
		position = 10
	)
	default boolean showOutliers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "useTbow",
		name = "Using twisted bow",
		description = "On keeps those room targets as entered. Off shortens them by the no-twisted-bow seconds below.",
		position = 11
	)
	default boolean useTbow()
	{
		return true;
	}

	@Range(min = 0, max = 600)
	@ConfigItem(
		keyName = "noTbowVanguards",
		name = "No tbow: Vanguards",
		description = "Seconds removed from the Vanguards target when twisted bow is off.",
		position = 12
	)
	default int noTbowVanguards()
	{
		return 15;
	}

	@Range(min = 0, max = 600)
	@ConfigItem(
		keyName = "noTbowVasa",
		name = "No tbow: Vasa",
		description = "Seconds removed from the Vasa target when twisted bow is off.",
		position = 13
	)
	default int noTbowVasa()
	{
		return 20;
	}

	@Range(min = 0, max = 600)
	@ConfigItem(
		keyName = "noTbowMystics",
		name = "No tbow: Mystics",
		description = "Seconds removed from the Mystics target when twisted bow is off.",
		position = 14
	)
	default int noTbowMystics()
	{
		return 25;
	}

	@Range(min = 0, max = 600)
	@ConfigItem(
		keyName = "noTbowMuttadiles",
		name = "No tbow: Muttadiles",
		description = "Seconds removed from the Muttadiles target when twisted bow is off.",
		position = 15
	)
	default int noTbowMuttadiles()
	{
		return 20;
	}

	@Range(min = 0, max = 600)
	@ConfigItem(
		keyName = "noTbowTightrope",
		name = "No tbow: Tightrope",
		description = "Seconds removed from Tightrope when twisted bow is off and killing rope is on.",
		position = 16
	)
	default int noTbowTightrope()
	{
		return 10;
	}

	@Range(min = 0, max = 600)
	@ConfigItem(
		keyName = "noTbowOlmHead",
		name = "No tbow: Olm head",
		description = "Seconds removed from Olm head when twisted bow is off.",
		position = 17
	)
	default int noTbowOlmHead()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "iceMilking",
		name = "Ice milking",
		description = "On adds the ice milking seconds to the target, and includes Ice demon splits over 3:50, up to 4:30.",
		position = 18
	)
	default boolean iceMilking()
	{
		return false;
	}

	@Range(min = 0, max = 600)
	@ConfigItem(
		keyName = "iceMilkSeconds",
		name = "Ice milking seconds",
		description = "Seconds added to the Ice demon target when ice milking is on.",
		position = 19
	)
	default int iceMilkSeconds()
	{
		return 70;
	}

	@ConfigItem(
		keyName = "killRope",
		name = "Killing rope",
		description = "Off leaves Tightrope as entered. On adds the killing rope seconds.",
		position = 20
	)
	default boolean killRope()
	{
		return false;
	}

	@Range(min = 0, max = 600)
	@ConfigItem(
		keyName = "killRopeSeconds",
		name = "Killing rope seconds",
		description = "Seconds added to the Tightrope target when killing rope is on.",
		position = 21
	)
	default int killRopeSeconds()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "milkVespula",
		name = "Milking Vespula",
		description = "Off leaves Vespula as entered. On adds the Vespula milking seconds.",
		position = 22
	)
	default boolean milkVespula()
	{
		return false;
	}

	@Range(min = 0, max = 600)
	@ConfigItem(
		keyName = "milkVespulaSeconds",
		name = "Vespula milking seconds",
		description = "Seconds added to the Vespula target when milking Vespula is on.",
		position = 23
	)
	default int milkVespulaSeconds()
	{
		return 10;
	}

	@ConfigSection(
		name = "Regular targets",
		description = "Benchmark times for regular raids. Times are MM:SS. Points and PPH are whole numbers. Blank means no target.",
		position = 40,
		closedByDefault = true
	)
	String regularTargets = "regularTargets";

	@ConfigSection(
		name = "CM targets",
		description = "Benchmark times for Challenge Mode. Times are MM:SS. Points and PPH are whole numbers. Blank means no target.",
		position = 41,
		closedByDefault = true
	)
	String cmTargets = "cmTargets";

	@ConfigItem(keyName = "regTekton", name = "Tekton", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 0)
	default String regTekton() { return ""; }

	@ConfigItem(keyName = "regCrabs", name = "Crabs", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 1)
	default String regCrabs() { return ""; }

	@ConfigItem(keyName = "regIceDemon", name = "Ice demon", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 2)
	default String regIceDemon() { return ""; }

	@ConfigItem(keyName = "regShamans", name = "Shamans", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 3)
	default String regShamans() { return ""; }

	@ConfigItem(keyName = "regVanguards", name = "Vanguards", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 4)
	default String regVanguards() { return ""; }

	@ConfigItem(keyName = "regThieving", name = "Thieving", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 5)
	default String regThieving() { return ""; }

	@ConfigItem(keyName = "regVespula", name = "Vespula", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 6)
	default String regVespula() { return ""; }

	@ConfigItem(keyName = "regTightrope", name = "Tightrope", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 7)
	default String regTightrope() { return ""; }

	@ConfigItem(keyName = "regGuardians", name = "Guardians", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 8)
	default String regGuardians() { return ""; }

	@ConfigItem(keyName = "regVasa", name = "Vasa", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 9)
	default String regVasa() { return ""; }

	@ConfigItem(keyName = "regMystics", name = "Mystics", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 10)
	default String regMystics() { return ""; }

	@ConfigItem(keyName = "regMuttadiles", name = "Muttadiles", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 11)
	default String regMuttadiles() { return ""; }

	@ConfigItem(keyName = "regPreOlm", name = "Pre-Olm", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 12)
	default String regPreOlm() { return ""; }

	@ConfigItem(keyName = "regOlmMage1", name = "Olm mage hand phase 1", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 13)
	default String regOlmMage1() { return ""; }

	@ConfigItem(keyName = "regOlmPhase1", name = "Olm phase 1", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 14)
	default String regOlmPhase1() { return ""; }

	@ConfigItem(keyName = "regOlmMage2", name = "Olm mage hand phase 2", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 15)
	default String regOlmMage2() { return ""; }

	@ConfigItem(keyName = "regOlmPhase2", name = "Olm phase 2", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 16)
	default String regOlmPhase2() { return ""; }

	@ConfigItem(keyName = "regOlmPhase3", name = "Olm phase 3", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 17)
	default String regOlmPhase3() { return ""; }

	@ConfigItem(keyName = "regOlmHead", name = "Olm head", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 18)
	default String regOlmHead() { return ""; }

	@ConfigItem(keyName = "regOlm", name = "Olm", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 19)
	default String regOlm() { return ""; }

	@ConfigItem(keyName = "regRaidCompleted", name = "Raid Completed", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 20)
	default String regRaidCompleted() { return ""; }

	@ConfigItem(keyName = "regBetweenRooms", name = "Between room time", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 21)
	default String regBetweenRooms() { return ""; }

	@ConfigItem(keyName = "regTotalPoints", name = "Total Points", description = "Whole number. Blank means no target.", section = "regularTargets", position = 22)
	default String regTotalPoints() { return ""; }

	@ConfigItem(keyName = "regPph", name = "PPH", description = "Whole number. Blank means no target.", section = "regularTargets", position = 23)
	default String regPph() { return ""; }

	@ConfigItem(keyName = "cmTekton", name = "Tekton", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 0)
	default String cmTekton() { return "1:08"; }

	@ConfigItem(keyName = "cmCrabs", name = "Crabs", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 1)
	default String cmCrabs() { return "0:57"; }

	@ConfigItem(keyName = "cmIceDemon", name = "Ice demon", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 2)
	default String cmIceDemon() { return "2:19"; }

	@ConfigItem(keyName = "cmShamans", name = "Shamans", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 3)
	default String cmShamans() { return "1:02"; }

	@ConfigItem(keyName = "cmVanguards", name = "Vanguards", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 4)
	default String cmVanguards() { return "2:18"; }

	@ConfigItem(keyName = "cmThieving", name = "Thieving", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 5)
	default String cmThieving() { return "1:21"; }

	@ConfigItem(keyName = "cmVespula", name = "Vespula", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 6)
	default String cmVespula() { return "1:01"; }

	@ConfigItem(keyName = "cmTightrope", name = "Tightrope", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 7)
	default String cmTightrope() { return "1:00"; }

	@ConfigItem(keyName = "cmGuardians", name = "Guardians", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 8)
	default String cmGuardians() { return "1:52"; }

	@ConfigItem(keyName = "cmVasa", name = "Vasa", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 9)
	default String cmVasa() { return "1:11"; }

	@ConfigItem(keyName = "cmMystics", name = "Mystics", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 10)
	default String cmMystics() { return "1:46"; }

	@ConfigItem(keyName = "cmMuttadiles", name = "Muttadiles", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 11)
	default String cmMuttadiles() { return "1:42"; }

	@ConfigItem(keyName = "cmPreOlm", name = "Pre-Olm", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 12)
	default String cmPreOlm() { return "17:34"; }

	@ConfigItem(keyName = "cmOlmMage1", name = "Olm mage hand phase 1", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 13)
	default String cmOlmMage1() { return "0:56"; }

	@ConfigItem(keyName = "cmOlmPhase1", name = "Olm phase 1", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 14)
	default String cmOlmPhase1() { return "2:00"; }

	@ConfigItem(keyName = "cmOlmMage2", name = "Olm mage hand phase 2", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 15)
	default String cmOlmMage2() { return "0:56"; }

	@ConfigItem(keyName = "cmOlmPhase2", name = "Olm phase 2", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 16)
	default String cmOlmPhase2() { return "2:00"; }

	@ConfigItem(keyName = "cmOlmPhase3", name = "Olm phase 3", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 17)
	default String cmOlmPhase3() { return "2:00"; }

	@ConfigItem(keyName = "cmOlmHead", name = "Olm head", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 18)
	default String cmOlmHead() { return "1:04"; }

	@ConfigItem(keyName = "cmOlm", name = "Olm", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 19)
	default String cmOlm() { return "8:00"; }

	@ConfigItem(keyName = "cmRaidCompleted", name = "Raid Completed", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 20)
	default String cmRaidCompleted() { return "27:04"; }

	@ConfigItem(keyName = "cmBetweenRooms", name = "Between room time", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 21)
	default String cmBetweenRooms() { return "1:19"; }

	@ConfigItem(keyName = "cmTotalPoints", name = "Total Points", description = "Whole number. Blank means no target.", section = "cmTargets", position = 22)
	default String cmTotalPoints() { return "63750"; }

	@ConfigItem(keyName = "cmPph", name = "PPH", description = "Whole number. Blank means no target.", section = "cmTargets", position = 23)
	default String cmPph() { return "130000"; }
}
