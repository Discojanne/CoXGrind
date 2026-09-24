package com.coxgrind.model;

/**
 * One timed slice of a raid: a room, a floor, an Olm phase, or the full raid.
 * {@code seconds} is the in-game raid clock, rounded down to whole seconds.
 */
public class RoomSplit
{
	private String room;
	private int seconds;

	public RoomSplit()
	{
	}

	public RoomSplit(String room, int seconds)
	{
		this.room = room;
		this.seconds = seconds;
	}

	public String getRoom()
	{
		return room;
	}

	public void setRoom(String room)
	{
		this.room = room;
	}

	public int getSeconds()
	{
		return seconds;
	}

	public void setSeconds(int seconds)
	{
		this.seconds = seconds;
	}
}
