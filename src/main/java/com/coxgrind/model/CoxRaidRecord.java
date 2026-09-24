package com.coxgrind.model;

import com.coxgrind.track.RoomNames;
import java.util.ArrayList;
import java.util.List;

/**
 * One completed Chambers of Xeric raid. Stored once in the account JSON file.
 */
public class CoxRaidRecord
{
	private String id;
	private String timestamp;
	private String playerName;
	private String accountHash;
	private boolean challengeMode;
	private int kc;
	private int teamSize;
	private int deaths;
	private int personalPoints;
	private int teamPoints;
	private int totalSeconds;
	private List<RoomSplit> splits = new ArrayList<>();
	private String purple = "";
	private List<String> extras = new ArrayList<>();
	private List<PartyPurple> partyPurples = new ArrayList<>();

	/** Coxparser treats 11 or more logged prep rooms as a full layout. */
	public static final int FULL_LAYOUT_PREP_ROOMS = 11;

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(String timestamp)
	{
		this.timestamp = timestamp;
	}

	public String getPlayerName()
	{
		return playerName;
	}

	public void setPlayerName(String playerName)
	{
		this.playerName = playerName;
	}

	public String getAccountHash()
	{
		return accountHash;
	}

	public void setAccountHash(String accountHash)
	{
		this.accountHash = accountHash;
	}

	public boolean isChallengeMode()
	{
		return challengeMode;
	}

	public void setChallengeMode(boolean challengeMode)
	{
		this.challengeMode = challengeMode;
	}

	public int getKc()
	{
		return kc;
	}

	public void setKc(int kc)
	{
		this.kc = kc;
	}

	public int getTeamSize()
	{
		return teamSize;
	}

	public void setTeamSize(int teamSize)
	{
		this.teamSize = teamSize;
	}

	public int getDeaths()
	{
		return Math.max(0, deaths);
	}

	public void setDeaths(int deaths)
	{
		this.deaths = Math.max(0, deaths);
	}

	public int getPersonalPoints()
	{
		return personalPoints;
	}

	public void setPersonalPoints(int personalPoints)
	{
		this.personalPoints = personalPoints;
	}

	public int getTeamPoints()
	{
		return teamPoints;
	}

	public void setTeamPoints(int teamPoints)
	{
		this.teamPoints = teamPoints;
	}

	public int getTotalSeconds()
	{
		return totalSeconds;
	}

	public void setTotalSeconds(int totalSeconds)
	{
		this.totalSeconds = totalSeconds;
	}

	public List<RoomSplit> getSplits()
	{
		if (splits == null)
		{
			splits = new ArrayList<>();
		}
		return splits;
	}

	public void setSplits(List<RoomSplit> splits)
	{
		this.splits = splits == null ? new ArrayList<RoomSplit>() : splits;
	}

	public String getPurple()
	{
		return purple == null ? "" : purple;
	}

	public void setPurple(String purple)
	{
		this.purple = purple == null ? "" : purple;
	}

	public boolean hasPurple()
	{
		return getPurple().length() > 0;
	}

	/**
	 * Personal pet, kit, or dust. These are not purples and do not change purple counts.
	 */
	public List<String> getExtras()
	{
		if (extras == null)
		{
			extras = new ArrayList<>();
		}
		return extras;
	}

	public void setExtras(List<String> extras)
	{
		this.extras = extras == null ? new ArrayList<String>() : extras;
	}

	public void addExtra(String item)
	{
		if (item == null || item.length() == 0)
		{
			return;
		}
		List<String> list = getExtras();
		for (int i = 0; i < list.size(); i++)
		{
			if (item.equals(list.get(i)))
			{
				return;
			}
		}
		list.add(item);
	}

	public List<PartyPurple> getPartyPurples()
	{
		if (partyPurples == null)
		{
			partyPurples = new ArrayList<>();
		}
		return partyPurples;
	}

	public void setPartyPurples(List<PartyPurple> partyPurples)
	{
		this.partyPurples = partyPurples == null ? new ArrayList<PartyPurple>() : partyPurples;
	}

	public void addPartyPurple(String playerName, String item)
	{
		if (playerName == null || item == null || item.length() == 0)
		{
			return;
		}
		String name = playerName.replace('\u00A0', ' ').trim();
		if (name.length() == 0)
		{
			return;
		}
		List<PartyPurple> list = getPartyPurples();
		for (int i = 0; i < list.size(); i++)
		{
			PartyPurple existing = list.get(i);
			if (existing != null
				&& item.equals(existing.getItem())
				&& name.equalsIgnoreCase(existing.getPlayerName()))
			{
				return;
			}
		}
		list.add(new PartyPurple(name, item));
	}

	public int prepRoomCount()
	{
		int count = 0;
		for (int i = 0; i < RoomNames.PREP_ROOMS.size(); i++)
		{
			if (secondsFor(RoomNames.PREP_ROOMS.get(i)) > 0)
			{
				count++;
			}
		}
		return count;
	}

	public boolean isFullLayout()
	{
		return prepRoomCount() >= FULL_LAYOUT_PREP_ROOMS;
	}

	public int secondsFor(String room)
	{
		if (room == null || splits == null)
		{
			return -1;
		}
		for (int i = 0; i < splits.size(); i++)
		{
			RoomSplit split = splits.get(i);
			if (split != null && room.equals(split.getRoom()))
			{
				return split.getSeconds();
			}
		}
		return -1;
	}

	public void putSplit(String room, int seconds)
	{
		if (room == null || seconds < 0)
		{
			return;
		}
		List<RoomSplit> list = getSplits();
		for (int i = 0; i < list.size(); i++)
		{
			RoomSplit split = list.get(i);
			if (split != null && room.equals(split.getRoom()))
			{
				split.setSeconds(seconds);
				return;
			}
		}
		list.add(new RoomSplit(room, seconds));
	}
}
