package com.bankgearslots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.client.util.Text;

final class BankSlotRule
{
	static final String WILDCARD = "*";
	static final String BANK = "bank";
	static final String TAGS = "tags";
	private static final String NONE = "none";

	private final List<String> selectors;
	private final List<BankSlotCells.CellRange> cells;
	private final BankSlotStyle style;

	private BankSlotRule(List<String> selectors, List<BankSlotCells.CellRange> cells, BankSlotStyle style)
	{
		this.selectors = selectors;
		this.cells = cells;
		this.style = style;
	}

	static List<BankSlotRule> parseRules(String value)
	{
		if (value == null || value.trim().isEmpty())
		{
			return Collections.emptyList();
		}

		List<BankSlotRule> rules = new ArrayList<>();
		for (String line : value.split("\\R"))
		{
			BankSlotRule rule = parse(line);
			if (rule != null)
			{
				rules.add(rule);
			}
		}
		return rules;
	}

	static BankSlotRule parse(String line)
	{
		if (line == null)
		{
			return null;
		}

		String cleaned = line.trim();
		if (cleaned.isEmpty())
		{
			return null;
		}

		String[] parts = cleaned.split(":", 4);
		if (parts.length != 3 && parts.length != 4)
		{
			return null;
		}

		List<String> selectors = parseSelectors(parts[0]);
		if (selectors.isEmpty())
		{
			return null;
		}

		List<BankSlotCells.CellRange> cells = BankSlotCells.parse(parts[1]);
		if (cells.isEmpty())
		{
			return null;
		}

		String typeText = parts[2].trim();
		BankSlotStyle style;
		if (BankSlotStyle.isClearType(typeText))
		{
			if (parts.length != 3)
			{
				return null;
			}
			style = null;
		}
		else
		{
			style = BankSlotStyle.parse(typeText, parts.length == 4 ? parts[3] : null);
			if (style == null)
			{
				return null;
			}
		}

		return new BankSlotRule(selectors, cells, style);
	}

	static BankSlotStyle resolve(List<BankSlotRule> rules, BankSlotTarget target)
	{
		BankSlotStyle resolved = null;
		for (BankSlotRule rule : rules)
		{
			if (rule.matches(target))
			{
				resolved = rule.style;
			}
		}
		return resolved;
	}

	static String formatLine(String selector, int cell, BankSlotType type)
	{
		return formatLine(selector, BankSlotCells.single(cell), BankSlotStyle.of(type));
	}

	static String formatLine(String selector, String cells, BankSlotStyle style)
	{
		return selector + ":" + cells + ":" + (style == null ? NONE : style.format());
	}

	private boolean matches(BankSlotTarget target)
	{
		if (!BankSlotCells.contains(cells, target.getCell()))
		{
			return false;
		}

		for (String selector : selectors)
		{
			if (matchesSelector(selector, target))
			{
				return true;
			}
		}
		return false;
	}

	private boolean matchesSelector(String selector, BankSlotTarget target)
	{
		if (WILDCARD.equals(selector))
		{
			return true;
		}
		if (target.isTagTab())
		{
			return TAGS.equals(selector) || selector.equals(target.getSelector());
		}

		return BANK.equals(selector) || selector.equals(target.getSelector());
	}

	private static List<String> parseSelectors(String value)
	{
		List<String> selectors = new ArrayList<>();
		for (String selector : value.split(","))
		{
			String normalized = normalize(selector);
			if (!normalized.isEmpty())
			{
				selectors.add(normalized);
			}
		}
		return selectors;
	}

	static String normalize(String value)
	{
		return Text.standardize(value == null ? "" : value.trim());
	}
}
