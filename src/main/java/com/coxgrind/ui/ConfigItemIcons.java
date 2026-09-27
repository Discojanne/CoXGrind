package com.coxgrind.ui;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * RuneLite's settings rows are plain labels. This attaches the matching item sprite
 * when the CoXGrind config page is built.
 */
public final class ConfigItemIcons implements AWTEventListener
{
	private static final int SIZE = 22;
	private static final String MARK = "coxgrind-icon";
	private static final Map<String, Integer> ITEMS = items();

	private final ItemManager itemManager;

	public ConfigItemIcons(ItemManager itemManager)
	{
		this.itemManager = itemManager;
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
		if (!(event instanceof ContainerEvent) || event.getID() != ContainerEvent.COMPONENT_ADDED)
		{
			return;
		}
		Component child = ((ContainerEvent) event).getChild();
		if (child != null)
		{
			// The label is created before its row is attached to the settings page.
			decorateTree(child);
		}
	}

	static int itemId(String name)
	{
		Integer id = ITEMS.get(name);
		return id == null ? -1 : id;
	}

	/** True when this label sits on the CoXGrind settings page. The title is a nearby label, not a parent. */
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
			decorateTree(window);
		}
	}

	private void decorateTree(Component component)
	{
		if (component instanceof JLabel)
		{
			decorate((JLabel) component);
		}
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				decorateTree(child);
			}
		}
	}

	private void decorate(JLabel label)
	{
		if (label.getClientProperty(MARK) != null || itemManager == null)
		{
			return;
		}
		int id = itemId(label.getText());
		if (id < 0 || !onCoxGrindSettings(label))
		{
			return;
		}
		label.putClientProperty(MARK, Boolean.TRUE);
		try
		{
			AsyncBufferedImage image = itemManager.getImage(id);
			image.onLoaded(new Runnable()
			{
				@Override
				public void run()
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							apply(label, image);
						}
					});
				}
			});
			if (image.getWidth() > 1)
			{
				apply(label, image);
			}
		}
		catch (RuntimeException ignored)
		{
			label.putClientProperty(MARK, null);
		}
	}

	private static void apply(JLabel label, BufferedImage image)
	{
		label.setIcon(new ImageIcon(scale(image, SIZE)));
		label.setIconTextGap(6);
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
		return component instanceof JLabel && "CoXGrind".equals(((JLabel) component).getText());
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

	private static Map<String, Integer> items()
	{
		Map<String, Integer> map = new HashMap<>();
		map.put("Using twisted bow", ItemID.TWISTED_BOW);
		map.put("Slayer helm", ItemID.SLAYER_HELM_I);
		map.put("Lockpick", ItemID.LOCKPICK);
		map.put("Axe", ItemID.DRAGON_AXE);
		map.put("Salve", ItemID.NZONE_SALVE_AMULET_E);
		map.put("Pre-veng", ItemID.ASTRALRUNE);
		map.put("Vesp pot skip", ItemID._4DOSESTAMINA);
		map.put("Crab tank", ItemID.RED_CRAB);
		map.put("Killing rope", ItemID.ROPE);
		map.put("Vesp milk", ItemID.VESPULAFLYINGPET);
		map.put("Ice milk", ItemID.TINDERBOX);
		map.put("Over-thieve", ItemID.LOCKPICK);
		return Collections.unmodifiableMap(map);
	}
}
