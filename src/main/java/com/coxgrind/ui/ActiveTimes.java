package com.coxgrind.ui;

import com.coxgrind.report.RaidReportFormatter;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;

/**
 * Drawn split list for Active, Target, and Bests.
 * One line per room, grouped into Rooms, Olm, and Finish.
 * A quiet line splits the upper floor from the middle, and the middle from the lower.
 * Right-click copies the list to the clipboard as a Discord code block.
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
	private static final Font HEADER = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
	private static final Font HEADER_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 11);
	private static final Color FLOOR_LINE = new Color(96, 96, 96);
	private static final int ROW = 18;
	private static final int SECTION_H = 14;
	private static final int FLOOR_GAP = 4;

	private String notice = "No completed raids in this filter yet.";
	private List<RaidReportFormatter.TimeRow> rows = Collections.emptyList();
	private String badge = "";
	private Color badgeColor = MUTED;
	private String caption = "";
	private boolean goldTimes;
	private boolean againstTarget;
	private String note = "";
	private int[] rowTop = new int[0];
	private String[] choices = new String[0];
	private int choice;
	private Runnable onChoice;
	private int[] choiceX = new int[0];
	private int[] choiceW = new int[0];
	private int count = 10;
	private int countMin = 1;
	private int countMax = 100;
	private int countChoice = -1;
	private Runnable onCount;
	private int stepMinusX = -1;
	private int stepMinusW;
	private int stepPlusX = -1;
	private int stepPlusW;

	public ActiveTimes()
	{
		setOpaque(true);
		setBackground(PAPER);
		addMouseMotionListener(new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent event)
			{
				int step = stepAt(event.getX(), event.getY());
				if (step != 0)
				{
					setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					setToolTipText(stepTip(step));
					return;
				}
				int index = choiceAt(event.getX(), event.getY());
				setCursor(index >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
				setToolTipText(index >= 0 ? choiceTip(index) : tipAt(event.getY()));
			}
		});
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent event)
			{
				showCopyMenu(event);
			}

			@Override
			public void mouseReleased(MouseEvent event)
			{
				showCopyMenu(event);
			}

			@Override
			public void mouseClicked(MouseEvent event)
			{
				if (!SwingUtilities.isLeftMouseButton(event))
				{
					return;
				}
				int step = stepAt(event.getX(), event.getY());
				if (step == -1 || step == 1)
				{
					nudgeCount(step, event.isShiftDown());
					return;
				}
				int index = choiceAt(event.getX(), event.getY());
				if (index < 0 || index == choice || onChoice == null)
				{
					return;
				}
				choice = index;
				onChoice.run();
			}
		});
	}

	/**
	 * Discord code block of the rows on screen. Comparison diffs stay off the paste.
	 * A kill count in the right column, used by Best splits, is kept.
	 */
	static String discordSplits(List<RaidReportFormatter.TimeRow> rows, String title)
	{
		if (rows == null || rows.isEmpty())
		{
			return "";
		}
		String[] names = new String[rows.size()];
		String[] values = new String[rows.size()];
		String[] counts = new String[rows.size()];
		int nameWidth = 0;
		int valueWidth = 0;
		for (int i = 0; i < rows.size(); i++)
		{
			RaidReportFormatter.TimeRow row = rows.get(i);
			names[i] = shown(row.getLabel());
			values[i] = discordValue(row);
			counts[i] = killCount(row.getDiff());
			nameWidth = Math.max(nameWidth, names[i].length());
			valueWidth = Math.max(valueWidth, values[i].length());
		}
		StringBuilder out = new StringBuilder();
		out.append("```\n");
		if (title != null && !title.isEmpty())
		{
			out.append(title).append('\n');
		}
		String section = "";
		boolean grouped = false;
		for (int i = 0; i < rows.size(); i++)
		{
			String next = sectionOf(rows.get(i));
			if (next != null && !next.equals(section))
			{
				if (grouped)
				{
					out.append('\n');
				}
				section = next;
				grouped = true;
			}
			out.append(padRight(names[i], nameWidth));
			out.append("  ");
			out.append(padLeft(values[i], valueWidth));
			if (!counts[i].isEmpty())
			{
				out.append("  ").append(counts[i]);
			}
			out.append('\n');
		}
		out.append("```");
		return out.toString();
	}

	private void showCopyMenu(MouseEvent event)
	{
		if (!event.isPopupTrigger() || rows.isEmpty())
		{
			return;
		}
		JPopupMenu menu = new JPopupMenu();
		JMenuItem copy = new JMenuItem("Copy splits");
		copy.addActionListener(action -> copySplits());
		menu.add(copy);
		menu.show(this, event.getX(), event.getY());
	}

	private void copySplits()
	{
		String text = discordSplits(rows, copyTitle());
		if (text.isEmpty())
		{
			return;
		}
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	private String copyTitle()
	{
		if (showsLastAverage())
		{
			return "Last " + count;
		}
		if (!badge.isEmpty())
		{
			return badge;
		}
		if (choice >= 0 && choice < choices.length)
		{
			return choices[choice];
		}
		return "";
	}

	private static String discordValue(RaidReportFormatter.TimeRow row)
	{
		String value = row.getValue();
		if (row.isPoints() || value.indexOf(':') < 0)
		{
			return value;
		}
		int colon = value.indexOf(':');
		int start = 0;
		while (start < colon - 1 && value.charAt(start) == '0')
		{
			start++;
		}
		return value.substring(start);
	}

	private static String killCount(String diff)
	{
		if (diff != null && (diff.startsWith("CM ") || diff.startsWith("KC ")))
		{
			return diff;
		}
		return "";
	}

	private static String padRight(String text, int width)
	{
		StringBuilder out = new StringBuilder(text);
		while (out.length() < width)
		{
			out.append(' ');
		}
		return out.toString();
	}

	private static String padLeft(String text, int width)
	{
		StringBuilder out = new StringBuilder();
		while (out.length() + text.length() < width)
		{
			out.append(' ');
		}
		out.append(text);
		return out.toString();
	}

	/** Small titles in the header. Used by the Bests list to switch what is shown. */
	public void setChoices(String[] labels, int selected, Runnable listener)
	{
		choices = labels == null ? new String[0] : labels;
		choice = selected;
		onChoice = listener;
		repaint();
	}

	public int getChoice()
	{
		return choice;
	}

	/** True when the Last N title is selected. */
	public boolean showsLastAverage()
	{
		return countChoice >= 0 && choice == countChoice;
	}

	/**
	 * The title at {@code choiceIndex} shows how many raids are averaged, as {@code Last 10}.
	 * Minus and plus sit on either side of that title. Shift-click moves by 10.
	 * {@code listener} runs after the number changes. The number stays inside {@code min} and {@code max}.
	 */
	public void setCount(int choiceIndex, int value, int min, int max, Runnable listener)
	{
		countChoice = choiceIndex;
		countMin = Math.min(min, max);
		countMax = Math.max(min, max);
		count = Math.max(countMin, Math.min(countMax, value));
		onCount = listener;
		repaint();
	}

	public int getCount()
	{
		return count;
	}

	private void nudgeCount(int direction, boolean shift)
	{
		int next = count + direction * (shift ? 10 : 1);
		if (next < countMin)
		{
			next = countMin;
		}
		if (next > countMax)
		{
			next = countMax;
		}
		if (next == count)
		{
			return;
		}
		count = next;
		if (onCount != null)
		{
			onCount.run();
		}
		else
		{
			repaint();
		}
	}

	/**
	 * Draws this comparison. Returns true when the list height changed and the parent should lay out again.
	 * A clock tick that only changes a time repaints in place.
	 */
	public boolean show(RaidReportFormatter.PaceComparison pace)
	{
		boolean layout = !sameShape(pace);
		if (pace == null || pace.getRows().isEmpty())
		{
			rows = Collections.emptyList();
			notice = pace == null ? "" : pace.getText().trim();
			badge = "";
			caption = "";
			goldTimes = false;
			againstTarget = false;
			note = "";
		}
		else
		{
			rows = new ArrayList<>(pace.getRows());
			notice = null;
			caption = pace.getCaption();
			goldTimes = pace.isGoldTimes();
			againstTarget = pace.isAgainstTarget();
			note = pace.getNote();
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
		if (layout)
		{
			revalidate();
		}
		repaint();
		return layout;
	}

	/** Same notice-or-rows shape, so the preferred height is unchanged. */
	private boolean sameShape(RaidReportFormatter.PaceComparison pace)
	{
		boolean nextNotice = pace == null || pace.getRows().isEmpty();
		if (nextNotice != (notice != null))
		{
			return false;
		}
		if (nextNotice)
		{
			return true;
		}
		String nextNote = pace.getNote() == null ? "" : pace.getNote();
		if (nextNote.isEmpty() != note.isEmpty())
		{
			return false;
		}
		List<RaidReportFormatter.TimeRow> nextRows = pace.getRows();
		if (nextRows.size() != rows.size())
		{
			return false;
		}
		for (int i = 0; i < nextRows.size(); i++)
		{
			if (!nextRows.get(i).getLabel().equals(rows.get(i).getLabel()))
			{
				return false;
			}
		}
		return true;
	}

	public int preferredHeight(int width)
	{
		if (notice != null)
		{
			return choices.length == 0 ? 22 : 36;
		}
		return 18 + cardHeight() + (note.isEmpty() ? 0 : 28);
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
			paintChoices(g);
			g.setFont(NAME);
			g.setColor(INK);
			g.drawString(notice, 8, choices.length == 0 ? 16 : 30);
			rowTop = new int[0];
			g.dispose();
			return;
		}

		if (!showsLastAverage())
		{
			g.setFont(BADGE);
			g.setColor(badgeColor);
			if (!badge.isEmpty())
			{
				g.drawString(badge, 8, 12);
			}
		}
		if (choices.length == 0)
		{
			g.setFont(HEADER);
			g.setColor(MUTED);
			int captionX = getWidth() - 8 - g.getFontMetrics().stringWidth(caption);
			if (!caption.isEmpty())
			{
				g.drawString(caption, Math.max(8 + g.getFontMetrics(BADGE).stringWidth(badge) + 8, captionX), 12);
			}
		}
		else
		{
			paintChoices(g);
		}

		int cardTop = 16 + (note.isEmpty() ? 0 : 26);
		if (!note.isEmpty())
		{
			g.setFont(HEADER);
			g.setColor(MUTED);
			g.drawString(clip(g.getFontMetrics(), note, Math.max(20, getWidth() - 16)), 8, 28);
		}
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
				g.setColor(FLOOR_LINE);
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

	private void paintChoices(Graphics2D g)
	{
		stepMinusW = 0;
		stepPlusW = 0;
		if (choices.length == 0)
		{
			choiceX = new int[0];
			choiceW = new int[0];
			return;
		}
		g.setFont(HEADER);
		FontMetrics plain = g.getFontMetrics();
		g.setFont(HEADER_BOLD);
		FontMetrics bold = g.getFontMetrics();
		choiceX = new int[choices.length];
		choiceW = new int[choices.length];
		int x = getWidth() - 8;
		for (int i = choices.length - 1; i >= 0; i--)
		{
			if (i == countChoice)
			{
				x = paintCountChoice(g, i, x, i == choice ? bold : plain);
			}
			else
			{
				int width = i == choice ? bold.stringWidth(choices[i]) : plain.stringWidth(choices[i]);
				x -= width;
				choiceX[i] = x;
				choiceW[i] = width;
				g.setFont(i == choice ? HEADER_BOLD : HEADER);
				g.setColor(i == choice ? INK : MUTED);
				g.drawString(choices[i], x, 12);
			}
			x -= 10;
		}
	}

	/** Draws {@code Last 10} with minus on the left and plus on the right. Returns the left edge. */
	private int paintCountChoice(Graphics2D g, int index, int right, FontMetrics metrics)
	{
		String word = choices[index] == null ? "" : choices[index];
		String title = word + " " + count;
		int plusW = Math.max(12, metrics.stringWidth("+") + 6);
		int minusW = Math.max(12, metrics.stringWidth("-") + 6);
		int titleW = metrics.stringWidth(title);
		int gap = 2;
		int x = right - plusW;
		stepPlusX = x;
		stepPlusW = plusW;
		g.setFont(metrics.getFont());
		g.setColor(count < countMax ? (index == choice ? INK : MUTED) : HAIR);
		g.drawString("+", x + 2, 12);
		x -= gap + titleW;
		choiceX[index] = x;
		choiceW[index] = titleW;
		g.setColor(index == choice ? INK : MUTED);
		g.drawString(title, x, 12);
		x -= gap + minusW;
		stepMinusX = x;
		stepMinusW = minusW;
		g.setColor(count > countMin ? (index == choice ? INK : MUTED) : HAIR);
		g.drawString("-", x + 2, 12);
		return x;
	}

	/** -1 is fewer raids, 1 is more, 0 is anywhere else. */
	private int stepAt(int x, int y)
	{
		if (countChoice < 0 || y > 16)
		{
			return 0;
		}
		if (stepMinusW > 0 && x >= stepMinusX && x < stepMinusX + stepMinusW)
		{
			return -1;
		}
		if (stepPlusW > 0 && x >= stepPlusX && x < stepPlusX + stepPlusW)
		{
			return 1;
		}
		return 0;
	}

	private String stepTip(int step)
	{
		if (step < 0)
		{
			return "Fewer raids. Shift changes by 10.";
		}
		return "More raids. Shift changes by 10.";
	}

	private int choiceAt(int x, int y)
	{
		if (y > 16 || choiceX.length != choices.length)
		{
			return -1;
		}
		for (int i = 0; i < choiceX.length; i++)
		{
			if (x >= choiceX[i] && x <= choiceX[i] + choiceW[i])
			{
				return i;
			}
		}
		return -1;
	}

	private String choiceTip(int index)
	{
		if (index < 0 || index >= choices.length)
		{
			return null;
		}
		if ("Raid time".equals(choices[index]))
		{
			return "Fastest raid and its splits";
		}
		if ("PPH".equals(choices[index]))
		{
			return "Highest points per hour";
		}
		if (index == countChoice)
		{
			return "Average of your last " + count + " raids vs your targets";
		}
		if ("This".equals(choices[index]))
		{
			return "This raid vs your targets";
		}
		return "Fastest split in this filter";
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
