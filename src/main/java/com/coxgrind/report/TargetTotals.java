package com.coxgrind.report;

import com.coxgrind.CoxGrindConfig;
import com.coxgrind.model.TargetSheet;
import com.coxgrind.track.TimeFormat;
import java.util.Map;

/**
 * The raid target shown at the top of a sheet. It is Pre-Olm plus Olm plus between rooms, after strategy adds.
 */
public final class TargetTotals
{
	private TargetTotals()
	{
	}

	public static String text(CoxGrindConfig config, TargetSheet sheet)
	{
		if (config == null || sheet == null)
		{
			return "";
		}
		Map<String, Integer> applied = TargetSettings.style(config).apply(TargetSettings.from(config).sheet(sheet));
		Integer raid = applied.get("Raid Completed");
		if (raid == null || raid <= 0)
		{
			return "";
		}
		return TimeFormat.formatSeconds(raid);
	}
}
