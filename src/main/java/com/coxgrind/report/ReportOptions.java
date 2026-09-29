package com.coxgrind.report;

import com.coxgrind.model.ComparisonTargets;
import com.coxgrind.model.RaidModeFilter;
import com.coxgrind.model.RaidSizeFilter;
import com.coxgrind.model.TargetSheet;
import java.util.Collections;
import java.util.Map;

/**
 * Numbers and switches for one report. Defaults match a full Coxparser-style printout.
 */
public final class ReportOptions
{
	private int lastN = 10;
	private ComparisonTargets targets = new ComparisonTargets();
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

	public ComparisonTargets getTargets()
	{
		return targets == null ? new ComparisonTargets() : targets;
	}

	public void setTargets(ComparisonTargets targets)
	{
		this.targets = targets == null ? new ComparisonTargets() : targets;
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
	 * Regular and regular-full share the sheet for that team size. All, for mode or size, has no comparison column.
	 * Method checkboxes adjust that sheet. The saved targets are left as typed.
	 */
	public Map<String, Integer> comparisonSheet(RaidModeFilter mode, RaidSizeFilter size)
	{
		TargetSheet which = TargetSheet.of(mode, size);
		if (which == null)
		{
			return Collections.emptyMap();
		}
		return getTargetStyle().apply(getTargets().sheet(which));
	}
}
