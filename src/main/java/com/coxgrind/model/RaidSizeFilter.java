package com.coxgrind.model;

public enum RaidSizeFilter
{
	ALL("All"),
	SOLO("Solo"),
	TEAM("Team");

	private final String label;

	RaidSizeFilter(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
