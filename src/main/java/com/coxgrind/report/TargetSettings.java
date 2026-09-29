package com.coxgrind.report;

import com.coxgrind.CoxGrindConfig;
import com.coxgrind.model.ComparisonTargets;
import com.coxgrind.model.TargetSheet;
import com.coxgrind.track.TimeFormat;
import java.util.List;
import net.runelite.client.config.ConfigManager;

/**
 * Reads the four target sheets from the plugin settings.
 * Time rows follow {@link ComparisonTargets#TIME_ROWS}.
 */
public final class TargetSettings
{
	public static final String GROUP = "coxgrind";

	private static final String[] REG_SOLO_KEYS = {
		"regTekton", "regCrabs", "regIceDemon", "regShamans", "regVanguards", "regThieving",
		"regVespula", "regTightrope", "regGuardians", "regVasa", "regMystics", "regMuttadiles",
		"regOlmMage", "regOlmPhase", "regOlmHead",
		"regBetweenRooms", "regTotalPoints", "regPph"
	};
	private static final String[] REG_TEAM_KEYS = {
		"regTeamTekton", "regTeamCrabs", "regTeamIceDemon", "regTeamShamans", "regTeamVanguards", "regTeamThieving",
		"regTeamVespula", "regTeamTightrope", "regTeamGuardians", "regTeamVasa", "regTeamMystics", "regTeamMuttadiles",
		"regTeamOlmMage", "regTeamOlmPhase", "regTeamOlmHead",
		"regTeamBetweenRooms", "regTeamTotalPoints", "regTeamPph"
	};
	private static final String[] CM_SOLO_KEYS = {
		"cmTekton", "cmCrabs", "cmIceDemon", "cmShamans", "cmVanguards", "cmThieving",
		"cmVespula", "cmTightrope", "cmGuardians", "cmVasa", "cmMystics", "cmMuttadiles",
		"cmOlmMage", "cmOlmPhase", "cmOlmHead",
		"cmBetweenRooms", "cmTotalPoints", "cmPph"
	};
	private static final String[] CM_TEAM_KEYS = {
		"cmTeamTekton", "cmTeamCrabs", "cmTeamIceDemon", "cmTeamShamans", "cmTeamVanguards", "cmTeamThieving",
		"cmTeamVespula", "cmTeamTightrope", "cmTeamGuardians", "cmTeamVasa", "cmTeamMystics", "cmTeamMuttadiles",
		"cmTeamOlmMage", "cmTeamOlmPhase", "cmTeamOlmHead",
		"cmTeamBetweenRooms", "cmTeamTotalPoints", "cmTeamPph"
	};

	private TargetSettings()
	{
	}

	public static ComparisonTargets from(CoxGrindConfig config)
	{
		ComparisonTargets targets = new ComparisonTargets();
		if (config == null)
		{
			return targets;
		}
		fill(targets, TargetSheet.REGULAR_SOLO, regularSoloTimes(config), config.regTotalPoints(), config.regPph());
		fill(targets, TargetSheet.REGULAR_TEAM, regularTeamTimes(config), config.regTeamTotalPoints(), config.regTeamPph());
		fill(targets, TargetSheet.CM_SOLO, cmSoloTimes(config), config.cmTotalPoints(), config.cmPph());
		fill(targets, TargetSheet.CM_TEAM, cmTeamTimes(config), config.cmTeamTotalPoints(), config.cmTeamPph());
		return targets;
	}

	public static TargetStyle style(CoxGrindConfig config)
	{
		TargetStyle style = new TargetStyle();
		if (config == null)
		{
			return style;
		}
		style.setUseTbow(config.useTbow());
		style.setSlayerHelm(config.slayerHelm());
		style.setLockpick(config.lockpick());
		style.setAxe(config.axe());
		style.setSalve(config.salve());
		style.setPreVeng(config.preVeng());
		style.setVespPotSkip(config.vespPotSkip());
		style.setCrabTank(config.crabTank());
		style.setIceMilking(config.iceMilking());
		style.setIceMilkSeconds(config.iceMilkSeconds());
		style.setKillRope(config.killRope());
		style.setMilkVespula(config.milkVespula());
		style.setOverThieve(config.overThieve());
		style.setSippingPools(config.sippingPools());
		style.setBankAfterRope(config.bankAfterRope());
		return style;
	}

	/** Boxes cleared when that sheet is reset. Null for any other key. */
	public static String[] keysForReset(String resetKey)
	{
		if ("resetRegSolo".equals(resetKey))
		{
			return REG_SOLO_KEYS;
		}
		if ("resetRegTeam".equals(resetKey))
		{
			return REG_TEAM_KEYS;
		}
		if ("resetCmSolo".equals(resetKey))
		{
			return CM_SOLO_KEYS;
		}
		if ("resetCmTeam".equals(resetKey))
		{
			return CM_TEAM_KEYS;
		}
		return null;
	}

	public static void reset(ConfigManager manager, TargetSheet sheet)
	{
		String[] keys = keysFor(sheet);
		if (manager == null || keys == null)
		{
			return;
		}
		for (int i = 0; i < keys.length; i++)
		{
			manager.unsetConfiguration(GROUP, keys[i]);
		}
		forgetTotals(manager);
	}

	/** The text a settings row should show. Target total is the calculated raid time. */
	public static String shown(CoxGrindConfig config, TargetSheet sheet, String label)
	{
		if (config == null || sheet == null || label == null)
		{
			return "";
		}
		if ("Target total".equals(label))
		{
			return TargetTotals.text(config, sheet);
		}
		if ("Tekton".equals(label))
		{
			return pick(sheet, config.regTekton(), config.regTeamTekton(), config.cmTekton(), config.cmTeamTekton());
		}
		if ("Crabs".equals(label))
		{
			return pick(sheet, config.regCrabs(), config.regTeamCrabs(), config.cmCrabs(), config.cmTeamCrabs());
		}
		if ("Ice demon".equals(label))
		{
			return pick(sheet, config.regIceDemon(), config.regTeamIceDemon(), config.cmIceDemon(), config.cmTeamIceDemon());
		}
		if ("Shamans".equals(label))
		{
			return pick(sheet, config.regShamans(), config.regTeamShamans(), config.cmShamans(), config.cmTeamShamans());
		}
		if ("Vanguards".equals(label))
		{
			return pick(sheet, config.regVanguards(), config.regTeamVanguards(), config.cmVanguards(), config.cmTeamVanguards());
		}
		if ("Thieving".equals(label))
		{
			return pick(sheet, config.regThieving(), config.regTeamThieving(), config.cmThieving(), config.cmTeamThieving());
		}
		if ("Vespula".equals(label))
		{
			return pick(sheet, config.regVespula(), config.regTeamVespula(), config.cmVespula(), config.cmTeamVespula());
		}
		if ("Tightrope".equals(label))
		{
			return pick(sheet, config.regTightrope(), config.regTeamTightrope(), config.cmTightrope(), config.cmTeamTightrope());
		}
		if ("Guardians".equals(label))
		{
			return pick(sheet, config.regGuardians(), config.regTeamGuardians(), config.cmGuardians(), config.cmTeamGuardians());
		}
		if ("Vasa".equals(label))
		{
			return pick(sheet, config.regVasa(), config.regTeamVasa(), config.cmVasa(), config.cmTeamVasa());
		}
		if ("Mystics".equals(label))
		{
			return pick(sheet, config.regMystics(), config.regTeamMystics(), config.cmMystics(), config.cmTeamMystics());
		}
		if ("Muttadiles".equals(label))
		{
			return pick(sheet, config.regMuttadiles(), config.regTeamMuttadiles(), config.cmMuttadiles(), config.cmTeamMuttadiles());
		}
		if ("Olm mage hand".equals(label))
		{
			return pick(sheet, config.regOlmMage(), config.regTeamOlmMage(), config.cmOlmMage(), config.cmTeamOlmMage());
		}
		if ("Olm phase".equals(label))
		{
			return pick(sheet, config.regOlmPhase(), config.regTeamOlmPhase(), config.cmOlmPhase(), config.cmTeamOlmPhase());
		}
		if ("Olm head".equals(label))
		{
			return pick(sheet, config.regOlmHead(), config.regTeamOlmHead(), config.cmOlmHead(), config.cmTeamOlmHead());
		}
		if ("Between room time".equals(label))
		{
			return pick(sheet, config.regBetweenRooms(), config.regTeamBetweenRooms(), config.cmBetweenRooms(), config.cmTeamBetweenRooms());
		}
		if ("Total Points".equals(label))
		{
			return pick(sheet, config.regTotalPoints(), config.regTeamTotalPoints(), config.cmTotalPoints(), config.cmTeamTotalPoints());
		}
		if ("PPH".equals(label))
		{
			return pick(sheet, config.regPph(), config.regTeamPph(), config.cmPph(), config.cmTeamPph());
		}
		return "";
	}

	private static String[] keysFor(TargetSheet sheet)
	{
		if (sheet == TargetSheet.REGULAR_SOLO)
		{
			return REG_SOLO_KEYS;
		}
		if (sheet == TargetSheet.REGULAR_TEAM)
		{
			return REG_TEAM_KEYS;
		}
		if (sheet == TargetSheet.CM_SOLO)
		{
			return CM_SOLO_KEYS;
		}
		if (sheet == TargetSheet.CM_TEAM)
		{
			return CM_TEAM_KEYS;
		}
		return null;
	}

	private static String pick(TargetSheet sheet, String regularSolo, String regularTeam, String cmSolo, String cmTeam)
	{
		if (sheet == TargetSheet.REGULAR_SOLO)
		{
			return regularSolo == null ? "" : regularSolo;
		}
		if (sheet == TargetSheet.REGULAR_TEAM)
		{
			return regularTeam == null ? "" : regularTeam;
		}
		if (sheet == TargetSheet.CM_SOLO)
		{
			return cmSolo == null ? "" : cmSolo;
		}
		if (sheet == TargetSheet.CM_TEAM)
		{
			return cmTeam == null ? "" : cmTeam;
		}
		return "";
	}

	public static boolean isTotalKey(String key)
	{
		return "regSoloTotal".equals(key)
			|| "regTeamTotal".equals(key)
			|| "cmSoloTotal".equals(key)
			|| "cmTeamTotal".equals(key);
	}

	/** Drop a saved total so the calculated value is what the settings show. */
	public static void forgetTotals(ConfigManager manager)
	{
		if (manager == null)
		{
			return;
		}
		manager.unsetConfiguration(GROUP, "regSoloTotal");
		manager.unsetConfiguration(GROUP, "regTeamTotal");
		manager.unsetConfiguration(GROUP, "cmSoloTotal");
		manager.unsetConfiguration(GROUP, "cmTeamTotal");
	}

	private static void fill(ComparisonTargets targets, TargetSheet sheet, String[] times, String points, String pph)
	{
		List<String> rows = ComparisonTargets.TIME_ROWS;
		if (times.length != rows.size())
		{
			throw new IllegalStateException("Target boxes do not match the target rows.");
		}
		for (int i = 0; i < rows.size(); i++)
		{
			putTime(targets, sheet, rows.get(i), times[i]);
		}
		putCount(targets, sheet, ComparisonTargets.POINTS, points);
		putCount(targets, sheet, ComparisonTargets.PPH, pph);
	}

	private static String[] regularSoloTimes(CoxGrindConfig config)
	{
		return new String[] {
			config.regTekton(),
			config.regCrabs(),
			config.regIceDemon(),
			config.regShamans(),
			config.regVanguards(),
			config.regThieving(),
			config.regVespula(),
			config.regTightrope(),
			config.regGuardians(),
			config.regVasa(),
			config.regMystics(),
			config.regMuttadiles(),
			config.regOlmMage(),
			config.regOlmPhase(),
			config.regOlmHead(),
			config.regBetweenRooms()
		};
	}

	private static String[] regularTeamTimes(CoxGrindConfig config)
	{
		return new String[] {
			config.regTeamTekton(),
			config.regTeamCrabs(),
			config.regTeamIceDemon(),
			config.regTeamShamans(),
			config.regTeamVanguards(),
			config.regTeamThieving(),
			config.regTeamVespula(),
			config.regTeamTightrope(),
			config.regTeamGuardians(),
			config.regTeamVasa(),
			config.regTeamMystics(),
			config.regTeamMuttadiles(),
			config.regTeamOlmMage(),
			config.regTeamOlmPhase(),
			config.regTeamOlmHead(),
			config.regTeamBetweenRooms()
		};
	}

	private static String[] cmSoloTimes(CoxGrindConfig config)
	{
		return new String[] {
			config.cmTekton(),
			config.cmCrabs(),
			config.cmIceDemon(),
			config.cmShamans(),
			config.cmVanguards(),
			config.cmThieving(),
			config.cmVespula(),
			config.cmTightrope(),
			config.cmGuardians(),
			config.cmVasa(),
			config.cmMystics(),
			config.cmMuttadiles(),
			config.cmOlmMage(),
			config.cmOlmPhase(),
			config.cmOlmHead(),
			config.cmBetweenRooms()
		};
	}

	private static String[] cmTeamTimes(CoxGrindConfig config)
	{
		return new String[] {
			config.cmTeamTekton(),
			config.cmTeamCrabs(),
			config.cmTeamIceDemon(),
			config.cmTeamShamans(),
			config.cmTeamVanguards(),
			config.cmTeamThieving(),
			config.cmTeamVespula(),
			config.cmTeamTightrope(),
			config.cmTeamGuardians(),
			config.cmTeamVasa(),
			config.cmTeamMystics(),
			config.cmTeamMuttadiles(),
			config.cmTeamOlmMage(),
			config.cmTeamOlmPhase(),
			config.cmTeamOlmHead(),
			config.cmTeamBetweenRooms()
		};
	}

	private static void putTime(ComparisonTargets targets, TargetSheet sheet, String row, String text)
	{
		try
		{
			targets.put(sheet, row, TimeFormat.parseClock(text));
		}
		catch (IllegalArgumentException ex)
		{
			targets.put(sheet, row, null);
		}
	}

	private static void putCount(ComparisonTargets targets, TargetSheet sheet, String row, String text)
	{
		try
		{
			targets.put(sheet, row, TimeFormat.parseCount(text));
		}
		catch (IllegalArgumentException ex)
		{
			targets.put(sheet, row, null);
		}
	}
}
