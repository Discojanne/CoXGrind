package com.coxgrind.track;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RoomNames
{
	public static final List<String> PREP_ROOMS = Collections.unmodifiableList(Arrays.asList(
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
		"Muttadiles"
	));

	private static final Map<String, String> CANONICAL = new HashMap<>();

	static
	{
		for (int i = 0; i < PREP_ROOMS.size(); i++)
		{
			String name = PREP_ROOMS.get(i);
			CANONICAL.put(name.toLowerCase(Locale.ENGLISH), name);
		}
		CANONICAL.put("ice demon", "Ice demon");
		CANONICAL.put("jewelled crabs", "Crabs");
		CANONICAL.put("crabs", "Crabs");
		CANONICAL.put("lizardman shamans", "Shamans");
		CANONICAL.put("skeletal mystics", "Mystics");
		CANONICAL.put("guardian", "Guardians");
		CANONICAL.put("guardians", "Guardians");
	}

	private RoomNames()
	{
	}

	public static boolean isPrepRoom(String room)
	{
		return room != null && CANONICAL.containsKey(room.toLowerCase(Locale.ENGLISH));
	}

	/**
	 * @return the Coxparser room name, or null when the game name is not a prep room
	 */
	public static String canonical(String raw)
	{
		if (raw == null)
		{
			return null;
		}
		String key = raw.trim().toLowerCase(Locale.ENGLISH);
		String direct = CANONICAL.get(key);
		if (direct != null)
		{
			return direct;
		}
		for (int i = 0; i < PREP_ROOMS.size(); i++)
		{
			String name = PREP_ROOMS.get(i);
			if (key.contains(name.toLowerCase(Locale.ENGLISH)))
			{
				return name;
			}
		}
		return null;
	}
}
