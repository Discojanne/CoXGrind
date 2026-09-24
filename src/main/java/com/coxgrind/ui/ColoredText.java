package com.coxgrind.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Paints Coxparser-style color codes in a text pane.
 */
public final class ColoredText
{
	private static final Color PAPER = new Color(24, 24, 24);
	private static final Color INK = new Color(220, 220, 220);
	private static final Color GREEN = new Color(80, 220, 120);
	private static final Color RED = new Color(255, 90, 90);
	private static final Color ORANGE = new Color(255, 176, 46);
	private static final Color CYAN = new Color(80, 210, 255);
	private static final Color GOLD = new Color(232, 196, 120);

	private ColoredText()
	{
	}

	public static JTextPane pane(String text, int size)
	{
		JTextPane pane = new JTextPane();
		apply(pane, text, size);
		return pane;
	}

	public static void apply(JTextPane pane, String text, int size)
	{
		pane.setEditable(false);
		pane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, size));
		pane.setBackground(PAPER);
		pane.setForeground(INK);
		pane.setCaretColor(PAPER);
		StyledDocument document = pane.getStyledDocument();
		try
		{
			document.remove(0, document.getLength());
		}
		catch (BadLocationException ex)
		{
			document = new DefaultStyledDocument();
			pane.setStyledDocument(document);
		}
		write(document, text == null ? "" : text, size);
		pane.revalidate();
		pane.repaint();
	}

	public static void show(Component parent, String title, String text)
	{
		JTextPane area = pane(text, 13);
		JScrollPane scroll = new JScrollPane(area);
		JDialog dialog = new JDialog((Frame) null, title, false);
		dialog.add(scroll);
		dialog.setSize(1100, 720);
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}

	private static void write(StyledDocument document, String text, int size)
	{
		Color color = INK;
		int fontSize = size;
		int start = 0;
		for (int i = 0; i < text.length(); i++)
		{
			if (text.charAt(i) != '\u001B' || i + 1 >= text.length() || text.charAt(i + 1) != '[')
			{
				continue;
			}
			int end = text.indexOf('m', i + 2);
			if (end < 0)
			{
				break;
			}
			insert(document, text.substring(start, i), color, fontSize);
			String code = text.substring(i + 2, end);
			if ("0".equals(code))
			{
				color = INK;
			}
			else if (code.startsWith("#"))
			{
				fontSize = sizeCode(code.substring(1), fontSize);
			}
			else if ("32".equals(code))
			{
				color = GREEN;
			}
			else if ("31".equals(code))
			{
				color = RED;
			}
			else if ("33".equals(code))
			{
				color = ORANGE;
			}
			else if ("36".equals(code))
			{
				color = CYAN;
			}
			else if ("93".equals(code))
			{
				color = GOLD;
			}
			i = end;
			start = end + 1;
		}
		insert(document, text.substring(start), color, fontSize);
	}

	private static int sizeCode(String code, int current)
	{
		try
		{
			int parsed = Integer.parseInt(code);
			if (parsed >= 8 && parsed <= 24)
			{
				return parsed;
			}
		}
		catch (NumberFormatException ex)
		{
			return current;
		}
		return current;
	}

	private static void insert(StyledDocument document, String text, Color color, int fontSize)
	{
		if (text.isEmpty())
		{
			return;
		}
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setForeground(style, color);
		StyleConstants.setFontFamily(style, Font.MONOSPACED);
		StyleConstants.setFontSize(style, fontSize);
		try
		{
			document.insertString(document.getLength(), text, style);
		}
		catch (BadLocationException ex)
		{
			// The document length is the insert point, so this does not happen for a live pane.
		}
	}
}
