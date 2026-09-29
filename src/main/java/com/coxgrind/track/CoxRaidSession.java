package com.coxgrind.track;

import com.coxgrind.model.CoxRaidRecord;
import com.coxgrind.model.PartyPurple;
import com.coxgrind.model.RaidDeath;
import com.coxgrind.model.RoomSplit;
import java.util.List;

/**
 * In-memory raid that turns game events into room, floor, and Olm splits.
 * The plugin feeds it. Tests can feed it without a client.
 */
public final class CoxRaidSession
{
	private boolean running;
	private boolean complete;
	private String savedId;
	private int teamSize;
	private int lastSplitUnits;
	private int upperUnits = -1;
	private int middleUnits = -1;
	private int lowerUnits = -1;
	private int olmStartUnits = -1;
	private int phaseStartUnits = -1;
	private int currentPhase = 1;
	private boolean mageRecorded;
	private int headStartUnits = -1;
	/** How many ticks a death's point drop is watched for. */
	private static final int DEATH_POINT_TICKS = 6;
	private int lastPoints;
	private boolean pendingDeath;
	private int pendingBaseline;
	private int pendingMin;
	private int pendingTicks;
	/** Bumps when the sidebar list changes. A clock tick does not. */
	private int generation;
	private final CoxRaidRecord record = new CoxRaidRecord();

	public void startRaid()
	{
		reset();
		running = true;
	}

	public void reset()
	{
		running = false;
		complete = false;
		savedId = null;
		teamSize = 0;
		lastSplitUnits = 0;
		upperUnits = -1;
		middleUnits = -1;
		lowerUnits = -1;
		olmStartUnits = -1;
		phaseStartUnits = -1;
		currentPhase = 1;
		mageRecorded = false;
		headStartUnits = -1;
		lastPoints = 0;
		pendingDeath = false;
		pendingBaseline = 0;
		pendingMin = 0;
		pendingTicks = 0;
		record.setId(null);
		record.setTimestamp(null);
		record.setPlayerName(null);
		record.setAccountHash(null);
		record.setChallengeMode(false);
		record.setKc(0);
		record.setTeamSize(0);
		record.setDeaths(0);
		record.getDeathList().clear();
		record.setPersonalPoints(0);
		record.setTeamPoints(0);
		record.setTotalSeconds(0);
		record.setPurple("");
		record.getExtras().clear();
		record.getPartyPurples().clear();
		record.getSplits().clear();
		generation++;
	}

	/** Changes when a split, the kill count, or the points on a finished raid change. */
	public int getGeneration()
	{
		return generation;
	}

	public boolean isRunning()
	{
		return running;
	}

	public boolean isComplete()
	{
		return complete;
	}

	public boolean isSaved()
	{
		return savedId != null;
	}

	public void markSaved(String id, String timestamp)
	{
		savedId = id;
		record.setId(id);
		record.setTimestamp(timestamp);
	}

	public void setTeamSize(int size)
	{
		if (size > 0 && size <= 100)
		{
			teamSize = size;
			record.setTeamSize(size);
		}
	}

	public int expectedHandPhases()
	{
		int scale = teamSize <= 0 ? 1 : teamSize;
		return 3 + (scale / 8);
	}

	public void completeRoom(String room, int timerUnits)
	{
		if (!running || complete || room == null)
		{
			return;
		}
		if (record.secondsFor(room) >= 0)
		{
			return;
		}
		int seconds = TimeFormat.unitsToSeconds(timerUnits - lastSplitUnits);
		if (seconds <= 0)
		{
			return;
		}
		record.putSplit(room, seconds);
		lastSplitUnits = timerUnits;
		generation++;
	}

	public void completeLevel(String level, int timerUnits)
	{
		if (!running || complete || level == null)
		{
			return;
		}
		if ("Upper".equals(level))
		{
			if (upperUnits >= 0)
			{
				return;
			}
			upperUnits = timerUnits;
			record.putSplit("Floor 1", TimeFormat.unitsToSeconds(timerUnits));
			lastSplitUnits = timerUnits;
			generation++;
		}
		else if ("Middle".equals(level))
		{
			if (middleUnits >= 0)
			{
				return;
			}
			middleUnits = timerUnits;
			int from = upperUnits >= 0 ? upperUnits : 0;
			record.putSplit("Floor 2", TimeFormat.unitsToSeconds(timerUnits - from));
			lastSplitUnits = timerUnits;
			generation++;
		}
		else if ("Lower".equals(level))
		{
			if (lowerUnits >= 0)
			{
				return;
			}
			lowerUnits = timerUnits;
			olmStartUnits = timerUnits;
			String floorName;
			int from;
			if (middleUnits >= 0)
			{
				floorName = "Floor 3";
				from = middleUnits;
			}
			else if (upperUnits >= 0)
			{
				floorName = "Floor 2";
				from = upperUnits;
			}
			else
			{
				floorName = "Floor 1";
				from = 0;
			}
			record.putSplit(floorName, TimeFormat.unitsToSeconds(timerUnits - from));
			lastSplitUnits = timerUnits;
			generation++;
		}
	}

	public void olmPhaseStarted(int timerUnits)
	{
		if (!running || complete || phaseStartUnits >= 0 || headStartUnits >= 0)
		{
			return;
		}
		phaseStartUnits = timerUnits;
		mageRecorded = false;
		if (olmStartUnits < 0)
		{
			olmStartUnits = timerUnits;
		}
		generation++;
	}

	public void mageHandDown(int timerUnits)
	{
		if (!running || complete || phaseStartUnits < 0 || mageRecorded)
		{
			return;
		}
		if (currentPhase < expectedHandPhases())
		{
			int seconds = TimeFormat.unitsToSeconds(timerUnits - phaseStartUnits);
			if (seconds > 0)
			{
				record.putSplit("Olm mage hand phase " + currentPhase, seconds);
				generation++;
			}
		}
		mageRecorded = true;
	}

	public void meleeHandDown(int timerUnits)
	{
		if (!running || complete || phaseStartUnits < 0)
		{
			return;
		}
		int seconds = TimeFormat.unitsToSeconds(timerUnits - phaseStartUnits);
		if (seconds > 0)
		{
			record.putSplit("Olm phase " + currentPhase, seconds);
		}
		int finished = currentPhase;
		currentPhase++;
		phaseStartUnits = -1;
		mageRecorded = false;
		if (finished >= expectedHandPhases() && headStartUnits < 0)
		{
			headStartUnits = timerUnits;
		}
		generation++;
	}

	/**
	 * Seconds spent on the open split. Uses the current Olm phase or head when one is running.
	 * Returns -1 once the raid is over.
	 */
	public int openSegmentSeconds(int timerUnits)
	{
		if (!running || complete || timerUnits < 0)
		{
			return -1;
		}
		int from = lastSplitUnits;
		if (phaseStartUnits >= 0)
		{
			from = phaseStartUnits;
		}
		else if (headStartUnits >= 0)
		{
			from = headStartUnits;
		}
		int units = timerUnits - from;
		if (units < 0)
		{
			units = 0;
		}
		return TimeFormat.unitsToSeconds(units);
	}

	public void raidCompleted(int timerUnits, int personalPoints, int teamPoints, int size)
	{
		if (!running || complete)
		{
			return;
		}
		if (pendingDeath && personalPoints > 0)
		{
			absorbDeathPoints(personalPoints);
		}
		pendingDeath = false;
		setTeamSize(size);
		if (headStartUnits >= 0)
		{
			int head = TimeFormat.unitsToSeconds(timerUnits - headStartUnits);
			if (head > 0)
			{
				record.putSplit("Olm head", head);
			}
		}
		if (olmStartUnits >= 0)
		{
			int olm = TimeFormat.unitsToSeconds(timerUnits - olmStartUnits);
			if (olm > 0)
			{
				record.putSplit("Olm", olm);
			}
		}
		int total = TimeFormat.unitsToSeconds(timerUnits);
		record.putSplit("Raid Completed", total);
		record.setTotalSeconds(total);
		updateScore(personalPoints, teamPoints, size);
		complete = true;
		generation++;
	}

	public void updateScore(int personalPoints, int teamPoints, int size)
	{
		boolean changed = false;
		if (personalPoints > 0 && personalPoints != record.getPersonalPoints())
		{
			record.setPersonalPoints(personalPoints);
			changed = true;
		}
		if (teamPoints > 0 && teamPoints != record.getTeamPoints())
		{
			record.setTeamPoints(teamPoints);
			changed = true;
		}
		int before = record.getTeamSize();
		setTeamSize(size);
		if (record.getTeamSize() != before)
		{
			changed = true;
		}
		if (changed)
		{
			generation++;
		}
	}

	public void setKillCount(int kc, boolean challengeMode)
	{
		boolean changed = false;
		if (kc > 0 && kc != record.getKc())
		{
			record.setKc(kc);
			changed = true;
		}
		if (challengeMode != record.isChallengeMode())
		{
			record.setChallengeMode(challengeMode);
			changed = true;
		}
		if (changed)
		{
			generation++;
		}
	}

	public void setPurple(String item)
	{
		if (item != null && item.length() > 0)
		{
			record.setPurple(item);
		}
	}

	public void addExtra(String item)
	{
		record.addExtra(item);
	}

	public void addPartyPurple(String playerName, String item)
	{
		record.addPartyPurple(playerName, item);
	}

	/**
	 * Personal points seen on a game tick, used to measure the drop after a death.
	 */
	public void notePoints(int pointsNow)
	{
		if (!running || complete || pointsNow <= 0)
		{
			return;
		}
		if (pendingDeath)
		{
			absorbDeathPoints(pointsNow);
			pendingTicks--;
			if (pendingTicks <= 0)
			{
				pendingDeath = false;
			}
		}
		lastPoints = pointsNow;
	}

	/**
	 * A witnessed death. Adds one list row and raises the count, unless the death varbit already counted it.
	 */
	public void recordDeath()
	{
		witnessDeath(false, 0);
	}

	/**
	 * A witnessed death with the personal points at that moment.
	 * The points lost are the drop from the last sampled score over the next few ticks.
	 */
	public void recordDeath(int pointsNow)
	{
		int sample = pointsNow > 0 ? pointsNow : lastPoints;
		witnessDeath(true, sample);
	}

	private void witnessDeath(boolean measurePoints, int pointsNow)
	{
		if (!running || complete)
		{
			return;
		}
		if (pendingDeath)
		{
			if (measurePoints)
			{
				absorbDeathPoints(pointsNow);
			}
			pendingDeath = false;
		}
		boolean alreadyCounted = record.getDeathList().size() < record.getDeaths();
		if (!alreadyCounted)
		{
			record.setDeaths(record.getDeaths() + 1);
		}
		int baseline = lastPoints > 0 ? lastPoints : Math.max(0, pointsNow);
		record.addDeath(deathRoom(), 0);
		if (!measurePoints)
		{
			return;
		}
		pendingDeath = true;
		pendingBaseline = baseline;
		pendingMin = Math.max(0, pointsNow);
		pendingTicks = DEATH_POINT_TICKS;
		absorbDeathPoints(pointsNow);
		if (pointsNow >= 0)
		{
			lastPoints = pointsNow;
		}
	}

	private void absorbDeathPoints(int pointsNow)
	{
		if (pointsNow >= 0 && pointsNow < pendingMin)
		{
			pendingMin = pointsNow;
		}
		if (pendingBaseline > pendingMin)
		{
			List<RaidDeath> list = record.getDeathList();
			if (!list.isEmpty())
			{
				RaidDeath death = list.get(list.size() - 1);
				if (death != null)
				{
					death.setPointsLost(pendingBaseline - pendingMin);
				}
			}
		}
	}

	/**
	 * Olm when a phase or the head is open.
	 * On the fixed layout, the next room only when nothing sits between it and the room that just finished.
	 */
	private String deathRoom()
	{
		if (olmStartUnits >= 0 || headStartUnits >= 0)
		{
			return "Olm";
		}
		String lastPrep = "";
		List<RoomSplit> splits = record.getSplits();
		for (int i = 0; i < splits.size(); i++)
		{
			RoomSplit split = splits.get(i);
			if (split != null && RoomNames.isPrepRoom(split.getRoom()))
			{
				lastPrep = split.getRoom();
			}
		}
		if (lastPrep.isEmpty())
		{
			return "Tekton";
		}
		if ("Tekton".equals(lastPrep))
		{
			return "Crabs";
		}
		if ("Ice demon".equals(lastPrep))
		{
			return "Shamans";
		}
		if ("Vanguards".equals(lastPrep))
		{
			return "Thieving";
		}
		if ("Guardians".equals(lastPrep))
		{
			return "Vasa";
		}
		if ("Mystics".equals(lastPrep))
		{
			return "Muttadiles";
		}
		return "";
	}

	/**
	 * Raises the death count when the raid death varbit is higher.
	 * Values outside 1..30 are ignored so an unrelated reading cannot overwrite the chat count.
	 */
	public void raiseDeathCount(int count)
	{
		if (!running || complete || count < 1 || count > 30)
		{
			return;
		}
		if (count > record.getDeaths())
		{
			record.setDeaths(count);
		}
	}

	public CoxRaidRecord snapshot()
	{
		CoxRaidRecord copy = new CoxRaidRecord();
		copy.setId(record.getId());
		copy.setTimestamp(record.getTimestamp());
		copy.setPlayerName(record.getPlayerName());
		copy.setAccountHash(record.getAccountHash());
		copy.setChallengeMode(record.isChallengeMode());
		copy.setKc(record.getKc());
		copy.setTeamSize(record.getTeamSize());
		copy.setDeaths(record.getDeaths());
		for (int i = 0; i < record.getDeathList().size(); i++)
		{
			RaidDeath death = record.getDeathList().get(i);
			if (death != null)
			{
				copy.addDeath(death.getRoom(), death.getPointsLost());
			}
		}
		copy.setPersonalPoints(record.getPersonalPoints());
		copy.setTeamPoints(record.getTeamPoints());
		copy.setTotalSeconds(record.getTotalSeconds());
		copy.setPurple(record.getPurple());
		for (int i = 0; i < record.getExtras().size(); i++)
		{
			copy.addExtra(record.getExtras().get(i));
		}
		for (int i = 0; i < record.getPartyPurples().size(); i++)
		{
			PartyPurple party = record.getPartyPurples().get(i);
			if (party != null)
			{
				copy.addPartyPurple(party.getPlayerName(), party.getItem());
			}
		}
		for (int i = 0; i < record.getSplits().size(); i++)
		{
			RoomSplit split = record.getSplits().get(i);
			copy.getSplits().add(new RoomSplit(split.getRoom(), split.getSeconds()));
		}
		return copy;
	}
}
