package com.bankgearslots;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BankGearSlotsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BankGearSlotsPlugin.class);
		RuneLite.main(args);
	}
}
