package com.coxgrind;

import com.coxgrind.model.TargetSheet;
import com.coxgrind.report.TargetTotals;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("coxgrind")
public interface CoxGrindConfig extends Config
{
	@ConfigItem(
		keyName = "useTbow",
		name = "Using Twisted bow",
		description = "Off adds 6 seconds to Shamans, 2 seconds to Vanguards, 8 seconds to Tightrope when killing rope is on, 30 seconds to Vasa, 10 seconds to Muttadiles, and 20 seconds to Olm head.",
		position = 0
	)
	default boolean useTbow()
	{
		return true;
	}

	@ConfigSection(
		name = "Other",
		description = "Settings that are not part of a raid target.",
		position = 60,
		closedByDefault = true
	)
	String otherSection = "otherSection";

	@ConfigItem(
		keyName = "trackVanguards",
		name = "Track Vanguards",
		description = "Logs each Challenge Mode Vanguard room to a vanguards file for later study. The raid log is unchanged. Nothing is drawn in game.",
		section = "otherSection",
		position = 0
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
		description = "Off adds 9 seconds to Shamans.",
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
		description = "Off adds 4 seconds to Thieving.",
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
		description = "Off adds 5 seconds to Ice demon.",
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
		description = "Off adds 5 seconds to Mystics.",
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
		description = "Off adds 4 seconds to Tekton.",
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
		description = "Off adds 6 seconds to Tightrope.",
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
		description = "Off adds 4 seconds to Crabs.",
		section = "strategySection",
		position = 2
	)
	default boolean crabTank()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sippingPools",
		name = "Sipping pools",
		description = "On adds 12 seconds to Between rooms.",
		section = "strategySection",
		position = 3
	)
	default boolean sippingPools()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bankAfterRope",
		name = "Banking after rope",
		description = "On adds 5 seconds to Between rooms.",
		section = "strategySection",
		position = 4
	)
	default boolean bankAfterRope()
	{
		return false;
	}

	@ConfigItem(
		keyName = "killRope",
		name = "Killing rope",
		description = "On adds 59 seconds to Tightrope.",
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
		description = "On adds 18 seconds to Vespula.",
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
		description = "On adds the ice milk seconds to Ice demon.",
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
		description = "Seconds added to Ice demon when ice milk is on.",
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
		description = "On adds 7 seconds to Thieving.",
		section = "moneyRunSection",
		position = 4
	)
	default boolean overThieve()
	{
		return false;
	}

	@ConfigSection(
		name = "Regular solo",
		description = "Benchmark times for regular solos, including regular full. Times are MM:SS. Points and PPH are whole numbers. Blank means no target.",
		position = 40,
		closedByDefault = true
	)
	String regularSolo = "regularSolo";

	@ConfigSection(
		name = "Regular team",
		description = "Benchmark times for regular teams, including regular full. Times are MM:SS. Points and PPH are whole numbers. Blank means no target.",
		position = 41,
		closedByDefault = true
	)
	String regularTeam = "regularTeam";

	@ConfigSection(
		name = "CM solo",
		description = "Benchmark times for Challenge Mode solos. Times are MM:SS. Points and PPH are whole numbers. Blank means no target.",
		position = 42,
		closedByDefault = true
	)
	String cmSolo = "cmSolo";

	@ConfigSection(
		name = "CM team",
		description = "Benchmark times for Challenge Mode teams. Times are MM:SS. Points and PPH are whole numbers. Blank means no target.",
		position = 43,
		closedByDefault = true
	)
	String cmTeam = "cmTeam";

	@ConfigItem(keyName = "regSoloTotal", name = "Target total", description = "Pre-Olm plus Olm plus between rooms, including strategy adds. Calculated from the boxes below.", section = "regularSolo", position = 1)
	default String regSoloTotal() { return TargetTotals.text(this, TargetSheet.REGULAR_SOLO); }

	@ConfigItem(keyName = "regTekton", name = "Tekton", description = "MM:SS. Blank means no target.", section = "regularSolo", position = 2)
	default String regTekton() { return ""; }

	@ConfigItem(keyName = "regCrabs", name = "Crabs", description = "MM:SS. Blank means no target.", section = "regularSolo", position = 3)
	default String regCrabs() { return ""; }

	@ConfigItem(keyName = "regIceDemon", name = "Ice demon", description = "MM:SS. Blank means no target.", section = "regularSolo", position = 4)
	default String regIceDemon() { return ""; }

	@ConfigItem(keyName = "regShamans", name = "Shamans", description = "MM:SS. Blank means no target.", section = "regularSolo", position = 5)
	default String regShamans() { return ""; }

	@ConfigItem(keyName = "regVanguards", name = "Vanguards", description = "MM:SS. Blank means no target.", section = "regularSolo", position = 6)
	default String regVanguards() { return ""; }

	@ConfigItem(keyName = "regThieving", name = "Thieving", description = "MM:SS. Blank means no target.", section = "regularSolo", position = 7)
	default String regThieving() { return ""; }

	@ConfigItem(keyName = "regVespula", name = "Vespula", description = "MM:SS. Blank means no target.", section = "regularSolo", position = 8)
	default String regVespula() { return ""; }

	@ConfigItem(keyName = "regTightrope", name = "Tightrope", description = "MM:SS. Blank means no target.", section = "regularSolo", position = 9)
	default String regTightrope() { return ""; }

	@ConfigItem(keyName = "regGuardians", name = "Guardians", description = "MM:SS. Blank means no target.", section = "regularSolo", position = 10)
	default String regGuardians() { return ""; }

	@ConfigItem(keyName = "regVasa", name = "Vasa", description = "MM:SS. Blank means no target.", section = "regularSolo", position = 11)
	default String regVasa() { return ""; }

	@ConfigItem(keyName = "regMystics", name = "Mystics", description = "MM:SS. Blank means no target.", section = "regularSolo", position = 12)
	default String regMystics() { return ""; }

	@ConfigItem(keyName = "regMuttadiles", name = "Muttadiles", description = "MM:SS. Blank means no target.", section = "regularSolo", position = 13)
	default String regMuttadiles() { return ""; }

	@ConfigItem(keyName = "regOlmMage", name = "Olm mage hand", description = "MM:SS. Used for every mage hand. The last phase has none. Blank means no target.", section = "regularSolo", position = 14)
	default String regOlmMage() { return ""; }

	@ConfigItem(keyName = "regOlmPhase", name = "Olm phase", description = "MM:SS. Used for every Olm phase. Blank means no target.", section = "regularSolo", position = 16)
	default String regOlmPhase() { return ""; }

	@ConfigItem(keyName = "regOlmHead", name = "Olm head", description = "MM:SS. Blank means no target.", section = "regularSolo", position = 20)
	default String regOlmHead() { return ""; }

	@ConfigItem(keyName = "regBetweenRooms", name = "Between room time", description = "MM:SS. Blank means no target.", section = "regularSolo", position = 21)
	default String regBetweenRooms() { return ""; }

	@ConfigItem(keyName = "regTotalPoints", name = "Total Points", description = "Whole number. Blank means no target.", section = "regularSolo", position = 24)
	default String regTotalPoints() { return ""; }

	@ConfigItem(keyName = "regPph", name = "PPH", description = "Whole number. Blank means no target.", section = "regularSolo", position = 25)
	default String regPph() { return ""; }

	@ConfigItem(keyName = "regTeamTotal", name = "Target total", description = "Pre-Olm plus Olm plus between rooms, including strategy adds. Calculated from the boxes below.", section = "regularTeam", position = 1)
	default String regTeamTotal() { return TargetTotals.text(this, TargetSheet.REGULAR_TEAM); }

	@ConfigItem(keyName = "regTeamTekton", name = "Tekton", description = "MM:SS. Blank means no target.", section = "regularTeam", position = 2)
	default String regTeamTekton() { return ""; }

	@ConfigItem(keyName = "regTeamCrabs", name = "Crabs", description = "MM:SS. Blank means no target.", section = "regularTeam", position = 3)
	default String regTeamCrabs() { return ""; }

	@ConfigItem(keyName = "regTeamIceDemon", name = "Ice demon", description = "MM:SS. Blank means no target.", section = "regularTeam", position = 4)
	default String regTeamIceDemon() { return ""; }

	@ConfigItem(keyName = "regTeamShamans", name = "Shamans", description = "MM:SS. Blank means no target.", section = "regularTeam", position = 5)
	default String regTeamShamans() { return ""; }

	@ConfigItem(keyName = "regTeamVanguards", name = "Vanguards", description = "MM:SS. Blank means no target.", section = "regularTeam", position = 6)
	default String regTeamVanguards() { return ""; }

	@ConfigItem(keyName = "regTeamThieving", name = "Thieving", description = "MM:SS. Blank means no target.", section = "regularTeam", position = 7)
	default String regTeamThieving() { return ""; }

	@ConfigItem(keyName = "regTeamVespula", name = "Vespula", description = "MM:SS. Blank means no target.", section = "regularTeam", position = 8)
	default String regTeamVespula() { return ""; }

	@ConfigItem(keyName = "regTeamTightrope", name = "Tightrope", description = "MM:SS. Blank means no target.", section = "regularTeam", position = 9)
	default String regTeamTightrope() { return ""; }

	@ConfigItem(keyName = "regTeamGuardians", name = "Guardians", description = "MM:SS. Blank means no target.", section = "regularTeam", position = 10)
	default String regTeamGuardians() { return ""; }

	@ConfigItem(keyName = "regTeamVasa", name = "Vasa", description = "MM:SS. Blank means no target.", section = "regularTeam", position = 11)
	default String regTeamVasa() { return ""; }

	@ConfigItem(keyName = "regTeamMystics", name = "Mystics", description = "MM:SS. Blank means no target.", section = "regularTeam", position = 12)
	default String regTeamMystics() { return ""; }

	@ConfigItem(keyName = "regTeamMuttadiles", name = "Muttadiles", description = "MM:SS. Blank means no target.", section = "regularTeam", position = 13)
	default String regTeamMuttadiles() { return ""; }

	@ConfigItem(keyName = "regTeamOlmMage", name = "Olm mage hand", description = "MM:SS. Used for every mage hand. The last phase has none. Blank means no target.", section = "regularTeam", position = 14)
	default String regTeamOlmMage() { return ""; }

	@ConfigItem(keyName = "regTeamOlmPhase", name = "Olm phase", description = "MM:SS. Used for every Olm phase. Blank means no target.", section = "regularTeam", position = 16)
	default String regTeamOlmPhase() { return ""; }

	@ConfigItem(keyName = "regTeamOlmHead", name = "Olm head", description = "MM:SS. Blank means no target.", section = "regularTeam", position = 20)
	default String regTeamOlmHead() { return ""; }

	@ConfigItem(keyName = "regTeamBetweenRooms", name = "Between room time", description = "MM:SS. Blank means no target.", section = "regularTeam", position = 21)
	default String regTeamBetweenRooms() { return ""; }

	@ConfigItem(keyName = "regTeamTotalPoints", name = "Total Points", description = "Whole number. Blank means no target.", section = "regularTeam", position = 24)
	default String regTeamTotalPoints() { return ""; }

	@ConfigItem(keyName = "regTeamPph", name = "PPH", description = "Whole number. Blank means no target.", section = "regularTeam", position = 25)
	default String regTeamPph() { return ""; }

	@ConfigItem(keyName = "cmSoloTotal", name = "Target total", description = "Pre-Olm plus Olm plus between rooms, including strategy adds. Calculated from the boxes below.", section = "cmSolo", position = 1)
	default String cmSoloTotal() { return TargetTotals.text(this, TargetSheet.CM_SOLO); }

	@ConfigItem(keyName = "cmTekton", name = "Tekton", description = "MM:SS. Blank means no target.", section = "cmSolo", position = 2)
	default String cmTekton() { return "1:10"; }

	@ConfigItem(keyName = "cmCrabs", name = "Crabs", description = "MM:SS. Blank means no target.", section = "cmSolo", position = 3)
	default String cmCrabs() { return "0:56"; }

	@ConfigItem(keyName = "cmIceDemon", name = "Ice demon", description = "MM:SS. Blank means no target.", section = "cmSolo", position = 4)
	default String cmIceDemon() { return "2:24"; }

	@ConfigItem(keyName = "cmShamans", name = "Shamans", description = "MM:SS. Blank means no target.", section = "cmSolo", position = 5)
	default String cmShamans() { return "1:03"; }

	@ConfigItem(keyName = "cmVanguards", name = "Vanguards", description = "MM:SS. Blank means no target.", section = "cmSolo", position = 6)
	default String cmVanguards() { return "2:12"; }

	@ConfigItem(keyName = "cmThieving", name = "Thieving", description = "MM:SS. Blank means no target.", section = "cmSolo", position = 7)
	default String cmThieving() { return "1:15"; }

	@ConfigItem(keyName = "cmVespula", name = "Vespula", description = "MM:SS. Blank means no target.", section = "cmSolo", position = 8)
	default String cmVespula() { return "0:57"; }

	@ConfigItem(keyName = "cmTightrope", name = "Tightrope", description = "MM:SS. Blank means no target.", section = "cmSolo", position = 9)
	default String cmTightrope() { return "0:47"; }

	@ConfigItem(keyName = "cmGuardians", name = "Guardians", description = "MM:SS. Blank means no target.", section = "cmSolo", position = 10)
	default String cmGuardians() { return "1:47"; }

	@ConfigItem(keyName = "cmVasa", name = "Vasa", description = "MM:SS. Blank means no target.", section = "cmSolo", position = 11)
	default String cmVasa() { return "1:10"; }

	@ConfigItem(keyName = "cmMystics", name = "Mystics", description = "MM:SS. Blank means no target.", section = "cmSolo", position = 12)
	default String cmMystics() { return "1:40"; }

	@ConfigItem(keyName = "cmMuttadiles", name = "Muttadiles", description = "MM:SS. Blank means no target.", section = "cmSolo", position = 13)
	default String cmMuttadiles() { return "1:25"; }

	@ConfigItem(keyName = "cmOlmMage", name = "Olm mage hand", description = "MM:SS. Used for every mage hand. The last phase has none. Blank means no target.", section = "cmSolo", position = 14)
	default String cmOlmMage() { return "0:56"; }

	@ConfigItem(keyName = "cmOlmPhase", name = "Olm phase", description = "MM:SS. Used for every Olm phase. Blank means no target.", section = "cmSolo", position = 16)
	default String cmOlmPhase() { return "1:54"; }

	@ConfigItem(keyName = "cmOlmHead", name = "Olm head", description = "MM:SS. Blank means no target.", section = "cmSolo", position = 20)
	default String cmOlmHead() { return "1:06"; }

	@ConfigItem(keyName = "cmBetweenRooms", name = "Between room time", description = "MM:SS. Blank means no target.", section = "cmSolo", position = 21)
	default String cmBetweenRooms() { return "0:56"; }

	@ConfigItem(keyName = "cmTotalPoints", name = "Total Points", description = "Whole number. Blank means no target.", section = "cmSolo", position = 24)
	default String cmTotalPoints() { return "63750"; }

	@ConfigItem(keyName = "cmPph", name = "PPH", description = "Whole number. Blank means no target.", section = "cmSolo", position = 25)
	default String cmPph() { return "130000"; }

	@ConfigItem(keyName = "cmTeamTotal", name = "Target total", description = "Pre-Olm plus Olm plus between rooms, including strategy adds. Calculated from the boxes below.", section = "cmTeam", position = 1)
	default String cmTeamTotal() { return TargetTotals.text(this, TargetSheet.CM_TEAM); }

	@ConfigItem(keyName = "cmTeamTekton", name = "Tekton", description = "MM:SS. Blank means no target.", section = "cmTeam", position = 2)
	default String cmTeamTekton() { return ""; }

	@ConfigItem(keyName = "cmTeamCrabs", name = "Crabs", description = "MM:SS. Blank means no target.", section = "cmTeam", position = 3)
	default String cmTeamCrabs() { return ""; }

	@ConfigItem(keyName = "cmTeamIceDemon", name = "Ice demon", description = "MM:SS. Blank means no target.", section = "cmTeam", position = 4)
	default String cmTeamIceDemon() { return ""; }

	@ConfigItem(keyName = "cmTeamShamans", name = "Shamans", description = "MM:SS. Blank means no target.", section = "cmTeam", position = 5)
	default String cmTeamShamans() { return ""; }

	@ConfigItem(keyName = "cmTeamVanguards", name = "Vanguards", description = "MM:SS. Blank means no target.", section = "cmTeam", position = 6)
	default String cmTeamVanguards() { return ""; }

	@ConfigItem(keyName = "cmTeamThieving", name = "Thieving", description = "MM:SS. Blank means no target.", section = "cmTeam", position = 7)
	default String cmTeamThieving() { return ""; }

	@ConfigItem(keyName = "cmTeamVespula", name = "Vespula", description = "MM:SS. Blank means no target.", section = "cmTeam", position = 8)
	default String cmTeamVespula() { return ""; }

	@ConfigItem(keyName = "cmTeamTightrope", name = "Tightrope", description = "MM:SS. Blank means no target.", section = "cmTeam", position = 9)
	default String cmTeamTightrope() { return ""; }

	@ConfigItem(keyName = "cmTeamGuardians", name = "Guardians", description = "MM:SS. Blank means no target.", section = "cmTeam", position = 10)
	default String cmTeamGuardians() { return ""; }

	@ConfigItem(keyName = "cmTeamVasa", name = "Vasa", description = "MM:SS. Blank means no target.", section = "cmTeam", position = 11)
	default String cmTeamVasa() { return ""; }

	@ConfigItem(keyName = "cmTeamMystics", name = "Mystics", description = "MM:SS. Blank means no target.", section = "cmTeam", position = 12)
	default String cmTeamMystics() { return ""; }

	@ConfigItem(keyName = "cmTeamMuttadiles", name = "Muttadiles", description = "MM:SS. Blank means no target.", section = "cmTeam", position = 13)
	default String cmTeamMuttadiles() { return ""; }

	@ConfigItem(keyName = "cmTeamOlmMage", name = "Olm mage hand", description = "MM:SS. Used for every mage hand. The last phase has none. Blank means no target.", section = "cmTeam", position = 14)
	default String cmTeamOlmMage() { return ""; }

	@ConfigItem(keyName = "cmTeamOlmPhase", name = "Olm phase", description = "MM:SS. Used for every Olm phase. Blank means no target.", section = "cmTeam", position = 16)
	default String cmTeamOlmPhase() { return ""; }

	@ConfigItem(keyName = "cmTeamOlmHead", name = "Olm head", description = "MM:SS. Blank means no target.", section = "cmTeam", position = 20)
	default String cmTeamOlmHead() { return ""; }

	@ConfigItem(keyName = "cmTeamBetweenRooms", name = "Between room time", description = "MM:SS. Blank means no target.", section = "cmTeam", position = 21)
	default String cmTeamBetweenRooms() { return ""; }

	@ConfigItem(keyName = "cmTeamTotalPoints", name = "Total Points", description = "Whole number. Blank means no target.", section = "cmTeam", position = 24)
	default String cmTeamTotalPoints() { return ""; }

	@ConfigItem(keyName = "cmTeamPph", name = "PPH", description = "Whole number. Blank means no target.", section = "cmTeam", position = 25)
	default String cmTeamPph() { return ""; }
}
