package com.coxgrind.track;

import org.junit.Assert;
import org.junit.Test;

public class TimeFormatTest
{
	@Test
	public void raidClockUsesOneHundredUnitsPerMinute()
	{
		Assert.assertEquals(60, TimeFormat.unitsToSeconds(100));
		Assert.assertEquals(90, TimeFormat.unitsToSeconds(150));
		Assert.assertEquals(48, TimeFormat.unitsToSeconds(80));
		Assert.assertEquals(0, TimeFormat.unitsToSeconds(0));
	}

	@Test
	public void formatsMinutesAndSeconds()
	{
		Assert.assertEquals("01:30", TimeFormat.formatSeconds(90));
		Assert.assertEquals("22:54", TimeFormat.formatSeconds(22 * 60 + 54));
		Assert.assertEquals("00:00", TimeFormat.formatSeconds(0));
	}

	@Test
	public void parsesClockAndBlank()
	{
		Assert.assertEquals(Integer.valueOf(80), TimeFormat.parseClock("01:20"));
		Assert.assertEquals(Integer.valueOf(90), TimeFormat.parseClock("90"));
		Assert.assertNull(TimeFormat.parseClock("  "));
		Assert.assertEquals(Integer.valueOf(55000), TimeFormat.parseCount("55000"));
	}
}
