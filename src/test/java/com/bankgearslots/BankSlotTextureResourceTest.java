package com.bankgearslots;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import org.junit.Test;

public class BankSlotTextureResourceTest
{
	@Test
	public void loadsSlotTextures() throws IOException
	{
		for (BankSlotType type : BankSlotType.values())
		{
			URL resource = BankSlotType.class.getResource(type.getTextureResource());
			assertNotNull(type.getConfigKey(), resource);

			BufferedImage image = ImageIO.read(resource);
			assertNotNull(type.getConfigKey(), image);
			assertEquals(type.getConfigKey(), image.getWidth(), image.getHeight());
			assertTrue(type.getConfigKey(), image.getWidth() > 0);
		}
	}
}
