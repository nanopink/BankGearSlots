package com.bankgearslots;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.awt.Color;
import java.util.List;
import org.junit.Test;

public class BankSlotRuleTest
{
	@Test
	public void resolvesSpecificTagRules()
	{
		List<BankSlotRule> rules = BankSlotRule.parseRules("mining,combat:42:legs");

		assertEquals(BankSlotType.LEGS, resolveType(rules, new BankSlotTarget("mining", 42, true, -1)));
		assertEquals(BankSlotType.LEGS, resolveType(rules, new BankSlotTarget("combat", 42, true, -1)));
		assertNull(resolveType(rules, new BankSlotTarget("skilling", 42, true, -1)));
	}

	@Test
	public void laterRulesOverrideEarlierRules()
	{
		List<BankSlotRule> rules = BankSlotRule.parseRules("*:31:head\nmining:31:cape\ncombat:31:none");

		assertEquals(BankSlotType.CAPE, resolveType(rules, new BankSlotTarget("mining", 31, true, -1)));
		assertNull(resolveType(rules, new BankSlotTarget("combat", 31, true, -1)));
		assertEquals(BankSlotType.HEAD, resolveType(rules, new BankSlotTarget("BANK_0", 31, false, 0)));
	}

	@Test
	public void resolvesHardcodedSelectors()
	{
		List<BankSlotRule> rules = BankSlotRule.parseRules("BANK:3:ring\nTAGS:3:ammunition\nBANK_2:3:two_handed");

		assertEquals(BankSlotType.RING, resolveType(rules, new BankSlotTarget("BANK_0", 3, false, 0)));
		assertEquals(BankSlotType.TWO_HANDED, resolveType(rules, new BankSlotTarget("BANK_2", 3, false, 2)));
		assertEquals(BankSlotType.AMMUNITION, resolveType(rules, new BankSlotTarget("mining", 3, true, -1)));
	}

	@Test
	public void resolvesRanges()
	{
		List<BankSlotRule> rules = BankSlotRule.parseRules("herblore:25-44,47=85:empty_slot");

		assertEquals(BankSlotType.EMPTY_SLOT, resolveType(rules, new BankSlotTarget("herblore", 25, true, -1)));
		assertEquals(BankSlotType.EMPTY_SLOT, resolveType(rules, new BankSlotTarget("herblore", 44, true, -1)));
		assertNull(resolveType(rules, new BankSlotTarget("herblore", 46, true, -1)));
		assertEquals(BankSlotType.EMPTY_SLOT, resolveType(rules, new BankSlotTarget("herblore", 85, true, -1)));
	}

	@Test
	public void resolvesWildcardCells()
	{
		List<BankSlotRule> rules = BankSlotRule.parseRules("farming:*:head");

		assertEquals(BankSlotType.HEAD, resolveType(rules, new BankSlotTarget("farming", 0, true, -1)));
		assertEquals(BankSlotType.HEAD, resolveType(rules, new BankSlotTarget("farming", 999, true, -1)));
		assertNull(resolveType(rules, new BankSlotTarget("mining", 999, true, -1)));
	}

	@Test
	public void wildcardCellRulesCanClearAllCellsForASelector()
	{
		List<BankSlotRule> rules = BankSlotRule.parseRules("*:31:head\nfarming:*:none");

		assertNull(resolveType(rules, new BankSlotTarget("farming", 31, true, -1)));
		assertEquals(BankSlotType.HEAD, resolveType(rules, new BankSlotTarget("mining", 31, true, -1)));
	}

	@Test
	public void resolvesTintedStyles()
	{
		List<BankSlotRule> rules = BankSlotRule.parseRules("herblore:31:head:#52530095");
		BankSlotStyle style = BankSlotRule.resolve(rules, new BankSlotTarget("herblore", 31, true, -1));

		assertEquals(BankSlotType.HEAD, style.getType());
		assertEquals(new Color(0x52, 0x53, 0x00, 0x95), style.getTint());
	}

	@Test
	public void ignoresInvalidTypeNames()
	{
		List<BankSlotRule> rules = BankSlotRule.parseRules("BANK_1:31:helmet\nBANK_1:32:empty\nBANK_1:33:two handed");

		assertNull(resolveType(rules, new BankSlotTarget("BANK_1", 31, false, 1)));
		assertNull(resolveType(rules, new BankSlotTarget("BANK_1", 32, false, 1)));
		assertNull(resolveType(rules, new BankSlotTarget("BANK_1", 33, false, 1)));
	}

	private static BankSlotType resolveType(List<BankSlotRule> rules, BankSlotTarget target)
	{
		BankSlotStyle style = BankSlotRule.resolve(rules, target);
		return style == null ? null : style.getType();
	}
}
