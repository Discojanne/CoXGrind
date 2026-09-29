package com.coxgrind.model;

/**
 * One personal death in a raid. The name is not stored.
 * {@code room} is empty when the room could not be named.
 */
public class RaidDeath
{
	private String room = "";
	private int pointsLost;

	public RaidDeath()
	{
	}

	public RaidDeath(String room, int pointsLost)
	{
		this.room = room == null ? "" : room;
		this.pointsLost = Math.max(0, pointsLost);
	}

	public String getRoom()
	{
		return room == null ? "" : room;
	}

	public void setRoom(String room)
	{
		this.room = room == null ? "" : room;
	}

	public int getPointsLost()
	{
		return Math.max(0, pointsLost);
	}

	public void setPointsLost(int pointsLost)
	{
		this.pointsLost = Math.max(0, pointsLost);
	}
}
