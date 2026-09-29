package com.coxgrind.ui;

import com.coxgrind.CoxGrindConfig;
import com.coxgrind.model.TargetSheet;
import com.coxgrind.report.TargetSettings;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/**
 * RuneLite's settings page does not show a calculated total or a sheet reset.
 * This writes the gray Target total and adds Reset to default when the Cox Grind
 * config page is built. Settings rows stay plain text. Item sprites belong on the Purples tab.
 */
public final class ConfigItemIcons implements AWTEventListener
{
	/** Plugin lists and world lists are wider than a settings page. Do not walk them. */
	private static final int MAX_PAGE_CHILDREN = 64;
	private static final String PAGE = "coxgrind-page";
	private static final String TOTAL = "coxgrind-total";
	private static final String RESET = "coxgrind-reset";
	private static final String NO_RIGHT = "coxgrind-noright";
	private static final Color TOTAL_INK = new Color(150, 150, 150);
	/** Names that mark a Cox Grind settings row. They are not drawn as sprites. */
	private static final Set<String> SETTING_NAMES = settingNames();

	private final CoxGrindConfig config;
	private final Consumer<TargetSheet> resetSheet;
	/** True after the Cox Grind settings page has been marked. Unrelated labels skip the parent walk until then. */
	private boolean settingsOpen;

	public ConfigItemIcons(CoxGrindConfig config, Consumer<TargetSheet> resetSheet)
	{
		this.config = config;
		this.resetSheet = resetSheet;
	}

	/** Rewrites the gray total while the settings page stays open. */
	public void refreshTotals()
	{
		SwingUtilities.invokeLater(() -> fillOpenSheets(false, null));
	}

	/** Rewrites every box on this sheet after a reset, including the total. */
	public void showSheet(TargetSheet sheet)
	{
		fillOpenSheets(true, sheet);
	}

	public void start()
	{
		Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.CONTAINER_EVENT_MASK);
		SwingUtilities.invokeLater(this::decorateOpenSettings);
	}

	public void stop()
	{
		Toolkit.getDefaultToolkit().removeAWTEventListener(this);
	}

	@Override
	public void eventDispatched(AWTEvent event)
	{
		if (event.getID() != ContainerEvent.COMPONENT_ADDED || !(event instanceof ContainerEvent))
		{
			return;
		}
		Component child = ((ContainerEvent) event).getChild();
		if (child == null || isFat(child))
		{
			return;
		}
		if (child instanceof JLabel)
		{
			if (!interestingLabel((JLabel) child) && !(settingsOpen && marked(child)))
			{
				return;
			}
		}
		else if (!shallowCue(child) && !(settingsOpen && marked(child)))
		{
			return;
		}
		noticeAdded(child);
	}

	/**
	 * Decorate a component that just joined the Cox Grind settings page.
	 * Anything else, including a plugin list row named Cox Grind, is ignored.
	 */
	void noticeAdded(Component child)
	{
		if (child == null)
		{
			return;
		}
		if (marked(child))
		{
			decorateTree(child);
			return;
		}
		if (!touchesSettings(child))
		{
			return;
		}
		Container page = settingsPage(child);
		if (page == null || !pageHasSettings(page) || !(page instanceof JComponent))
		{
			return;
		}
		JComponent box = (JComponent) page;
		if (Boolean.TRUE.equals(box.getClientProperty(PAGE)))
		{
			decorateTree(child);
			return;
		}
		box.putClientProperty(PAGE, Boolean.TRUE);
		settingsOpen = true;
		decorateTree(page);
	}

	/** True when this label sits on the Cox Grind settings page. The title is a nearby label, not a parent. */
	static boolean onCoxGrindSettings(Component component)
	{
		Container cursor = component.getParent();
		while (cursor != null)
		{
			if (hasTitle(cursor))
			{
				return true;
			}
			cursor = cursor.getParent();
		}
		return false;
	}

	private void decorateOpenSettings()
	{
		for (Window window : Window.getWindows())
		{
			if (window.getComponentCount() > MAX_PAGE_CHILDREN)
			{
				continue;
			}
			JLabel title = findTitle(window, new int[] {0});
			if (title != null)
			{
				noticeAdded(title);
			}
		}
	}

	/** True when this component sits under a Cox Grind settings page we have already seen. */
	private static boolean marked(Component component)
	{
		Component cursor = component;
		while (cursor != null)
		{
			if (cursor instanceof JComponent && Boolean.TRUE.equals(((JComponent) cursor).getClientProperty(PAGE)))
			{
				return true;
			}
			cursor = cursor.getParent();
		}
		return false;
	}

	/** A settings title or a settings row. A long list of unrelated rows is not. */
	private static boolean touchesSettings(Component child)
	{
		if (isTitle(child) || knownLabel(child))
		{
			return true;
		}
		return shallowCue(child);
	}

	/**
	 * Two levels of children, and only a few of them.
	 * Plugin lists and world lists are not walked.
	 */
	private static boolean shallowCue(Component component)
	{
		if (!(component instanceof Container) || isFat(component))
		{
			return false;
		}
		Component[] children = ((Container) component).getComponents();
		int seen = 0;
		for (int i = 0; i < children.length; i++)
		{
			if (seen++ > 24)
			{
				return false;
			}
			Component child = children[i];
			if (isTitle(child) || knownLabel(child))
			{
				return true;
			}
			if (!(child instanceof Container) || isFat(child))
			{
				continue;
			}
			Component[] grand = ((Container) child).getComponents();
			for (int j = 0; j < grand.length; j++)
			{
				if (seen++ > 24)
				{
					return false;
				}
				if (isTitle(grand[j]) || knownLabel(grand[j]))
				{
					return true;
				}
			}
		}
		return false;
	}

	/** A label this plugin cares about. Plain text does not go through a pattern. */
	private static boolean interestingLabel(JLabel label)
	{
		String text = label.getText();
		if (text == null || text.isEmpty())
		{
			return false;
		}
		if (text.indexOf('<') >= 0)
		{
			return isTitle(label) || knownLabel(label);
		}
		return "Cox Grind".equals(text)
			|| SETTING_NAMES.contains(text)
			|| "Tekton".equals(text)
			|| "Target total".equals(text)
			|| TargetSheet.fromTitle(text) != null;
	}

	/**
	 * Outermost small parent that shows the Cox Grind title.
	 * Stops at a wide list so a plugin row named Cox Grind does not claim the whole list.
	 */
	private static Container settingsPage(Component start)
	{
		Container cursor = start.getParent();
		Container found = null;
		while (cursor != null && !isFat(cursor))
		{
			if (hasTitle(cursor))
			{
				found = cursor;
			}
			cursor = cursor.getParent();
		}
		return found;
	}

	private static boolean pageHasSettings(Component page)
	{
		return hasSettingsName(page, new int[] {0});
	}

	private static boolean hasSettingsName(Component component, int[] seen)
	{
		if (seen[0]++ > 200 || isFat(component))
		{
			return false;
		}
		if (knownLabel(component))
		{
			return true;
		}
		if (!(component instanceof Container))
		{
			return false;
		}
		Component[] children = ((Container) component).getComponents();
		for (int i = 0; i < children.length; i++)
		{
			if (hasSettingsName(children[i], seen))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean knownLabel(Component component)
	{
		if (!(component instanceof JLabel))
		{
			return false;
		}
		String text = plain(((JLabel) component).getText());
		return SETTING_NAMES.contains(text) || "Tekton".equals(text) || "Target total".equals(text) || TargetSheet.fromTitle(text) != null;
	}

	private static boolean isFat(Component component)
	{
		return component instanceof Container && ((Container) component).getComponentCount() > MAX_PAGE_CHILDREN;
	}

	private static JLabel findTitle(Component component, int[] seen)
	{
		if (seen[0]++ > 200 || isFat(component))
		{
			return null;
		}
		if (isTitle(component))
		{
			return (JLabel) component;
		}
		if (!(component instanceof Container))
		{
			return null;
		}
		Component[] children = ((Container) component).getComponents();
		for (int i = 0; i < children.length; i++)
		{
			JLabel found = findTitle(children[i], seen);
			if (found != null)
			{
				return found;
			}
		}
		return null;
	}

	void decorateTree(Component component)
	{
		if (isFat(component) && !isPage(component))
		{
			return;
		}
		if (component instanceof JLabel)
		{
			disarm((JLabel) component);
		}
		if (component instanceof JTextComponent)
		{
			fillField((JTextComponent) component, false, null);
		}
		installReset(component);
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				decorateTree(child);
			}
		}
	}

	private void fillOpenSheets(boolean allFields, TargetSheet only)
	{
		for (Window window : Window.getWindows())
		{
			fillMarked(window, allFields, only);
		}
	}

	/** Fills the Cox Grind settings page under this root. A long list is not entered. */
	void fillMarked(Component root, boolean allFields, TargetSheet only)
	{
		JComponent page = findSettingsPage(root);
		if (page != null)
		{
			fillTree(page, allFields, only);
		}
	}

	/** The marked Cox Grind settings page, or null when it sits inside a long list. */
	JComponent findSettingsPage(Component component)
	{
		return findSettingsPage(component, new int[] {0});
	}

	private static JComponent findSettingsPage(Component component, int[] seen)
	{
		if (component == null || seen[0]++ > 400)
		{
			return null;
		}
		if (isPage(component))
		{
			return (JComponent) component;
		}
		if (isFat(component) || !(component instanceof Container))
		{
			return null;
		}
		Component[] children = ((Container) component).getComponents();
		for (int i = 0; i < children.length; i++)
		{
			JComponent found = findSettingsPage(children[i], seen);
			if (found != null)
			{
				return found;
			}
		}
		return null;
	}

	private static boolean isPage(Component component)
	{
		return component instanceof JComponent && Boolean.TRUE.equals(((JComponent) component).getClientProperty(PAGE));
	}

	private void fillTree(Component component, boolean allFields, TargetSheet only)
	{
		if (isFat(component) && !isPage(component))
		{
			return;
		}
		if (component instanceof JTextComponent)
		{
			fillField((JTextComponent) component, allFields, only);
		}
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				fillTree(child, allFields, only);
			}
		}
	}

	private void fillField(JTextComponent field, boolean allFields, TargetSheet only)
	{
		if (!onCoxGrindSettings(field))
		{
			return;
		}
		JLabel label = rowLabel(field);
		TargetSheet sheet = sheetOf(field);
		if (label == null || sheet == null)
		{
			return;
		}
		if (only != null && only != sheet)
		{
			return;
		}
		String name = plain(label.getText());
		if ("Target total".equals(name))
		{
			applyTotal(field, sheet);
			return;
		}
		if (!allFields || field.isFocusOwner())
		{
			return;
		}
		field.setText(TargetSettings.shown(config, sheet, name));
	}

	private void applyTotal(JTextComponent field, TargetSheet sheet)
	{
		field.setEnabled(true);
		field.setText(TargetSettings.shown(config, sheet, "Target total"));
		field.setEditable(false);
		field.setFocusable(false);
		field.setForeground(TOTAL_INK);
		field.setDisabledTextColor(TOTAL_INK);
		for (FocusListener listener : field.getFocusListeners())
		{
			field.removeFocusListener(listener);
		}
		field.putClientProperty(TOTAL, Boolean.TRUE);
	}

	private void installReset(Component component)
	{
		JComponent body = sheetBody(component);
		if (body == null || !onCoxGrindSettings(body) || Boolean.TRUE.equals(body.getClientProperty(RESET)))
		{
			return;
		}
		TargetSheet sheet = sheetOf(body);
		if (sheet == null || !hasLabel(body, "Tekton"))
		{
			return;
		}
		body.putClientProperty(RESET, Boolean.TRUE);
		JButton button = new JButton("Reset to default");
		button.setToolTipText("Restore every box on this sheet to its default.");
		button.addActionListener(event -> confirmReset(button, sheet));
		body.add(button, 0);
		body.revalidate();
		body.repaint();
	}

	private void confirmReset(Component parent, TargetSheet sheet)
	{
		int choice = JOptionPane.showOptionDialog(
			parent,
			"Reset the " + sheet.getLabel() + " to their defaults?",
			"Reset targets",
			JOptionPane.DEFAULT_OPTION,
			JOptionPane.PLAIN_MESSAGE,
			null,
			new Object[] {"Reset", "Cancel"},
			"Cancel");
		if (choice != 0 || resetSheet == null)
		{
			return;
		}
		resetSheet.accept(sheet);
	}

	private void disarm(JLabel label)
	{
		if (Boolean.TRUE.equals(label.getClientProperty(NO_RIGHT)) || sheetOf(label) == null || !onCoxGrindSettings(label))
		{
			return;
		}
		for (MouseListener listener : label.getMouseListeners())
		{
			label.removeMouseListener(listener);
		}
		label.putClientProperty(NO_RIGHT, Boolean.TRUE);
	}

	/** The target sheet that contains this settings row. The section title itself is not inside the sheet. */
	static TargetSheet sheetOf(Component component)
	{
		JComponent body = sheetBody(component);
		if (body == null)
		{
			return null;
		}
		Container parent = body.getParent();
		if (parent == null)
		{
			return null;
		}
		return onlySiblingTitle(parent, body);
	}

	/** Contents panel of the target section that holds this component. */
	private static JComponent sheetBody(Component component)
	{
		Component cursor = component;
		while (cursor != null)
		{
			Container parent = cursor.getParent();
			if (parent == null)
			{
				return null;
			}
			if (cursor instanceof JComponent && onlySiblingTitle(parent, cursor) != null && contains(cursor, component))
			{
				return (JComponent) cursor;
			}
			cursor = parent;
		}
		return null;
	}

	/** Section title on a sibling header. Null when several sections share this parent. */
	private static TargetSheet onlySiblingTitle(Container parent, Component cursor)
	{
		TargetSheet found = null;
		for (Component child : parent.getComponents())
		{
			if (child == cursor)
			{
				continue;
			}
			TargetSheet title = titleIn(child);
			if (title == null)
			{
				continue;
			}
			if (found != null && found != title)
			{
				return null;
			}
			if (hasLabel(child, "Tekton"))
			{
				return null;
			}
			found = title;
		}
		return found;
	}

	private static TargetSheet titleIn(Component component)
	{
		if (component instanceof JLabel)
		{
			TargetSheet sheet = TargetSheet.fromTitle(((JLabel) component).getText());
			if (sheet != null)
			{
				return sheet;
			}
		}
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				TargetSheet sheet = titleIn(child);
				if (sheet != null)
				{
					return sheet;
				}
			}
		}
		return null;
	}

	private static boolean hasLabel(Component component, String text)
	{
		if (component instanceof JLabel && text.equals(plain(((JLabel) component).getText())))
		{
			return true;
		}
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				if (hasLabel(child, text))
				{
					return true;
				}
			}
		}
		return false;
	}

	private static boolean contains(Component parent, Component child)
	{
		if (parent == child)
		{
			return true;
		}
		if (!(parent instanceof Container))
		{
			return false;
		}
		for (Component next : ((Container) parent).getComponents())
		{
			if (contains(next, child))
			{
				return true;
			}
		}
		return false;
	}

	/** The name label on the same settings row as this field. */
	private static JLabel rowLabel(Component component)
	{
		Component cursor = component;
		while (cursor != null)
		{
			Container parent = cursor.getParent();
			if (parent == null)
			{
				return null;
			}
			JLabel label = null;
			int labels = 0;
			for (Component child : parent.getComponents())
			{
				if (child instanceof JLabel)
				{
					labels++;
					label = (JLabel) child;
				}
			}
			if (labels == 1)
			{
				return label;
			}
			cursor = parent;
		}
		return null;
	}

	private static String plain(String text)
	{
		if (text == null || text.isEmpty())
		{
			return "";
		}
		if (text.indexOf('<') < 0)
		{
			return text.trim();
		}
		StringBuilder out = new StringBuilder(text.length());
		boolean tag = false;
		for (int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);
			if (c == '<')
			{
				tag = true;
			}
			else if (c == '>')
			{
				tag = false;
			}
			else if (!tag)
			{
				out.append(c);
			}
		}
		return out.toString().trim();
	}

	private static boolean hasTitle(Container container)
	{
		for (Component child : container.getComponents())
		{
			if (isTitle(child))
			{
				return true;
			}
			if (child instanceof Container)
			{
				for (Component grand : ((Container) child).getComponents())
				{
					if (isTitle(grand))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean isTitle(Component component)
	{
		return component instanceof JLabel && "Cox Grind".equals(((JLabel) component).getText());
	}

	private static Set<String> settingNames()
	{
		return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			"Using Twisted bow",
			"Slayer helm",
			"Lockpick",
			"Axe",
			"Salve",
			"Pre-veng",
			"Vesp pot skip",
			"Crab tank",
			"Killing rope",
			"Vesp milk",
			"Ice milk",
			"Over-thieve"
		)));
	}
}
