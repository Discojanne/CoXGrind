package com.coxgrind.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-entered benchmark times. Regular and regular-full raids share one sheet.
 * Challenge Mode has its own sheet. Points and points-per-hour are single targets, not per room.
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
		"Pre-Olm",
		"Olm mage hand phase 1",
		"Olm phase 1",
		"Olm mage hand phase 2",
		"Olm phase 2",
		"Olm phase 3",
		"Olm head",
		"Olm",
		"Raid Completed",
		"Between room time"
	));

	public static final String POINTS = "Total Points";
	public static final String PPH = "PPH";

	private Map<String, Integer> regular = new LinkedHashMap<>();
	private Map<String, Integer> cm = new LinkedHashMap<>();

	public Map<String, Integer> getRegular()
	{
		if (regular == null)
		{
			regular = new LinkedHashMap<>();
		}
		return regular;
	}

	public Map<String, Integer> getCm()
	{
		if (cm == null)
		{
			cm = new LinkedHashMap<>();
		}
		return cm;
	}

	public Map<String, Integer> sheet(boolean challengeMode)
	{
		Map<String, Integer> source = challengeMode ? getCm() : getRegular();
		Map<String, Integer> out = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> entry : source.entrySet())
		{
			if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0)
			{
				out.put(entry.getKey(), entry.getValue());
			}
		}
		return out;
	}

	public void put(boolean challengeMode, String row, Integer value)
	{
		Map<String, Integer> sheet = challengeMode ? getCm() : getRegular();
		if (row == null)
		{
			return;
		}
		if (value == null || value <= 0)
		{
			sheet.remove(row);
			return;
		}
		sheet.put(row, value);
	}
}
