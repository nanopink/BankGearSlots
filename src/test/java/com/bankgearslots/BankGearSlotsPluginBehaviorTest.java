package com.bankgearslots;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.ItemID;
import org.junit.Test;

public class BankGearSlotsPluginBehaviorTest
{
	@Test
	public void rendersEmptySlotTextureForOccupiedAssignedCells()
	{
		assertEquals(BankSlotType.EMPTY_SLOT, BankGearSlotsPlugin.getRenderedSlotType(BankSlotType.HEAD, ItemID.ABYSSAL_WHIP));
	}

	@Test
	public void preservesTintWhenOccupiedCellsRenderAsEmptySlots()
	{
		BankSlotStyle rendered = BankGearSlotsPlugin.getRenderedSlotStyle(
			BankSlotStyle.of(BankSlotType.HEAD, new Color(0x52, 0x53, 0x00, 0x95)),
			ItemID.ABYSSAL_WHIP,
			false);

		assertEquals(BankSlotType.EMPTY_SLOT, rendered.getType());
		assertEquals(new Color(0x52, 0x53, 0x00, 0x95), rendered.getTint());
	}

	@Test
	public void rendersAssignedTextureWhileOccupiedCellIsDragged()
	{
		assertEquals(BankSlotType.HEAD, BankGearSlotsPlugin.getRenderedSlotType(BankSlotType.HEAD, ItemID.ABYSSAL_WHIP, true));
	}

	@Test
	public void keepsAssignedTextureForEmptyOrFillerCells()
	{
		assertEquals(BankSlotType.HEAD, BankGearSlotsPlugin.getRenderedSlotType(BankSlotType.HEAD, -1));
		assertEquals(BankSlotType.HEAD, BankGearSlotsPlugin.getRenderedSlotType(BankSlotType.HEAD, 0));
		assertEquals(BankSlotType.HEAD, BankGearSlotsPlugin.getRenderedSlotType(BankSlotType.HEAD, ItemID.BANK_FILLER));
	}

	@Test
	public void doesNotRenderUnassignedOccupiedCells()
	{
		assertNull(BankGearSlotsPlugin.getRenderedSlotType(null, ItemID.ABYSSAL_WHIP));
	}

	@Test
	public void detectsRealBankItemIds()
	{
		assertTrue(BankGearSlotsPlugin.isRealBankItemId(ItemID.ABYSSAL_WHIP));
		assertFalse(BankGearSlotsPlugin.isRealBankItemId(-1));
		assertFalse(BankGearSlotsPlugin.isRealBankItemId(0));
		assertFalse(BankGearSlotsPlugin.isRealBankItemId(ItemID.BANK_FILLER));
	}

	@Test
	public void removesBankTagsValueSuffixFromActiveTagName()
	{
		assertEquals("herblore", BankGearSlotsPlugin.normalizeActiveTagName("herblore (158k)"));
		assertEquals("gear", BankGearSlotsPlugin.normalizeActiveTagName("gear (1.2m)"));
		assertEquals("loot", BankGearSlotsPlugin.normalizeActiveTagName("loot (12,345)"));
		assertEquals("herblore supplies", BankGearSlotsPlugin.normalizeActiveTagName("herblore supplies"));
		assertEquals("herblore (main)", BankGearSlotsPlugin.normalizeActiveTagName("herblore (main)"));
	}

	@Test
	public void preservesTypoedSlotRulesDuringUpsert()
	{
		String rules = "mining:31:hed";

		assertEquals(
			"mining:31:hed\nmining:31:head",
			BankGearSlotsPlugin.upsertSlotRule(rules, "mining", 31, BankSlotType.HEAD));
	}

	@Test
	public void preservesUnrelatedRuleTextDuringUpsert()
	{
		String rules = "bank:1:ring\nherblore:25=44:empty_slot:#ABCDEF95";

		assertEquals(
			"bank:1:ring\nherblore:25=44:empty_slot:#ABCDEF95\nmining:31:head",
			BankGearSlotsPlugin.upsertSlotRule(rules, "mining", 31, BankSlotType.HEAD));
	}

	@Test
	public void upsertsSlotRulesWithoutDuplicatingExactSelectorCells()
	{
		String rules = "mining:31:head\ncombat:31:head";

		assertEquals(
			"combat:31:head\nmining:31:cape",
			BankGearSlotsPlugin.upsertSlotRule(rules, "mining", 31, BankSlotType.CAPE));
	}

	@Test
	public void upsertsSlotRulesBySplittingCommaSelectors()
	{
		String rules = "mining,combat:42:legs";

		assertEquals(
			"combat:42:legs\nmining:42:head",
			BankGearSlotsPlugin.upsertSlotRule(rules, "mining", 42, BankSlotType.HEAD));
	}

	@Test
	public void upsertsClearRuleOverBroadRules()
	{
		String rules = "*:31:head";

		assertEquals(
			"*:31:head\nmining:31:none",
			BankGearSlotsPlugin.upsertSlotRule(rules, "mining", 31, null));
	}

	@Test
	public void removesExactSlotRuleWhenClearNeedsNoOverride()
	{
		String rules = "mining:31:head\ncombat:31:head";

		assertEquals(
			"combat:31:head",
			BankGearSlotsPlugin.upsertSlotRule(rules, "mining", 31, null));
	}

	@Test
	public void upsertsSanitizedTagSelectors()
	{
		String rules = "herblore (158k):31:head";

		assertEquals(
			"herblore:31:ring",
			BankGearSlotsPlugin.upsertSlotRule(rules, "herblore (158k)", 31, BankSlotType.RING));
	}

	@Test
	public void upsertsCanonicalBankTabSelectors()
	{
		assertEquals(
			"BANK_2:31:ring",
			BankGearSlotsPlugin.upsertSlotRule("", "BANK_2", 31, BankSlotType.RING));
	}

	@Test
	public void upsertsSlotRulesBySplittingCellRanges()
	{
		String rules = "mining,combat:25-44:legs";

		assertEquals(
			"combat:25-44:legs\nmining:25-30,32-44:legs\nmining:31:head",
			BankGearSlotsPlugin.upsertSlotRule(rules, "mining", 31, BankSlotType.HEAD));
	}

	@Test
	public void upsertsCellSpecificOverrideAfterWildcardCellRule()
	{
		String rules = "farming:*:head";

		assertEquals(
			"farming:*:head\nfarming:31:cape",
			BankGearSlotsPlugin.upsertSlotRule(rules, "farming", 31, BankSlotType.CAPE));
	}

	@Test
	public void upsertsClearOverrideAfterWildcardCellRule()
	{
		String rules = "farming:*:head";

		assertEquals(
			"farming:*:head\nfarming:31:none",
			BankGearSlotsPlugin.upsertSlotRule(rules, "farming", 31, null));
	}

	@Test
	public void detectsBankTagsTabRename()
	{
		Map<String, String> rename = BankGearSlotsPlugin.detectSingleRename(
			BankGearSlotsPlugin.parseBankTagsTabList("combat,herblore,skilling"),
			BankGearSlotsPlugin.parseBankTagsTabList("combat,potions,skilling"));

		assertEquals("potions", rename.get("herblore"));
	}

	@Test
	public void rewritesRenamedTagSelectors()
	{
		Map<String, String> rename = new HashMap<>();
		rename.put("herblore", "potions");

		assertEquals(
			"potions:31:head\ncombat,potions:42:legs\nBANK:1:ring",
			BankGearSlotsPlugin.rewriteSlotSelectors("herblore:31:head\ncombat,herblore:42:legs\nBANK:1:ring", rename));
	}

	@Test
	public void preservesInvalidLinesDuringSelectorRewrite()
	{
		Map<String, String> rename = new HashMap<>();
		rename.put("herblore", "potions");

		assertEquals(
			"herblore:31:hed\npotions:42:head",
			BankGearSlotsPlugin.rewriteSlotSelectors("herblore:31:hed\nherblore:42:head", rename));
	}

	@Test
	public void detectsBankTabOrderMoves()
	{
		List<BankGearSlotsPlugin.BankTabSignature> oldTabs = Arrays.asList(
			signature(1, 100, 101),
			signature(2, 200, 201));
		List<BankGearSlotsPlugin.BankTabSignature> newTabs = Arrays.asList(
			signature(1, 200, 201),
			signature(2, 100, 101));

		Map<String, String> moves = BankGearSlotsPlugin.findBankTabSelectorMoves(oldTabs, newTabs);

		assertEquals("BANK_2", moves.get(BankSlotRule.normalize("BANK_1")));
		assertEquals("BANK_1", moves.get(BankSlotRule.normalize("BANK_2")));
		assertEquals(
			"BANK_2:31:ring\nBANK_1:42:head",
			BankGearSlotsPlugin.rewriteSlotSelectors("BANK_1:31:ring\nBANK_2:42:head", moves));
	}

	@Test
	public void rewrittenBankTabRulesResolveAfterMove()
	{
		List<BankGearSlotsPlugin.BankTabSignature> oldTabs = Arrays.asList(
			signature(1, 100, 101),
			signature(2, 200, 201));
		List<BankGearSlotsPlugin.BankTabSignature> newTabs = Arrays.asList(
			signature(1, 200, 201),
			signature(2, 100, 101));

		String updated = BankGearSlotsPlugin.rewriteSlotSelectors(
			"bank_1:31:ring",
			BankGearSlotsPlugin.findBankTabSelectorMoves(oldTabs, newTabs));

		assertEquals("BANK_2:31:ring", updated);
		assertEquals(BankSlotType.RING, resolveType(BankSlotRule.parseRules(updated), new BankSlotTarget("BANK_2", 31, false, 2)));
		assertNull(resolveType(BankSlotRule.parseRules(updated), new BankSlotTarget("BANK_1", 31, false, 1)));
	}

	@Test
	public void doesNotMoveAmbiguousDuplicateBankTabSignatures()
	{
		List<BankGearSlotsPlugin.BankTabSignature> oldTabs = Arrays.asList(
			signature(1, 100),
			signature(2, 100));
		List<BankGearSlotsPlugin.BankTabSignature> newTabs = Arrays.asList(
			signature(1, 100),
			signature(2, 100));

		assertTrue(BankGearSlotsPlugin.findBankTabSelectorMoves(oldTabs, newTabs).isEmpty());
	}

	@Test
	public void allBankViewMapsCellsToOwningBankTabs()
	{
		int[] tabCounts = {2, 3, 0, 0, 0, 0, 0, 0, 0};

		assertTarget("BANK_0", 0, 0, BankGearSlotsPlugin.resolveBankTarget(0, 0, 9, tabCounts));
		assertTarget("BANK_0", 3, 0, BankGearSlotsPlugin.resolveBankTarget(0, 3, 9, tabCounts));
		assertTarget("BANK_1", 0, 1, BankGearSlotsPlugin.resolveBankTarget(0, 4, 9, tabCounts));
		assertTarget("BANK_1", 1, 1, BankGearSlotsPlugin.resolveBankTarget(0, 5, 9, tabCounts));
		assertTarget("BANK_2", 0, 2, BankGearSlotsPlugin.resolveBankTarget(0, 6, 9, tabCounts));
		assertTarget("BANK_2", 2, 2, BankGearSlotsPlugin.resolveBankTarget(0, 8, 9, tabCounts));
	}

	@Test
	public void allBankViewIgnoresCellsPastKnownBankItems()
	{
		int[] tabCounts = {2, 3, 0, 0, 0, 0, 0, 0, 0};

		assertNull(BankGearSlotsPlugin.resolveBankTarget(0, 9, 9, tabCounts));
	}

	@Test
	public void selectedBankTabsIgnoreCellsPastTheirItemCount()
	{
		int[] tabCounts = {2, 6, 0, 0, 0, 0, 0, 0, 0};

		assertTarget("BANK_2", 5, 2, BankGearSlotsPlugin.resolveBankTarget(2, 5, 8, tabCounts));
		assertNull(BankGearSlotsPlugin.resolveBankTarget(2, 6, 8, tabCounts));
		assertNull(BankGearSlotsPlugin.resolveBankTarget(2, 50, 8, tabCounts));
	}

	@Test
	public void allBankViewDoesNotClampLargeLocalCellsOntoLastCell()
	{
		int[] tabCounts = {6, 0, 0, 0, 0, 0, 0, 0, 0};
		List<BankSlotRule> rules = BankSlotRule.parseRules("BANK_1:50:neck");
		BankSlotTarget target = BankGearSlotsPlugin.resolveBankTarget(0, 5, 6, tabCounts);

		assertTarget("BANK_1", 5, 1, target);
		assertNull(resolveType(rules, target));
	}

	private static BankGearSlotsPlugin.BankTabSignature signature(int tab, long... itemIds)
	{
		List<Long> items = new java.util.ArrayList<>();
		for (long itemId : itemIds)
		{
			items.add(itemId << 32);
		}
		return new BankGearSlotsPlugin.BankTabSignature(tab, items);
	}

	private static void assertTarget(String selector, int cell, int bankTab, BankSlotTarget target)
	{
		assertEquals(selector.toLowerCase(), target.getSelector());
		assertEquals(cell, target.getCell());
		assertEquals(bankTab, target.getBankTab());
		assertFalse(target.isTagTab());
	}

	private static BankSlotType resolveType(List<BankSlotRule> rules, BankSlotTarget target)
	{
		BankSlotStyle style = BankSlotRule.resolve(rules, target);
		return style == null ? null : style.getType();
	}
}
