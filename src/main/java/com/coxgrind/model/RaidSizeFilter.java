package com.coxgrind.model;

/**
 * All, solo, every team size, or one party size. A saved Team choice still means every team of 2 or more.
 */
public final class RaidSizeFilter
{
	public static final RaidSizeFilter ALL = new RaidSizeFilter(0, "All", "ALL");
	public static final RaidSizeFilter SOLO = new RaidSizeFilter(1, "Solo", "SOLO");
	public static final RaidSizeFilter TEAM = new RaidSizeFilter(-1, "Team", "TEAM");

	private final int party;
	private final String label;
	private final String key;

	private RaidSizeFilter(int party, String label, String key)
	{
		this.party = party;
		this.label = label;
		this.key = key;
	}

	/** One party size of 2 or more. */
	public static RaidSizeFilter of(int party)
	{
		if (party <= 1)
		{
			return SOLO;
		}
		return new RaidSizeFilter(party, Integer.toString(party), Integer.toString(party));
	}

	public static RaidSizeFilter fromKey(String key)
	{
		if (key == null || key.isEmpty())
		{
			return null;
		}
		if ("ALL".equals(key))
		{
			return ALL;
		}
		if ("SOLO".equals(key))
		{
			return SOLO;
		}
		if ("TEAM".equals(key))
		{
			return TEAM;
		}
		try
		{
			int party = Integer.parseInt(key);
			if (party >= 2 && party <= 100)
			{
				return of(party);
			}
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
		return null;
	}

	public String key()
	{
		return key;
	}

	/** 1 for solo, -1 for every team, 2 or more for one party size, 0 for all. */
	public int getParty()
	{
		return party;
	}

	public boolean isAll()
	{
		return party == 0;
	}

	/** Team, or one party size of 2 or more. Both use the team target sheet. */
	public boolean usesTeamSheet()
	{
		return party < 0 || party >= 2;
	}

	public boolean matches(int teamSize)
	{
		if (party == 0)
		{
			return true;
		}
		if (party == 1)
		{
			return teamSize == 1;
		}
		if (party < 0)
		{
			return teamSize >= 2;
		}
		return teamSize == party;
	}

	@Override
	public String toString()
	{
		return label;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof RaidSizeFilter))
		{
			return false;
		}
		return party == ((RaidSizeFilter) other).party;
	}

	@Override
	public int hashCode()
	{
		return party;
	}
}
