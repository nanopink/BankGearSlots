package com.bankgearslots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class BankSlotCells
{
	private static final String WILDCARD = "*";

	private BankSlotCells()
	{
	}

	static List<CellRange> parse(String value)
	{
		if (value == null || value.trim().isEmpty())
		{
			return Collections.emptyList();
		}

		List<CellRange> ranges = new ArrayList<>();
		for (String part : value.split(","))
		{
			CellRange range = parsePart(part.trim());
			if (range != null)
			{
				if (range.wildcard)
				{
					return Collections.singletonList(range);
				}
				ranges.add(range);
			}
		}
		return normalize(ranges);
	}

	static boolean contains(List<CellRange> ranges, int cell)
	{
		for (CellRange range : ranges)
		{
			if (range.contains(cell))
			{
				return true;
			}
		}
		return false;
	}

	static boolean contains(String value, int cell)
	{
		return contains(parse(value), cell);
	}

	static String canonicalize(String value)
	{
		return format(parse(value));
	}

	static boolean isWildcard(String value)
	{
		List<CellRange> ranges = parse(value);
		return ranges.size() == 1 && ranges.get(0).wildcard;
	}

	static String single(int cell)
	{
		return Integer.toString(cell);
	}

	static String removeCell(String value, int cell)
	{
		if (isWildcard(value))
		{
			return WILDCARD;
		}

		List<CellRange> remaining = new ArrayList<>();
		for (CellRange range : parse(value))
		{
			if (!range.contains(cell))
			{
				remaining.add(range);
			}
			else
			{
				if (cell > range.start)
				{
					remaining.add(new CellRange(range.start, cell - 1));
				}
				if (cell < range.end)
				{
					remaining.add(new CellRange(cell + 1, range.end));
				}
			}
		}

		return remaining.isEmpty() ? null : format(normalize(remaining));
	}

	static String format(List<CellRange> ranges)
	{
		StringBuilder builder = new StringBuilder();
		for (CellRange range : ranges)
		{
			if (range.wildcard)
			{
				return WILDCARD;
			}
			if (builder.length() > 0)
			{
				builder.append(',');
			}
			builder.append(range.start);
			if (range.start != range.end)
			{
				builder.append('-').append(range.end);
			}
		}
		return builder.toString();
	}

	private static CellRange parsePart(String value)
	{
		if (value.isEmpty())
		{
			return null;
		}
		if (WILDCARD.equals(value))
		{
			return CellRange.wildcard();
		}

		int separator = value.indexOf('-');
		if (separator < 0)
		{
			separator = value.indexOf('=');
		}

		try
		{
			if (separator < 0)
			{
				int cell = Integer.parseInt(value);
				return cell < 0 ? null : new CellRange(cell, cell);
			}

			int start = Integer.parseInt(value.substring(0, separator).trim());
			int end = Integer.parseInt(value.substring(separator + 1).trim());
			if (start < 0 || end < 0)
			{
				return null;
			}
			return new CellRange(Math.min(start, end), Math.max(start, end));
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}

	private static List<CellRange> normalize(List<CellRange> ranges)
	{
		if (ranges.isEmpty())
		{
			return ranges;
		}
		for (CellRange range : ranges)
		{
			if (range.wildcard)
			{
				return Collections.singletonList(range);
			}
		}

		ranges.sort(Comparator.comparingInt(range -> range.start));
		List<CellRange> normalized = new ArrayList<>();
		for (CellRange range : ranges)
		{
			if (normalized.isEmpty())
			{
				normalized.add(range);
				continue;
			}

			CellRange last = normalized.get(normalized.size() - 1);
			if (range.start <= last.end + 1)
			{
				normalized.set(normalized.size() - 1, new CellRange(last.start, Math.max(last.end, range.end)));
			}
			else
			{
				normalized.add(range);
			}
		}
		return normalized;
	}

	static final class CellRange
	{
		private final int start;
		private final int end;
		private final boolean wildcard;

		private CellRange(int start, int end)
		{
			this(start, end, false);
		}

		private CellRange(int start, int end, boolean wildcard)
		{
			this.start = start;
			this.end = end;
			this.wildcard = wildcard;
		}

		private static CellRange wildcard()
		{
			return new CellRange(0, Integer.MAX_VALUE, true);
		}

		private boolean contains(int cell)
		{
			return cell >= start && cell <= end;
		}
	}
}
