package com.coxgrind.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.api.gameval.ItemID;
import org.junit.Assert;
import org.junit.Test;

public class ConfigItemIconsTest
{
	@Test
	public void settingsLabelsMapToItemSprites()
	{
		Assert.assertEquals(ItemID.TWISTED_BOW, ConfigItemIcons.itemId("Using twisted bow"));
		Assert.assertEquals(ItemID.SLAYER_HELM_I, ConfigItemIcons.itemId("Slayer helm"));
		Assert.assertEquals(ItemID.LOCKPICK, ConfigItemIcons.itemId("Lockpick"));
		Assert.assertEquals(ItemID.DRAGON_AXE, ConfigItemIcons.itemId("Axe"));
		Assert.assertEquals(ItemID.NZONE_SALVE_AMULET_E, ConfigItemIcons.itemId("Salve"));
		Assert.assertEquals(ItemID.ASTRALRUNE, ConfigItemIcons.itemId("Pre-veng"));
		Assert.assertEquals(ItemID._4DOSESTAMINA, ConfigItemIcons.itemId("Vesp pot skip"));
		Assert.assertEquals(ItemID.RED_CRAB, ConfigItemIcons.itemId("Crab tank"));
		Assert.assertEquals(ItemID.ROPE, ConfigItemIcons.itemId("Killing rope"));
		Assert.assertEquals(ItemID.VESPULAFLYINGPET, ConfigItemIcons.itemId("Vesp milk"));
		Assert.assertEquals(ItemID.TINDERBOX, ConfigItemIcons.itemId("Ice milk"));
		Assert.assertEquals(ItemID.LOCKPICK, ConfigItemIcons.itemId("Over-thieve"));
		Assert.assertEquals(-1, ConfigItemIcons.itemId("Ice milk seconds"));
	}

	@Test
	public void iconsStayOnTheCoxGrindSettingsPage()
	{
		JPanel page = new JPanel();
		JPanel top = new JPanel();
		top.add(new JLabel("CoXGrind"));
		page.add(top);
		JLabel row = new JLabel("Slayer helm");
		JPanel item = new JPanel();
		item.add(row);
		Assert.assertFalse(ConfigItemIcons.onCoxGrindSettings(row));
		page.add(item);

		JLabel elsewhere = new JLabel("Axe");
		new JPanel().add(elsewhere);

		Assert.assertTrue(ConfigItemIcons.onCoxGrindSettings(row));
		Assert.assertFalse(ConfigItemIcons.onCoxGrindSettings(elsewhere));
	}
}
