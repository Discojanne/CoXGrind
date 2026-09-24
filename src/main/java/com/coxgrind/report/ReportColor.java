package com.coxgrind.report;

/**
 * Console color codes, same cuts as Coxparser.
 * Faster by 20 seconds or more is cyan, any faster is green, under 10 seconds slower is orange, and the rest is red.
 */
public final class ReportColor
{
	public static final String RESET = "\u001B[0m";
	public static final String GREEN = "\u001B[32m";
	public static final String RED = "\u001B[31m";
	public static final String ORANGE = "\u001B[33m";
	public static final String CYAN = "\u001B[36m";
	public static final String GOLD = "\u001B[93m";

	private ReportColor()
	{
	}

	public static String time(int delta, String text)
	{
		if (Math.abs(delta) < 1)
		{
			return text;
		}
		if (delta <= -20)
		{
			return wrap(CYAN, text);
		}
		if (delta < 0)
		{
			return wrap(GREEN, text);
		}
		if (delta < 10)
		{
			return wrap(ORANGE, text);
		}
		return wrap(RED, text);
	}

	public static String points(int delta, String text)
	{
		if (Math.abs(delta) < 1)
		{
			return text;
		}
		return wrap(delta > 0 ? GREEN : RED, text);
	}

	public static String signed(double delta, String text)
	{
		if (Math.abs(delta) < 0.05)
		{
			return text;
		}
		return wrap(delta > 0 ? GREEN : RED, text);
	}

	public static String cyan(String text)
	{
		return wrap(CYAN, text);
	}

	public static String gold(String text)
	{
		return wrap(GOLD, text);
	}

	public static String good(String text)
	{
		return wrap(GREEN, text);
	}

	public static String bad(String text)
	{
		return wrap(RED, text);
	}

	private static String wrap(String color, String text)
	{
		return color + text + RESET;
	}
}
