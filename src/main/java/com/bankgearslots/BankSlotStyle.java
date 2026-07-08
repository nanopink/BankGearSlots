package com.bankgearslots;

import java.awt.Color;
import java.util.Locale;

final class BankSlotStyle
{
	private static final String NONE = "none";

	private final BankSlotType type;
	private final Color tint;

	private BankSlotStyle(BankSlotType type, Color tint)
	{
		this.type = type;
		this.tint = tint;
	}

	static BankSlotStyle of(BankSlotType type)
	{
		return type == null ? null : new BankSlotStyle(type, null);
	}

	static BankSlotStyle of(BankSlotType type, Color tint)
	{
		return type == null ? null : new BankSlotStyle(type, tint);
	}

	static BankSlotStyle parse(String typeText, String tintText)
	{
		if (isClearType(typeText))
		{
			return null;
		}

		BankSlotType type = BankSlotType.fromConfigKey(typeText);
		if (type == null)
		{
			return null;
		}

		Color tint = parseTint(tintText);
		if (hasText(tintText) && tint == null)
		{
			return null;
		}

		return new BankSlotStyle(type, tint);
	}

	static boolean isClearType(String value)
	{
		return NONE.equals(normalizeType(value));
	}

	BankSlotType getType()
	{
		return type;
	}

	Color getTint()
	{
		return tint;
	}

	BankSlotStyle withType(BankSlotType newType)
	{
		return new BankSlotStyle(newType, tint);
	}

	String format()
	{
		return type.getConfigKey() + (tint == null ? "" : ":" + formatTint(tint));
	}

	private static Color parseTint(String value)
	{
		if (!hasText(value))
		{
			return null;
		}

		String hex = value.trim();
		if (hex.startsWith("#"))
		{
			hex = hex.substring(1);
		}
		if (hex.length() != 6 && hex.length() != 8)
		{
			return null;
		}

		try
		{
			int red = Integer.parseInt(hex.substring(0, 2), 16);
			int green = Integer.parseInt(hex.substring(2, 4), 16);
			int blue = Integer.parseInt(hex.substring(4, 6), 16);
			int alpha = hex.length() == 8 ? Integer.parseInt(hex.substring(6, 8), 16) : 255;
			return new Color(red, green, blue, alpha);
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}

	private static boolean hasText(String value)
	{
		return value != null && !value.trim().isEmpty();
	}

	private static String normalizeType(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private static String formatTint(Color color)
	{
		if (color.getAlpha() == 255)
		{
			return String.format(Locale.ROOT, "#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
		}
		return String.format(Locale.ROOT, "#%02x%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
	}
}
