package com.bankgearslots;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import org.junit.Test;

public class BankSlotTypeTest
{
	@Test
	public void blackTintPreservesTextureBrightnessDifferences()
	{
		int dark = BankSlotType.tintPixel(new Color(80, 80, 80, 180).getRGB(), new Color(0, 0, 0, 255));
		int bright = BankSlotType.tintPixel(new Color(200, 200, 200, 180).getRGB(), new Color(0, 0, 0, 255));

		assertEquals(180, alpha(dark));
		assertEquals(180, alpha(bright));
		assertTrue(red(dark) < 80);
		assertTrue(red(bright) > red(dark));
		assertEquals(red(dark), green(dark));
		assertEquals(green(dark), blue(dark));
	}

	@Test
	public void whiteTintBrightensWhilePreservingTextureBrightnessDifferences()
	{
		int dark = BankSlotType.tintPixel(new Color(80, 80, 80, 180).getRGB(), Color.WHITE);
		int bright = BankSlotType.tintPixel(new Color(200, 200, 200, 180).getRGB(), Color.WHITE);

		assertEquals(180, alpha(dark));
		assertEquals(180, alpha(bright));
		assertTrue(red(dark) > 80);
		assertTrue(red(bright) > red(dark));
		assertEquals(red(dark), green(dark));
		assertEquals(green(dark), blue(dark));
	}

	@Test
	public void transparentTintLeavesPixelUnchanged()
	{
		int source = new Color(120, 80, 40, 200).getRGB();

		assertEquals(source, BankSlotType.tintPixel(source, new Color(0, 0, 0, 0)));
	}

	@Test
	public void colorTintPreservesSourceAlpha()
	{
		int source = new Color(160, 160, 160, 123).getRGB();
		int tinted = BankSlotType.tintPixel(source, new Color(255, 0, 0, 255));

		assertEquals(123, alpha(tinted));
		assertTrue(red(tinted) > green(tinted));
		assertEquals(green(tinted), blue(tinted));
	}

	@Test
	public void fullBlueTintDoesNotFlattenTextureIntoPureBlue()
	{
		int dark = BankSlotType.tintPixel(new Color(80, 80, 80, 255).getRGB(), Color.BLUE);
		int bright = BankSlotType.tintPixel(new Color(200, 200, 200, 255).getRGB(), Color.BLUE);

		assertTrue(blue(bright) > blue(dark));
		assertTrue(blue(dark) > red(dark));
		assertTrue(blue(dark) > green(dark));
		assertTrue(blue(dark) < 255);
		assertTrue(bright != dark);
	}

	@Test
	public void partialTintBlendsBetweenSourceAndColorizedTexture()
	{
		int source = new Color(120, 120, 120, 255).getRGB();
		int tinted = BankSlotType.tintPixel(source, new Color(0, 0, 255, 128));

		assertTrue(blue(tinted) > red(tinted));
		assertTrue(red(tinted) > 0);
	}

	private static int alpha(int argb)
	{
		return argb >>> 24;
	}

	private static int red(int argb)
	{
		return (argb >>> 16) & 0xFF;
	}

	private static int green(int argb)
	{
		return (argb >>> 8) & 0xFF;
	}

	private static int blue(int argb)
	{
		return argb & 0xFF;
	}
}
