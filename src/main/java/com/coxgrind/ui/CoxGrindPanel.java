package com.coxgrind.ui;

import com.coxgrind.CoxGrindConfig;
import com.coxgrind.log.RaidLogStore;
import com.coxgrind.model.CoxRaidRecord;
import com.coxgrind.model.RaidModeFilter;
import com.coxgrind.model.RaidSizeFilter;
import com.coxgrind.report.RaidReportFormatter;
import com.coxgrind.report.ReportOptions;
import com.coxgrind.report.TargetSettings;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.runelite.client.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.client.ui.PluginPanel;

public class CoxGrindPanel extends PluginPanel
{
	private static final Logger log = LoggerFactory.getLogger(CoxGrindPanel.class);
	private static final String FILTER_GROUP = "coxgrind";
	private static final String MODE_KEY = "panelMode";
	private static final String SIZE_KEY = "panelSize";
	private final RaidLogStore store;
	private final CoxGrindConfig config;
	private final ConfigManager configManager;
	private final JComboBox<RaidModeFilter> modeBox = new JComboBox<>(RaidModeFilter.values());
	private final JComboBox<RaidSizeFilter> sizeBox = new JComboBox<>();
	private boolean fillingSizes;
	private RaidSizeFilter pendingSize;
	private final ActiveTimes activeTimes = new ActiveTimes();
	private final PaceGraph paceGraph = new PaceGraph();
	private final PaceColumn paceColumn = new PaceColumn(activeTimes, paceGraph);
	private final JScrollPane paceScroll = new JScrollPane(paceColumn);
	private final JTabbedPane tabs = new JTabbedPane();
	private final ActiveTimes targetTimes = new ActiveTimes();
	private final PaceGraph targetGraph = PaceGraph.againstTarget();
	private final PaceColumn targetColumn = new PaceColumn(targetTimes, targetGraph);
	private final JScrollPane targetScroll = new JScrollPane(targetColumn);
	private final PurplePanel purplePanel = new PurplePanel();
	private final ActiveTimes bestTimes = new ActiveTimes();
	private String accountHash;
	private List<CoxRaidRecord> cachedRaids = java.util.Collections.emptyList();
	private RaidReportFormatter.PaceStats paceStats;
	private boolean live;
	private CoxRaidRecord liveRaid;
	private int liveOpenSeconds = -1;
	private boolean liveInProgress;
	private boolean pendingLive;
	private int dataGeneration;
	private int paintedGeneration = -1;
	/** True while this sidebar is the one on screen. Read from the client thread. */
	private volatile boolean shown;
	/** Set when the sidebar opens so the next raid tick draws the current second. */
	private volatile boolean reveal;

	public CoxGrindPanel(RaidLogStore store, CoxGrindConfig config, ConfigManager configManager)
	{
		this(store, config, configManager, null);
	}

	public CoxGrindPanel(RaidLogStore store, CoxGrindConfig config, ConfigManager configManager, net.runelite.client.game.ItemManager itemManager)
	{
		super(false);
		this.store = store;
		this.config = config;
		this.configManager = configManager;
		purplePanel.setItemManager(itemManager);
		sizeBox.addItem(RaidSizeFilter.ALL);
		sizeBox.addItem(RaidSizeFilter.SOLO);
		sizeBox.addItem(RaidSizeFilter.TEAM);
		restoreFilters();

		Font small = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
		modeBox.setFont(small);
		sizeBox.setFont(small);
		modeBox.setToolTipText("Raid type");
		sizeBox.setToolTipText("Party size");
		setLayout(new BorderLayout());
		JPanel top = new JPanel(new BorderLayout());
		top.setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 4));

		JPanel controls = new JPanel(new GridLayout(1, 4, 2, 0));
		JButton report = button("Full report", new Runnable()
		{
			@Override
			public void run()
			{
				printReport();
			}
		});
		JButton folder = button("Log folder", new Runnable()
		{
			@Override
			public void run()
			{
				openFolder();
			}
		});
		report.setFont(small);
		folder.setFont(small);
		report.setMargin(new Insets(1, 2, 1, 2));
		folder.setMargin(new Insets(1, 2, 1, 2));
		report.setToolTipText("Full report");
		folder.setToolTipText("Log folder");
		controls.add(modeBox);
		controls.add(sizeBox);
		controls.add(report);
		controls.add(folder);
		top.add(controls, BorderLayout.NORTH);
		add(top, BorderLayout.NORTH);

		tabs.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		tabs.setBorder(BorderFactory.createEmptyBorder());
		tabs.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		tabs.putClientProperty("JTabbedPane.tabInsets", new Insets(2, 0, 2, 0));
		tabs.putClientProperty("JTabbedPane.tabAreaInsets", new Insets(0, 0, 0, 0));
		tabs.putClientProperty("JTabbedPane.tabWidthMode", "equal");
		tabs.putClientProperty("JTabbedPane.tabAreaAlignment", "fill");
		activeTimes.setAlignmentX(Component.LEFT_ALIGNMENT);
		paceGraph.setAlignmentX(Component.LEFT_ALIGNMENT);
		paceColumn.add(activeTimes);
		paceColumn.add(paceGraph);
		paceScroll.setBorder(BorderFactory.createEmptyBorder());
		paceScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		paceScroll.getViewport().setBackground(new Color(24, 24, 24));
		tabs.addTab("Average", paceScroll);
		tabs.setToolTipTextAt(0, "Recent raid vs your average");
		targetTimes.setAlignmentX(Component.LEFT_ALIGNMENT);
		targetGraph.setAlignmentX(Component.LEFT_ALIGNMENT);
		targetColumn.add(targetTimes);
		targetColumn.add(targetGraph);
		targetScroll.setBorder(BorderFactory.createEmptyBorder());
		targetScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		targetScroll.getViewport().setBackground(new Color(24, 24, 24));
		tabs.addTab("Target", targetScroll);
		tabs.setToolTipTextAt(1, "This raid vs your targets");
		tabs.addTab("Best", reading(bestTimes));
		tabs.setToolTipTextAt(2, "Fastest split, points, and PPH in this filter");
		tabs.addTab("Purples", reading(purplePanel));
		tabs.setToolTipTextAt(3, "Every logged raid");
		add(tabs, BorderLayout.CENTER);
		tabs.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent event)
			{
				int tab = tabs.getSelectedIndex();
				if (tab > 0 && paintedGeneration != dataGeneration)
				{
					paintRest();
					return;
				}
				if (tab == 1 && targetTimes.showsLastAverage())
				{
					paintTarget();
					return;
				}
				if (!live)
				{
					return;
				}
				if (tab <= 0)
				{
					paintPace();
				}
				else if (tab == 1)
				{
					paintTarget();
				}
			}
		});
		addHierarchyListener(new HierarchyListener()
		{
			@Override
			public void hierarchyChanged(HierarchyEvent event)
			{
				if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0)
				{
					return;
				}
				boolean now = isShowing();
				if (now && !shown)
				{
					reveal = true;
				}
				shown = now;
				if (now && pendingLive)
				{
					paintLive();
				}
			}
		});

		modeBox.addActionListener(event ->
		{
			rememberFilters();
			reload();
		});
		sizeBox.addActionListener(event ->
		{
			if (fillingSizes)
			{
				return;
			}
			rememberFilters();
			reload();
		});
		bestTimes.setChoices(new String[] {"Splits", "Raid time", "PPH"}, 0, new Runnable()
		{
			@Override
			public void run()
			{
				paintBest();
			}
		});
		targetTimes.setChoices(new String[] {"This", "Last"}, 0, new Runnable()
		{
			@Override
			public void run()
			{
				paintTarget();
			}
		});
		targetTimes.setCount(1, 10, 1, 100, new Runnable()
		{
			@Override
			public void run()
			{
				paintTarget();
			}
		});
		accountHash = store.latestAccountHash();
		reload();
	}

	/** The sidebar is open. Safe to read from the client thread. */
	public boolean isShown()
	{
		return shown;
	}

	/** True once, when the sidebar has just been opened. */
	public boolean consumeReveal()
	{
		if (!reveal)
		{
			return false;
		}
		reveal = false;
		return true;
	}

	public void setAccount(String hash)
	{
		onEdt(new Runnable()
		{
			@Override
			public void run()
			{
				if (hash == null || hash.isEmpty())
				{
					reload();
					return;
				}
				if (hash.equals(accountHash))
				{
					return;
				}
				accountHash = hash;
				reload();
			}
		});
	}

	public void reload()
	{
		onEdt(new Runnable()
		{
			@Override
			public void run()
			{
				refreshCache();
				paintFromCache();
			}
		});
	}

	/**
	 * The raid in progress. {@code inProgress} is false once Olm is dead and the splits are final.
	 * {@code openSeconds} is the split that is still running, or -1.
	 */
	public void showLive(CoxRaidRecord raid, int openSeconds, boolean inProgress)
	{
		onEdt(new Runnable()
		{
			@Override
			public void run()
			{
				live = true;
				liveRaid = raid == null ? new CoxRaidRecord() : raid;
				liveOpenSeconds = openSeconds;
				liveInProgress = inProgress;
				if (!isShowing())
				{
					pendingLive = true;
					return;
				}
				paintLive();
			}
		});
	}

	/** Latest saved raid. Used after a raid is logged, and after leaving before Olm dies. */
	public void showRecent()
	{
		onEdt(new Runnable()
		{
			@Override
			public void run()
			{
				live = false;
				pendingLive = false;
				liveRaid = null;
				liveOpenSeconds = -1;
				liveInProgress = false;
				refreshCache();
				paintFromCache();
			}
		});
	}

	private void refreshCache()
	{
		if (accountHash == null || accountHash.isEmpty())
		{
			accountHash = store.latestAccountHash();
		}
		try
		{
			cachedRaids = loadAll();
			fillSizeBox(cachedRaids);
			paceStats = RaidReportFormatter.paceStats(cachedRaids, (RaidModeFilter) modeBox.getSelectedItem(), (RaidSizeFilter) sizeBox.getSelectedItem(), reportOptions());
		}
		catch (IOException ex)
		{
			cachedRaids = java.util.Collections.emptyList();
			paceStats = RaidReportFormatter.paceStats(cachedRaids, (RaidModeFilter) modeBox.getSelectedItem(), (RaidSizeFilter) sizeBox.getSelectedItem(), reportOptions());
		}
		dataGeneration++;
	}

	private void paintLive()
	{
		pendingLive = false;
		int tab = tabs.getSelectedIndex();
		if (tab <= 0)
		{
			paintPace();
		}
		else if (tab == 1 && !targetTimes.showsLastAverage())
		{
			paintTarget();
		}
	}

	private void paintFromCache()
	{
		paintPace();
		paintRest();
	}

	private void paintPace()
	{
		RaidReportFormatter.PaceComparison pace;
		try
		{
			RaidModeFilter mode = (RaidModeFilter) modeBox.getSelectedItem();
			RaidSizeFilter size = (RaidSizeFilter) sizeBox.getSelectedItem();
			if (live && paceStats != null)
			{
				pace = RaidReportFormatter.livePace(paceStats, liveRaid, liveOpenSeconds, liveInProgress);
			}
			else
			{
				CoxRaidRecord subject = live ? liveRaid : null;
				pace = RaidReportFormatter.paceView(cachedRaids, mode, size, reportOptions(), subject, liveOpenSeconds, live && liveInProgress);
			}
		}
		catch (RuntimeException ex)
		{
			pace = new RaidReportFormatter.PaceComparison("Could not read the log.\n", java.util.Collections.<RaidReportFormatter.PacePoint>emptyList());
		}
		ScrollSpot spot = ScrollSpot.capture(paceScroll);
		boolean laidOut = activeTimes.show(pace);
		tabs.setToolTipTextAt(0, live ? "This raid vs your average" : "Recent raid vs your average");
		paceGraph.setPoints(pace.getPoints());
		if (laidOut)
		{
			paceColumn.revalidate();
			paceScroll.revalidate();
			spot.restoreLater();
		}
	}

	private void paintRest()
	{
		RaidReportFormatter.PaceComparison targets = new RaidReportFormatter.PaceComparison("No raids saved yet.\n", java.util.Collections.<RaidReportFormatter.PacePoint>emptyList());
		RaidReportFormatter.PaceComparison bests = targets;
		try
		{
			RaidModeFilter mode = (RaidModeFilter) modeBox.getSelectedItem();
			RaidSizeFilter size = (RaidSizeFilter) sizeBox.getSelectedItem();
			ReportOptions options = reportOptions();
			targets = targetComparison(mode, size, options);
			bests = RaidReportFormatter.bestView(cachedRaids, mode, size, options, bestFocus());
			if (cachedRaids.isEmpty())
			{
				purplePanel.showNotice("No completed raids logged yet.");
			}
			else
			{
				purplePanel.showBoard(
					RaidReportFormatter.purpleBoard(cachedRaids),
					options.isPurpleSummary(),
					options.isTrackedPurples()
				);
			}
		}
		catch (RuntimeException ex)
		{
			targets = new RaidReportFormatter.PaceComparison("Could not read the log.\n", java.util.Collections.<RaidReportFormatter.PacePoint>emptyList());
			bests = targets;
			purplePanel.showNotice("Could not read the log.");
		}
		applyTarget(targets);
		bestTimes.show(bests);
		paintedGeneration = dataGeneration;
	}

	private void paintBest()
	{
		RaidReportFormatter.PaceComparison bests;
		try
		{
			bests = RaidReportFormatter.bestView(
				cachedRaids,
				(RaidModeFilter) modeBox.getSelectedItem(),
				(RaidSizeFilter) sizeBox.getSelectedItem(),
				reportOptions(),
				bestFocus()
			);
		}
		catch (RuntimeException ex)
		{
			bests = new RaidReportFormatter.PaceComparison("Could not read the log.\n", java.util.Collections.<RaidReportFormatter.PacePoint>emptyList());
		}
		bestTimes.show(bests);
	}

	private void paintTarget()
	{
		RaidReportFormatter.PaceComparison targets;
		try
		{
			targets = targetComparison(
				(RaidModeFilter) modeBox.getSelectedItem(),
				(RaidSizeFilter) sizeBox.getSelectedItem(),
				reportOptions()
			);
		}
		catch (RuntimeException ex)
		{
			targets = new RaidReportFormatter.PaceComparison("Could not read the log.\n", java.util.Collections.<RaidReportFormatter.PacePoint>emptyList());
		}
		applyTarget(targets);
	}

	/** This is one raid against the sheet. Last N is the average of that many filtered raids. */
	private RaidReportFormatter.PaceComparison targetComparison(RaidModeFilter mode, RaidSizeFilter size, ReportOptions options)
	{
		if (targetTimes.showsLastAverage())
		{
			return RaidReportFormatter.lastAverageTargetView(cachedRaids, mode, size, options, targetTimes.getCount());
		}
		CoxRaidRecord subject = live ? liveRaid : null;
		return RaidReportFormatter.targetView(cachedRaids, mode, size, options, subject, liveOpenSeconds, live && liveInProgress);
	}

	private void applyTarget(RaidReportFormatter.PaceComparison targets)
	{
		ScrollSpot spot = ScrollSpot.capture(targetScroll);
		boolean laidOut = targetTimes.show(targets);
		targetGraph.setEmptyText(graphNotice(targets.getText()));
		targetGraph.setPoints(targets.getPoints());
		if (targetTimes.showsLastAverage())
		{
			tabs.setToolTipTextAt(1, "Last " + targetTimes.getCount() + " raids vs your targets");
		}
		else
		{
			tabs.setToolTipTextAt(1, "This raid vs your targets");
		}
		if (laidOut)
		{
			targetColumn.revalidate();
			targetScroll.revalidate();
			spot.restoreLater();
		}
	}

	private void restoreFilters()
	{
		if (configManager == null)
		{
			return;
		}
		RaidModeFilter mode = enumValue(RaidModeFilter.class, configManager.getConfiguration(FILTER_GROUP, MODE_KEY));
		RaidSizeFilter size = RaidSizeFilter.fromKey(configManager.getConfiguration(FILTER_GROUP, SIZE_KEY));
		if (mode != null)
		{
			modeBox.setSelectedItem(mode);
		}
		if (size != null)
		{
			pendingSize = size;
			sizeBox.setSelectedItem(size);
		}
	}

	private void rememberFilters()
	{
		if (configManager == null)
		{
			return;
		}
		RaidModeFilter mode = (RaidModeFilter) modeBox.getSelectedItem();
		RaidSizeFilter size = (RaidSizeFilter) sizeBox.getSelectedItem();
		if (mode != null)
		{
			configManager.setConfiguration(FILTER_GROUP, MODE_KEY, mode.name());
		}
		if (size != null)
		{
			configManager.setConfiguration(FILTER_GROUP, SIZE_KEY, size.key());
		}
	}

	/**
	 * All, Solo, and Team stay put. Each party size of 2 or more in the log is added after Team.
	 * A chosen size that is not in this log stays in the menu.
	 */
	private void fillSizeBox(List<CoxRaidRecord> raids)
	{
		java.util.TreeSet<Integer> parties = new java.util.TreeSet<>();
		if (raids != null)
		{
			for (int i = 0; i < raids.size(); i++)
			{
				CoxRaidRecord raid = raids.get(i);
				if (raid != null && raid.getTeamSize() >= 2)
				{
					parties.add(raid.getTeamSize());
				}
			}
		}
		RaidSizeFilter selected = pendingSize != null ? pendingSize : (RaidSizeFilter) sizeBox.getSelectedItem();
		pendingSize = null;
		if (selected != null && selected.getParty() >= 2)
		{
			parties.add(selected.getParty());
		}
		if (sameSizes(parties))
		{
			if (selected != null && !selected.equals(sizeBox.getSelectedItem()))
			{
				fillingSizes = true;
				sizeBox.setSelectedItem(selected);
				fillingSizes = false;
			}
			return;
		}
		fillingSizes = true;
		sizeBox.removeAllItems();
		sizeBox.addItem(RaidSizeFilter.ALL);
		sizeBox.addItem(RaidSizeFilter.SOLO);
		sizeBox.addItem(RaidSizeFilter.TEAM);
		for (Integer party : parties)
		{
			sizeBox.addItem(RaidSizeFilter.of(party));
		}
		if (selected != null)
		{
			sizeBox.setSelectedItem(selected);
		}
		fillingSizes = false;
	}

	private boolean sameSizes(java.util.TreeSet<Integer> parties)
	{
		int expected = 3 + parties.size();
		if (sizeBox.getItemCount() != expected)
		{
			return false;
		}
		int index = 3;
		for (Integer party : parties)
		{
			RaidSizeFilter item = sizeBox.getItemAt(index);
			if (item == null || item.getParty() != party)
			{
				return false;
			}
			index++;
		}
		return true;
	}

	private static <T extends Enum<T>> T enumValue(Class<T> type, String name)
	{
		if (name == null || name.isEmpty())
		{
			return null;
		}
		try
		{
			return Enum.valueOf(type, name);
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	private void printReport()
	{
		try
		{
			List<CoxRaidRecord> raids = loadAll();
			String report = RaidReportFormatter.format(
				raids,
				(RaidModeFilter) modeBox.getSelectedItem(),
				(RaidSizeFilter) sizeBox.getSelectedItem(),
				reportOptions()
			);
			ColoredText.show(this, "Cox Grind report", report);
		}
		catch (Exception ex)
		{
			log.warn("Cox Grind could not open the report", ex);
		}
	}

	private void openFolder()
	{
		try
		{
			Files.createDirectories(store.getRoot());
			if (!Desktop.isDesktopSupported())
			{
				return;
			}
			Desktop.getDesktop().open(store.getRoot().toFile());
		}
		catch (Exception ex)
		{
			log.warn("Cox Grind could not open the log folder", ex);
		}
	}

	private ReportOptions reportOptions()
	{
		ReportOptions options = ReportOptions.defaults(10);
		options.setTargets(TargetSettings.from(config));
		options.setTargetStyle(TargetSettings.style(config));
		options.setPurpleSummary(true);
		options.setTrackedPurples(true);
		options.setRoomEfficiency(true);
		options.setCommonRooms(true);
		options.setOutliers(true);
		return options;
	}

	private List<CoxRaidRecord> loadAll() throws IOException
	{
		if (accountHash == null || accountHash.isEmpty())
		{
			return java.util.Collections.emptyList();
		}
		return store.load(accountHash);
	}

	private String bestFocus()
	{
		int index = bestTimes.getChoice();
		if (index == 1)
		{
			return "raid";
		}
		if (index == 2)
		{
			return "pph";
		}
		return "fastest";
	}

	private static String graphNotice(String text)
	{
		if (text == null)
		{
			return "Set targets in plugin settings.";
		}
		if (text.startsWith("No times"))
		{
			return "No times in this window.";
		}
		if (text.startsWith("No completed") || text.startsWith("No raids"))
		{
			return "No raids in this filter.";
		}
		if (text.startsWith("Pick Regular"))
		{
			return "Pick Regular, Regular full, or CM.";
		}
		if (text.startsWith("Pick Solo"))
		{
			return "Pick Solo or Team.";
		}
		if (text.startsWith("Could not"))
		{
			return "Could not read the log.";
		}
		return "Set targets in plugin settings.";
	}

	private JScrollPane reading(ActiveTimes list)
	{
		JScrollPane scroll = scroll(list);
		scroll.getViewport().setBackground(new Color(24, 24, 24));
		return scroll;
	}

	private JScrollPane reading(PurplePanel pane)
	{
		JScrollPane scroll = scroll(pane);
		scroll.getViewport().setBackground(net.runelite.client.ui.ColorScheme.DARK_GRAY_COLOR);
		return scroll;
	}

	private JScrollPane scroll(java.awt.Component pane)
	{
		JScrollPane scroll = new JScrollPane(pane);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getViewport().setBackground(new Color(24, 24, 24));
		return scroll;
	}

	private JButton button(String text, Runnable action)
	{
		JButton button = new JButton(text);
		button.addActionListener(event -> action.run());
		return button;
	}

	private static void onEdt(Runnable action)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			action.run();
		}
		else
		{
			SwingUtilities.invokeLater(action);
		}
	}

	/** Scroll position taken before a list changes height, put back after layout. */
	private static final class ScrollSpot
	{
		private final JScrollPane scroll;
		private final int value;
		private final boolean followEnd;

		private ScrollSpot(JScrollPane scroll, int value, boolean followEnd)
		{
			this.scroll = scroll;
			this.value = value;
			this.followEnd = followEnd;
		}

		private static ScrollSpot capture(JScrollPane scroll)
		{
			JScrollBar bar = scroll.getVerticalScrollBar();
			int value = bar.getValue();
			boolean followEnd = value + bar.getVisibleAmount() >= bar.getMaximum() - 24;
			return new ScrollSpot(scroll, value, followEnd);
		}

		private void restoreLater()
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					JScrollBar again = scroll.getVerticalScrollBar();
					again.setValue(followEnd ? again.getMaximum() : value);
				}
			});
		}
	}

	/**
	 * One column: the split list, then the graph. The tab scrolls this as a single piece.
	 */
	private final class PaceColumn extends JPanel implements Scrollable
	{
		private final ActiveTimes list;
		private final PaceGraph graph;
		private int appliedWidth = -1;
		private int appliedHeight = -1;

		private PaceColumn(ActiveTimes list, PaceGraph graph)
		{
			this.list = list;
			this.graph = graph;
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setOpaque(true);
			setBackground(new Color(24, 24, 24));
		}

		@Override
		public Dimension getPreferredSize()
		{
			int width = columnWidth();
			int listHeight = list.preferredHeight(width);
			if (appliedWidth != width || appliedHeight != listHeight)
			{
				appliedWidth = width;
				appliedHeight = listHeight;
				Dimension size = new Dimension(width, listHeight);
				list.setPreferredSize(size);
				list.setMinimumSize(size);
				list.setMaximumSize(new Dimension(Integer.MAX_VALUE, listHeight));
			}
			int graphHeight = graph.getPreferredSize().height;
			return new Dimension(width, listHeight + graphHeight);
		}

		private int columnWidth()
		{
			if (getParent() instanceof JViewport)
			{
				int viewportWidth = ((JViewport) getParent()).getWidth();
				if (viewportWidth > 0)
				{
					return viewportWidth;
				}
			}
			if (getWidth() > 0)
			{
				return getWidth();
			}
			return Math.max(160, CoxGrindPanel.this.getWidth() - 8);
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return Math.max(16, visibleRect.height - 16);
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}
}
