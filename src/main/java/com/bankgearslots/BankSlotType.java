package com.bankgearslots;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.runelite.client.util.ImageUtil;

enum BankSlotType
{
	HEAD("head", "Head", "Head_slot.png"),
	CAPE("cape", "Cape", "Cape_slot.png"),
	NECK("neck", "Neck", "Neck_slot.png"),
	WEAPON("weapon", "Weapon", "Weapon_slot.png"),
	TWO_HANDED("two_handed", "Two-handed", "2h_slot.png"),
	BODY("body", "Body", "Body_slot.png"),
	SHIELD("shield", "Shield", "Shield_slot.png"),
	LEGS("legs", "Legs", "Legs_slot.png"),
	HANDS("hands", "Hands", "Hands_slot.png"),
	FEET("feet", "Feet", "Feet_slot.png"),
	RING("ring", "Ring", "Ring_slot.png"),
	AMMUNITION("ammunition", "Ammunition", "Ammo_slot.png"),
	EMPTY_SLOT("empty_slot", "Empty Slot", "empty.png");

	private static final float MAX_TINT_STRENGTH = 0.70f;

	private final String configKey;
	private final String displayName;
	private final String textureResource;
	private boolean textureLoadAttempted;
	private BufferedImage texture;
	private final Map<Integer, BufferedImage> tintedTextures = new HashMap<>();

	BankSlotType(String configKey, String displayName, String textureResource)
	{
		this.configKey = configKey;
		this.displayName = displayName;
		this.textureResource = textureResource;
	}

	String getConfigKey()
	{
		return configKey;
	}

	String getDisplayName()
	{
		return displayName;
	}

	String getTextureResource()
	{
		return textureResource;
	}

	static BankSlotType fromConfigKey(String key)
	{
		if (key == null)
		{
			return null;
		}

		String normalized = normalizeKey(key);
		for (BankSlotType type : values())
		{
			if (normalizeKey(type.configKey).equals(normalized))
			{
				return type;
			}
		}
		return null;
	}

	void render(Graphics2D graphics, Rectangle bounds, int textureSize, int textureAlpha, Color tint)
	{
		if (bounds.width <= 0 || bounds.height <= 0)
		{
			return;
		}

		BufferedImage slotTexture = getTexture();
		if (slotTexture != null)
		{
			drawTexture(graphics, bounds, tint == null ? slotTexture : tintedTexture(slotTexture, tint), textureSize, clampAlpha(textureAlpha));
		}
	}

	private BufferedImage getTexture()
	{
		if (textureResource == null)
		{
			return null;
		}
		if (!textureLoadAttempted)
		{
			textureLoadAttempted = true;
			try
			{
				texture = ImageUtil.loadImageResource(BankSlotType.class, textureResource);
			}
			catch (RuntimeException ex)
			{
				texture = null;
			}
		}
		return texture;
	}

	private BufferedImage tintedTexture(BufferedImage image, Color tint)
	{
		int tintKey = tint.getRGB();
		BufferedImage cached = tintedTextures.get(tintKey);
		if (cached != null)
		{
			return cached;
		}

		BufferedImage tinted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				tinted.setRGB(x, y, tintPixel(image.getRGB(x, y), tint));
			}
		}
		tintedTextures.put(tintKey, tinted);
		return tinted;
	}

	static int tintPixel(int argb, Color tint)
	{
		int alpha = argb >>> 24;
		if (alpha == 0 || tint == null || tint.getAlpha() == 0)
		{
			return argb;
		}

		float strength = tint.getAlpha() / 255.0f * MAX_TINT_STRENGTH;
		int red = (argb >>> 16) & 0xFF;
		int green = (argb >>> 8) & 0xFF;
		int blue = argb & 0xFF;

		int tintedRed = red * tint.getRed() / 255;
		int tintedGreen = green * tint.getGreen() / 255;
		int tintedBlue = blue * tint.getBlue() / 255;

		return alpha << 24
			| blendChannel(red, tintedRed, strength) << 16
			| blendChannel(green, tintedGreen, strength) << 8
			| blendChannel(blue, tintedBlue, strength);
	}

	private static int blendChannel(int source, int tinted, float strength)
	{
		return Math.max(0, Math.min(255, Math.round(source + (tinted - source) * strength)));
	}

	private void drawTexture(Graphics2D graphics, Rectangle bounds, BufferedImage image, int textureSize, int alpha)
	{
		int size = Math.max(1, textureSize);
		int width = size;
		int height = size;
		int x = bounds.x + (bounds.width - width) / 2;
		int y = bounds.y + (bounds.height - height) / 2;

		Composite composite = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampAlpha(alpha) / 255.0f));
		graphics.drawImage(image, x, y, width, height, null);
		graphics.setComposite(composite);
	}

	private int clampAlpha(int alpha)
	{
		return Math.max(0, Math.min(255, alpha));
	}

	private static String normalizeKey(String key)
	{
		return key.trim().toLowerCase(Locale.ROOT);
	}
}
