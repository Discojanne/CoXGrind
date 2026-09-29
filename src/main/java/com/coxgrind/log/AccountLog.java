package com.coxgrind.log;

import java.util.Collection;
import net.runelite.api.WorldType;

/**
 * Which file a raid belongs in.
 * One RuneScape account is one hash. A leagues world on that account is a second file.
 * A different account is a different hash, including when both are used from the same RuneLite profile.
 */
public final class AccountLog
{
	public static final String LEAGUE_SUFFIX = "-league";

	private AccountLog()
	{
	}

	/**
	 * File key for this account on this world, or null when the client is logged out
	 * or the world type is not known yet. Hash {@code 0} and hash {@code -1} are logged out.
	 * A leagues world is {@link WorldType#SEASONAL}.
	 */
	public static String key(long accountHash, Collection<WorldType> worldTypes)
	{
		if (accountHash == 0L || accountHash == -1L || worldTypes == null || worldTypes.isEmpty())
		{
			return null;
		}
		String hash = Long.toUnsignedString(accountHash);
		if (worldTypes.contains(WorldType.SEASONAL))
		{
			return hash + LEAGUE_SUFFIX;
		}
		return hash;
	}
}
