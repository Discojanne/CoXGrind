package com.coxgrind.ui;

import com.coxgrind.CoxGrindConfig;
import com.coxgrind.model.TargetSheet;
import java.awt.BorderLayout;
import java.awt.event.ContainerEvent;
import java.awt.event.MouseAdapter;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.junit.Assert;
import org.junit.Test;

public class ConfigItemIconsTest
{
	@Test
	public void settingsRowsDoNotGetItemSprites()
	{
		JPanel page = new JPanel();
		JPanel top = new JPanel();
		top.add(new JLabel("Cox Grind"));
		page.add(top);
		JLabel helm = new JLabel("Slayer helm");
		JLabel seconds = new JLabel("Ice milk seconds");
		JPanel row = new JPanel();
		row.add(helm);
		row.add(seconds);
		page.add(row);

		new ConfigItemIcons(new CoxGrindConfig() {}, null).decorateTree(page);

		Assert.assertNull(helm.getIcon());
		Assert.assertNull(seconds.getIcon());
	}

	@Test
	public void iconsStayOnTheCoxGrindSettingsPage()
	{
		JPanel page = new JPanel();
		JPanel top = new JPanel();
		top.add(new JLabel("Cox Grind"));
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

	@Test
	public void targetSheetGetsOneResetButtonAndDropsTheRowMenu()
	{
		JPanel page = new JPanel();
		JPanel top = new JPanel();
		top.add(new JLabel("Cox Grind"));
		page.add(top);

		JLabel regularTitle = new JLabel("Regular solo");
		JPanel regularHeader = new JPanel(new BorderLayout());
		regularHeader.add(regularTitle, BorderLayout.CENTER);
		JPanel regularBody = new JPanel();
		JPanel tekton = new JPanel(new BorderLayout());
		tekton.add(new JLabel("Tekton"), BorderLayout.CENTER);
		regularBody.add(tekton);
		JPanel regular = new JPanel();
		regular.add(regularHeader);
		regular.add(regularBody);

		JLabel sectionTitle = new JLabel("CM solo");
		sectionTitle.addMouseListener(new MouseAdapter() {});
		JPanel header = new JPanel(new BorderLayout());
		header.add(sectionTitle, BorderLayout.CENTER);
		JLabel totalLabel = new JLabel("Target total");
		totalLabel.addMouseListener(new MouseAdapter() {});
		JTextArea total = new JTextArea();
		JPanel row = new JPanel(new BorderLayout());
		row.add(totalLabel, BorderLayout.CENTER);
		row.add(total, BorderLayout.SOUTH);
		JPanel tektonRow = new JPanel(new BorderLayout());
		tektonRow.add(new JLabel("Tekton"), BorderLayout.CENTER);
		JPanel contents = new JPanel();
		contents.add(row);
		contents.add(tektonRow);
		JPanel wrapper = new JPanel();
		wrapper.add(header);
		wrapper.add(contents);

		JLabel reportTitle = new JLabel("Full report");
		JPanel reportHeader = new JPanel(new BorderLayout());
		reportHeader.add(reportTitle, BorderLayout.CENTER);
		JPanel reportBody = new JPanel();
		reportBody.add(new JLabel("Death: CM team"));
		JPanel report = new JPanel();
		report.add(reportHeader);
		report.add(reportBody);

		page.add(regular);
		page.add(wrapper);
		page.add(report);

		Assert.assertEquals(TargetSheet.CM_SOLO, ConfigItemIcons.sheetOf(total));
		Assert.assertNull(ConfigItemIcons.sheetOf(sectionTitle));

		new ConfigItemIcons(new CoxGrindConfig() {}, null).decorateTree(page);

		Assert.assertEquals("25:30", total.getText());
		Assert.assertFalse(total.isEditable());
		Assert.assertTrue(contents.getComponent(0) instanceof JButton);
		Assert.assertEquals("Reset to default", ((JButton) contents.getComponent(0)).getText());
		Assert.assertEquals(0, totalLabel.getMouseListeners().length);
		Assert.assertEquals(1, sectionTitle.getMouseListeners().length);
		Assert.assertEquals(1, countButtons(contents));
		Assert.assertEquals(0, countButtons(reportHeader));
		Assert.assertEquals(0, countButtons(reportBody));
	}

	@Test
	public void pluginListsAndWorldListsAreLeftAlone()
	{
		ConfigItemIcons icons = new ConfigItemIcons(new CoxGrindConfig() {}, null);
		JPanel list = new JPanel();
		for (int i = 0; i < 70; i++)
		{
			JPanel row = new JPanel();
			row.add(new JLabel("Plugin " + i));
			list.add(row);
		}
		JPanel named = new JPanel();
		named.add(new JLabel("Cox Grind"));
		list.add(named);
		icons.noticeAdded(list);
		icons.noticeAdded(named);
		icons.noticeAdded(named.getComponent(0));
		Assert.assertEquals(0, countButtons(list));
		Assert.assertEquals(0, countButtons(named));

		JPanel worlds = new JPanel();
		for (int i = 0; i < 70; i++)
		{
			worlds.add(new JLabel(Integer.toString(300 + i)));
		}
		icons.noticeAdded(worlds);
		Assert.assertEquals(0, countButtons(worlds));
	}

	@Test
	public void noticeAddedStillDecoratesTheSettingsPage()
	{
		ConfigItemIcons icons = new ConfigItemIcons(new CoxGrindConfig() {}, null);
		JPanel page = new JPanel();
		JPanel top = new JPanel();
		top.add(new JLabel("Cox Grind"));
		page.add(top);
		icons.noticeAdded(top);

		JPanel header = new JPanel(new BorderLayout());
		header.add(new JLabel("CM solo"), BorderLayout.CENTER);
		JTextArea total = new JTextArea();
		JPanel row = new JPanel(new BorderLayout());
		row.add(new JLabel("Target total"), BorderLayout.CENTER);
		row.add(total, BorderLayout.SOUTH);
		JPanel tektonRow = new JPanel(new BorderLayout());
		tektonRow.add(new JLabel("Tekton"), BorderLayout.CENTER);
		JPanel contents = new JPanel();
		contents.add(row);
		contents.add(tektonRow);
		JPanel wrapper = new JPanel();
		wrapper.add(header);
		wrapper.add(contents);
		page.add(wrapper);
		icons.noticeAdded(wrapper);

		Assert.assertEquals("25:30", total.getText());
		Assert.assertFalse(total.isEditable());
		Assert.assertEquals(1, countButtons(contents));
	}

	@Test
	public void addedLabelsOutsideSettingsAreIgnored()
	{
		ConfigItemIcons icons = new ConfigItemIcons(new CoxGrindConfig() {}, null);
		JPanel worlds = new JPanel();
		for (int i = 0; i < 40; i++)
		{
			JLabel world = new JLabel("World " + (300 + i));
			worlds.add(world);
			icons.eventDispatched(new ContainerEvent(worlds, ContainerEvent.COMPONENT_ADDED, world));
		}
		Assert.assertEquals(0, countButtons(worlds));
	}

	@Test
	public void addedSettingsPageStillGetsTheResetButton()
	{
		ConfigItemIcons icons = new ConfigItemIcons(new CoxGrindConfig() {}, null);
		JPanel page = new JPanel();
		JPanel top = new JPanel();
		JLabel title = new JLabel("Cox Grind");
		top.add(title);
		page.add(top);
		icons.eventDispatched(new ContainerEvent(page, ContainerEvent.COMPONENT_ADDED, top));

		JPanel header = new JPanel(new BorderLayout());
		header.add(new JLabel("CM solo"), BorderLayout.CENTER);
		JTextArea total = new JTextArea();
		JPanel row = new JPanel(new BorderLayout());
		row.add(new JLabel("Target total"), BorderLayout.CENTER);
		row.add(total, BorderLayout.SOUTH);
		JPanel tektonRow = new JPanel(new BorderLayout());
		tektonRow.add(new JLabel("Tekton"), BorderLayout.CENTER);
		JPanel contents = new JPanel();
		contents.add(row);
		contents.add(tektonRow);
		JPanel wrapper = new JPanel();
		wrapper.add(header);
		wrapper.add(contents);
		page.add(wrapper);
		icons.eventDispatched(new ContainerEvent(page, ContainerEvent.COMPONENT_ADDED, wrapper));

		Assert.assertEquals("25:30", total.getText());
		Assert.assertFalse(total.isEditable());
		Assert.assertEquals(1, countButtons(contents));
	}

	@Test
	public void settingsRefreshDoesNotEnterALongList()
	{
		ConfigItemIcons icons = new ConfigItemIcons(new CoxGrindConfig() {}, null);
		JPanel page = markedPage(icons);
		JPanel list = new JPanel();
		for (int i = 0; i < 70; i++)
		{
			list.add(new JLabel("Plugin " + i));
		}
		JPanel buried = markedPage(icons);
		list.add(buried);
		JPanel root = new JPanel();
		root.add(list);
		root.add(page);

		Assert.assertSame(page, icons.findSettingsPage(root));
		Assert.assertNull(icons.findSettingsPage(list));
	}

	private static JPanel markedPage(ConfigItemIcons icons)
	{
		JPanel page = new JPanel();
		JPanel top = new JPanel();
		top.add(new JLabel("Cox Grind"));
		page.add(top);
		JPanel header = new JPanel(new BorderLayout());
		header.add(new JLabel("CM solo"), BorderLayout.CENTER);
		JPanel contents = new JPanel();
		contents.add(new JLabel("Tekton"));
		JPanel wrapper = new JPanel();
		wrapper.add(header);
		wrapper.add(contents);
		page.add(wrapper);
		icons.noticeAdded(top);
		icons.noticeAdded(wrapper);
		return page;
	}

	private static int countButtons(JPanel panel)
	{
		int count = 0;
		for (java.awt.Component child : panel.getComponents())
		{
			if (child instanceof JButton)
			{
				count++;
			}
		}
		return count;
	}
}
