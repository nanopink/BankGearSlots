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
		int dark = BankSlotType.tintPixel(new Color(80, 80, 80, 180).getRGB(), Color.BLACK);
		int bright = BankSlotType.tintPixel(new Color(200, 200, 200, 180).getRGB(), Color.BLACK);

		assertEquals(180, alpha(dark));
		assertEquals(180, alpha(bright));
		assertTrue(red(dark) > 0);
		assertTrue(red(bright) > red(dark));
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
		assertTrue(green(tinted) > 0);
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
}
