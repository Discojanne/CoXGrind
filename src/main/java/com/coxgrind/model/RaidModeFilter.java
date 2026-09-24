package com.coxgrind.model;

public enum RaidModeFilter
{
	ALL("All"),
	REGULAR("Regular"),
	REGULAR_FULL("Regular full"),
	CM("CM");

	private final String label;

	RaidModeFilter(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
