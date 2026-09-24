package com.coxgrind.model;

/**
 * A unique received by someone else in the same raid.
 * The local player's purple stays on {@link CoxRaidRecord#getPurple()}.
 */
public class PartyPurple
{
	private String playerName = "";
	private String item = "";

	public PartyPurple()
	{
	}

	public PartyPurple(String playerName, String item)
	{
		this.playerName = playerName == null ? "" : playerName;
		this.item = item == null ? "" : item;
	}

	public String getPlayerName()
	{
		return playerName == null ? "" : playerName;
	}

	public String getItem()
	{
		return item == null ? "" : item;
	}
}
