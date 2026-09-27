package com.coxgrind;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("coxgrind")
public interface CoxGrindConfig extends Config
{
	@ConfigSection(
		name = "Full report",
		description = "Columns and extra tables in the full report.",
		position = 50,
		closedByDefault = true
	)
	String reportSection = "reportSection";

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "lastRaids",
		name = "Last N raids",
		description = "How many recent raids to average in the Last N column of the report.",
		section = "reportSection",
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
		section = "reportSection",
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
		section = "reportSection",
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
		section = "reportSection",
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
		section = "reportSection",
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
		section = "reportSection",
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
		section = "reportSection",
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
		section = "reportSection",
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
		section = "reportSection",
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
		section = "reportSection",
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
		section = "reportSection",
		position = 10
	)
	default boolean showOutliers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "useTbow",
		name = "Using twisted bow",
		description = "On keeps those room targets as entered. Off adds Shamans 6s, Vanguards 2s, Tightrope 8s when killing rope is on, Vasa 30s, Muttadiles 10s, and Olm head 20s.",
		position = 0
	)
	default boolean useTbow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackVanguards",
		name = "Track Vanguards",
		description = "Logs each Challenge Mode Vanguard room to a vanguards file for later study. The raid log is unchanged. Nothing is drawn in game.",
		position = 1
	)
	default boolean trackVanguards()
	{
		return false;
	}

	@ConfigSection(
		name = "Items",
		description = "Untick an item you are not bringing. That adds time to the room.",
		position = 20,
		closedByDefault = false
	)
	String itemSection = "itemSection";

	@ConfigSection(
		name = "Strategies",
		description = "Untick a method you are not using. That adds time to the room.",
		position = 21,
		closedByDefault = false
	)
	String strategySection = "strategySection";

	@ConfigSection(
		name = "Money run",
		description = "Tick a slower method. That adds time to the room.",
		position = 22,
		closedByDefault = false
	)
	String moneyRunSection = "moneyRunSection";

	@ConfigItem(
		keyName = "slayerHelm",
		name = "Slayer helm",
		description = "On keeps Shamans as entered. Off adds 9 seconds.",
		section = "itemSection",
		position = 0
	)
	default boolean slayerHelm()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lockpick",
		name = "Lockpick",
		description = "On keeps Thieving as entered. Off adds 4 seconds.",
		section = "itemSection",
		position = 1
	)
	default boolean lockpick()
	{
		return true;
	}

	@ConfigItem(
		keyName = "axe",
		name = "Axe",
		description = "On keeps Ice demon as entered. Off adds 5 seconds.",
		section = "itemSection",
		position = 2
	)
	default boolean axe()
	{
		return true;
	}

	@ConfigItem(
		keyName = "salve",
		name = "Salve",
		description = "On keeps Mystics as entered. Off adds 5 seconds.",
		section = "itemSection",
		position = 3
	)
	default boolean salve()
	{
		return true;
	}

	@ConfigItem(
		keyName = "preVeng",
		name = "Pre-veng",
		description = "On keeps Tekton as entered. Off adds 4 seconds.",
		section = "strategySection",
		position = 0
	)
	default boolean preVeng()
	{
		return true;
	}

	@ConfigItem(
		keyName = "vespPotSkip",
		name = "Vesp pot skip",
		description = "On keeps Tightrope as entered. Off adds 6 seconds.",
		section = "strategySection",
		position = 1
	)
	default boolean vespPotSkip()
	{
		return true;
	}

	@ConfigItem(
		keyName = "crabTank",
		name = "Crab tank",
		description = "On keeps Crabs as entered. Off adds 4 seconds.",
		section = "strategySection",
		position = 2
	)
	default boolean crabTank()
	{
		return true;
	}

	@ConfigItem(
		keyName = "killRope",
		name = "Killing rope",
		description = "Off leaves Tightrope as entered. On adds 59 seconds.",
		section = "moneyRunSection",
		position = 0
	)
	default boolean killRope()
	{
		return false;
	}

	@ConfigItem(
		keyName = "milkVespula",
		name = "Vesp milk",
		description = "Off leaves Vespula as entered. On adds 18 seconds.",
		section = "moneyRunSection",
		position = 1
	)
	default boolean milkVespula()
	{
		return false;
	}

	@ConfigItem(
		keyName = "iceMilking",
		name = "Ice milk",
		description = "On adds the ice milk seconds to Ice demon, and includes Ice demon splits over 3:50, up to 4:30.",
		section = "moneyRunSection",
		position = 2
	)
	default boolean iceMilking()
	{
		return false;
	}

	@Range(min = 0, max = 600)
	@ConfigItem(
		keyName = "iceMilkSeconds",
		name = "Ice milk seconds",
		description = "Seconds added to the Ice demon target when ice milk is on.",
		section = "moneyRunSection",
		position = 3
	)
	default int iceMilkSeconds()
	{
		return 70;
	}

	@ConfigItem(
		keyName = "overThieve",
		name = "Over-thieve",
		description = "Off leaves Thieving as entered. On adds 7 seconds.",
		section = "moneyRunSection",
		position = 4
	)
	default boolean overThieve()
	{
		return false;
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

	@ConfigItem(keyName = "regOlmMage1", name = "Olm mage hand phase 1", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 12)
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

	@ConfigItem(keyName = "regBetweenRooms", name = "Between room time", description = "MM:SS. Blank means no target.", section = "regularTargets", position = 19)
	default String regBetweenRooms() { return ""; }

	@ConfigItem(keyName = "regTotalPoints", name = "Total Points", description = "Whole number. Blank means no target.", section = "regularTargets", position = 22)
	default String regTotalPoints() { return ""; }

	@ConfigItem(keyName = "regPph", name = "PPH", description = "Whole number. Blank means no target.", section = "regularTargets", position = 23)
	default String regPph() { return ""; }

	@ConfigItem(keyName = "cmTekton", name = "Tekton", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 0)
	default String cmTekton() { return "1:10"; }

	@ConfigItem(keyName = "cmCrabs", name = "Crabs", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 1)
	default String cmCrabs() { return "0:56"; }

	@ConfigItem(keyName = "cmIceDemon", name = "Ice demon", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 2)
	default String cmIceDemon() { return "2:24"; }

	@ConfigItem(keyName = "cmShamans", name = "Shamans", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 3)
	default String cmShamans() { return "1:03"; }

	@ConfigItem(keyName = "cmVanguards", name = "Vanguards", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 4)
	default String cmVanguards() { return "2:12"; }

	@ConfigItem(keyName = "cmThieving", name = "Thieving", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 5)
	default String cmThieving() { return "1:15"; }

	@ConfigItem(keyName = "cmVespula", name = "Vespula", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 6)
	default String cmVespula() { return "0:57"; }

	@ConfigItem(keyName = "cmTightrope", name = "Tightrope", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 7)
	default String cmTightrope() { return "0:47"; }

	@ConfigItem(keyName = "cmGuardians", name = "Guardians", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 8)
	default String cmGuardians() { return "1:47"; }

	@ConfigItem(keyName = "cmVasa", name = "Vasa", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 9)
	default String cmVasa() { return "1:10"; }

	@ConfigItem(keyName = "cmMystics", name = "Mystics", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 10)
	default String cmMystics() { return "1:40"; }

	@ConfigItem(keyName = "cmMuttadiles", name = "Muttadiles", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 11)
	default String cmMuttadiles() { return "1:25"; }

	@ConfigItem(keyName = "cmOlmMage1", name = "Olm mage hand phase 1", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 12)
	default String cmOlmMage1() { return "0:56"; }

	@ConfigItem(keyName = "cmOlmPhase1", name = "Olm phase 1", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 14)
	default String cmOlmPhase1() { return "1:53"; }

	@ConfigItem(keyName = "cmOlmMage2", name = "Olm mage hand phase 2", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 15)
	default String cmOlmMage2() { return "0:56"; }

	@ConfigItem(keyName = "cmOlmPhase2", name = "Olm phase 2", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 16)
	default String cmOlmPhase2() { return "1:53"; }

	@ConfigItem(keyName = "cmOlmPhase3", name = "Olm phase 3", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 17)
	default String cmOlmPhase3() { return "1:55"; }

	@ConfigItem(keyName = "cmOlmHead", name = "Olm head", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 18)
	default String cmOlmHead() { return "1:06"; }

	@ConfigItem(keyName = "cmBetweenRooms", name = "Between room time", description = "MM:SS. Blank means no target.", section = "cmTargets", position = 19)
	default String cmBetweenRooms() { return "0:56"; }

	@ConfigItem(keyName = "cmTotalPoints", name = "Total Points", description = "Whole number. Blank means no target.", section = "cmTargets", position = 22)
	default String cmTotalPoints() { return "63750"; }

	@ConfigItem(keyName = "cmPph", name = "PPH", description = "Whole number. Blank means no target.", section = "cmTargets", position = 23)
	default String cmPph() { return "130000"; }
}
