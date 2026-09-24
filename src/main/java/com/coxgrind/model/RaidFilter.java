package com.coxgrind.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Solo is a team of 1. Team is 2 or more. A raid with an unknown team size is included only in All.
 */
public final class RaidFilter
{
	private RaidFilter()
	{
	}

	public static List<CoxRaidRecord> apply(List<CoxRaidRecord> raids, RaidModeFilter mode, RaidSizeFilter size)
	{
		List<CoxRaidRecord> out = new ArrayList<>();
		if (raids == null)
		{
			return out;
		}
		for (int i = 0; i < raids.size(); i++)
		{
			CoxRaidRecord raid = raids.get(i);
			if (raid != null && matches(raid, mode, size))
			{
				out.add(raid);
			}
		}
		return out;
	}

	public static boolean matches(CoxRaidRecord raid, RaidModeFilter mode, RaidSizeFilter size)
	{
		if (mode == RaidModeFilter.CM && !raid.isChallengeMode())
		{
			return false;
		}
		if (mode == RaidModeFilter.REGULAR && (raid.isChallengeMode() || raid.isFullLayout()))
		{
			return false;
		}
		if (mode == RaidModeFilter.REGULAR_FULL && (raid.isChallengeMode() || !raid.isFullLayout()))
		{
			return false;
		}
		if (size == RaidSizeFilter.SOLO && raid.getTeamSize() != 1)
		{
			return false;
		}
		if (size == RaidSizeFilter.TEAM && raid.getTeamSize() < 2)
		{
			return false;
		}
		return true;
	}
}
