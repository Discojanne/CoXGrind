package com.coxgrind.track;

import com.coxgrind.model.CoxRaidRecord;
import com.coxgrind.model.PartyPurple;
import com.coxgrind.model.RoomSplit;

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
		record.setId(null);
		record.setTimestamp(null);
		record.setPlayerName(null);
		record.setAccountHash(null);
		record.setChallengeMode(false);
		record.setKc(0);
		record.setTeamSize(0);
		record.setDeaths(0);
		record.setPersonalPoints(0);
		record.setTeamPoints(0);
		record.setTotalSeconds(0);
		record.setPurple("");
		record.getExtras().clear();
		record.getPartyPurples().clear();
		record.getSplits().clear();
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
	}

	public void updateScore(int personalPoints, int teamPoints, int size)
	{
		if (personalPoints > 0)
		{
			record.setPersonalPoints(personalPoints);
		}
		if (teamPoints > 0)
		{
			record.setTeamPoints(teamPoints);
		}
		setTeamSize(size);
	}

	public void setKillCount(int kc, boolean challengeMode)
	{
		if (kc > 0)
		{
			record.setKc(kc);
		}
		record.setChallengeMode(challengeMode);
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

	public void recordDeath()
	{
		if (!running || complete)
		{
			return;
		}
		record.setDeaths(record.getDeaths() + 1);
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
