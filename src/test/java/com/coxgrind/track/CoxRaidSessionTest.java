package com.coxgrind.track;

import com.coxgrind.model.CoxRaidRecord;
import com.coxgrind.model.RaidDeath;
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

	@Test
	public void deathListRecordsTheDropAndNamesTheNextRoomOnAFixedLayout()
	{
		CoxRaidSession session = new CoxRaidSession();
		session.startRaid();
		session.notePoints(40000);
		session.recordDeath(40000);
		session.notePoints(32000);
		RaidDeath first = session.snapshot().getDeathList().get(0);
		Assert.assertEquals("Tekton", first.getRoom());
		Assert.assertEquals(8000, first.getPointsLost());

		session.completeRoom("Tekton", 100);
		session.recordDeath(32000);
		session.raidCompleted(200, 25000, 25000, 1);
		session.setKillCount(8, true);
		CoxRaidRecord cm = session.snapshot();
		cm.settleDeathRooms();
		Assert.assertEquals(2, cm.getDeaths());
		Assert.assertEquals("Tekton", cm.getDeathList().get(0).getRoom());
		Assert.assertEquals(8000, cm.getDeathList().get(0).getPointsLost());
		Assert.assertEquals("Crabs", cm.getDeathList().get(1).getRoom());
		Assert.assertEquals(7000, cm.getDeathList().get(1).getPointsLost());

		CoxRaidSession gap = new CoxRaidSession();
		gap.startRaid();
		gap.completeRoom("Crabs", 80);
		gap.recordDeath();
		Assert.assertEquals("", gap.snapshot().getDeathList().get(0).getRoom());

		CoxRaidSession olm = new CoxRaidSession();
		olm.startRaid();
		olm.olmPhaseStarted(100);
		olm.recordDeath();
		Assert.assertEquals("Olm", olm.snapshot().getDeathList().get(0).getRoom());

		CoxRaidSession both = new CoxRaidSession();
		both.startRaid();
		both.raiseDeathCount(1);
		both.notePoints(10000);
		both.recordDeath(8000);
		Assert.assertEquals(1, both.snapshot().getDeaths());
		Assert.assertEquals(1, both.snapshot().getDeathList().size());
		Assert.assertEquals(2000, both.snapshot().getDeathList().get(0).getPointsLost());

		CoxRaidSession regular = new CoxRaidSession();
		regular.startRaid();
		regular.notePoints(20000);
		regular.completeRoom("Tekton", 100);
		regular.recordDeath(16000);
		regular.raidCompleted(200, 16000, 16000, 1);
		regular.setKillCount(4, false);
		CoxRaidRecord saved = regular.snapshot();
		saved.settleDeathRooms();
		Assert.assertEquals("", saved.getDeathList().get(0).getRoom());
		Assert.assertEquals(4000, saved.getDeathList().get(0).getPointsLost());

		CoxRaidSession full = new CoxRaidSession();
		full.startRaid();
		full.completeRoom("Tekton", 40);
		full.recordDeath();
		int clock = 80;
		for (int i = 1; i < 11; i++)
		{
			full.completeRoom(RoomNames.PREP_ROOMS.get(i), clock);
			clock += 40;
		}
		full.raidCompleted(clock, 10000, 10000, 1);
		full.setKillCount(1, false);
		CoxRaidRecord laidOut = full.snapshot();
		Assert.assertTrue(laidOut.isFullLayout());
		laidOut.settleDeathRooms();
		Assert.assertEquals("Crabs", laidOut.getDeathList().get(0).getRoom());
	}
}
