package com.coxgrind.log;

import java.util.Collections;
import java.util.EnumSet;
import net.runelite.api.WorldType;
import org.junit.Assert;
import org.junit.Test;

public class AccountLogTest
{
	@Test
	public void eachAccountAndALeagueWorldGetTheirOwnKey()
	{
		EnumSet<WorldType> normal = EnumSet.of(WorldType.MEMBERS);
		EnumSet<WorldType> league = EnumSet.of(WorldType.MEMBERS, WorldType.SEASONAL);
		EnumSet<WorldType> deadman = EnumSet.of(WorldType.MEMBERS, WorldType.DEADMAN);

		Assert.assertEquals("1", AccountLog.key(1L, normal));
		Assert.assertEquals("2", AccountLog.key(2L, normal));
		Assert.assertEquals("1-league", AccountLog.key(1L, league));
		Assert.assertEquals("1", AccountLog.key(1L, deadman));
		Assert.assertEquals(Long.toUnsignedString(-2L), AccountLog.key(-2L, normal));
		Assert.assertEquals(Long.toUnsignedString(-2L) + "-league", AccountLog.key(-2L, league));

		Assert.assertNull(AccountLog.key(0L, normal));
		Assert.assertNull(AccountLog.key(-1L, normal));
		Assert.assertNull(AccountLog.key(1L, EnumSet.noneOf(WorldType.class)));
		Assert.assertNull(AccountLog.key(1L, null));
		Assert.assertNull(AccountLog.key(1L, Collections.<WorldType>emptySet()));
	}
}
