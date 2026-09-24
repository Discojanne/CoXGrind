package com.coxgrind.report;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Method checkboxes adjust the comparison column. Typed targets stay as saved.
 * Twisted bow off shortens tbow rooms. Ice milking, killing rope, and Vespula milking add time when on.
 */
public final class TargetStyle
{
	private boolean useTbow = true;
	private boolean iceMilking;
	private boolean killRope;
	private boolean milkVespula;
	private int noTbowVanguards = 15;
	private int noTbowTightrope = 10;
	private int noTbowVasa = 20;
	private int noTbowMystics = 25;
	private int noTbowMuttadiles = 20;
	private int noTbowOlmHead = 30;
	private int iceMilkSeconds = 70;
	private int killRopeSeconds = 50;
	private int milkVespulaSeconds = 10;

	public boolean isUseTbow()
	{
		return useTbow;
	}

	public void setUseTbow(boolean useTbow)
	{
		this.useTbow = useTbow;
	}

	public boolean isIceMilking()
	{
		return iceMilking;
	}

	public void setIceMilking(boolean iceMilking)
	{
		this.iceMilking = iceMilking;
	}

	public boolean isKillRope()
	{
		return killRope;
	}

	public void setKillRope(boolean killRope)
	{
		this.killRope = killRope;
	}

	public boolean isMilkVespula()
	{
		return milkVespula;
	}

	public void setMilkVespula(boolean milkVespula)
	{
		this.milkVespula = milkVespula;
	}

	public void setNoTbowVanguards(int seconds)
	{
		noTbowVanguards = clamp(seconds);
	}

	public void setNoTbowTightrope(int seconds)
	{
		noTbowTightrope = clamp(seconds);
	}

	public void setNoTbowVasa(int seconds)
	{
		noTbowVasa = clamp(seconds);
	}

	public void setNoTbowMystics(int seconds)
	{
		noTbowMystics = clamp(seconds);
	}

	public void setNoTbowMuttadiles(int seconds)
	{
		noTbowMuttadiles = clamp(seconds);
	}

	public void setNoTbowOlmHead(int seconds)
	{
		noTbowOlmHead = clamp(seconds);
	}

	public void setIceMilkSeconds(int seconds)
	{
		iceMilkSeconds = clamp(seconds);
	}

	public void setKillRopeSeconds(int seconds)
	{
		killRopeSeconds = clamp(seconds);
	}

	public void setMilkVespulaSeconds(int seconds)
	{
		milkVespulaSeconds = clamp(seconds);
	}

	public boolean changesTargets()
	{
		return (!useTbow && tbowSeconds() > 0)
			|| (iceMilking && iceMilkSeconds > 0)
			|| (killRope && (killRopeSeconds > 0 || (!useTbow && noTbowTightrope > 0)))
			|| (milkVespula && milkVespulaSeconds > 0);
	}

	public String note()
	{
		if (!changesTargets())
		{
			return "";
		}
		StringBuilder note = new StringBuilder("Targets adjusted:");
		if (!useTbow && tbowSeconds() > 0)
		{
			note.append(" no twisted bow");
		}
		if (iceMilking && iceMilkSeconds > 0)
		{
			note.append(" ice milking");
		}
		if (killRope && killRopeSeconds > 0)
		{
			note.append(" killing rope");
		}
		if (milkVespula && milkVespulaSeconds > 0)
		{
			note.append(" milking Vespula");
		}
		note.append('.');
		return note.toString();
	}

	public Map<String, Integer> apply(Map<String, Integer> sheet)
	{
		Map<String, Integer> out = new LinkedHashMap<>();
		if (sheet != null)
		{
			out.putAll(sheet);
		}
		int prep = 0;
		if (!useTbow)
		{
			prep += shift(out, "Vanguards", -noTbowVanguards);
			prep += shift(out, "Vasa", -noTbowVasa);
			prep += shift(out, "Mystics", -noTbowMystics);
			prep += shift(out, "Muttadiles", -noTbowMuttadiles);
			if (killRope)
			{
				prep += shift(out, "Tightrope", -noTbowTightrope);
			}
		}
		if (iceMilking)
		{
			prep += shift(out, "Ice demon", iceMilkSeconds);
		}
		if (killRope)
		{
			prep += shift(out, "Tightrope", killRopeSeconds);
		}
		if (milkVespula)
		{
			prep += shift(out, "Vespula", milkVespulaSeconds);
		}
		int head = useTbow ? 0 : shift(out, "Olm head", -noTbowOlmHead);
		shift(out, "Pre-Olm", prep);
		shift(out, "Olm", head);
		shift(out, "Raid Completed", prep + head);
		return out;
	}

	private int tbowSeconds()
	{
		return noTbowVanguards + noTbowVasa + noTbowMystics + noTbowMuttadiles + noTbowOlmHead
			+ (killRope ? noTbowTightrope : 0);
	}

	private static int shift(Map<String, Integer> sheet, String room, int delta)
	{
		if (delta == 0)
		{
			return 0;
		}
		Integer value = sheet.get(room);
		if (value == null || value <= 0)
		{
			return 0;
		}
		int next = value + delta;
		if (next < 1)
		{
			next = 1;
		}
		sheet.put(room, next);
		return next - value;
	}

	private static int clamp(int seconds)
	{
		if (seconds < 0)
		{
			return 0;
		}
		return Math.min(seconds, 600);
	}
}
