package com.coxgrind.model;

/**
 * Which target sheet a filter uses. Regular full shares the regular sheet for that team size.
 * All, for mode or size, has no sheet.
 */
public enum TargetSheet
{
	REGULAR_SOLO("Regular solo targets"),
	REGULAR_TEAM("Regular team targets"),
	CM_SOLO("CM solo targets"),
	CM_TEAM("CM team targets");

	private final String label;

	TargetSheet(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}

	/** Section title on the settings page. Ignores the open and closed marks RuneLite puts on the button. */
	public static TargetSheet fromTitle(String title)
	{
		if (title == null || title.isEmpty())
		{
			return null;
		}
		if ("Regular solo".equals(title))
		{
			return REGULAR_SOLO;
		}
		if ("Regular team".equals(title))
		{
			return REGULAR_TEAM;
		}
		if ("CM solo".equals(title))
		{
			return CM_SOLO;
		}
		if ("CM team".equals(title))
		{
			return CM_TEAM;
		}
		if (title.indexOf('<') < 0 && title.indexOf('+') < 0 && title.indexOf('\u2212') < 0)
		{
			return null;
		}
		String plain = title.replaceAll("<[^>]*>", "").replace('\u2212', ' ').replace('+', ' ').trim().replaceAll("\\s+", " ");
		if ("Regular solo".equals(plain))
		{
			return REGULAR_SOLO;
		}
		if ("Regular team".equals(plain))
		{
			return REGULAR_TEAM;
		}
		if ("CM solo".equals(plain))
		{
			return CM_SOLO;
		}
		if ("CM team".equals(plain))
		{
			return CM_TEAM;
		}
		return null;
	}

	public static TargetSheet of(RaidModeFilter mode, RaidSizeFilter size)
	{
		if (mode == null || size == null || mode == RaidModeFilter.ALL || size == RaidSizeFilter.ALL)
		{
			return null;
		}
		boolean team = size.usesTeamSheet();
		if (mode == RaidModeFilter.CM)
		{
			return team ? CM_TEAM : CM_SOLO;
		}
		if (mode == RaidModeFilter.REGULAR || mode == RaidModeFilter.REGULAR_FULL)
		{
			return team ? REGULAR_TEAM : REGULAR_SOLO;
		}
		return null;
	}
}
