package com.coxgrind;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class CoxGrindPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CoxGrindPlugin.class);
		RuneLite.main(args);
	}
}
