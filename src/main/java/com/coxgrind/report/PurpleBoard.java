package com.coxgrind.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Numbers behind the Purples tab. The text report still prints these itself.
 */
public final class PurpleBoard
{
	public enum Mark
	{
		DRY,
		PURPLE,
		EXPECTED,
		/** A purple that also landed on the expected raid. */
		BOTH,
		NEXT
	}

	public static final class Item
	{
		private final String name;
		private final int got;
		private final double onRate;

		public Item(String name, int got, double onRate)
		{
			this.name = name == null ? "" : name;
			this.got = got;
			this.onRate = onRate;
		}

		public String getName()
		{
			return name;
		}

		public int getGot()
		{
			return got;
		}

		public double getOnRate()
		{
			return onRate;
		}

		public double getDiff()
		{
			return got - onRate;
		}
	}

	public static final class Drop
	{
		private final String kc;
		private final String item;
		private final boolean challengeMode;
		/** Empty for a purple. Pet, kit, and dust use {@code pet}, {@code kit}, and {@code dust}. */
		private final String kind;

		public Drop(String kc, String item, boolean challengeMode)
		{
			this(kc, item, challengeMode, "");
		}

		public Drop(String kc, String item, boolean challengeMode, String kind)
		{
			this.kc = kc == null ? "" : kc;
			this.item = item == null ? "" : item;
			this.challengeMode = challengeMode;
			this.kind = kind == null ? "" : kind;
		}

		public String getKc()
		{
			return kc;
		}

		public String getItem()
		{
			return item;
		}

		public boolean isChallengeMode()
		{
			return challengeMode;
		}

		public String getKind()
		{
			return kind;
		}
	}

	private final int raids;
	private final int regular;
	private final int challengeMode;
	private final int actual;
	private final int scrolls;
	private final long allPoints;
	private final long pointsLost;
	private final int deathRaids;
	private final double averagePoints;
	private final double expected;
	private final int currentDry;
	private final int longestDry;
	private final int averageDry;
	private final int expectedEvery;
	private final List<Item> items;
	private final List<Item> sideItems;
	private final List<Mark> marks;
	/** Parallel to {@link #marks}. Empty when that raid has no pet, kit, or dust. */
	private final List<String> sideKinds;
	private final List<Drop> tracked;

	public PurpleBoard(
		int raids,
		int regular,
		int challengeMode,
		int actual,
		int scrolls,
		long allPoints,
		long pointsLost,
		int deathRaids,
		double averagePoints,
		double expected,
		int currentDry,
		int longestDry,
		int averageDry,
		int expectedEvery,
		List<Item> items,
		List<Item> sideItems,
		List<Mark> marks,
		List<String> sideKinds,
		List<Drop> tracked)
	{
		this.raids = raids;
		this.regular = regular;
		this.challengeMode = challengeMode;
		this.actual = actual;
		this.scrolls = scrolls;
		this.allPoints = allPoints;
		this.pointsLost = pointsLost;
		this.deathRaids = deathRaids;
		this.averagePoints = averagePoints;
		this.expected = expected;
		this.currentDry = currentDry;
		this.longestDry = longestDry;
		this.averageDry = averageDry;
		this.expectedEvery = expectedEvery;
		this.items = items == null ? new ArrayList<Item>() : items;
		this.sideItems = sideItems == null ? new ArrayList<Item>() : sideItems;
		this.marks = marks == null ? new ArrayList<Mark>() : marks;
		this.sideKinds = sideKinds == null ? new ArrayList<String>() : sideKinds;
		this.tracked = tracked == null ? new ArrayList<Drop>() : tracked;
	}

	public static PurpleBoard empty()
	{
		return new PurpleBoard(0, 0, 0, 0, 0, 0L, 0L, 0, 0, 0, 0, 0, 0, 0,
			Collections.<Item>emptyList(), Collections.<Item>emptyList(),
			Collections.<Mark>emptyList(), Collections.<String>emptyList(), Collections.<Drop>emptyList());
	}

	public boolean isEmpty()
	{
		return raids == 0;
	}

	public int getRaids()
	{
		return raids;
	}

	public int getRegular()
	{
		return regular;
	}

	public int getChallengeMode()
	{
		return challengeMode;
	}

	public int getActual()
	{
		return actual;
	}

	public int getScrolls()
	{
		return scrolls;
	}

	public long getAllPoints()
	{
		return allPoints;
	}

	public long getPointsLost()
	{
		return pointsLost;
	}

	/** Raids with at least one logged death. */
	public int getDeathRaids()
	{
		return deathRaids;
	}

	/** Share of logged raids with at least one death. Zero when there are no raids. */
	public double getDeathRate()
	{
		return raids > 0 ? (double) deathRaids / raids : 0;
	}

	/** Points lost divided by the points one purple takes. */
	public double getLostPurples()
	{
		return pointsLost / RaidReportFormatter.PURPLE_POINTS;
	}

	/** Points lost divided by the average personal points. Zero when that average is 0. */
	public double getLostRaids()
	{
		return averagePoints > 0 ? pointsLost / averagePoints : 0;
	}

	public double getAveragePoints()
	{
		return averagePoints;
	}

	public double getExpected()
	{
		return expected;
	}

	public double getDiff()
	{
		return actual - expected;
	}

	public double getRate()
	{
		return expected > 0 ? raids / expected : 0;
	}

	public long getRaidsWorth()
	{
		return Math.round(getDiff() * getRate());
	}

	public int getCurrentDry()
	{
		return currentDry;
	}

	public int getLongestDry()
	{
		return longestDry;
	}

	public int getAverageDry()
	{
		return averageDry;
	}

	public int getExpectedEvery()
	{
		return expectedEvery;
	}

	public List<Item> getItems()
	{
		return items;
	}

	public List<Item> getSideItems()
	{
		return sideItems;
	}

	public List<Mark> getMarks()
	{
		return marks;
	}

	public List<String> getSideKinds()
	{
		return sideKinds;
	}

	public List<Drop> getTracked()
	{
		return tracked;
	}

	/** True when both boards would draw the same Purples tab. */
	public boolean same(PurpleBoard other)
	{
		if (other == null)
		{
			return false;
		}
		if (raids != other.raids || regular != other.regular || challengeMode != other.challengeMode
			|| actual != other.actual || scrolls != other.scrolls || allPoints != other.allPoints
			|| pointsLost != other.pointsLost || deathRaids != other.deathRaids
			|| currentDry != other.currentDry || longestDry != other.longestDry
			|| averageDry != other.averageDry || expectedEvery != other.expectedEvery
			|| Double.compare(averagePoints, other.averagePoints) != 0
			|| Double.compare(expected, other.expected) != 0)
		{
			return false;
		}
		return sameItems(items, other.items) && sameItems(sideItems, other.sideItems)
			&& sameMarks(marks, other.marks) && sameStrings(sideKinds, other.sideKinds)
			&& sameDrops(tracked, other.tracked);
	}

	private static boolean sameItems(List<Item> left, List<Item> right)
	{
		if (left.size() != right.size())
		{
			return false;
		}
		for (int i = 0; i < left.size(); i++)
		{
			Item a = left.get(i);
			Item b = right.get(i);
			if (a.got != b.got || Double.compare(a.onRate, b.onRate) != 0 || !a.name.equals(b.name))
			{
				return false;
			}
		}
		return true;
	}

	private static boolean sameDrops(List<Drop> left, List<Drop> right)
	{
		if (left.size() != right.size())
		{
			return false;
		}
		for (int i = 0; i < left.size(); i++)
		{
			Drop a = left.get(i);
			Drop b = right.get(i);
			if (a.challengeMode != b.challengeMode || !a.kc.equals(b.kc) || !a.item.equals(b.item) || !a.kind.equals(b.kind))
			{
				return false;
			}
		}
		return true;
	}

	private static boolean sameMarks(List<Mark> left, List<Mark> right)
	{
		if (left.size() != right.size())
		{
			return false;
		}
		for (int i = 0; i < left.size(); i++)
		{
			if (left.get(i) != right.get(i))
			{
				return false;
			}
		}
		return true;
	}

	private static boolean sameStrings(List<String> left, List<String> right)
	{
		if (left.size() != right.size())
		{
			return false;
		}
		for (int i = 0; i < left.size(); i++)
		{
			String a = left.get(i);
			String b = right.get(i);
			if (a == null ? b != null : !a.equals(b))
			{
				return false;
			}
		}
		return true;
	}
}
