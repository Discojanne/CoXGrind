package com.coxgrind.track;

/**
 * The Chambers of Xeric raid clock is stored as ticks on varbit 6386.
 * 100 ticks is one minute. Leftover ticks convert at 0.6 seconds, matching the in-game clock.
 */
public final class TimeFormat
{
	private TimeFormat()
	{
	}

	public static int unitsToSeconds(int units)
	{
		if (units <= 0)
		{
			return 0;
		}
		int minutes = units / 100;
		int seconds = (units % 100) * 6 / 10;
		return minutes * 60 + seconds;
	}

	public static String formatSeconds(int seconds)
	{
		if (seconds <= 0)
		{
			return "00:00";
		}
		int minutes = seconds / 60;
		int remain = seconds % 60;
		return String.format("%02d:%02d", minutes, remain);
	}

	/**
	 * @return null when the field is blank. Seconds, or MM:SS. A trailing decimal on the seconds is ignored.
	 */
	public static Integer parseClock(String text)
	{
		if (text == null || text.trim().isEmpty())
		{
			return null;
		}
		String raw = text.trim();
		try
		{
			if (raw.indexOf(':') >= 0)
			{
				String[] parts = raw.split(":");
				if (parts.length != 2)
				{
					throw new IllegalArgumentException();
				}
				int minutes = Integer.parseInt(parts[0].trim());
				String secondsText = parts[1].trim();
				int dot = secondsText.indexOf('.');
				if (dot >= 0)
				{
					secondsText = secondsText.substring(0, dot);
				}
				int seconds = Integer.parseInt(secondsText);
				if (minutes < 0 || seconds < 0 || seconds >= 60)
				{
					throw new IllegalArgumentException();
				}
				return minutes * 60 + seconds;
			}
			int seconds = Integer.parseInt(raw);
			if (seconds < 0)
			{
				throw new IllegalArgumentException();
			}
			return seconds;
		}
		catch (NumberFormatException ex)
		{
			throw new IllegalArgumentException();
		}
	}

	public static Integer parseCount(String text)
	{
		if (text == null || text.trim().isEmpty())
		{
			return null;
		}
		try
		{
			int value = Integer.parseInt(text.trim());
			if (value < 0)
			{
				throw new IllegalArgumentException();
			}
			return value;
		}
		catch (NumberFormatException ex)
		{
			throw new IllegalArgumentException();
		}
	}
}
