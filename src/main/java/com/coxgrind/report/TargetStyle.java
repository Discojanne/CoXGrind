package com.coxgrind.report;

import com.coxgrind.track.RoomNames;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Checkboxes adjust the comparison column. Typed targets stay as saved.
 * Twisted bow, items, and strategies add time when off. Money-run methods add time when on.
 */
public final class TargetStyle
{
	static final int NO_TBOW_SHAMANS = 6;
	static final int NO_TBOW_VANGUARDS = 2;
	static final int NO_TBOW_TIGHTROPE = 8;
	static final int NO_TBOW_VASA = 30;
	static final int NO_TBOW_MUTTADILES = 10;
	static final int NO_TBOW_OLM_HEAD = 20;
	static final int NO_HELM_SHAMANS = 9;
	static final int NO_LOCKPICK_THIEVING = 4;
	static final int NO_AXE_ICE = 5;
	static final int NO_SALVE_MYSTICS = 5;
	static final int NO_PRE_VENG_TEKTON = 4;
	static final int NO_VESP_POT_SKIP_TIGHTROPE = 6;
	static final int NO_CRAB_TANK_CRABS = 4;
	static final int KILL_ROPE_SECONDS = 59;
	static final int MILK_VESPULA_SECONDS = 18;
	static final int OVER_THIEVE_SECONDS = 7;

	private boolean useTbow = true;
	private boolean slayerHelm = true;
	private boolean lockpick = true;
	private boolean axe = true;
	private boolean salve = true;
	private boolean preVeng = true;
	private boolean vespPotSkip = true;
	private boolean crabTank = true;
	private boolean iceMilking;
	private boolean killRope;
	private boolean milkVespula;
	private boolean overThieve;
	private int iceMilkSeconds = 70;

	public boolean isUseTbow()
	{
		return useTbow;
	}

	public void setUseTbow(boolean useTbow)
	{
		this.useTbow = useTbow;
	}

	public boolean isSlayerHelm()
	{
		return slayerHelm;
	}

	public void setSlayerHelm(boolean slayerHelm)
	{
		this.slayerHelm = slayerHelm;
	}

	public boolean isLockpick()
	{
		return lockpick;
	}

	public void setLockpick(boolean lockpick)
	{
		this.lockpick = lockpick;
	}

	public boolean isAxe()
	{
		return axe;
	}

	public void setAxe(boolean axe)
	{
		this.axe = axe;
	}

	public boolean isSalve()
	{
		return salve;
	}

	public void setSalve(boolean salve)
	{
		this.salve = salve;
	}

	public boolean isPreVeng()
	{
		return preVeng;
	}

	public void setPreVeng(boolean preVeng)
	{
		this.preVeng = preVeng;
	}

	public boolean isVespPotSkip()
	{
		return vespPotSkip;
	}

	public void setVespPotSkip(boolean vespPotSkip)
	{
		this.vespPotSkip = vespPotSkip;
	}

	public boolean isCrabTank()
	{
		return crabTank;
	}

	public void setCrabTank(boolean crabTank)
	{
		this.crabTank = crabTank;
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

	public boolean isOverThieve()
	{
		return overThieve;
	}

	public void setOverThieve(boolean overThieve)
	{
		this.overThieve = overThieve;
	}

	public void setIceMilkSeconds(int seconds)
	{
		iceMilkSeconds = clamp(seconds);
	}

	public boolean changesTargets()
	{
		return !useTbow
			|| !slayerHelm
			|| !lockpick
			|| !axe
			|| !salve
			|| !preVeng
			|| !vespPotSkip
			|| !crabTank
			|| (iceMilking && iceMilkSeconds > 0)
			|| killRope
			|| milkVespula
			|| overThieve;
	}

	public String note()
	{
		if (!changesTargets())
		{
			return "";
		}
		StringBuilder note = new StringBuilder("Targets adjusted:");
		if (!useTbow)
		{
			note.append(" no twisted bow");
		}
		if (!slayerHelm)
		{
			note.append(" no slayer helm");
		}
		if (!lockpick)
		{
			note.append(" no lockpick");
		}
		if (!axe)
		{
			note.append(" no axe");
		}
		if (!salve)
		{
			note.append(" no salve");
		}
		if (!preVeng)
		{
			note.append(" no pre-veng");
		}
		if (!vespPotSkip)
		{
			note.append(" no vesp pot skip");
		}
		if (!crabTank)
		{
			note.append(" no crab tank");
		}
		if (killRope)
		{
			note.append(" killing rope");
		}
		if (milkVespula)
		{
			note.append(" vesp milk");
		}
		if (iceMilking && iceMilkSeconds > 0)
		{
			note.append(" ice milking");
		}
		if (overThieve)
		{
			note.append(" over-thieve");
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
		if (!useTbow)
		{
			shift(out, "Shamans", NO_TBOW_SHAMANS);
			shift(out, "Vanguards", NO_TBOW_VANGUARDS);
			shift(out, "Vasa", NO_TBOW_VASA);
			shift(out, "Muttadiles", NO_TBOW_MUTTADILES);
			shift(out, "Olm head", NO_TBOW_OLM_HEAD);
			if (killRope)
			{
				shift(out, "Tightrope", NO_TBOW_TIGHTROPE);
			}
		}
		if (!slayerHelm)
		{
			shift(out, "Shamans", NO_HELM_SHAMANS);
		}
		if (!lockpick)
		{
			shift(out, "Thieving", NO_LOCKPICK_THIEVING);
		}
		if (!axe)
		{
			shift(out, "Ice demon", NO_AXE_ICE);
		}
		if (!salve)
		{
			shift(out, "Mystics", NO_SALVE_MYSTICS);
		}
		if (!preVeng)
		{
			shift(out, "Tekton", NO_PRE_VENG_TEKTON);
		}
		if (!vespPotSkip)
		{
			shift(out, "Tightrope", NO_VESP_POT_SKIP_TIGHTROPE);
		}
		if (!crabTank)
		{
			shift(out, "Crabs", NO_CRAB_TANK_CRABS);
		}
		if (iceMilking)
		{
			shift(out, "Ice demon", iceMilkSeconds);
		}
		if (killRope)
		{
			shift(out, "Tightrope", KILL_ROPE_SECONDS);
		}
		if (milkVespula)
		{
			shift(out, "Vespula", MILK_VESPULA_SECONDS);
		}
		if (overThieve)
		{
			shift(out, "Thieving", OVER_THIEVE_SECONDS);
		}
		derivePreOlm(out);
		deriveOlm(out);
		deriveRaid(out);
		return out;
	}

	/** Pre-Olm is the sum of the prep-room targets. Rooms with no target are left out. */
	private static void derivePreOlm(Map<String, Integer> sheet)
	{
		int sum = 0;
		boolean any = false;
		for (int i = 0; i < RoomNames.PREP_ROOMS.size(); i++)
		{
			Integer value = sheet.get(RoomNames.PREP_ROOMS.get(i));
			if (value != null && value > 0)
			{
				sum += value;
				any = true;
			}
		}
		if (any)
		{
			sheet.put("Pre-Olm", sum);
		}
		else
		{
			sheet.remove("Pre-Olm");
		}
	}

	/** Olm is the phase targets plus the head, plus one minute between phases. Mage hand is already inside the phase. */
	private static final int OLM_GAP_SECONDS = 60;

	private static void deriveOlm(Map<String, Integer> sheet)
	{
		int sum = 0;
		boolean any = false;
		for (int phase = 1; phase <= 8; phase++)
		{
			Integer value = sheet.get("Olm phase " + phase);
			if (value != null && value > 0)
			{
				sum += value;
				any = true;
			}
		}
		Integer head = sheet.get("Olm head");
		if (head != null && head > 0)
		{
			sum += head;
			any = true;
		}
		if (any)
		{
			sheet.put("Olm", sum + OLM_GAP_SECONDS);
		}
		else
		{
			sheet.remove("Olm");
		}
	}

	/** Raid time is Pre-Olm plus Olm plus the between-rooms target. */
	private static void deriveRaid(Map<String, Integer> sheet)
	{
		Integer pre = sheet.get("Pre-Olm");
		Integer olm = sheet.get("Olm");
		Integer between = sheet.get("Between room time");
		if (pre == null || pre <= 0 || olm == null || olm <= 0 || between == null || between <= 0)
		{
			sheet.remove("Raid Completed");
			return;
		}
		sheet.put("Raid Completed", pre + olm + between);
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
