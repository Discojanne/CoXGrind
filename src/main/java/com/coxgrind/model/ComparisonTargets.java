package com.coxgrind.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-entered benchmark times. One sheet for regular solo, regular team, CM solo, and CM team.
 * Regular full uses the regular sheet for that team size. Points and points-per-hour are single targets, not per room.
 */
public class ComparisonTargets
{
	public static final List<String> TIME_ROWS = Collections.unmodifiableList(Arrays.asList(
		"Tekton",
		"Crabs",
		"Ice demon",
		"Shamans",
		"Vanguards",
		"Thieving",
		"Vespula",
		"Tightrope",
		"Guardians",
		"Vasa",
		"Mystics",
		"Muttadiles",
		"Olm mage hand",
		"Olm phase",
		"Olm head",
		"Between room time"
	));

	public static final String POINTS = "Total Points";
	public static final String PPH = "PPH";

	private final Map<TargetSheet, Map<String, Integer>> sheets = new EnumMap<>(TargetSheet.class);

	public Map<String, Integer> sheet(TargetSheet which)
	{
		Map<String, Integer> out = new LinkedHashMap<>();
		if (which == null)
		{
			return out;
		}
		Map<String, Integer> source = sheets.get(which);
		if (source == null)
		{
			return out;
		}
		for (Map.Entry<String, Integer> entry : source.entrySet())
		{
			if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0)
			{
				out.put(entry.getKey(), entry.getValue());
			}
		}
		return out;
	}

	public void put(TargetSheet which, String row, Integer value)
	{
		if (which == null || row == null)
		{
			return;
		}
		Map<String, Integer> sheet = sheets.get(which);
		if (sheet == null)
		{
			sheet = new LinkedHashMap<>();
			sheets.put(which, sheet);
		}
		if (value == null || value <= 0)
		{
			sheet.remove(row);
			return;
		}
		sheet.put(row, value);
	}

	/** Solo sheet. True is Challenge Mode solo. False is regular solo. */
	public void put(boolean challengeMode, String row, Integer value)
	{
		put(challengeMode ? TargetSheet.CM_SOLO : TargetSheet.REGULAR_SOLO, row, value);
	}
}
