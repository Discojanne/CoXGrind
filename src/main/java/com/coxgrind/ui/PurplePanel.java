package com.coxgrind.ui;

import com.coxgrind.report.PurpleBoard;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Card layout for the Purples tab. The full report stays plain text.
 */
public class PurplePanel extends JPanel implements Scrollable
{
	private static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;
	private static final Color INK = ColorScheme.TEXT_COLOR;
	private static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;
	private static final Color GREEN = new Color(80, 220, 120);
	private static final Color PURPLE = new Color(196, 96, 255);
	private static final Color RED = new Color(255, 90, 90);
	private static final Color CYAN = new Color(80, 210, 255);
	private static final Color GOLD = new Color(232, 196, 120);
	private static final Color DRY = new Color(58, 58, 58);
	private static final Color KIT = new Color(12, 92, 48);
	private static final Color PET = Color.WHITE;
	private static final Font LABEL = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
	private static final Font VALUE = new Font(Font.SANS_SERIF, Font.BOLD, 16);
	private static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

	private PurpleBoard board = PurpleBoard.empty();
	private boolean summaryOn = true;
	private boolean trackedOn = true;
	private String notice = "No completed raids logged yet.";
	private boolean summaryOpen = true;
	private boolean itemsOpen = true;
	private boolean historyOpen = true;
	private boolean trackedOpen = true;
	private ItemManager itemManager;

	public PurplePanel()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(true);
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		rebuild();
	}

	public void setItemManager(ItemManager itemManager)
	{
		this.itemManager = itemManager;
	}

	public void showBoard(PurpleBoard next, boolean summary, boolean tracked)
	{
		board = next == null ? PurpleBoard.empty() : next;
		summaryOn = summary;
		trackedOn = tracked;
		notice = null;
		rebuild();
	}

	public void showNotice(String text)
	{
		board = PurpleBoard.empty();
		notice = text == null ? "" : text;
		rebuild();
	}

	private void rebuild()
	{
		removeAll();
		if (notice != null)
		{
			add(notice(notice));
		}
		else if (board.isEmpty())
		{
			add(notice("No completed raids logged yet."));
		}
		else if (!summaryOn && (!trackedOn || board.getTracked().isEmpty()))
		{
			add(notice("Purple summary is turned off in the plugin settings."));
		}
		else
		{
			if (summaryOn)
			{
				add(section("Summary", summaryOpen, new Runnable()
				{
					@Override
					public void run()
					{
						summaryOpen = !summaryOpen;
						rebuild();
					}
				}, summaryOpen ? summaryBody() : null));
				add(gap());
				add(section("Items", itemsOpen, new Runnable()
				{
					@Override
					public void run()
					{
						itemsOpen = !itemsOpen;
						rebuild();
					}
				}, itemsOpen ? itemsBody() : null));
				add(gap());
				add(section("History", historyOpen, new Runnable()
				{
					@Override
					public void run()
					{
						historyOpen = !historyOpen;
						rebuild();
					}
				}, historyOpen ? historyBody() : null));
			}
			if (trackedOn && !board.getTracked().isEmpty())
			{
				if (summaryOn)
				{
					add(gap());
				}
				add(section("Tracked items", trackedOpen, new Runnable()
				{
					@Override
					public void run()
					{
						trackedOpen = !trackedOpen;
						rebuild();
					}
				}, trackedOpen ? trackedBody() : null));
			}
		}
		add(Box.createVerticalGlue());
		revalidate();
		repaint();
	}

	private JPanel summaryBody()
	{
		JPanel grid = new JPanel(new GridLayout(0, 2, 6, 6));
		grid.setOpaque(false);
		grid.add(stat("Raids", Integer.toString(board.getRaids()), INK));
		String rate = board.getExpected() > 0
			? String.format(Locale.US, "1/%.2f", board.getRate())
			: "--";
		grid.add(stat("Rate", rate, INK));
		grid.add(stat("Regular", Integer.toString(board.getRegular()), CYAN));
		grid.add(stat("CM", Integer.toString(board.getChallengeMode()), GOLD));
		grid.add(stat("Expected", String.format(Locale.US, "%.1f", board.getExpected()), INK));
		grid.add(stat("Actual", Integer.toString(board.getActual()), INK));
		double scrollPct = board.getActual() == 0 ? 0 : board.getScrolls() * 100.0 / board.getActual();
		grid.add(stat("Scrolls", String.format(Locale.US, "%.0f%%", scrollPct), INK));
		grid.add(stat("Diff", String.format(Locale.US, "%+.1f", board.getDiff()), diffColor(board.getDiff())));

		JPanel dry = new JPanel(new GridLayout(1, 3, 6, 0));
		dry.setOpaque(false);
		dry.add(stat("Dry now", Integer.toString(board.getCurrentDry()), INK));
		dry.add(stat("Longest", Integer.toString(board.getLongestDry()), INK));
		dry.add(stat("Average", Integer.toString(board.getAverageDry()), INK));

		JPanel points = new JPanel(new GridLayout(1, 2, 6, 0));
		points.setOpaque(false);
		points.add(stat("Avg points", grouped(Math.round(board.getAveragePoints())), INK));
		points.add(stat("Total points", grouped(board.getAllPoints()), INK));

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);
		body.add(grid);
		body.add(Box.createVerticalStrut(6));
		body.add(dry);
		body.add(Box.createVerticalStrut(6));
		body.add(points);
		return body;
	}

	private JPanel itemsBody()
	{
		JPanel grid = new JPanel(new GridLayout(0, 2, 4, 4));
		grid.setOpaque(false);
		List<PurpleBoard.Item> items = board.getItems();
		for (int i = 0; i < items.size(); i++)
		{
			grid.add(itemRow(items.get(i)));
		}
		fillWidth(grid);
		JPanel side = new JPanel(new GridLayout(1, 3, 4, 0));
		side.setOpaque(false);
		List<PurpleBoard.Item> extras = board.getSideItems();
		for (int i = 0; i < extras.size(); i++)
		{
			side.add(sideRow(extras.get(i)));
		}
		fillWidth(side);
		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);
		body.add(grid);
		body.add(Box.createVerticalStrut(10));
		body.add(side);
		fillWidth(body);
		return body;
	}

	private JPanel itemRow(PurpleBoard.Item item)
	{
		boolean held = item.getGot() > 0;
		JPanel row = card();
		row.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		JLabel icon = itemIcon(item.getName(), 20);
		JLabel count = new JLabel(Integer.toString(item.getGot()));
		count.setFont(LABEL);
		count.setForeground(held ? INK : MUTED);
		String diff = String.format(Locale.US, "%+.2f", item.getDiff());
		JLabel rate = new JLabel(diff);
		rate.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
		rate.setForeground(held || Math.abs(item.getDiff()) >= 0.05 ? diffColor(item.getDiff()) : MUTED);
		row.add(icon);
		row.add(Box.createHorizontalStrut(4));
		row.add(count);
		row.add(Box.createHorizontalStrut(4));
		row.add(rate);
		row.add(Box.createHorizontalGlue());
		fillWidth(row);
		icon.setToolTipText(item.getName());
		String tip = String.format(Locale.US, "Got %d, on rate %.2f", item.getGot(), item.getOnRate());
		count.setToolTipText(tip);
		rate.setToolTipText(tip);
		return row;
	}

	private JPanel sideRow(PurpleBoard.Item item)
	{
		boolean held = item.getGot() > 0;
		JPanel row = card();
		row.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		JLabel icon = itemIcon(item.getName(), 20);
		JLabel count = new JLabel(Integer.toString(item.getGot()));
		count.setFont(LABEL);
		count.setForeground(held ? sideColor(item.getName()) : MUTED);
		row.add(icon);
		row.add(Box.createHorizontalStrut(4));
		row.add(count);
		row.add(Box.createHorizontalGlue());
		fillWidth(row);
		String tip = item.getName() + ", got " + item.getGot();
		icon.setToolTipText(tip);
		count.setToolTipText(tip);
		return row;
	}

	private static Color sideColor(String name)
	{
		if ("Olmlet".equals(name))
		{
			return PET;
		}
		if ("Twisted ancestral colour kit".equals(name))
		{
			return KIT;
		}
		return CYAN;
	}

	private JLabel itemIcon(String name, int size)
	{
		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(size, size));
		icon.setMinimumSize(new Dimension(size, size));
		int id = itemId(name);
		if (itemManager == null || id < 0)
		{
			return icon;
		}
		try
		{
			AsyncBufferedImage image = itemManager.getImage(id);
			image.onLoaded(new Runnable()
			{
				@Override
				public void run()
				{
					icon.setIcon(new ImageIcon(scale(image, size)));
					icon.repaint();
				}
			});
			if (image.getWidth() > 1)
			{
				icon.setIcon(new ImageIcon(scale(image, size)));
			}
		}
		catch (RuntimeException ex)
		{
			return icon;
		}
		return icon;
	}

	private static BufferedImage scale(BufferedImage image, int size)
	{
		BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = scaled.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, 0, 0, size, size, null);
		g.dispose();
		return scaled;
	}

	private static int itemId(String name)
	{
		if ("Dexterous prayer scroll".equals(name))
		{
			return ItemID.RAIDS_PRAYERSCROLL;
		}
		if ("Arcane prayer scroll".equals(name))
		{
			return ItemID.RAIDS_PRAYERSCROLL_AUGURY;
		}
		if ("Twisted buckler".equals(name))
		{
			return ItemID.TWISTED_BUCKLER;
		}
		if ("Dragon hunter crossbow".equals(name))
		{
			return ItemID.DRAGONHUNTER_XBOW;
		}
		if ("Dinh's bulwark".equals(name))
		{
			return ItemID.DINHS_BULWARK;
		}
		if ("Ancestral hat".equals(name))
		{
			return ItemID.ANCESTRAL_HAT;
		}
		if ("Ancestral robe top".equals(name))
		{
			return ItemID.ANCESTRAL_ROBE_TOP;
		}
		if ("Ancestral robe bottom".equals(name))
		{
			return ItemID.ANCESTRAL_ROBE_BOTTOM;
		}
		if ("Dragon claws".equals(name))
		{
			return ItemID.DRAGON_CLAWS;
		}
		if ("Elder maul".equals(name))
		{
			return ItemID.ELDER_MAUL;
		}
		if ("Kodai insignia".equals(name))
		{
			return ItemID.KODAI_INSIGNIA;
		}
		if ("Twisted bow".equals(name))
		{
			return ItemID.TWISTED_BOW;
		}
		if ("Olmlet".equals(name))
		{
			return ItemID.OLMPET;
		}
		if ("Twisted ancestral colour kit".equals(name))
		{
			return ItemID.ANCESTRAL_ROBES_TWISTED_KIT;
		}
		if ("Metamorphic dust".equals(name))
		{
			return ItemID.RAIDS_CHALLENGE_MORPH;
		}
		return -1;
	}

	private JPanel historyBody()
	{
		JPanel wrap = new JPanel();
		wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
		wrap.setOpaque(false);
		String every = board.getExpectedEvery() > 0
			? "Purple is a unique you received. A dot is where one was expected, every "
				+ board.getExpectedEvery() + " raids. Grey is a white light. Gold is next. "
				+ "Kit is dark green, pet is white, and dust is cyan."
			: "Purple is a unique you received. Grey is a white light. Gold is next. "
				+ "Kit is dark green, pet is white, and dust is cyan.";
		JLabel caption = new JLabel("<html><body style='width:150px'>" + every + "</body></html>");
		caption.setFont(LABEL);
		caption.setForeground(MUTED);
		wrap.add(caption);
		wrap.add(Box.createVerticalStrut(6));
		HistoryStrip strip = new HistoryStrip(board.getMarks(), board.getSideKinds());
		strip.setAlignmentX(Component.LEFT_ALIGNMENT);
		wrap.add(strip);
		long worth = board.getRaidsWorth();
		String worthText = String.format(Locale.US, "%+.1f purples, about %d raids", board.getDiff(), worth);
		JLabel diff = new JLabel(worthText);
		diff.setFont(LABEL);
		diff.setForeground(diffColor(board.getDiff()));
		diff.setAlignmentX(Component.LEFT_ALIGNMENT);
		wrap.add(Box.createVerticalStrut(6));
		wrap.add(diff);
		return wrap;
	}

	private JPanel trackedBody()
	{
		JPanel list = new JPanel();
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setOpaque(false);
		List<PurpleBoard.Drop> drops = board.getTracked();
		for (int i = 0; i < drops.size(); i++)
		{
			PurpleBoard.Drop drop = drops.get(i);
			JPanel row = card();
			row.setLayout(new BorderLayout(8, 0));
			String kc = drop.isChallengeMode() ? "CM " + drop.getKc() : "KC " + drop.getKc();
			JLabel tag = new JLabel(kc);
			tag.setFont(LABEL);
			tag.setForeground(drop.isChallengeMode() ? GOLD : CYAN);
			JLabel name = new JLabel(drop.getItem());
			name.setFont(BODY);
			name.setForeground(trackedColor(drop.getKind()));
			name.setMinimumSize(new Dimension(0, 16));
			row.add(tag, BorderLayout.WEST);
			row.add(name, BorderLayout.CENTER);
			list.add(row);
			if (i + 1 < drops.size())
			{
				list.add(Box.createVerticalStrut(4));
			}
		}
		return list;
	}

	private JPanel section(String title, boolean open, Runnable toggle, JPanel body)
	{
		JPanel block = new JPanel();
		block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
		block.setOpaque(false);
		block.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel header = new JLabel((open ? "▾  " : "▸  ") + title);
		header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
		header.setForeground(ColorScheme.BRAND_ORANGE);
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		header.setAlignmentX(Component.LEFT_ALIGNMENT);
		header.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent event)
			{
				toggle.run();
			}
		});
		block.add(header);
		if (body != null)
		{
			body.setAlignmentX(Component.LEFT_ALIGNMENT);
			block.add(Box.createVerticalStrut(6));
			block.add(body);
		}
		fillWidth(block);
		return block;
	}

	private static void fillWidth(JComponent component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		int height = Math.max(1, component.getPreferredSize().height);
		component.setMinimumSize(new Dimension(0, height));
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
	}

	private static JPanel stat(String label, String value, Color color)
	{
		JPanel card = card();
		card.setLayout(new BorderLayout());
		JLabel caption = new JLabel(label);
		caption.setFont(LABEL);
		caption.setForeground(MUTED);
		JLabel number = new JLabel(value);
		number.setFont(VALUE);
		number.setForeground(color);
		card.add(caption, BorderLayout.NORTH);
		card.add(number, BorderLayout.CENTER);
		return card;
	}

	private static JPanel card()
	{
		JPanel card = new JPanel();
		card.setBackground(CARD);
		card.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		return card;
	}

	private static JLabel notice(String text)
	{
		JLabel label = new JLabel("<html><body style='width:150px'>" + text + "</body></html>");
		label.setFont(BODY);
		label.setForeground(INK);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	private static Component gap()
	{
		return Box.createVerticalStrut(12);
	}

	private static Color trackedColor(String kind)
	{
		if ("pet".equals(kind))
		{
			return PET;
		}
		if ("kit".equals(kind))
		{
			return KIT;
		}
		if ("dust".equals(kind))
		{
			return CYAN;
		}
		return GREEN;
	}

	private static Color diffColor(double delta)
	{
		if (Math.abs(delta) < 0.05)
		{
			return INK;
		}
		return delta > 0 ? GREEN : RED;
	}

	/** 53999 becomes 53'999. */
	private static String grouped(long value)
	{
		boolean negative = value < 0;
		String digits = Long.toString(Math.abs(value));
		StringBuilder out = new StringBuilder();
		int count = 0;
		for (int i = digits.length() - 1; i >= 0; i--)
		{
			if (count > 0 && count % 3 == 0)
			{
				out.append('\'');
			}
			out.append(digits.charAt(i));
			count++;
		}
		out.reverse();
		if (negative)
		{
			out.insert(0, '-');
		}
		return out.toString();
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

	private static final class HistoryStrip extends JPanel
	{
		private static final int CELL = 8;
		private static final int GAP = 2;
		private static final int ROW_GAP = 5;
		private static final Color ROW = new Color(48, 48, 48);
		private final List<PurpleBoard.Mark> marks;
		private final List<String> sideKinds;

		private HistoryStrip(List<PurpleBoard.Mark> marks, List<String> sideKinds)
		{
			this.marks = marks;
			this.sideKinds = sideKinds == null ? java.util.Collections.<String>emptyList() : sideKinds;
			setOpaque(false);
			setToolTipText("Each square is one logged raid, read left to right");
			addComponentListener(new ComponentAdapter()
			{
				@Override
				public void componentResized(ComponentEvent event)
				{
					if (getHeight() != getPreferredSize().height)
					{
						revalidate();
					}
				}
			});
			addMouseMotionListener(new MouseAdapter()
			{
				@Override
				public void mouseMoved(MouseEvent event)
				{
					setToolTipText(tip(event.getX(), event.getY()));
				}
			});
		}

		private int columns(int width)
		{
			return Math.max(1, (width + GAP) / (CELL + GAP));
		}

		private int rowStride()
		{
			return CELL + ROW_GAP;
		}

		@Override
		public Dimension getPreferredSize()
		{
			int width = getWidth() > 0 ? getWidth() : 160;
			int cols = columns(width);
			int rows = Math.max(1, (marks.size() + cols - 1) / cols);
			return new Dimension(width, rows * rowStride());
		}

		@Override
		public Dimension getMinimumSize()
		{
			return new Dimension(0, getPreferredSize().height);
		}

		@Override
		public Dimension getMaximumSize()
		{
			return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int cols = columns(getWidth());
			int rows = Math.max(1, (marks.size() + cols - 1) / cols);
			for (int row = 0; row < rows; row++)
			{
				int y = row * rowStride();
				g.setColor(ROW);
				g.fillRect(0, y, getWidth(), CELL);
			}
			for (int i = 0; i < marks.size(); i++)
			{
				int x = (i % cols) * (CELL + GAP);
				int y = (i / cols) * rowStride();
				paintMark(g, marks.get(i), sideAt(i), x, y);
			}
			g.dispose();
		}

		private String sideAt(int index)
		{
			if (index < 0 || index >= sideKinds.size())
			{
				return "";
			}
			String kind = sideKinds.get(index);
			return kind == null ? "" : kind;
		}

		private static void paintMark(Graphics2D g, PurpleBoard.Mark mark, String side, int x, int y)
		{
			boolean expected = mark == PurpleBoard.Mark.EXPECTED || mark == PurpleBoard.Mark.BOTH;
			g.setColor(color(mark, side));
			g.fillRect(x, y, CELL, CELL);
			if (expected)
			{
				paintRateDot(g, x, y, mark == PurpleBoard.Mark.BOTH);
			}
			if (mark == PurpleBoard.Mark.NEXT)
			{
				paintArrow(g, x, y);
			}
		}

		/** A light dot marks the rate. The square color stays the drop: grey or purple. */
		private static void paintRateDot(Graphics2D g, int x, int y, boolean onPurple)
		{
			g.setColor(onPurple ? new Color(255, 236, 180) : new Color(220, 220, 220));
			int dot = 3;
			g.fillOval(x + (CELL - dot) / 2, y + (CELL - dot) / 2, dot, dot);
		}

		private static void paintArrow(Graphics2D g, int x, int y)
		{
			g.setColor(new Color(48, 36, 16));
			g.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			int mid = y + CELL / 2;
			int tip = x + CELL - 2;
			g.drawLine(x + 2, y + 2, tip, mid);
			g.drawLine(tip, mid, x + 2, y + CELL - 2);
		}

		private String tip(int x, int y)
		{
			int cols = columns(getWidth());
			int col = x / (CELL + GAP);
			int row = y / rowStride();
			if (col < 0 || col >= cols || row < 0)
			{
				return null;
			}
			if (y % rowStride() >= CELL)
			{
				return null;
			}
			int index = row * cols + col;
			if (index < 0 || index >= marks.size())
			{
				return null;
			}
			PurpleBoard.Mark mark = marks.get(index);
			String kind;
			if (mark == PurpleBoard.Mark.NEXT)
			{
				kind = "next raid";
			}
			else if (mark == PurpleBoard.Mark.PURPLE)
			{
				kind = "purple";
			}
			else if (mark == PurpleBoard.Mark.EXPECTED)
			{
				kind = "expected";
			}
			else if (mark == PurpleBoard.Mark.BOTH)
			{
				kind = "purple and expected";
			}
			else if (sideAt(index).length() > 0)
			{
				kind = sideLabel(sideAt(index));
			}
			else
			{
				kind = "white light";
			}
			String side = sideAt(index);
			if (side.length() > 0 && mark != PurpleBoard.Mark.NEXT
				&& mark != PurpleBoard.Mark.DRY && mark != PurpleBoard.Mark.EXPECTED)
			{
				kind = kind + ", " + sideLabel(side);
			}
			return "Raid " + (index + 1) + ", " + kind;
		}

		private static String sideLabel(String side)
		{
			if ("pet".equals(side))
			{
				return "pet";
			}
			if ("kit".equals(side))
			{
				return "kit";
			}
			if ("dust".equals(side))
			{
				return "dust";
			}
			return side;
		}

		private static Color color(PurpleBoard.Mark mark, String side)
		{
			if (mark == PurpleBoard.Mark.PURPLE || mark == PurpleBoard.Mark.BOTH)
			{
				return PURPLE;
			}
			if (mark == PurpleBoard.Mark.NEXT)
			{
				return GOLD;
			}
			if ("pet".equals(side))
			{
				return PET;
			}
			if ("kit".equals(side))
			{
				return KIT;
			}
			if ("dust".equals(side))
			{
				return CYAN;
			}
			return DRY;
		}

		@Override
		public Insets getInsets()
		{
			return new Insets(0, 0, 0, 0);
		}
	}
}
