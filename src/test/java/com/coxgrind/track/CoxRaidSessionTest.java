package com.coxgrind.track;

import com.coxgrind.model.CoxRaidRecord;
import org.junit.Assert;
import org.junit.Test;

public class CoxRaidSessionTest
{
	@Test
	public void recordsRoomsFloorsAndSoloOlmPhases()
	{
		CoxRaidSession session = new CoxRaidSession();
		session.startRaid();
		session.setTeamSize(1);

		session.completeRoom("Tekton", 80);
		session.completeLevel("Upper", 200);
		session.completeRoom("Crabs", 250);
		session.completeLevel("Lower", 400);

		session.olmPhaseStarted(450);
		session.mageHandDown(500);
		session.meleeHandDown(560);
		session.olmPhaseStarted(570);
		session.mageHandDown(620);
		session.meleeHandDown(700);
		session.olmPhaseStarted(710);
		session.mageHandDown(760);
		session.meleeHandDown(800);
		session.recordDeath();
		session.recordDeath();
		session.raiseDeathCount(1);
		session.raidCompleted(900, 30000, 30000, 1);
		session.setKillCount(12, true);
		session.setPurple("Twisted bow");
		session.addPartyPurple("Someone Else", "Elder maul");

		CoxRaidRecord raid = session.snapshot();
		Assert.assertEquals(48, raid.secondsFor("Tekton"));
		Assert.assertEquals(120, raid.secondsFor("Floor 1"));
		Assert.assertEquals(30, raid.secondsFor("Crabs"));
		Assert.assertEquals(120, raid.secondsFor("Floor 2"));
		Assert.assertEquals(30, raid.secondsFor("Olm mage hand phase 1"));
		Assert.assertEquals(30, raid.secondsFor("Olm mage hand phase 2"));
		Assert.assertEquals(-1, raid.secondsFor("Olm mage hand phase 3"));
		Assert.assertEquals(66, raid.secondsFor("Olm phase 1"));
		Assert.assertEquals(78, raid.secondsFor("Olm phase 2"));
		Assert.assertEquals(54, raid.secondsFor("Olm phase 3"));
		Assert.assertEquals(60, raid.secondsFor("Olm head"));
		Assert.assertEquals(300, raid.secondsFor("Olm"));
		Assert.assertEquals(540, raid.getTotalSeconds());
		Assert.assertEquals(1, raid.getTeamSize());
		Assert.assertEquals(30000, raid.getPersonalPoints());
		Assert.assertTrue(raid.isChallengeMode());
		Assert.assertEquals(2, raid.getDeaths());
		Assert.assertEquals("Twisted bow", raid.getPurple());
		Assert.assertEquals(1, raid.getPartyPurples().size());
		Assert.assertEquals("Elder maul", raid.getPartyPurples().get(0).getItem());
		Assert.assertTrue(session.isComplete());
		session.startRaid();
		Assert.assertTrue(session.snapshot().getPartyPurples().isEmpty());
	}

	@Test
	public void ignoresASecondSplitForTheSameRoom()
	{
		CoxRaidSession session = new CoxRaidSession();
		session.startRaid();
		session.completeRoom("Tekton", 100);
		session.completeRoom("Tekton", 400);
		Assert.assertEquals(60, session.snapshot().secondsFor("Tekton"));
	}

	@Test
	public void leavingBeforeTheEndDropsTheRaid()
	{
		CoxRaidSession session = new CoxRaidSession();
		session.startRaid();
		session.completeRoom("Vasa", 80);
		session.reset();
		Assert.assertFalse(session.isRunning());
		Assert.assertEquals(-1, session.snapshot().secondsFor("Vasa"));
	}

	@Test
	public void openSegmentFollowsTheCurrentPhase()
	{
		CoxRaidSession session = new CoxRaidSession();
		session.startRaid();
		Assert.assertEquals(0, session.openSegmentSeconds(0));
		session.completeRoom("Tekton", 100);
		Assert.assertEquals(30, session.openSegmentSeconds(150));
		session.olmPhaseStarted(200);
		Assert.assertEquals(30, session.openSegmentSeconds(250));
		session.raidCompleted(300, 1000, 1000, 1);
		Assert.assertEquals(-1, session.openSegmentSeconds(400));
	}
}
