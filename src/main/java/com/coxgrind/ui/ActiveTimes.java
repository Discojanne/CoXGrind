package com.coxgrind.ui;

import com.coxgrind.report.RaidReportFormatter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import net.runelite.client.ui.ColorScheme;

/**
 * Drawn split list for Active, Target, and Bests.
 * One line per room, grouped into Rooms, Olm, and Finish.
 * A quiet line splits the upper floor from the middle, and the middle from the lower.
 */
public class ActiveTimes extends JPanel implements Scrollable
{
	private static final Color PAPER = new Color(24, 24, 24);
	private static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;
	private static final Color INK = ColorScheme.TEXT_COLOR;
	private static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;
	private static final Color HAIR = new Color(70, 70, 70);
	private static final Color GREEN = new Color(80, 220, 120);
	private static final Color RED = new Color(255, 90, 90);
	private static final Color ORANGE = new Color(255, 176, 46);
	private static final Color CYAN = new Color(80, 210, 255);
	private static final Color GOLD = new Color(232, 196, 120);
	private static final Font NAME = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
	private static final Font NAME_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 12);
	private static final Font SECTION = new Font(Font.SANS_SERIF, Font.BOLD, 11);
	private static final Font TIME = new Font(Font.SANS_SERIF, Font.BOLD, 13);
	private static final Font DIFF = new Font(Font.SANS_SERIF, Font.BOLD, 12);
	private static final Font BADGE = new Font(Font.SANS_SERIF, Font.BOLD, 11);
	private static final int ROW = 18;
	private static final int SECTION_H = 14;
	private static final int FLOOR_GAP = 4;

	private String notice = "No completed raids in this filter yet.";
	private List<RaidReportFormatter.TimeRow> rows = Collections.emptyList();
	private String badge = "";
	private Color badgeColor = MUTED;
	private String caption = "vs average";
	private boolean goldTimes;
	private boolean againstTarget;
	private int[] rowTop = new int[0];

	public ActiveTimes()
	{
		setOpaque(true);
		setBackground(PAPER);
		addMouseMotionListener(new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent event)
			{
				setToolTipText(tipAt(event.getY()));
			}
		});
	}

	public void show(RaidReportFormatter.PaceComparison pace)
	{
		if (pace == null || pace.getRows().isEmpty())
		{
			rows = Collections.emptyList();
			notice = pace == null ? "" : pace.getText().trim();
			badge = "";
			caption = "";
			goldTimes = false;
			againstTarget = false;
		}
		else
		{
			rows = new ArrayList<>(pace.getRows());
			notice = null;
			caption = pace.getCaption();
			goldTimes = pace.isGoldTimes();
			againstTarget = pace.isAgainstTarget();
			if (pace.isInProgress() && pace.getKc() <= 0)
			{
				badge = "In progress";
				badgeColor = MUTED;
			}
			else if (pace.getKc() > 0 && pace.isChallengeMode())
			{
				badge = "CM " + pace.getKc();
				badgeColor = GOLD;
			}
			else if (pace.getKc() > 0)
			{
				badge = "KC " + pace.getKc();
				badgeColor = CYAN;
			}
			else if (!pace.isGoldTimes())
			{
				badge = "Recent";
				badgeColor = MUTED;
			}
			else
			{
				badge = "";
			}
		}
		revalidate();
		repaint();
	}

	public int preferredHeight(int width)
	{
		if (notice != null)
		{
			return 22;
		}
		return 18 + cardHeight();
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(PAPER);
		g.fillRect(0, 0, getWidth(), getHeight());
		if (notice != null)
		{
			g.setFont(NAME);
			g.setColor(INK);
			g.drawString(notice, 8, 16);
			rowTop = new int[0];
			g.dispose();
			return;
		}

		g.setFont(BADGE);
		g.setColor(badgeColor);
		if (!badge.isEmpty())
		{
			g.drawString(badge, 8, 12);
		}
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		g.setColor(MUTED);
		int captionX = getWidth() - 8 - g.getFontMetrics().stringWidth(caption);
		if (!caption.isEmpty())
		{
			g.drawString(caption, Math.max(8 + g.getFontMetrics(BADGE).stringWidth(badge) + 8, captionX), 12);
		}

		int cardTop = 16;
		int cardHeight = cardHeight();
		int cardWidth = Math.max(20, getWidth() - 8);
		g.setColor(CARD);
		g.fillRoundRect(4, cardTop, cardWidth, cardHeight, 8, 8);

		FontMetrics timeMetrics = g.getFontMetrics(TIME);
		FontMetrics diffMetrics = g.getFontMetrics(DIFF);
		int timeWidth = timeMetrics.stringWidth("00:00");
		int diffWidth = diffMetrics.stringWidth("--");
		for (int i = 0; i < rows.size(); i++)
		{
			RaidReportFormatter.TimeRow row = rows.get(i);
			if (!row.isPoints())
			{
				timeWidth = Math.max(timeWidth, timeMetrics.stringWidth(row.getValue()));
			}
			if (!row.getDiff().isEmpty())
			{
				diffWidth = Math.max(diffWidth, diffMetrics.stringWidth(row.getDiff()));
			}
		}

		int y = cardTop + 4;
		rowTop = new int[rows.size()];
		String section = "";
		for (int i = 0; i < rows.size(); i++)
		{
			RaidReportFormatter.TimeRow row = rows.get(i);
			String next = sectionOf(row);
			if (next != null && !next.equals(section))
			{
				section = next;
				g.setFont(SECTION);
				g.setColor(ColorScheme.BRAND_ORANGE);
				g.drawString(section, 12, y + 11);
				y += SECTION_H;
			}
			if (floorBreakBefore(i))
			{
				int lineY = y + (FLOOR_GAP / 2);
				g.setColor(new Color(96, 96, 96));
				g.drawLine(20, lineY, 4 + cardWidth - 20, lineY);
				y += FLOOR_GAP;
			}
			if (subtotal(row))
			{
				g.setColor(HAIR);
				g.drawLine(12, y, 4 + cardWidth - 8, y);
			}
			rowTop[i] = y;
			paintRow(g, row, y, cardWidth, timeWidth, diffWidth);
			y += ROW;
		}
		g.dispose();
	}

	private void paintRow(Graphics2D g, RaidReportFormatter.TimeRow row, int y, int cardWidth, int timeWidth, int diffWidth)
	{
		Color tone = tone(row);
		g.setColor(goldTimes && !row.isPoints() ? GOLD : tone);
		g.fillRect(8, y + 3, 3, ROW - 6);
		Font nameFont = subtotal(row) ? NAME_BOLD : NAME;
		g.setFont(nameFont);
		g.setColor(row.isOpen() ? CYAN : INK);
		int nameX = 16;
		int valueWidth = g.getFontMetrics(TIME).stringWidth(row.getValue());
		int column = Math.max(timeWidth, valueWidth);
		int diffX = 4 + cardWidth - 8 - diffWidth;
		int timeX = diffX - 6 - column;
		int nameLimit = Math.max(8, timeX - 6 - nameX);
		String name = clip(g.getFontMetrics(), shown(row.getLabel()), nameLimit);
		int baseline = y + 13;
		g.drawString(name, nameX, baseline);
		g.setFont(TIME);
		g.setColor(goldTimes && !row.isPoints() ? GOLD : INK);
		g.drawString(row.getValue(), timeX + column - valueWidth, baseline);
		if (goldTimes && !row.getDiff().isEmpty())
		{
			g.setFont(DIFF);
			g.setColor(row.getDiff().startsWith("CM") ? GOLD : CYAN);
			g.drawString(row.getDiff(), diffX, baseline);
		}
		else if (!row.getDiff().isEmpty() && !"--".equals(row.getDiff()))
		{
			g.setFont(DIFF);
			g.setColor(tone);
			g.drawString(row.getDiff(), diffX, baseline);
		}
		else if ("--".equals(row.getDiff()))
		{
			g.setFont(DIFF);
			g.setColor(MUTED);
			g.drawString("--", 4 + cardWidth - 8 - diffWidth, baseline);
		}
	}

	private int cardHeight()
	{
		int height = 8;
		String section = "";
		for (int i = 0; i < rows.size(); i++)
		{
			String next = sectionOf(rows.get(i));
			if (next != null && !next.equals(section))
			{
				section = next;
				height += SECTION_H;
			}
			if (floorBreakBefore(i))
			{
				height += FLOOR_GAP;
			}
			height += ROW;
		}
		return height;
	}

	private String tipAt(int y)
	{
		for (int i = 0; i < rowTop.length && i < rows.size(); i++)
		{
			if (y >= rowTop[i] && y < rowTop[i] + ROW)
			{
				return tip(rows.get(i));
			}
		}
		return null;
	}

	private String tip(RaidReportFormatter.TimeRow row)
	{
		String name = shown(row.getLabel());
		String lead = "PPH".equals(row.getLabel()) ? "Points per hour. " : "";
		if (goldTimes)
		{
			return lead + name + "  " + row.getValue() + ", " + row.getDiff();
		}
		if (row.isOpen())
		{
			return lead + name + "  " + row.getValue() + ", still running";
		}
		String bench = againstTarget ? "target" : "average";
		if (row.getDelta() == null || row.getAverage() == null)
		{
			return lead + name + "  " + row.getValue() + ". No " + bench + " in this filter yet.";
		}
		String way = row.isPoints()
			? (row.getDelta() > 0 ? "above " + bench : row.getDelta() < 0 ? "below " + bench : "even with " + bench)
			: (row.getDelta() < 0 ? "ahead of " + bench : row.getDelta() > 0 ? "behind " + bench : "even with " + bench);
		String benchName = againstTarget ? "Target" : "Average";
		return lead + name + "  " + row.getValue() + ", " + row.getDiff() + " " + way + ". " + benchName + " " + row.getAverage() + ".";
	}

	/** Sidebar names. Mage hand and Olm phases stay short so the time column fits. */
	private static String shown(String label)
	{
		if (label.startsWith("mage hand p"))
		{
			return "Mage P" + label.substring("mage hand p".length());
		}
		if (label.startsWith("Olm phase "))
		{
			return "Olm P" + label.substring("Olm phase ".length());
		}
		return label;
	}

	private static String sectionOf(RaidReportFormatter.TimeRow row)
	{
		if (row.isOpen())
		{
			return null;
		}
		String label = row.getLabel();
		if ("Raid Completed".equals(label) || "Between rooms".equals(label) || "Total Points".equals(label) || "PPH".equals(label))
		{
			return "Finish";
		}
		if ("Olm".equals(label) || "Olm head".equals(label) || label.startsWith("Olm phase") || label.startsWith("mage hand"))
		{
			return "Olm";
		}
		return "Rooms";
	}

	/** Upper floor ends at Shamans, middle floor ends at Tightrope. */
	private static int floorOf(String label)
	{
		if ("Tekton".equals(label) || "Crabs".equals(label) || "Ice demon".equals(label) || "Shamans".equals(label))
		{
			return 1;
		}
		if ("Vanguards".equals(label) || "Thieving".equals(label) || "Vespula".equals(label) || "Tightrope".equals(label))
		{
			return 2;
		}
		if ("Guardians".equals(label) || "Vasa".equals(label) || "Mystics".equals(label) || "Muttadiles".equals(label))
		{
			return 3;
		}
		return 0;
	}

	private boolean floorBreakBefore(int index)
	{
		if (index <= 0)
		{
			return false;
		}
		int previous = floorOf(rows.get(index - 1).getLabel());
		int next = floorOf(rows.get(index).getLabel());
		return previous > 0 && next > previous;
	}

	private static boolean subtotal(RaidReportFormatter.TimeRow row)
	{
		String label = row.getLabel();
		return "Pre-Olm".equals(label) || "Olm".equals(label) || "Raid Completed".equals(label);
	}

	private static Color tone(RaidReportFormatter.TimeRow row)
	{
		if (row.isOpen() || row.getDelta() == null || Math.abs(row.getDelta()) < 1)
		{
			return MUTED;
		}
		int delta = row.getDelta();
		if (row.isPoints())
		{
			return delta > 0 ? GREEN : RED;
		}
		if (delta <= -20)
		{
			return CYAN;
		}
		if (delta < 0)
		{
			return GREEN;
		}
		if (delta < 10)
		{
			return ORANGE;
		}
		return RED;
	}

	@Override
	public Dimension getPreferredScrollableViewportSize()
	{
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return ROW;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return Math.max(ROW, visibleRect.height - ROW);
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

	@Override
	public Dimension getPreferredSize()
	{
		int width = getWidth() > 0 ? getWidth() : 200;
		return new Dimension(width, preferredHeight(width));
	}

	private static String clip(FontMetrics metrics, String text, int width)
	{
		if (metrics.stringWidth(text) <= width)
		{
			return text;
		}
		String ellipsis = "...";
		int keep = text.length();
		while (keep > 1 && metrics.stringWidth(text.substring(0, keep) + ellipsis) > width)
		{
			keep--;
		}
		return text.substring(0, keep) + ellipsis;
	}
}
