package com.coxgrind.ui;

import com.coxgrind.CoxGrindConfig;
import com.coxgrind.log.RaidLogStore;
import com.coxgrind.model.CoxRaidRecord;
import com.coxgrind.model.RaidModeFilter;
import com.coxgrind.model.RaidSizeFilter;
import com.coxgrind.report.RaidReportFormatter;
import com.coxgrind.report.ReportOptions;
import com.coxgrind.report.TargetSettings;
import com.coxgrind.report.TargetStyle;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import net.runelite.client.ui.PluginPanel;

public class CoxGrindPanel extends PluginPanel
{
	private static final String FILTER_GROUP = "coxgrind";
	private static final String MODE_KEY = "panelMode";
	private static final String SIZE_KEY = "panelSize";
	private final RaidLogStore store;
	private final CoxGrindConfig config;
	private final ConfigManager configManager;
	private final JComboBox<RaidModeFilter> modeBox = new JComboBox<>(RaidModeFilter.values());
	private final JComboBox<RaidSizeFilter> sizeBox = new JComboBox<>(RaidSizeFilter.values());
	private final ActiveTimes activeTimes = new ActiveTimes();
	private final PaceGraph paceGraph = new PaceGraph();
	private final PaceColumn paceColumn = new PaceColumn();
	private final JScrollPane paceScroll = new JScrollPane(paceColumn);
	private final JTabbedPane tabs = new JTabbedPane();
	private final ActiveTimes targetTimes = new ActiveTimes();
	private final PurplePanel purplePanel = new PurplePanel();
	private final ActiveTimes bestTimes = new ActiveTimes();
	private String accountHash;
	private List<CoxRaidRecord> cachedRaids = java.util.Collections.emptyList();
	private boolean live;
	private CoxRaidRecord liveRaid;
	private int liveOpenSeconds = -1;
	private boolean liveInProgress;
	private long logStamp = Long.MIN_VALUE;

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
		tabs.addTab("Active", paceScroll);
		tabs.setToolTipTextAt(0, "Recent raid vs your average");
		tabs.addTab("Target", reading(targetTimes));
		tabs.setToolTipTextAt(1, "Recent raid vs your targets");
		tabs.addTab("Bests", reading(bestTimes));
		tabs.setToolTipTextAt(2, "Fastest split, points, and PPH in this filter");
		tabs.addTab("Purples", reading(purplePanel));
		tabs.setToolTipTextAt(3, "Every logged raid");
		add(tabs, BorderLayout.CENTER);
		tabs.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent event)
			{
				if (tabs.getSelectedIndex() > 0)
				{
					refreshCache();
					paintRest();
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
			rememberFilters();
			reload();
		});
		accountHash = store.latestAccountHash();
		reload();
	}

	public void setAccount(String hash)
	{
		onEdt(new Runnable()
		{
			@Override
			public void run()
			{
				if (hash != null && !hash.isEmpty())
				{
					accountHash = hash;
				}
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
				if (currentLogStamp() != logStamp)
				{
					refreshCache();
					paintRest();
				}
				paintPace();
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
			logStamp = currentLogStamp();
		}
		catch (IOException ex)
		{
			cachedRaids = java.util.Collections.emptyList();
		}
	}

	private long currentLogStamp()
	{
		if (accountHash == null || accountHash.isEmpty())
		{
			return Long.MIN_VALUE;
		}
		try
		{
			Path file = store.accountFile(accountHash);
			if (!Files.exists(file))
			{
				return Long.MIN_VALUE;
			}
			return Files.getLastModifiedTime(file).toMillis();
		}
		catch (IOException ex)
		{
			return Long.MIN_VALUE;
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
			CoxRaidRecord subject = live ? liveRaid : null;
			pace = RaidReportFormatter.paceView(cachedRaids, mode, size, reportOptions(), subject, liveOpenSeconds, live && liveInProgress);
		}
		catch (RuntimeException ex)
		{
			pace = new RaidReportFormatter.PaceComparison("Could not read the log.\n", java.util.Collections.<RaidReportFormatter.PacePoint>emptyList());
		}
		JScrollBar bar = paceScroll.getVerticalScrollBar();
		int value = bar.getValue();
		boolean followEnd = value + bar.getVisibleAmount() >= bar.getMaximum() - 24;
		activeTimes.show(pace);
		tabs.setToolTipTextAt(0, live ? "This raid vs your average" : "Recent raid vs your average");
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				JScrollBar again = paceScroll.getVerticalScrollBar();
				again.setValue(followEnd ? again.getMaximum() : value);
			}
		});
		paceGraph.setPoints(pace.getPoints());
		paceColumn.revalidate();
		paceScroll.revalidate();
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
			targets = RaidReportFormatter.targetView(cachedRaids, mode, size, options);
			bests = RaidReportFormatter.bestView(cachedRaids, mode, size, options);
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
		targetTimes.show(targets);
		bestTimes.show(bests);
	}

	private void restoreFilters()
	{
		if (configManager == null)
		{
			return;
		}
		RaidModeFilter mode = enumValue(RaidModeFilter.class, configManager.getConfiguration(FILTER_GROUP, MODE_KEY));
		RaidSizeFilter size = enumValue(RaidSizeFilter.class, configManager.getConfiguration(FILTER_GROUP, SIZE_KEY));
		if (mode != null)
		{
			modeBox.setSelectedItem(mode);
		}
		if (size != null)
		{
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
			configManager.setConfiguration(FILTER_GROUP, SIZE_KEY, size.name());
		}
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
			ColoredText.show(this, "CoXGrind report", report);
		}
		catch (Exception ignored)
		{
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
		catch (Exception ignored)
		{
		}
	}

	private ReportOptions reportOptions()
	{
		ReportOptions options = ReportOptions.defaults(lastN());
		options.setTargets(TargetSettings.from(config));
		if (config == null)
		{
			return options;
		}
		options.setReportRaids(config.reportRaids());
		options.setDeathFullRegular(config.deathFullRegular());
		options.setDeathRegular(config.deathRegular());
		options.setDeathCmSolo(config.deathCmSolo());
		options.setDeathCmTeam(config.deathCmTeam());
		options.setPurpleSummary(config.showPurpleSummary());
		options.setTrackedPurples(config.showTrackedPurples());
		options.setRoomEfficiency(config.showRoomEfficiency());
		options.setCommonRooms(config.showCommonRooms());
		options.setOutliers(config.showOutliers());
		TargetStyle style = options.getTargetStyle();
		style.setUseTbow(config.useTbow());
		style.setNoTbowVanguards(config.noTbowVanguards());
		style.setNoTbowVasa(config.noTbowVasa());
		style.setNoTbowMystics(config.noTbowMystics());
		style.setNoTbowMuttadiles(config.noTbowMuttadiles());
		style.setNoTbowTightrope(config.noTbowTightrope());
		style.setNoTbowOlmHead(config.noTbowOlmHead());
		style.setIceMilking(config.iceMilking());
		style.setIceMilkSeconds(config.iceMilkSeconds());
		style.setKillRope(config.killRope());
		style.setKillRopeSeconds(config.killRopeSeconds());
		style.setMilkVespula(config.milkVespula());
		style.setMilkVespulaSeconds(config.milkVespulaSeconds());
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

	private int lastN()
	{
		int value = config == null ? 10 : config.lastRaids();
		if (value < 1)
		{
			return 10;
		}
		return Math.min(value, 100);
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

	/**
	 * One column: the split list, then the graph. The tab scrolls this as a single piece.
	 */
	private final class PaceColumn extends JPanel implements Scrollable
	{
		private PaceColumn()
		{
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setOpaque(true);
			setBackground(new Color(24, 24, 24));
		}

		@Override
		public Dimension getPreferredSize()
		{
			int width = columnWidth();
			int listHeight = activeTimes.preferredHeight(width);
			Dimension list = new Dimension(width, listHeight);
			activeTimes.setPreferredSize(list);
			activeTimes.setMinimumSize(list);
			activeTimes.setMaximumSize(new Dimension(Integer.MAX_VALUE, listHeight));
			activeTimes.setSize(list);
			int graphHeight = paceGraph.getPreferredSize().height;
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
