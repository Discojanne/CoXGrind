package com.coxgrind.report;

import com.coxgrind.CoxGrindConfig;
import com.coxgrind.model.ComparisonTargets;
import com.coxgrind.track.TimeFormat;
import java.util.List;

/**
 * Reads the Regular and CM target boxes from the plugin settings.
 * Time rows follow {@link ComparisonTargets#TIME_ROWS}.
 */
public final class TargetSettings
{
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
		String[] regular = regularTimes(config);
		String[] cm = cmTimes(config);
		List<String> rows = ComparisonTargets.TIME_ROWS;
		if (regular.length != rows.size() || cm.length != rows.size())
		{
			throw new IllegalStateException("Target boxes do not match the target rows.");
		}
		for (int i = 0; i < rows.size(); i++)
		{
			putTime(targets, false, rows.get(i), regular[i]);
			putTime(targets, true, rows.get(i), cm[i]);
		}
		putCount(targets, false, ComparisonTargets.POINTS, config.regTotalPoints());
		putCount(targets, false, ComparisonTargets.PPH, config.regPph());
		putCount(targets, true, ComparisonTargets.POINTS, config.cmTotalPoints());
		putCount(targets, true, ComparisonTargets.PPH, config.cmPph());
		return targets;
	}

	private static String[] regularTimes(CoxGrindConfig config)
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
			config.regPreOlm(),
			config.regOlmMage1(),
			config.regOlmPhase1(),
			config.regOlmMage2(),
			config.regOlmPhase2(),
			config.regOlmPhase3(),
			config.regOlmHead(),
			config.regOlm(),
			config.regRaidCompleted(),
			config.regBetweenRooms()
		};
	}

	private static String[] cmTimes(CoxGrindConfig config)
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
			config.cmPreOlm(),
			config.cmOlmMage1(),
			config.cmOlmPhase1(),
			config.cmOlmMage2(),
			config.cmOlmPhase2(),
			config.cmOlmPhase3(),
			config.cmOlmHead(),
			config.cmOlm(),
			config.cmRaidCompleted(),
			config.cmBetweenRooms()
		};
	}

	private static void putTime(ComparisonTargets targets, boolean challengeMode, String row, String text)
	{
		try
		{
			targets.put(challengeMode, row, TimeFormat.parseClock(text));
		}
		catch (IllegalArgumentException ex)
		{
			targets.put(challengeMode, row, null);
		}
	}

	private static void putCount(ComparisonTargets targets, boolean challengeMode, String row, String text)
	{
		try
		{
			targets.put(challengeMode, row, TimeFormat.parseCount(text));
		}
		catch (IllegalArgumentException ex)
		{
			targets.put(challengeMode, row, null);
		}
	}
}
