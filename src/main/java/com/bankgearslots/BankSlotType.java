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

	private static final float TINT_LIGHTNESS_INFLUENCE = 0.50f;

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

		int red = (argb >>> 16) & 0xFF;
		int green = (argb >>> 8) & 0xFF;
		int blue = argb & 0xFF;
		float[] tintHsl = rgbToHsl(tint.getRed(), tint.getGreen(), tint.getBlue());
		float[] sourceHsl = rgbToHsl(red, green, blue);
		int colorized = hslToRgb(tintHsl[0], tintHsl[1], tintLightness(sourceHsl[2], tintHsl[2]));
		float strength = tint.getAlpha() / 255.0f;

		return alpha << 24
			| blendChannel(red, (colorized >>> 16) & 0xFF, strength) << 16
			| blendChannel(green, (colorized >>> 8) & 0xFF, strength) << 8
			| blendChannel(blue, colorized & 0xFF, strength);
	}

	private static int blendChannel(int source, int tinted, float strength)
	{
		return Math.max(0, Math.min(255, Math.round(source + (tinted - source) * strength)));
	}

	private static float tintLightness(float sourceLightness, float tintLightness)
	{
		float shift = (tintLightness - 0.5f) * 2.0f * TINT_LIGHTNESS_INFLUENCE;
		if (shift >= 0.0f)
		{
			return sourceLightness + (1.0f - sourceLightness) * shift;
		}
		return sourceLightness * (1.0f + shift);
	}

	private static float[] rgbToHsl(int red, int green, int blue)
	{
		float r = red / 255.0f;
		float g = green / 255.0f;
		float b = blue / 255.0f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float lightness = (max + min) / 2.0f;
		float delta = max - min;
		if (delta == 0.0f)
		{
			return new float[] {0.0f, 0.0f, lightness};
		}

		float hue;
		if (max == r)
		{
			hue = 60.0f * (((g - b) / delta) % 6.0f);
		}
		else if (max == g)
		{
			hue = 60.0f * (((b - r) / delta) + 2.0f);
		}
		else
		{
			hue = 60.0f * (((r - g) / delta) + 4.0f);
		}
		if (hue < 0.0f)
		{
			hue += 360.0f;
		}

		float saturation = delta / (1.0f - Math.abs(2.0f * lightness - 1.0f));
		return new float[] {hue, saturation, lightness};
	}

	private static int hslToRgb(float hue, float saturation, float lightness)
	{
		float chroma = (1.0f - Math.abs(2.0f * lightness - 1.0f)) * saturation;
		float h = hue / 60.0f;
		float x = chroma * (1.0f - Math.abs(h % 2.0f - 1.0f));
		float r = 0.0f;
		float g = 0.0f;
		float b = 0.0f;
		if (h < 1.0f)
		{
			r = chroma;
			g = x;
		}
		else if (h < 2.0f)
		{
			r = x;
			g = chroma;
		}
		else if (h < 3.0f)
		{
			g = chroma;
			b = x;
		}
		else if (h < 4.0f)
		{
			g = x;
			b = chroma;
		}
		else if (h < 5.0f)
		{
			r = x;
			b = chroma;
		}
		else
		{
			r = chroma;
			b = x;
		}

		float match = lightness - chroma / 2.0f;
		return toChannel(r + match) << 16
			| toChannel(g + match) << 8
			| toChannel(b + match);
	}

	private static int toChannel(float value)
	{
		return Math.max(0, Math.min(255, Math.round(value * 255.0f)));
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
