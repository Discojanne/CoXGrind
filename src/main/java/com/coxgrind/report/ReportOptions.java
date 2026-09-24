package com.coxgrind.report;

import com.coxgrind.model.ComparisonTargets;
import com.coxgrind.model.RaidModeFilter;
import java.util.Collections;
import java.util.Map;

/**
 * Numbers and switches for one report. Defaults match a full Coxparser-style printout.
 */
public final class ReportOptions
{
	private int lastN = 10;
	private int reportRaids;
	private ComparisonTargets targets = new ComparisonTargets();
	private int deathFullRegular = 48000;
	private int deathRegular = 29000;
	private int deathCmSolo = 60000;
	private int deathCmTeam = 40000;
	private boolean purpleSummary = true;
	private boolean trackedPurples = true;
	private boolean roomEfficiency;
	private boolean commonRooms;
	private boolean outliers = true;
	private TargetStyle targetStyle = new TargetStyle();

	public static ReportOptions defaults(int lastN)
	{
		ReportOptions options = new ReportOptions();
		options.lastN = lastN < 1 ? 10 : Math.min(lastN, 100);
		return options;
	}

	public int getLastN()
	{
		return lastN < 1 ? 10 : Math.min(lastN, 100);
	}

	public int getReportRaids()
	{
		return reportRaids;
	}

	public void setReportRaids(int reportRaids)
	{
		this.reportRaids = reportRaids < 0 ? 0 : Math.min(reportRaids, 10000);
	}

	public ComparisonTargets getTargets()
	{
		return targets == null ? new ComparisonTargets() : targets;
	}

	public void setTargets(ComparisonTargets targets)
	{
		this.targets = targets == null ? new ComparisonTargets() : targets;
	}

	public int getDeathFullRegular()
	{
		return deathFullRegular;
	}

	public void setDeathFullRegular(int deathFullRegular)
	{
		this.deathFullRegular = deathFullRegular;
	}

	public int getDeathRegular()
	{
		return deathRegular;
	}

	public void setDeathRegular(int deathRegular)
	{
		this.deathRegular = deathRegular;
	}

	public int getDeathCmSolo()
	{
		return deathCmSolo;
	}

	public void setDeathCmSolo(int deathCmSolo)
	{
		this.deathCmSolo = deathCmSolo;
	}

	public int getDeathCmTeam()
	{
		return deathCmTeam;
	}

	public void setDeathCmTeam(int deathCmTeam)
	{
		this.deathCmTeam = deathCmTeam;
	}

	public boolean isPurpleSummary()
	{
		return purpleSummary;
	}

	public void setPurpleSummary(boolean purpleSummary)
	{
		this.purpleSummary = purpleSummary;
	}

	public boolean isTrackedPurples()
	{
		return trackedPurples;
	}

	public void setTrackedPurples(boolean trackedPurples)
	{
		this.trackedPurples = trackedPurples;
	}

	public boolean isRoomEfficiency()
	{
		return roomEfficiency;
	}

	public void setRoomEfficiency(boolean roomEfficiency)
	{
		this.roomEfficiency = roomEfficiency;
	}

	public boolean isCommonRooms()
	{
		return commonRooms;
	}

	public void setCommonRooms(boolean commonRooms)
	{
		this.commonRooms = commonRooms;
	}

	public boolean isOutliers()
	{
		return outliers;
	}

	public void setOutliers(boolean outliers)
	{
		this.outliers = outliers;
	}

	public TargetStyle getTargetStyle()
	{
		if (targetStyle == null)
		{
			targetStyle = new TargetStyle();
		}
		return targetStyle;
	}

	public void setTargetStyle(TargetStyle targetStyle)
	{
		this.targetStyle = targetStyle == null ? new TargetStyle() : targetStyle;
	}

	/**
	 * Regular and regular-full share the regular sheet. All has no comparison column.
	 * Method checkboxes shorten that sheet. The saved targets are left as typed.
	 */
	public Map<String, Integer> comparisonSheet(RaidModeFilter mode)
	{
		Map<String, Integer> sheet;
		if (mode == RaidModeFilter.CM)
		{
			sheet = getTargets().sheet(true);
		}
		else if (mode == RaidModeFilter.REGULAR || mode == RaidModeFilter.REGULAR_FULL)
		{
			sheet = getTargets().sheet(false);
		}
		else
		{
			return Collections.emptyMap();
		}
		return getTargetStyle().apply(sheet);
	}
}
