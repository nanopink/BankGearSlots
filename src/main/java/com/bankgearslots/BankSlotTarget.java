package com.bankgearslots;

final class BankSlotTarget
{
	private final String selector;
	private final int cell;
	private final boolean tagTab;
	private final int bankTab;

	BankSlotTarget(String selector, int cell, boolean tagTab, int bankTab)
	{
		this.selector = BankSlotRule.normalize(selector);
		this.cell = cell;
		this.tagTab = tagTab;
		this.bankTab = bankTab;
	}

	String getSelector()
	{
		return selector;
	}

	int getCell()
	{
		return cell;
	}

	boolean isTagTab()
	{
		return tagTab;
	}

	int getBankTab()
	{
		return bankTab;
	}
}
