package com.coxgrind.report;

import com.coxgrind.model.CoxRaidRecord;
import com.coxgrind.model.RaidFilter;
import com.coxgrind.model.RaidModeFilter;
import com.coxgrind.model.RaidSizeFilter;
import com.coxgrind.model.RoomSplit;
import com.coxgrind.track.RoomNames;
import com.coxgrind.track.TimeFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Plain-text raid report in the same shape as the owner's Coxparser tables.
 * Time diffs use + when the split is slower than your average.
 * Point diffs use + when the value is higher than your average.
 */
public final class RaidReportFormatter
{
	static final double PURPLE_POINTS = 867600.0;

	/** Sidebar purple text, history dots, and the tracked-purples rule share this width. */
	private static final int SIDEBAR_W = 28;
	/** Active, Target, and Bests lines: room, time, and a short diff. */
	private static final int TIME_RULE = 29;
	private static final int ROOM_W = 22;
	private static final int BEST_W = 7;
	private static final int AVG_W = 8;
	private static final int VALUE_W = 7;
	private static final int DIFF_W = 8;
	private static final int SEP = 2;
	private static final int CELL_W = VALUE_W + 1 + DIFF_W;

	private static final Map<String, Integer> MIN_SECONDS = mapOf(
		"Tekton", 30,
		"Crabs", 45,
		"Ice demon", 90,
		"Shamans", 27,
		"Vanguards", 60,
		"Thieving", 45,
		"Vespula", 15,
		"Tightrope", 25,
		"Guardians", 35,
		"Vasa", 30,
		"Mystics", 30,
		"Muttadiles", 45,
		"Between room time", 20
	);

	/** Ice demon at or under 3:50 is a normal kill. Over 3:50 is milking. */
	static final int ICE_NORMAL_MAX = 230;
	/** Milking times longer than 4:30 stay out of the averages. */
	static final int ICE_MILK_MAX = 270;

	private static final Map<String, Integer> MAX_SECONDS = mapOf(
		"Tekton", 240,
		"Crabs", 240,
		"Shamans", 240,
		"Vanguards", 240,
		"Thieving", 240,
		"Vespula", 240,
		"Tightrope", 240,
		"Guardians", 240,
		"Vasa", 240,
		"Mystics", 240,
		"Muttadiles", 240,
		"Between room time", 240
	);

	private static final List<ItemWeight> REGULAR_WEIGHTS = Arrays.asList(
		new ItemWeight("Dexterous prayer scroll", 14),
		new ItemWeight("Arcane prayer scroll", 14),
		new ItemWeight("Twisted buckler", 4),
		new ItemWeight("Dragon hunter crossbow", 4),
		new ItemWeight("Dinh's bulwark", 3),
		new ItemWeight("Ancestral hat", 4),
		new ItemWeight("Ancestral robe top", 4),
		new ItemWeight("Ancestral robe bottom", 4),
		new ItemWeight("Dragon claws", 3),
		new ItemWeight("Elder maul", 2),
		new ItemWeight("Kodai insignia", 2),
		new ItemWeight("Twisted bow", 2)
	);

	private static final List<ItemWeight> CM_WEIGHTS = Arrays.asList(
		new ItemWeight("Dexterous prayer scroll", 12),
		new ItemWeight("Arcane prayer scroll", 12),
		new ItemWeight("Twisted buckler", 4),
		new ItemWeight("Dragon hunter crossbow", 4),
		new ItemWeight("Dinh's bulwark", 3),
		new ItemWeight("Ancestral hat", 4),
		new ItemWeight("Ancestral robe top", 4),
		new ItemWeight("Ancestral robe bottom", 4),
		new ItemWeight("Dragon claws", 3),
		new ItemWeight("Elder maul", 2),
		new ItemWeight("Kodai insignia", 2),
		new ItemWeight("Twisted bow", 2)
	);

	private RaidReportFormatter()
	{
	}

	public static String format(List<CoxRaidRecord> allRaids, RaidModeFilter mode, RaidSizeFilter size, int lastN)
	{
		return format(allRaids, mode, size, ReportOptions.defaults(lastN));
	}

	public static String format(List<CoxRaidRecord> allRaids, RaidModeFilter mode, RaidSizeFilter size, ReportOptions options)
	{
		ReportOptions settings = options == null ? ReportOptions.defaults(10) : options;
		int window = settings.getLastN();
		boolean milk = includeIceMilking(settings);
		List<CoxRaidRecord> raids = select(allRaids, mode, size, settings);
		List<CoxRaidRecord> account = allRaids == null ? new ArrayList<CoxRaidRecord>() : allRaids;
		Map<String, Integer> targets = settings.comparisonSheet(mode);
		boolean compare = !targets.isEmpty();
		StringBuilder out = new StringBuilder();
		out.append(heading(mode, size, raids.size())).append('\n');
		out.append('\n');
		if (raids.isEmpty())
		{
			out.append("No completed raids in this filter yet.\n");
			out.append("Finish a Chambers of Xeric raid in this client.\n");
			out.append('\n');
		}
		else
		{
		List<String> rows = rowsFor(raids);
		Map<String, Column> columns = buildColumns(raids, rows, milk);
		int excluded = countExcluded(raids, rows, milk);
		String header = header(window, compare);
		String rule = repeat('=', header.length());
		String thin = repeat('-', header.length());
		out.append(header).append('\n');
		out.append(thin).append('\n');
		for (int i = 0; i < rows.size(); i++)
		{
			String row = rows.get(i);
			Column column = columns.get(row);
			if (column == null || (RoomNames.isPrepRoom(row) && column.count() == 0))
			{
				continue;
			}
			boolean points = "Total Points".equals(row) || "PPH".equals(row);
			out.append(formatRow(row, column, window, points, compare ? targets.get(row) : null, compare));
			if ("Pre-Olm".equals(row) || "Raid Completed".equals(row) || "Between room time".equals(row))
			{
				out.append(thin).append('\n');
			}
		}
		out.append(rule).append('\n');
		double avgPoints = averagePoints(raids);
		if (avgPoints > 0)
		{
			out.append(String.format(Locale.US, "Purple pace (this table): 1 in %.2f%n", PURPLE_POINTS / avgPoints));
		}
		else
		{
			out.append("Purple pace (this table): 1 in --\n");
		}
		out.append("Time diffs: + is slower than your average. Point diffs: + is higher than your average.\n");
		if (compare)
		{
			out.append("vs Target is the benchmark you entered, compared with your average.\n");
			String styleNote = settings.getTargetStyle().note();
			if (styleNote.length() > 0)
			{
				out.append(styleNote).append('\n');
			}
		}
		if (excluded > 0)
		{
			out.append("Excluded ").append(excluded).append(" very short or very long splits from the averages.\n");
		}
		out.append('\n');
		if (settings.isRoomEfficiency())
		{
			appendRoomEfficiency(out, raids, milk);
		}
		if (settings.isCommonRooms())
		{
			appendCommonRooms(out, raids, milk);
		}
		}
		if (!account.isEmpty())
		{
			if (settings.isPurpleSummary() || settings.isTrackedPurples())
			{
				out.append("Purple numbers use every logged raid, not the time filter.\n");
				out.append('\n');
			}
			if (settings.isPurpleSummary())
			{
				appendPurple(out, account);
			}
			if (settings.isTrackedPurples())
			{
				appendTrackedPurples(out, account);
			}
			if (settings.isPurpleSummary())
			{
				appendLoggedDeaths(out, account);
				appendDeath(out, account, settings);
			}
			appendAccount(out, account);
		}
		if (settings.isOutliers() && !raids.isEmpty())
		{
			appendOutliers(out, raids, milk);
		}
		return out.toString();
	}

	public static String recentVersusAverage(List<CoxRaidRecord> allRaids, RaidModeFilter mode, RaidSizeFilter size, ReportOptions options)
	{
		return paceView(allRaids, mode, size, options, null, -1, false).getText();
	}

	/**
	 * Sidebar pace. A null live raid is the latest saved raid in the filter.
	 * A live raid replaces that view until the raid is abandoned.
	 * {@code openSeconds} is the unnamed split still running. Pass -1 when there is nothing to show.
	 */
	public static PaceComparison paceView(
		List<CoxRaidRecord> allRaids,
		RaidModeFilter mode,
		RaidSizeFilter size,
		ReportOptions options,
		CoxRaidRecord live,
		int openSeconds,
		boolean inProgress)
	{
		ReportOptions settings = options == null ? ReportOptions.defaults(10) : options;
		boolean milk = includeIceMilking(settings);
		List<CoxRaidRecord> raids = select(allRaids, mode, size, settings);
		if (live == null)
		{
			return recentPace(raids, milk);
		}
		StringBuilder out = new StringBuilder();
		if (inProgress)
		{
			if (!appendInProgress(out, raids, live, openSeconds, milk))
			{
				out.append("Waiting for the first room.\n");
			}
		}
		else
		{
			List<CoxRaidRecord> combined = new ArrayList<>(raids);
			combined.add(live);
			appendRecentCompare(out, combined, null, false, milk);
		}
		boolean finished = !inProgress && live.getTotalSeconds() > 0;
		return new PaceComparison(out.toString(), paceLine(raids, live, finished, milk));
	}

	public static String recentVersusTarget(List<CoxRaidRecord> allRaids, RaidModeFilter mode, RaidSizeFilter size, ReportOptions options)
	{
		ReportOptions settings = options == null ? ReportOptions.defaults(10) : options;
		List<CoxRaidRecord> raids = select(allRaids, mode, size, settings);
		if (raids.isEmpty())
		{
			return "No completed raids in this filter yet.\n";
		}
		Map<String, Integer> targets = settings.comparisonSheet(mode);
		if (targets.isEmpty())
		{
			if (mode == RaidModeFilter.ALL)
			{
				return "Pick Regular, Regular full, or CM to compare with your targets.\n";
			}
			return "Set targets in the CoXGrind plugin settings.\n";
		}
		StringBuilder out = new StringBuilder();
		appendRecentCompare(out, raids, targets, true, includeIceMilking(settings));
		return out.toString();
	}

	/**
	 * Fastest valid split for each row in the filter, with the kill count it came from.
	 * Points and PPH use the highest value. Times are gold.
	 */
	public static String bestSplits(List<CoxRaidRecord> allRaids, RaidModeFilter mode, RaidSizeFilter size, ReportOptions options)
	{
		ReportOptions settings = options == null ? ReportOptions.defaults(10) : options;
		boolean milk = includeIceMilking(settings);
		List<CoxRaidRecord> raids = select(allRaids, mode, size, settings);
		if (raids.isEmpty())
		{
			return "No completed raids in this filter yet.\n";
		}
		StringBuilder out = new StringBuilder();
		List<String> rows = rowsFor(raids);
		boolean any = false;
		for (int r = 0; r < rows.size(); r++)
		{
			String row = rows.get(r);
			boolean higher = "Total Points".equals(row) || "PPH".equals(row);
			CoxRaidRecord bestRaid = null;
			int best = higher ? Integer.MIN_VALUE : Integer.MAX_VALUE;
			for (int i = 0; i < raids.size(); i++)
			{
				Integer value = valueFor(raids.get(i), row, milk);
				if (value == null || (higher ? value <= best : value >= best))
				{
					continue;
				}
				best = value;
				bestRaid = raids.get(i);
			}
			if (bestRaid == null)
			{
				continue;
			}
			any = true;
			String kc = (bestRaid.isChallengeMode() ? "CM " : "KC ") + bestRaid.getKc();
			String shown = higher ? right(Integer.toString(best), 6) : ReportColor.gold(right(TimeFormat.formatSeconds(best), 6));
			out.append(left(shortRoom(row), 15))
				.append(' ')
				.append(shown)
				.append(' ')
				.append(kc)
				.append('\n');
			appendTimeBreak(out, row);
		}
		if (!any)
		{
			return "No room times in this filter yet.\n";
		}
		return out.toString();
	}

	private static PaceComparison recentPace(List<CoxRaidRecord> raids, boolean milk)
	{
		if (raids.isEmpty())
		{
			return new PaceComparison("No completed raids in this filter yet.\n", new ArrayList<PacePoint>());
		}
		StringBuilder out = new StringBuilder();
		appendRecentCompare(out, raids, null, false, milk);
		CoxRaidRecord subject = raids.get(raids.size() - 1);
		List<CoxRaidRecord> prior = new ArrayList<>(raids.subList(0, raids.size() - 1));
		return new PaceComparison(out.toString(), paceLine(prior, subject, subject.getTotalSeconds() > 0, milk));
	}

	/**
	 * Sidebar purple tab. Mode, size, and the raids-in-report limit are ignored.
	 */
	public static String purpleView(List<CoxRaidRecord> allRaids, ReportOptions options)
	{
		ReportOptions settings = options == null ? ReportOptions.defaults(10) : options;
		List<CoxRaidRecord> raids = allRaids == null ? new ArrayList<CoxRaidRecord>() : allRaids;
		if (raids.isEmpty())
		{
			return "No completed raids logged yet.\n";
		}
		StringBuilder out = new StringBuilder();
		if (settings.isPurpleSummary())
		{
			appendPurple(out, raids, true);
		}
		if (settings.isTrackedPurples())
		{
			appendTrackedPurples(out, raids, true);
		}
		if (out.length() == 0)
		{
			return "Purple summary is turned off in the plugin settings.\n";
		}
		return out.toString();
	}

	/**
	 * Same whole-log purple numbers as {@link #purpleView}, without the text layout.
	 */
	public static PurpleBoard purpleBoard(List<CoxRaidRecord> allRaids)
	{
		List<CoxRaidRecord> raids = allRaids == null ? new ArrayList<CoxRaidRecord>() : allRaids;
		if (raids.isEmpty())
		{
			return PurpleBoard.empty();
		}
		int regularRaids = 0;
		int cmRaids = 0;
		int actual = 0;
		int scrolls = 0;
		double expected = 0;
		Map<String, Integer> got = new LinkedHashMap<>();
		Map<String, Double> expectedItems = new LinkedHashMap<>();
		for (int i = 0; i < REGULAR_WEIGHTS.size(); i++)
		{
			String name = REGULAR_WEIGHTS.get(i).name;
			got.put(name, 0);
			expectedItems.put(name, 0.0);
		}
		for (int i = 0; i < raids.size(); i++)
		{
			CoxRaidRecord raid = raids.get(i);
			if (raid.isChallengeMode())
			{
				cmRaids++;
			}
			else
			{
				regularRaids++;
			}
			if (raid.hasPurple())
			{
				actual++;
				String item = raid.getPurple();
				if (got.containsKey(item))
				{
					got.put(item, got.get(item) + 1);
				}
				if ("Dexterous prayer scroll".equals(item) || "Arcane prayer scroll".equals(item))
				{
					scrolls++;
				}
			}
			if (raid.getPersonalPoints() <= 0)
			{
				continue;
			}
			double chance = raid.getPersonalPoints() / PURPLE_POINTS;
			expected += chance;
			List<ItemWeight> table = raid.isChallengeMode() ? CM_WEIGHTS : REGULAR_WEIGHTS;
			int totalWeight = raid.isChallengeMode() ? 56 : 60;
			for (int w = 0; w < table.size(); w++)
			{
				ItemWeight item = table.get(w);
				expectedItems.put(item.name, expectedItems.get(item.name) + chance * item.weight / totalWeight);
			}
		}
		int streak = 0;
		int longest = 0;
		int finishedSum = 0;
		int finishedCount = 0;
		for (int i = 0; i < raids.size(); i++)
		{
			if (raids.get(i).hasPurple())
			{
				if (streak > 0)
				{
					finishedSum += streak;
					finishedCount++;
					longest = Math.max(longest, streak);
				}
				streak = 0;
			}
			else
			{
				streak++;
				longest = Math.max(longest, streak);
			}
		}
		int current = streak;
		if (streak > 0)
		{
			finishedSum += streak;
			finishedCount++;
		}
		int averageDry = finishedCount == 0 ? 0 : (int) Math.round((double) finishedSum / finishedCount);
		double rate = expected > 0 ? raids.size() / expected : 0;
		int expectedEvery = rate > 0 ? Math.max(1, (int) Math.round(rate)) : 0;
		List<PurpleBoard.Item> items = new ArrayList<>();
		for (int i = 0; i < REGULAR_WEIGHTS.size(); i++)
		{
			String name = REGULAR_WEIGHTS.get(i).name;
			items.add(new PurpleBoard.Item(name, got.get(name), expectedItems.get(name)));
		}
		int pets = 0;
		int kits = 0;
		int dusts = 0;
		List<PurpleBoard.Mark> marks = new ArrayList<>();
		List<String> sideKinds = new ArrayList<>();
		for (int i = 0; i < raids.size(); i++)
		{
			boolean purple = raids.get(i).hasPurple();
			boolean onRate = expectedEvery > 0 && (i + 1) % expectedEvery == 0;
			if (purple && onRate)
			{
				marks.add(PurpleBoard.Mark.BOTH);
			}
			else if (purple)
			{
				marks.add(PurpleBoard.Mark.PURPLE);
			}
			else if (onRate)
			{
				marks.add(PurpleBoard.Mark.EXPECTED);
			}
			else
			{
				marks.add(PurpleBoard.Mark.DRY);
			}
			String kind = "";
			List<String> extras = raids.get(i).getExtras();
			for (int e = 0; e < extras.size(); e++)
			{
				String extraKind = com.coxgrind.track.RaidChat.sideKind(extras.get(e));
				if ("pet".equals(extraKind))
				{
					pets++;
				}
				else if ("kit".equals(extraKind))
				{
					kits++;
				}
				else if ("dust".equals(extraKind))
				{
					dusts++;
				}
				if (kind.isEmpty())
				{
					kind = extraKind;
				}
			}
			sideKinds.add(kind);
		}
		marks.add(PurpleBoard.Mark.NEXT);
		sideKinds.add("");
		List<PurpleBoard.Item> sideItems = new ArrayList<>();
		sideItems.add(new PurpleBoard.Item("Olmlet", pets, 0));
		sideItems.add(new PurpleBoard.Item("Twisted ancestral colour kit", kits, 0));
		sideItems.add(new PurpleBoard.Item("Metamorphic dust", dusts, 0));
		List<PurpleBoard.Drop> tracked = new ArrayList<>();
		for (int i = raids.size() - 1; i >= 0; i--)
		{
			CoxRaidRecord raid = raids.get(i);
			String kc = raid.getKc() > 0 ? Integer.toString(raid.getKc()) : "--";
			if (raid.hasPurple())
			{
				tracked.add(new PurpleBoard.Drop(kc, raid.getPurple(), raid.isChallengeMode()));
			}
			List<String> extras = raid.getExtras();
			for (int e = 0; e < extras.size(); e++)
			{
				String extra = extras.get(e);
				String extraKind = com.coxgrind.track.RaidChat.sideKind(extra);
				if (extraKind.isEmpty())
				{
					continue;
				}
				tracked.add(new PurpleBoard.Drop(kc, extra, raid.isChallengeMode(), extraKind));
			}
		}
		return new PurpleBoard(
			raids.size(),
			regularRaids,
			cmRaids,
			actual,
			scrolls,
			totalPoints(raids),
			averagePoints(raids),
			expected,
			current,
			longest,
			averageDry,
			expectedEvery,
			items,
			sideItems,
			marks,
			sideKinds,
			tracked
		);
	}

	private static List<CoxRaidRecord> select(List<CoxRaidRecord> allRaids, RaidModeFilter mode, RaidSizeFilter size, ReportOptions settings)
	{
		List<CoxRaidRecord> raids = RaidFilter.apply(allRaids, mode, size);
		if (settings.getReportRaids() > 0 && raids.size() > settings.getReportRaids())
		{
			raids = new ArrayList<>(raids.subList(raids.size() - settings.getReportRaids(), raids.size()));
		}
		return raids;
	}

	private static boolean appendInProgress(StringBuilder out, List<CoxRaidRecord> history, CoxRaidRecord live, int openSeconds, boolean milk)
	{
		boolean any = false;
		boolean sawPrep = false;
		if (live != null)
		{
			List<RoomSplit> splits = live.getSplits();
			for (int i = 0; i < splits.size(); i++)
			{
				RoomSplit split = splits.get(i);
				if (split == null || split.getRoom() == null || split.getSeconds() <= 0)
				{
					continue;
				}
				if (split.getRoom().startsWith("Floor "))
				{
					continue;
				}
				any = true;
				if (sawPrep && !RoomNames.isPrepRoom(split.getRoom()))
				{
					appendTimeBreak(out, "Pre-Olm");
					sawPrep = false;
				}
				appendTimeLine(out, split.getRoom(), split.getSeconds(), averageOf(history, split.getRoom(), milk));
				if (RoomNames.isPrepRoom(split.getRoom()))
				{
					sawPrep = true;
				}
				appendTimeBreak(out, split.getRoom());
			}
		}
		if (openSeconds > 0)
		{
			any = true;
			out.append(left("Current", 15))
				.append(' ')
				.append(right(TimeFormat.formatSeconds(openSeconds), 6))
				.append('\n');
		}
		return any;
	}

	private static Double averageOf(List<CoxRaidRecord> history, String row, boolean milk)
	{
		Column column = new Column();
		if (history != null)
		{
			for (int i = 0; i < history.size(); i++)
			{
				column.add(valueFor(history.get(i), row, milk));
			}
		}
		if (column.count() == 0)
		{
			return null;
		}
		return column.average();
	}

	private static void appendTimeLine(StringBuilder out, String room, int seconds, Double average)
	{
		String label = shortRoom(room);
		String colored = "--";
		if (average != null)
		{
			int delta = seconds - (int) Math.round(average);
			colored = ReportColor.time(delta, timeDiff(seconds, average));
		}
		out.append(left(label, 15))
			.append(' ')
			.append(right(TimeFormat.formatSeconds(seconds), 6))
			.append(' ')
			.append(colored)
			.append('\n');
	}

	/**
	 * Running seconds ahead of the personal best. Positive means this pace beats the PB if the rest of the raid matches your average.
	 * Mage hand is skipped because that time is already inside the phase. Phase gains are replaced when Olm ends.
	 * The last point of a finished raid is the PB minus the actual finish.
	 */
	private static List<PacePoint> paceLine(List<CoxRaidRecord> comparison, CoxRaidRecord subject, boolean finished, boolean milk)
	{
		List<PacePoint> points = new ArrayList<>();
		if (comparison == null || comparison.isEmpty() || subject == null)
		{
			return points;
		}
		Integer personalBest = bestTime(comparison, "Raid Completed", milk);
		Double avgTotal = averageOf(comparison, "Raid Completed", milk);
		if (personalBest == null || avgTotal == null)
		{
			return points;
		}
		int averageTotal = (int) Math.round(avgTotal);
		int roomBank = 0;
		int phaseBank = 0;
		int olmGain = 0;
		boolean olmApplied = false;
		points.add(new PacePoint("Start", personalBest - averageTotal));
		List<RoomSplit> splits = subject.getSplits();
		for (int i = 0; i < splits.size(); i++)
		{
			RoomSplit split = splits.get(i);
			if (split == null || split.getRoom() == null || split.getSeconds() <= 0)
			{
				continue;
			}
			String room = split.getRoom();
			if (room.startsWith("Floor ") || room.startsWith("Olm mage hand") || "Raid Completed".equals(room))
			{
				continue;
			}
			Double average = averageOf(comparison, room, milk);
			if (average == null)
			{
				continue;
			}
			int gain = (int) Math.round(average) - split.getSeconds();
			if (RoomNames.isPrepRoom(room))
			{
				roomBank += gain;
				points.add(new PacePoint(shortRoom(room), ahead(personalBest, averageTotal, roomBank, phaseBank, olmGain, olmApplied)));
			}
			else if (isOlmPhase(room))
			{
				phaseBank += gain;
				if (!olmApplied)
				{
					points.add(new PacePoint(shortRoom(room), ahead(personalBest, averageTotal, roomBank, phaseBank, olmGain, olmApplied)));
				}
			}
			else if ("Olm".equals(room))
			{
				olmGain = gain;
				olmApplied = true;
				points.add(new PacePoint("Olm", ahead(personalBest, averageTotal, roomBank, phaseBank, olmGain, olmApplied)));
			}
		}
		int total = subject.getTotalSeconds();
		if (total <= 0)
		{
			total = subject.secondsFor("Raid Completed");
		}
		if (finished && total > 0)
		{
			points.add(new PacePoint("Finish", personalBest - total));
		}
		return points;
	}

	private static int ahead(int personalBest, int averageTotal, int roomBank, int phaseBank, int olmGain, boolean olmApplied)
	{
		int bank = roomBank + (olmApplied ? olmGain : phaseBank);
		return personalBest - averageTotal + bank;
	}

	private static boolean isOlmPhase(String room)
	{
		return "Olm head".equals(room) || (room != null && room.startsWith("Olm phase "));
	}

	private static Integer bestTime(List<CoxRaidRecord> raids, String row, boolean milk)
	{
		Integer best = null;
		if (raids == null)
		{
			return null;
		}
		for (int i = 0; i < raids.size(); i++)
		{
			Integer value = valueFor(raids.get(i), row, milk);
			if (value == null)
			{
				continue;
			}
			if (best == null || value < best)
			{
				best = value;
			}
		}
		return best;
	}

	private static void appendRecentCompare(StringBuilder out, List<CoxRaidRecord> raids, Map<String, Integer> targets, boolean againstTarget, boolean milk)
	{
		List<String> rows = rowsFor(raids);
		Map<String, Column> columns = buildColumns(raids, rows, milk);
		for (int i = 0; i < rows.size(); i++)
		{
			String row = rows.get(i);
			Column column = columns.get(row);
			if (column == null || column.recent() == null)
			{
				continue;
			}
			if (RoomNames.isPrepRoom(row) && column.count() == 0)
			{
				continue;
			}
			boolean points = "Total Points".equals(row) || "PPH".equals(row);
			int recent = (int) Math.round(column.recent());
			String shown = points ? Integer.toString(recent) : TimeFormat.formatSeconds(recent);
			String diff = "--";
			String colored = diff;
			if (againstTarget)
			{
				Integer target = targets == null ? null : targets.get(row);
				if (target != null && target > 0)
				{
					int delta = recent - target;
					diff = points ? pointDiff(recent, target) : timeDiff(recent, target);
					colored = points ? ReportColor.points(delta, diff) : ReportColor.time(delta, diff);
				}
			}
			else if (column.count() > 0)
			{
				int delta = recent - (int) Math.round(column.average());
				diff = points ? pointDiff(recent, column.average()) : timeDiff(recent, column.average());
				colored = points ? ReportColor.points(delta, diff) : ReportColor.time(delta, diff);
			}
			out.append(left(shortRoom(row), 15))
				.append(' ')
				.append(right(shown, 6))
				.append(' ')
				.append(colored)
				.append('\n');
			appendTimeBreak(out, row);
		}
	}

	private static void appendTimeBreak(StringBuilder out, String row)
	{
		if ("Pre-Olm".equals(row) || "Raid Completed".equals(row) || "Between room time".equals(row))
		{
			out.append(repeat('-', TIME_RULE)).append('\n');
		}
	}

	static String shortRoom(String row)
	{
		if (row != null && row.startsWith("Olm mage hand phase "))
		{
			return "mage hand p" + row.substring("Olm mage hand phase ".length());
		}
		if ("Between room time".equals(row))
		{
			return "Between rooms";
		}
		return row;
	}

	public static String heading(RaidModeFilter mode, RaidSizeFilter size, int count)
	{
		return "Analyzing all " + kind(mode, size) + " (" + count + " raids)";
	}

	static String kind(RaidModeFilter mode, RaidSizeFilter size)
	{
		if (size == RaidSizeFilter.SOLO && mode == RaidModeFilter.CM)
		{
			return "solo CM raids";
		}
		if (size == RaidSizeFilter.SOLO && mode == RaidModeFilter.REGULAR_FULL)
		{
			return "solo regular full raids";
		}
		if (size == RaidSizeFilter.SOLO && mode == RaidModeFilter.REGULAR)
		{
			return "solo raids";
		}
		if (size == RaidSizeFilter.SOLO)
		{
			return "solo raids";
		}
		if (size == RaidSizeFilter.TEAM && mode == RaidModeFilter.CM)
		{
			return "team CM raids";
		}
		if (size == RaidSizeFilter.TEAM && mode == RaidModeFilter.REGULAR_FULL)
		{
			return "team regular full raids";
		}
		if (size == RaidSizeFilter.TEAM && mode == RaidModeFilter.REGULAR)
		{
			return "team raids";
		}
		if (size == RaidSizeFilter.TEAM)
		{
			return "team raids";
		}
		if (mode == RaidModeFilter.REGULAR_FULL)
		{
			return "regular full raids";
		}
		if (mode == RaidModeFilter.CM)
		{
			return "CM raids";
		}
		if (mode == RaidModeFilter.REGULAR)
		{
			return "regular raids";
		}
		return "raids";
	}

	private static String header(int lastN, boolean compare)
	{
		String header = left("Room", ROOM_W) + spaces(SEP)
			+ right("Best", BEST_W) + spaces(SEP)
			+ right("Average", AVG_W) + spaces(SEP)
			+ center("Recent", CELL_W) + spaces(SEP)
			+ center("Last " + lastN, CELL_W);
		if (compare)
		{
			header += spaces(SEP) + center("vs Target", CELL_W);
		}
		return header;
	}

	private static String formatRow(String name, Column column, int lastN, boolean points, Integer target, boolean compare)
	{
		String best = "--";
		String avg = "--";
		if (column.count() > 0)
		{
			best = points ? Integer.toString(column.best(true)) : TimeFormat.formatSeconds(column.best(false));
			avg = points
				? Integer.toString((int) Math.round(column.average()))
				: TimeFormat.formatSeconds((int) Math.round(column.average()));
		}
		String line = left(name, ROOM_W) + spaces(SEP)
			+ right(best, BEST_W) + spaces(SEP)
			+ right(avg, AVG_W) + spaces(SEP)
			+ cell(column.recent(), column.average(), points) + spaces(SEP)
			+ cell(column.lastAverage(lastN), column.average(), points);
		if (compare)
		{
			Double benchmark = target == null || target <= 0 ? null : target.doubleValue();
			line += spaces(SEP) + cell(benchmark, column.average(), points);
		}
		return line + "\n";
	}

	private static String cell(Double value, double average, boolean points)
	{
		if (value == null)
		{
			return right("-", CELL_W);
		}
		int rounded = (int) Math.round(value);
		int delta = rounded - (int) Math.round(average);
		String shown = points ? Integer.toString(rounded) : TimeFormat.formatSeconds(rounded);
		String diff = points ? pointDiff(rounded, average) : timeDiff(rounded, average);
		String colored = points ? ReportColor.points(delta, right(diff, DIFF_W)) : ReportColor.time(delta, right(diff, DIFF_W));
		return right(shown, VALUE_W) + " " + colored;
	}

	private static String timeDiff(int value, double average)
	{
		int delta = value - (int) Math.round(average);
		if (Math.abs(delta) < 1)
		{
			return "00:00";
		}
		return (delta > 0 ? "+" : "-") + TimeFormat.formatSeconds(Math.abs(delta));
	}

	private static String pointDiff(int value, double average)
	{
		int delta = value - (int) Math.round(average);
		if (Math.abs(delta) < 1)
		{
			return "0";
		}
		return (delta > 0 ? "+" : "") + delta;
	}

	private static void appendPurple(StringBuilder out, List<CoxRaidRecord> raids)
	{
		appendPurple(out, raids, false);
	}

	private static void appendPurple(StringBuilder out, List<CoxRaidRecord> raids, boolean sidebar)
	{
		int actual = 0;
		int actualRegular = 0;
		int actualCm = 0;
		int regularRaids = 0;
		int cmRaids = 0;
		int scrolls = 0;
		double expected = 0;
		Map<String, Integer> got = new LinkedHashMap<>();
		Map<String, Double> expectedItems = new LinkedHashMap<>();
		for (int i = 0; i < REGULAR_WEIGHTS.size(); i++)
		{
			String name = REGULAR_WEIGHTS.get(i).name;
			got.put(name, 0);
			expectedItems.put(name, 0.0);
		}
		for (int i = 0; i < raids.size(); i++)
		{
			CoxRaidRecord raid = raids.get(i);
			if (raid.isChallengeMode())
			{
				cmRaids++;
			}
			else
			{
				regularRaids++;
			}
			if (raid.hasPurple())
			{
				actual++;
				if (raid.isChallengeMode())
				{
					actualCm++;
				}
				else
				{
					actualRegular++;
				}
				String item = raid.getPurple();
				if (got.containsKey(item))
				{
					got.put(item, got.get(item) + 1);
				}
				if ("Dexterous prayer scroll".equals(item) || "Arcane prayer scroll".equals(item))
				{
					scrolls++;
				}
			}
			if (raid.getPersonalPoints() <= 0)
			{
				continue;
			}
			double chance = raid.getPersonalPoints() / PURPLE_POINTS;
			expected += chance;
			List<ItemWeight> table = raid.isChallengeMode() ? CM_WEIGHTS : REGULAR_WEIGHTS;
			int totalWeight = raid.isChallengeMode() ? 56 : 60;
			for (int w = 0; w < table.size(); w++)
			{
				ItemWeight item = table.get(w);
				expectedItems.put(item.name, expectedItems.get(item.name) + chance * item.weight / totalWeight);
			}
		}

		double scrollPct = actual == 0 ? 0 : (scrolls * 100.0 / actual);
		double diff = actual - expected;
		double rate = expected > 0 ? raids.size() / expected : 0;
		long raidsWorth = Math.round(diff * rate);
		String rateText = expected > 0 ? String.format(Locale.US, "1/%.2f", raids.size() / expected) : "--";
		String scrollText = String.format(Locale.US, "%.1f%% (%d)", scrollPct, scrolls);
		String diffText = String.format(Locale.US, "%+.1f (%d raids)", diff, raidsWorth);

		if (sidebar)
		{
			out.append("Summary\n");
			out.append(repeat('-', SIDEBAR_W)).append('\n');
			narrowLine(out, "Raids", Integer.toString(raids.size()));
			narrowLine(out, "  Regular", ReportColor.cyan(Integer.toString(regularRaids)));
			narrowLine(out, "  CM", ReportColor.gold(Integer.toString(cmRaids)));
			narrowLine(out, "Avg points", String.format(Locale.US, "%.0f", averagePoints(raids)));
			narrowLine(out, "All points", Long.toString(totalPoints(raids)));
			narrowLine(out, "Rate", rateText);
			narrowLine(out, "Expected", String.format(Locale.US, "%.1f", expected));
			narrowLine(out, "Actual", Integer.toString(actual));
			narrowLine(out, "Scrolls", scrollText);
			narrowLine(out, "Diff", ReportColor.signed(diff, diffText));
			out.append('\n');
			out.append("Items\n");
			out.append(repeat('-', SIDEBAR_W)).append('\n');
			out.append(left("Item", 16)).append(right("Got", 3)).append(" Rate\n");
			for (int i = 0; i < REGULAR_WEIGHTS.size(); i++)
			{
				String name = REGULAR_WEIGHTS.get(i).name;
				int count = got.get(name);
				double onRate = expectedItems.get(name);
				String rateDiff = String.format(Locale.US, "%+.2f", count - onRate);
				out.append(left(shortItem(name), 16))
					.append(right(Integer.toString(count), 3))
					.append(' ')
					.append(ReportColor.signed(count - onRate, right(rateDiff, 6)))
					.append('\n');
			}
			out.append('\n');
		}
		else
		{
			out.append("Purple Summary\n");
			out.append(repeat('-', 40)).append('\n');
			line(out, "Logged raids", Integer.toString(raids.size()));
			line(out, "Avg personal pts", String.format(Locale.US, "%.0f", averagePoints(raids)));
			line(out, "Purple rate", rateText);
			line(out, "Expected purples", String.format(Locale.US, "%.1f", expected));
			line(out, "Actual purples", Integer.toString(actual));
			line(out, "Prayer scrolls", scrollText);
			out.append(left("Difference", 22)).append(ReportColor.signed(diff, right(diffText, 16))).append('\n');
			out.append('\n');

			out.append("Purple Items\n");
			out.append(repeat('-', 70)).append('\n');
			out.append(left("Item", 28))
				.append(right("Got", 6))
				.append(right("Expected", 10))
				.append(right("Diff", 8))
				.append(right("On Rate", 10))
				.append(right("Diff", 8))
				.append('\n');
			out.append(repeat('-', 70)).append('\n');
			for (int i = 0; i < REGULAR_WEIGHTS.size(); i++)
			{
				String name = REGULAR_WEIGHTS.get(i).name;
				int count = got.get(name);
				double onRate = expectedItems.get(name);
				double fromDrops = actualRegular * weightOf(REGULAR_WEIGHTS, name) / 60.0
					+ actualCm * weightOf(CM_WEIGHTS, name) / 56.0;
				String expectedDiff = String.format(Locale.US, "%+.1f", count - fromDrops);
				String rateDiff = String.format(Locale.US, "%+.1f", count - onRate);
				out.append(left(name, 28))
					.append(right(Integer.toString(count), 6))
					.append(right(String.format(Locale.US, "%.1f", fromDrops), 10))
					.append(ReportColor.signed(count - fromDrops, right(expectedDiff, 8)))
					.append(right(String.format(Locale.US, "%.1f", onRate), 10))
					.append(ReportColor.signed(count - onRate, right(rateDiff, 8)))
					.append('\n');
			}
			out.append('\n');
		}
		appendDry(out, raids, actual, rate, sidebar);
	}

	private static int weightOf(List<ItemWeight> table, String name)
	{
		for (int i = 0; i < table.size(); i++)
		{
			if (name.equals(table.get(i).name))
			{
				return table.get(i).weight;
			}
		}
		return 0;
	}

	private static void appendDry(StringBuilder out, List<CoxRaidRecord> raids, int actual, double purpleRate, boolean sidebar)
	{
		int streak = 0;
		int longest = 0;
		int finishedSum = 0;
		int finishedCount = 0;
		for (int i = 0; i < raids.size(); i++)
		{
			if (raids.get(i).hasPurple())
			{
				if (streak > 0)
				{
					finishedSum += streak;
					finishedCount++;
					longest = Math.max(longest, streak);
				}
				streak = 0;
			}
			else
			{
				streak++;
				longest = Math.max(longest, streak);
			}
		}
		int current = streak;
		if (streak > 0)
		{
			finishedSum += streak;
			finishedCount++;
		}
		double averageDry = finishedCount == 0 ? 0 : (double) finishedSum / finishedCount;
		int expectedEvery = purpleRate > 0 ? Math.max(1, (int) Math.round(purpleRate)) : 0;
		int rowWidth = sidebar ? SIDEBAR_W : 80;
		if (!sidebar && expectedEvery > 0 && rowWidth >= expectedEvery)
		{
			rowWidth = (rowWidth / expectedEvery) * expectedEvery;
		}
		else if (!sidebar && expectedEvery > 0)
		{
			rowWidth = expectedEvery;
		}
		out.append(sidebar ? "History\n" : "Purple History\n");
		out.append(repeat('-', Math.min(rowWidth, 80))).append('\n');
		if (sidebar)
		{
			out.append("Raids: ").append(raids.size()).append("  Purples: ").append(actual).append('\n');
			if (expectedEvery > 0)
			{
				out.append("' = every ").append(expectedEvery).append(" raids\n");
			}
			out.append("+ purple, ' expected, . dry\n");
		}
		else
		{
			out.append("Raids: ").append(raids.size()).append(" | Purples: ").append(actual);
			if (expectedEvery > 0)
			{
				out.append("  |  ' = every ").append(expectedEvery).append(" raids");
			}
			out.append('\n');
			out.append("+ purple, ' expected, . dry\n");
		}
		out.append(ReportColor.gold("@")).append(" next raid\n");
		int column = 0;
		for (int i = 0; i < raids.size(); i++)
		{
			if (raids.get(i).hasPurple())
			{
				out.append(ReportColor.good("+"));
			}
			else if (expectedEvery > 0 && (i + 1) % expectedEvery == 0)
			{
				out.append(ReportColor.bad("'"));
			}
			else
			{
				out.append('.');
			}
			column++;
			if (column == rowWidth)
			{
				out.append('\n');
				column = 0;
			}
		}
		out.append(ReportColor.gold("@"));
		out.append('\n');
		out.append("Current dry streak: ").append(current).append(" raids\n");
		out.append("Longest dry streak: ").append(longest).append(" raids\n");
		out.append("Average dry streak: ").append(Math.round(averageDry)).append(" raids\n");
		out.append('\n');
	}

	private static void appendTrackedPurples(StringBuilder out, List<CoxRaidRecord> raids)
	{
		appendTrackedPurples(out, raids, false);
	}

	private static void appendTrackedPurples(StringBuilder out, List<CoxRaidRecord> raids, boolean colorItems)
	{
		boolean any = false;
		for (int i = 0; i < raids.size(); i++)
		{
			if (raids.get(i).hasPurple())
			{
				any = true;
				break;
			}
		}
		if (!any)
		{
			return;
		}
		if (colorItems)
		{
			out.append("\u001B[#11m");
			out.append("Tracked items\n");
		}
		else
		{
			out.append("Tracked purples (newest first)\n");
		}
		out.append(repeat('-', colorItems ? SIDEBAR_W : 48)).append('\n');
		for (int i = raids.size() - 1; i >= 0; i--)
		{
			CoxRaidRecord raid = raids.get(i);
			if (!raid.hasPurple())
			{
				continue;
			}
			String kc;
			if (raid.getKc() > 0)
			{
				kc = (raid.isChallengeMode() ? "CM " : "KC ") + raid.getKc();
			}
			else
			{
				kc = "--";
			}
			String item = colorItems ? ReportColor.good(raid.getPurple()) : raid.getPurple();
			out.append(left(kc, 10)).append(item).append('\n');
		}
		out.append('\n');
	}

	private static void appendLoggedDeaths(StringBuilder out, List<CoxRaidRecord> raids)
	{
		int total = 0;
		int raidsWithDeath = 0;
		int deathCount = 0;
		int nFullRegular = 0;
		int deathsFullRegular = 0;
		int nNormalRegular = 0;
		int deathsNormalRegular = 0;
		int nRegularTeam = 0;
		int deathsRegularTeam = 0;
		int nCmSolo = 0;
		int deathsCmSolo = 0;
		int nCmTeam = 0;
		int deathsCmTeam = 0;
		for (int i = 0; i < raids.size(); i++)
		{
			CoxRaidRecord raid = raids.get(i);
			total++;
			int deaths = raid.getDeaths();
			deathCount += deaths;
			boolean died = deaths > 0;
			if (died)
			{
				raidsWithDeath++;
			}
			if (raid.isChallengeMode())
			{
				if (raid.getTeamSize() <= 1)
				{
					nCmSolo++;
					if (died)
					{
						deathsCmSolo++;
					}
				}
				else
				{
					nCmTeam++;
					if (died)
					{
						deathsCmTeam++;
					}
				}
			}
			else if (raid.getTeamSize() > 1)
			{
				nRegularTeam++;
				if (died)
				{
					deathsRegularTeam++;
				}
			}
			else if (raid.isFullLayout())
			{
				nFullRegular++;
				if (died)
				{
					deathsFullRegular++;
				}
			}
			else
			{
				nNormalRegular++;
				if (died)
				{
					deathsNormalRegular++;
				}
			}
		}
		if (total <= 0)
		{
			return;
		}
		out.append("Deaths\n");
		out.append(repeat('-', 40)).append('\n');
		line(out, "Raids with a death", deathLine(raidsWithDeath, total));
		line(out, "Total deaths", Integer.toString(deathCount));
		if (nFullRegular > 0)
		{
			line(out, "  Full regular", deathLine(deathsFullRegular, nFullRegular));
		}
		if (nNormalRegular > 0)
		{
			line(out, "  Regular", deathLine(deathsNormalRegular, nNormalRegular));
		}
		if (nRegularTeam > 0)
		{
			line(out, "  Regular team", deathLine(deathsRegularTeam, nRegularTeam));
		}
		if (nCmSolo > 0)
		{
			line(out, "  CM solo", deathLine(deathsCmSolo, nCmSolo));
		}
		if (nCmTeam > 0)
		{
			line(out, "  CM team", deathLine(deathsCmTeam, nCmTeam));
		}
		out.append('\n');
	}

	private static void appendDeath(StringBuilder out, List<CoxRaidRecord> raids, ReportOptions settings)
	{
		int total = 0;
		int deaths = 0;
		int nFullRegular = 0;
		int deathsFullRegular = 0;
		int nNormalRegular = 0;
		int deathsNormalRegular = 0;
		int nCmSolo = 0;
		int deathsCmSolo = 0;
		int nCmTeam = 0;
		int deathsCmTeam = 0;
		for (int i = 0; i < raids.size(); i++)
		{
			CoxRaidRecord raid = raids.get(i);
			if (raid.getPersonalPoints() <= 0 || raid.getTeamSize() < 1)
			{
				continue;
			}
			if (raid.isChallengeMode())
			{
				if (raid.getTeamSize() == 1)
				{
					nCmSolo++;
					total++;
					if (raid.getPersonalPoints() < settings.getDeathCmSolo())
					{
						deathsCmSolo++;
						deaths++;
					}
				}
				else
				{
					nCmTeam++;
					total++;
					if (raid.getPersonalPoints() < settings.getDeathCmTeam())
					{
						deathsCmTeam++;
						deaths++;
					}
				}
				continue;
			}
			if (raid.getTeamSize() != 1)
			{
				continue;
			}
			if (raid.isFullLayout())
			{
				nFullRegular++;
				total++;
				if (raid.getPersonalPoints() < settings.getDeathFullRegular())
				{
					deathsFullRegular++;
					deaths++;
				}
			}
			else
			{
				nNormalRegular++;
				total++;
				if (raid.getPersonalPoints() < settings.getDeathRegular())
				{
					deathsNormalRegular++;
					deaths++;
				}
			}
		}
		if (total <= 0)
		{
			return;
		}
		out.append("Death estimate (from pts)\n");
		out.append(repeat('-', 40)).append('\n');
		line(out, "Raids with death", deathLine(deaths, total));
		if (nFullRegular > 0)
		{
			line(out, "  Full regular", deathLine(deathsFullRegular, nFullRegular));
		}
		if (nNormalRegular > 0)
		{
			line(out, "  Regular", deathLine(deathsNormalRegular, nNormalRegular));
		}
		if (nCmSolo > 0)
		{
			line(out, "  CM solo", deathLine(deathsCmSolo, nCmSolo));
		}
		if (nCmTeam > 0)
		{
			line(out, "  CM team", deathLine(deathsCmTeam, nCmTeam));
		}
		out.append('\n');
	}

	private static String deathLine(int deaths, int total)
	{
		double pct = total > 0 ? 100.0 * deaths / total : 0;
		return String.format(Locale.US, "%d/%d  (%.1f%%)", deaths, total, pct);
	}

	private static void appendRoomEfficiency(StringBuilder out, List<CoxRaidRecord> raids, boolean milk)
	{
		List<RoomPace> paces = new ArrayList<>();
		for (int r = 0; r < RoomNames.PREP_ROOMS.size(); r++)
		{
			String room = RoomNames.PREP_ROOMS.get(r);
			double points = 0;
			double seconds = 0;
			int count = 0;
			for (int i = 0; i < raids.size(); i++)
			{
				CoxRaidRecord raid = raids.get(i);
				int split = raid.secondsFor(room);
				if (split <= 0 || raid.getPersonalPoints() <= 0 || raid.getTotalSeconds() <= 0 || !validTime(room, split, milk))
				{
					continue;
				}
				points += raid.getPersonalPoints() * (split / (double) raid.getTotalSeconds());
				seconds += split;
				count++;
			}
			if (seconds <= 0 || count == 0)
			{
				continue;
			}
			paces.add(new RoomPace(room, (int) (points / (seconds / 3600.0)), count));
		}
		if (paces.isEmpty())
		{
			return;
		}
		Collections.sort(paces, new java.util.Comparator<RoomPace>()
		{
			@Override
			public int compare(RoomPace left, RoomPace right)
			{
				return Integer.compare(right.pph, left.pph);
			}
		});
		out.append("Room efficiency (PPH)\n");
		out.append(repeat('=', 38)).append('\n');
		out.append(left("Room", 18)).append(right("Avg PPH", 10)).append(right("Raids", 8)).append('\n');
		out.append(repeat('-', 38)).append('\n');
		for (int i = 0; i < paces.size(); i++)
		{
			RoomPace pace = paces.get(i);
			out.append(left(pace.room, 18)).append(right(Integer.toString(pace.pph), 10)).append(right(Integer.toString(pace.raids), 8)).append('\n');
		}
		out.append(repeat('=', 38)).append('\n');
		out.append('\n');
	}

	private static void appendCommonRooms(StringBuilder out, List<CoxRaidRecord> raids, boolean milk)
	{
		List<RoomPace> rooms = new ArrayList<>();
		for (int r = 0; r < RoomNames.PREP_ROOMS.size(); r++)
		{
			String room = RoomNames.PREP_ROOMS.get(r);
			int count = 0;
			long sum = 0;
			for (int i = 0; i < raids.size(); i++)
			{
				int seconds = raids.get(i).secondsFor(room);
				if (seconds > 0 && validTime(room, seconds, milk))
				{
					count++;
					sum += seconds;
				}
			}
			if (count > 0)
			{
				rooms.add(new RoomPace(room, (int) Math.round(sum / (double) count), count));
			}
		}
		int five = 0;
		int six = 0;
		int other = 0;
		for (int i = 0; i < raids.size(); i++)
		{
			int count = raids.get(i).prepRoomCount();
			if (count == 5)
			{
				five++;
			}
			else if (count == 6)
			{
				six++;
			}
			else
			{
				other++;
			}
		}
		if (rooms.isEmpty())
		{
			return;
		}
		Collections.sort(rooms, new java.util.Comparator<RoomPace>()
		{
			@Override
			public int compare(RoomPace left, RoomPace right)
			{
				if (left.raids != right.raids)
				{
					return Integer.compare(right.raids, left.raids);
				}
				return left.room.compareTo(right.room);
			}
		});
		out.append("Most common prep rooms\n");
		out.append(repeat('-', 40)).append('\n');
		out.append(left("Room", 12)).append(right("Avg time", 14)).append(right("Completed", 12)).append('\n');
		out.append(repeat('-', 40)).append('\n');
		for (int i = 0; i < rooms.size(); i++)
		{
			RoomPace room = rooms.get(i);
			out.append(left(room.room, 12))
				.append(right(TimeFormat.formatSeconds(room.pph), 14))
				.append(right(Integer.toString(room.raids), 12))
				.append('\n');
		}
		out.append('\n');
		double pct5 = raids.isEmpty() ? 0 : five * 100.0 / raids.size();
		double pct6 = raids.isEmpty() ? 0 : six * 100.0 / raids.size();
		out.append(String.format(Locale.US, "Room count: 5 rooms = %d (%.1f%%), 6 rooms = %d (%.1f%%)", five, pct5, six, pct6));
		if (other > 0)
		{
			out.append(", other = ").append(other);
		}
		out.append('\n');
		out.append('\n');
	}

	private static void appendOutliers(StringBuilder out, List<CoxRaidRecord> raids, boolean milk)
	{
		List<String> lines = new ArrayList<>();
		List<Integer> kcs = new ArrayList<>();
		for (int i = 0; i < raids.size(); i++)
		{
			CoxRaidRecord raid = raids.get(i);
			List<String> rows = rowsFor(Collections.singletonList(raid));
			for (int r = 0; r < rows.size(); r++)
			{
				String row = rows.get(r);
				if ("Total Points".equals(row) || "PPH".equals(row))
				{
					continue;
				}
				int seconds = rawSeconds(raid, row);
				String reason = rejectReason(row, seconds, milk);
				if (reason == null)
				{
					continue;
				}
				kcs.add(raid.getKc());
				lines.add("KC " + String.format("%5d", raid.getKc()) + " | "
					+ left(row, 26)
					+ right(TimeFormat.formatSeconds(seconds), 8)
					+ "  (" + reason + ")");
			}
		}
		if (lines.isEmpty())
		{
			return;
		}
		Integer[] order = new Integer[lines.size()];
		for (int i = 0; i < order.length; i++)
		{
			order[i] = i;
		}
		Arrays.sort(order, new java.util.Comparator<Integer>()
		{
			@Override
			public int compare(Integer left, Integer right)
			{
				return Integer.compare(kcs.get(right), kcs.get(left));
			}
		});
		out.append("Discarded outliers - ").append(lines.size()).append('\n');
		out.append(repeat('-', 64)).append('\n');
		for (int i = 0; i < order.length; i++)
		{
			out.append(lines.get(order[i])).append('\n');
		}
		out.append('\n');
	}

	private static int rawSeconds(CoxRaidRecord raid, String row)
	{
		if ("Pre-Olm".equals(row))
		{
			return prepSeconds(raid);
		}
		if ("Between room time".equals(row))
		{
			int prep = prepSeconds(raid);
			int olm = raid.secondsFor("Olm");
			int total = raid.getTotalSeconds();
			if (prep <= 0 || olm <= 0 || total <= 0)
			{
				return 0;
			}
			return total - prep - olm;
		}
		if ("Raid Completed".equals(row))
		{
			return raid.getTotalSeconds();
		}
		int seconds = raid.secondsFor(row);
		return Math.max(seconds, 0);
	}

	private static String rejectReason(String room, int seconds, boolean milk)
	{
		if (seconds <= 0 || validTime(room, seconds, milk))
		{
			return null;
		}
		if (seconds < 20)
		{
			return "<20s";
		}
		Integer min = MIN_SECONDS.get(room);
		if (min != null && seconds < min)
		{
			return "below min";
		}
		if ("Ice demon".equals(room) && seconds > ICE_NORMAL_MAX && seconds <= ICE_MILK_MAX)
		{
			return "ice milking";
		}
		return "above max";
	}

	private static List<String> rowsFor(List<CoxRaidRecord> raids)
	{
		List<String> rows = new ArrayList<>();
		rows.addAll(RoomNames.PREP_ROOMS);
		rows.add("Pre-Olm");
		for (int phase = 1; phase <= 8; phase++)
		{
			String mage = "Olm mage hand phase " + phase;
			String full = "Olm phase " + phase;
			if (present(raids, mage))
			{
				rows.add(mage);
			}
			if (present(raids, full))
			{
				rows.add(full);
			}
		}
		if (present(raids, "Olm head"))
		{
			rows.add("Olm head");
		}
		rows.add("Olm");
		rows.add("Raid Completed");
		rows.add("Between room time");
		rows.add("Total Points");
		rows.add("PPH");
		return rows;
	}

	private static boolean present(List<CoxRaidRecord> raids, String room)
	{
		for (int i = 0; i < raids.size(); i++)
		{
			if (raids.get(i).secondsFor(room) >= 0)
			{
				return true;
			}
		}
		return false;
	}

	private static Map<String, Column> buildColumns(List<CoxRaidRecord> raids, List<String> rows, boolean milk)
	{
		Map<String, Column> columns = new LinkedHashMap<>();
		for (int r = 0; r < rows.size(); r++)
		{
			String row = rows.get(r);
			Column column = new Column();
			for (int i = 0; i < raids.size(); i++)
			{
				column.add(valueFor(raids.get(i), row, milk));
			}
			columns.put(row, column);
		}
		return columns;
	}

	private static Integer valueFor(CoxRaidRecord raid, String row, boolean milk)
	{
		if ("Total Points".equals(row))
		{
			return raid.getPersonalPoints() > 0 ? raid.getPersonalPoints() : null;
		}
		if ("PPH".equals(row))
		{
			if (raid.getPersonalPoints() <= 0 || raid.getTotalSeconds() <= 0)
			{
				return null;
			}
			double pph = raid.getPersonalPoints() / (raid.getTotalSeconds() / 3600.0);
			return (int) pph;
		}
		if ("Pre-Olm".equals(row))
		{
			int prep = prepSeconds(raid);
			if (prep <= 0 || !validTime(row, prep, milk))
			{
				return null;
			}
			return prep;
		}
		if ("Between room time".equals(row))
		{
			int prep = prepSeconds(raid);
			int olm = raid.secondsFor("Olm");
			int total = raid.getTotalSeconds();
			if (prep <= 0 || olm <= 0 || total <= 0)
			{
				return null;
			}
			int between = total - prep - olm;
			if (between <= 0 || !validTime(row, between, milk))
			{
				return null;
			}
			return between;
		}
		if ("Raid Completed".equals(row))
		{
			int total = raid.getTotalSeconds();
			if (total <= 0 || !validTime(row, total, milk))
			{
				return null;
			}
			return total;
		}
		int seconds = raid.secondsFor(row);
		if (seconds < 0 || !validTime(row, seconds, milk))
		{
			return null;
		}
		return seconds;
	}

	private static int prepSeconds(CoxRaidRecord raid)
	{
		int sum = 0;
		for (int i = 0; i < RoomNames.PREP_ROOMS.size(); i++)
		{
			int seconds = raid.secondsFor(RoomNames.PREP_ROOMS.get(i));
			if (seconds > 0)
			{
				sum += seconds;
			}
		}
		return sum;
	}

	private static int countExcluded(List<CoxRaidRecord> raids, List<String> rows, boolean milk)
	{
		int excluded = 0;
		for (int i = 0; i < raids.size(); i++)
		{
			CoxRaidRecord raid = raids.get(i);
			for (int r = 0; r < rows.size(); r++)
			{
				String row = rows.get(r);
				if ("Total Points".equals(row) || "PPH".equals(row))
				{
					continue;
				}
				if (rejectReason(row, rawSeconds(raid, row), milk) != null)
				{
					excluded++;
				}
			}
		}
		return excluded;
	}

	static boolean validTime(String room, int seconds)
	{
		return validTime(room, seconds, false);
	}

	static boolean validTime(String room, int seconds, boolean includeIceMilking)
	{
		if (seconds < 20)
		{
			return false;
		}
		if ("Ice demon".equals(room))
		{
			Integer min = MIN_SECONDS.get(room);
			if (min != null && seconds < min)
			{
				return false;
			}
			if (seconds <= ICE_NORMAL_MAX)
			{
				return true;
			}
			return includeIceMilking && seconds <= ICE_MILK_MAX;
		}
		Integer min = MIN_SECONDS.get(room);
		Integer max = MAX_SECONDS.get(room);
		if (min != null && seconds < min)
		{
			return false;
		}
		if (max != null && seconds > max)
		{
			return false;
		}
		return true;
	}

	private static boolean includeIceMilking(ReportOptions settings)
	{
		return settings != null && settings.getTargetStyle().isIceMilking();
	}

	/**
	 * Solo, team, and CM counts from the raids in this log.
	 * Holes are missing kill counts between the first and last logged KC.
	 * Lifetime KC from before this log is not estimated.
	 */
	private static void appendAccount(StringBuilder out, List<CoxRaidRecord> raids)
	{
		int solo = 0;
		int team = 0;
		int cmSolo = 0;
		int cmTeam = 0;
		long regularSoloPoints = 0;
		int regularSoloWithPoints = 0;
		long cmPoints = 0;
		long personal = 0;
		List<Integer> regularKcs = new ArrayList<>();
		List<Integer> cmKcs = new ArrayList<>();
		for (int i = 0; i < raids.size(); i++)
		{
			CoxRaidRecord raid = raids.get(i);
			if (raid == null)
			{
				continue;
			}
			if (raid.getPersonalPoints() > 0)
			{
				personal += raid.getPersonalPoints();
			}
			if (raid.isChallengeMode())
			{
				if (raid.getKc() > 0)
				{
					cmKcs.add(raid.getKc());
				}
				if (raid.getPersonalPoints() > 0)
				{
					cmPoints += raid.getPersonalPoints();
				}
				if (raid.getTeamSize() >= 2)
				{
					cmTeam++;
				}
				else if (raid.getTeamSize() == 1)
				{
					cmSolo++;
				}
			}
			else
			{
				if (raid.getKc() > 0)
				{
					regularKcs.add(raid.getKc());
				}
				if (raid.getTeamSize() >= 2)
				{
					team++;
				}
				else if (raid.getTeamSize() == 1)
				{
					solo++;
					if (raid.getPersonalPoints() > 0)
					{
						regularSoloPoints += raid.getPersonalPoints();
						regularSoloWithPoints++;
					}
				}
			}
		}
		out.append("Account Breakdown\n");
		out.append(repeat('-', 40)).append('\n');
		line(out, "Regular logged", Integer.toString(solo + team));
		line(out, "  Solo", Integer.toString(solo));
		line(out, "  Team", Integer.toString(team));
		line(out, "  Untracked", Integer.toString(kcHoles(regularKcs)));
		line(out, "CM logged", Integer.toString(cmSolo + cmTeam));
		line(out, "  Solo", Integer.toString(cmSolo));
		line(out, "  Team", Integer.toString(cmTeam));
		int cmHoles = kcHoles(cmKcs);
		if (cmHoles > 0)
		{
			line(out, "  CM (missing)", Integer.toString(cmHoles));
		}
		if (regularSoloWithPoints > 0 && cmPoints > 0)
		{
			double averageSolo = (double) regularSoloPoints / regularSoloWithPoints;
			double cmEquiv = cmPoints / averageSolo;
			line(out, "CM equiv", String.format(Locale.US, "%.1f", cmEquiv));
			line(out, "Effective raids", String.format(Locale.US, "%.1f", solo + team + cmEquiv));
		}
		line(out, "Personal points", Long.toString(personal));
		out.append("Logged raids only. KC before the first saved raid is not untracked.\n");
		out.append('\n');
	}

	private static int kcHoles(List<Integer> kcs)
	{
		if (kcs == null || kcs.isEmpty())
		{
			return 0;
		}
		int min = kcs.get(0);
		int max = kcs.get(0);
		java.util.HashSet<Integer> unique = new java.util.HashSet<>();
		for (int i = 0; i < kcs.size(); i++)
		{
			int kc = kcs.get(i);
			unique.add(kc);
			min = Math.min(min, kc);
			max = Math.max(max, kc);
		}
		return Math.max(0, max - min + 1 - unique.size());
	}

	private static double averagePoints(List<CoxRaidRecord> raids)
	{
		long sum = totalPoints(raids);
		int count = 0;
		for (int i = 0; i < raids.size(); i++)
		{
			if (raids.get(i).getPersonalPoints() > 0)
			{
				count++;
			}
		}
		return count == 0 ? 0 : (double) sum / count;
	}

	private static long totalPoints(List<CoxRaidRecord> raids)
	{
		long sum = 0;
		for (int i = 0; i < raids.size(); i++)
		{
			if (raids.get(i).getPersonalPoints() > 0)
			{
				sum += raids.get(i).getPersonalPoints();
			}
		}
		return sum;
	}

	private static void line(StringBuilder out, String label, String value)
	{
		out.append(left(label, 22)).append(right(value, 16)).append('\n');
	}

	private static void narrowLine(StringBuilder out, String label, String value)
	{
		out.append(left(label, 12)).append(value).append('\n');
	}

	private static String shortItem(String name)
	{
		if ("Dexterous prayer scroll".equals(name))
		{
			return "Dex scroll";
		}
		if ("Arcane prayer scroll".equals(name))
		{
			return "Arcane scroll";
		}
		if ("Dragon hunter crossbow".equals(name))
		{
			return "DHCB";
		}
		if ("Ancestral robe top".equals(name))
		{
			return "Ancestral top";
		}
		if ("Ancestral robe bottom".equals(name))
		{
			return "Ancestral bottom";
		}
		return name;
	}

	private static Map<String, Integer> mapOf(Object... pairs)
	{
		Map<String, Integer> map = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
		{
			map.put((String) pairs[i], (Integer) pairs[i + 1]);
		}
		return Collections.unmodifiableMap(map);
	}

	private static String left(String text, int width)
	{
		if (text.length() >= width)
		{
			return text;
		}
		return text + spaces(width - text.length());
	}

	private static String right(String text, int width)
	{
		if (text.length() >= width)
		{
			return text;
		}
		return spaces(width - text.length()) + text;
	}

	private static String center(String text, int width)
	{
		if (text.length() >= width)
		{
			return text;
		}
		int pad = width - text.length();
		int leftPad = pad / 2;
		return spaces(leftPad) + text + spaces(pad - leftPad);
	}

	private static String spaces(int count)
	{
		return repeat(' ', count);
	}

	private static String repeat(char c, int count)
	{
		char[] chars = new char[Math.max(0, count)];
		Arrays.fill(chars, c);
		return new String(chars);
	}

	private static final class Column
	{
		private final List<Integer> values = new ArrayList<>();

		private Column()
		{
		}

		private void add(Integer value)
		{
			values.add(value);
		}

		private int count()
		{
			int count = 0;
			for (int i = 0; i < values.size(); i++)
			{
				if (values.get(i) != null)
				{
					count++;
				}
			}
			return count;
		}

		private double average()
		{
			long sum = 0;
			int count = 0;
			for (int i = 0; i < values.size(); i++)
			{
				if (values.get(i) != null)
				{
					sum += values.get(i);
					count++;
				}
			}
			return count == 0 ? 0 : (double) sum / count;
		}

		private int best(boolean higher)
		{
			int best = higher ? Integer.MIN_VALUE : Integer.MAX_VALUE;
			for (int i = 0; i < values.size(); i++)
			{
				Integer value = values.get(i);
				if (value == null)
				{
					continue;
				}
				if (higher)
				{
					best = Math.max(best, value);
				}
				else
				{
					best = Math.min(best, value);
				}
			}
			return best;
		}

		private Double recent()
		{
			if (values.isEmpty() || values.get(values.size() - 1) == null)
			{
				return null;
			}
			return values.get(values.size() - 1).doubleValue();
		}

		private Double lastAverage(int lastN)
		{
			int start = Math.max(0, values.size() - lastN);
			long sum = 0;
			int count = 0;
			for (int i = start; i < values.size(); i++)
			{
				if (values.get(i) != null)
				{
					sum += values.get(i);
					count++;
				}
			}
			if (count == 0)
			{
				return null;
			}
			return (double) sum / count;
		}
	}

	private static final class ItemWeight
	{
		private final String name;
		private final int weight;

		private ItemWeight(String name, int weight)
		{
			this.name = name;
			this.weight = weight;
		}
	}

	private static final class RoomPace
	{
		private final String room;
		private final int pph;
		private final int raids;

		private RoomPace(String room, int pph, int raids)
		{
			this.room = room;
			this.pph = pph;
			this.raids = raids;
		}
	}

	/** One point on the PB pace line. Positive means ahead of your personal best. */
	public static final class PacePoint
	{
		private final String label;
		private final int aheadSeconds;

		public PacePoint(String label, int aheadSeconds)
		{
			this.label = label == null ? "" : label;
			this.aheadSeconds = aheadSeconds;
		}

		public String getLabel()
		{
			return label;
		}

		public int getAheadSeconds()
		{
			return aheadSeconds;
		}
	}

	public static final class PaceComparison
	{
		private final String text;
		private final List<PacePoint> points;

		public PaceComparison(String text, List<PacePoint> points)
		{
			this.text = text == null ? "" : text;
			this.points = points == null ? new ArrayList<PacePoint>() : points;
		}

		public String getText()
		{
			return text;
		}

		public List<PacePoint> getPoints()
		{
			return points;
		}
	}
}
