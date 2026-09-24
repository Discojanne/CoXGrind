package com.coxgrind.ui;

import com.coxgrind.report.RaidReportFormatter;
import com.coxgrind.track.TimeFormat;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;

/**
 * One line for the raid, centered on your average pace. The personal best is a second line above that, the time still to gain.
 * A point above the average is ahead of it: cyan at 20 seconds or more, green when ahead, orange under 10 seconds behind, red further behind. Past the PB line the point is gold.
 */
public class PaceGraph extends JPanel
{
	private static final Color PAPER = new Color(24, 24, 24);
	private static final Color INK = new Color(220, 220, 220);
	private static final Color AXIS = new Color(90, 90, 90);
	private static final Color TARGET = new Color(232, 196, 120);
	private static final Color GREEN = new Color(80, 220, 120);
	private static final Color RED = new Color(255, 90, 90);
	private static final Color ORANGE = new Color(255, 176, 46);
	private static final Color CYAN = new Color(80, 210, 255);
	private static final Stroke DASH = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] {4f, 3f}, 0f);
	private static final int HEIGHT = 174;

	private List<RaidReportFormatter.PacePoint> points = Collections.emptyList();
	private int[] pointX = new int[0];

	public PaceGraph()
	{
		setOpaque(true);
		setBackground(PAPER);
		setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		addMouseMotionListener(new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent event)
			{
				setToolTipText(tip(event.getX()));
			}
		});
	}

	public void setPoints(List<RaidReportFormatter.PacePoint> next)
	{
		if (next == null || next.isEmpty())
		{
			points = Collections.emptyList();
		}
		else
		{
			points = new ArrayList<>(next);
		}
		pointX = new int[points.size()];
		revalidate();
		repaint();
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(180, HEIGHT);
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, HEIGHT);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(getFont());
		g.setColor(PAPER);
		g.fillRect(0, 0, getWidth(), getHeight());
		if (points.isEmpty())
		{
			g.setColor(INK);
			g.drawString("Need another raid in this filter.", 8, 18);
			g.dispose();
			return;
		}

		int left = 28;
		int right = Math.max(left + 20, getWidth() - 8);
		int top = 8;
		int line = Math.max(13, g.getFontMetrics().getHeight());
		int bottom = getHeight() - line - 18;
		int averageAhead = averageAhead();
		int pbFromAverage = -averageAhead;
		int maxAbs = 24;
		for (int i = 0; i < points.size(); i++)
		{
			maxAbs = Math.max(maxAbs, Math.abs(points.get(i).getAheadSeconds() - averageAhead));
		}
		maxAbs = Math.max(maxAbs, Math.abs(pbFromAverage));
		int zeroY = top + (bottom - top) / 2;
		int half = Math.max(1, (bottom - top) / 2);
		int pbY = yAt(pbFromAverage, zeroY, half, maxAbs);
		g.setColor(AXIS);
		g.drawLine(left, zeroY, right, zeroY);
		boolean showPb = Math.abs(pbY - zeroY) >= 14;
		if (showPb)
		{
			Stroke solid = g.getStroke();
			g.setColor(TARGET);
			g.setStroke(DASH);
			g.drawLine(left, pbY, right, pbY);
			g.setStroke(solid);
			g.drawString("PB", 2, labelY(pbY, top, bottom));
			g.setColor(INK);
			g.drawString("Avg", 2, labelY(zeroY, top, bottom));
		}
		else
		{
			g.setColor(INK);
			g.drawString("PB", 2, labelY(zeroY, top, bottom));
		}

		int previousX = 0;
		int previousY = 0;
		for (int i = 0; i < points.size(); i++)
		{
			int x = points.size() == 1
				? (left + right) / 2
				: left + i * (right - left) / (points.size() - 1);
			int fromAverage = points.get(i).getAheadSeconds() - averageAhead;
			int y = yAt(fromAverage, zeroY, half, maxAbs);
			pointX[i] = x;
			if (i > 0)
			{
				g.setColor(colorFor(fromAverage, points.get(i).getAheadSeconds()));
				g.drawLine(previousX, previousY, x, y);
			}
			previousX = x;
			previousY = y;
		}
		for (int i = 0; i < points.size(); i++)
		{
			int fromAverage = points.get(i).getAheadSeconds() - averageAhead;
			int y = yAt(fromAverage, zeroY, half, maxAbs);
			g.setColor(colorFor(fromAverage, points.get(i).getAheadSeconds()));
			g.fillOval(pointX[i] - 3, y - 3, 6, 6);
		}

		RaidReportFormatter.PacePoint last = points.get(points.size() - 1);
		int fromAverage = last.getAheadSeconds() - averageAhead;
		g.setColor(averageColor(fromAverage));
		g.drawString(compared(last.getLabel(), fromAverage, "average"), 4, getHeight() - line - 2);
		g.setColor(pbColor(last.getAheadSeconds()));
		g.drawString(compared(last.getLabel(), last.getAheadSeconds(), "PB"), 4, getHeight() - 2);
		g.dispose();
	}

	private static int yAt(int secondsFromAverage, int zeroY, int half, int maxAbs)
	{
		return zeroY - (int) Math.round(secondsFromAverage * (double) half / maxAbs);
	}

	private static int labelY(int lineY, int top, int bottom)
	{
		return Math.max(top + 10, Math.min(bottom - 2, lineY + 4));
	}

	private String tip(int x)
	{
		if (points.isEmpty() || pointX.length != points.size())
		{
			return null;
		}
		int nearest = 0;
		int distance = Math.abs(x - pointX[0]);
		for (int i = 1; i < pointX.length; i++)
		{
			int next = Math.abs(x - pointX[i]);
			if (next < distance)
			{
				distance = next;
				nearest = i;
			}
		}
		RaidReportFormatter.PacePoint point = points.get(nearest);
		int fromAverage = point.getAheadSeconds() - averageAhead();
		return "<html>" + compared(point.getLabel(), fromAverage, "average")
			+ "<br>" + compared(point.getLabel(), point.getAheadSeconds(), "PB") + "</html>";
	}

	private int averageAhead()
	{
		if (!points.isEmpty() && "Start".equals(points.get(0).getLabel()))
		{
			return points.get(0).getAheadSeconds();
		}
		return 0;
	}

	private static String compared(String label, int seconds, String target)
	{
		String pace;
		if (seconds > 0)
		{
			pace = TimeFormat.formatSeconds(seconds) + " ahead of " + target;
		}
		else if (seconds < 0)
		{
			pace = TimeFormat.formatSeconds(-seconds) + " behind " + target;
		}
		else
		{
			pace = "even with " + target;
		}
		return label + "  " + pace;
	}

	private static Color colorFor(int secondsFromAverage, int aheadOfPb)
	{
		if (aheadOfPb > 0)
		{
			return TARGET;
		}
		return averageColor(secondsFromAverage);
	}

	private static Color pbColor(int aheadOfPb)
	{
		if (aheadOfPb > 0)
		{
			return TARGET;
		}
		return averageColor(aheadOfPb);
	}

	private static Color averageColor(int secondsFromAverage)
	{
		if (secondsFromAverage >= 20)
		{
			return CYAN;
		}
		if (secondsFromAverage > 0)
		{
			return GREEN;
		}
		if (secondsFromAverage == 0)
		{
			return INK;
		}
		if (secondsFromAverage > -10)
		{
			return ORANGE;
		}
		return RED;
	}
}
