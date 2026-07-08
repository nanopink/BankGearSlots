package com.bankgearslots;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class BankGearSlotsOverlay extends Overlay
{
	private static final int BANK_ITEM_WIDTH = 36;
	private static final int BANK_ITEM_HEIGHT = 32;
	private static final int SLOT_X_OFFSET = -2;
	private static final int TEXTURE_SIZE = 36;
	private static final int TEXTURE_ALPHA = 255;
	private static final int CLIP_TOP_EXPANSION = 4;

	private final Client client;
	private final BankGearSlotsPlugin plugin;

	@Inject
	BankGearSlotsOverlay(Client client, BankGearSlotsPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.MANUAL);
		setPriority(OverlayPriority.LOW);
		drawAfterLayer(InterfaceID.Bankmain.TABS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Widget root = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (root == null || root.isHidden() || container == null || container.isHidden())
		{
			return null;
		}

		Widget[] children = plugin.getBankItemWidgets(container);
		if (children == null)
		{
			return null;
		}

		BankGearSlotsPlugin.BankCellResolver resolver = plugin.createBankCellResolver();
		int cell = 0;
		for (Widget widget : children)
		{
			if (!plugin.isVisibleBankCell(widget))
			{
				continue;
			}

			renderWidget(graphics, widget, cell, resolver);
			cell++;
		}

		return null;
	}

	private void renderWidget(Graphics2D graphics, Widget widget, int cell, BankGearSlotsPlugin.BankCellResolver resolver)
	{
		if (!plugin.isVisibleBankCell(widget))
		{
			return;
		}

		BankSlotStyle style = plugin.getRenderedSlotStyle(widget, cell, resolver);
		if (style == null)
		{
			return;
		}

		Rectangle bounds = getSlotBounds(widget);
		if (bounds.isEmpty())
		{
			return;
		}

		Shape clip = graphics.getClip();
		Rectangle viewportBounds = getViewportBounds(widget);
		if (viewportBounds != null && !viewportBounds.isEmpty())
		{
			viewportBounds = new Rectangle(viewportBounds);
			viewportBounds.y -= CLIP_TOP_EXPANSION;
			viewportBounds.height += CLIP_TOP_EXPANSION;
			graphics.clip(viewportBounds);
		}
		try
		{
			style.getType().render(graphics, bounds, TEXTURE_SIZE, TEXTURE_ALPHA, style.getTint());
		}
		finally
		{
			graphics.setClip(clip);
		}
	}

	private Rectangle getSlotBounds(Widget widget)
	{
		Rectangle bounds = widget.getBounds();
		if (bounds.isEmpty())
		{
			return bounds;
		}

		return new Rectangle(bounds.x + SLOT_X_OFFSET, bounds.y, Math.min(BANK_ITEM_WIDTH, bounds.width), Math.min(BANK_ITEM_HEIGHT, bounds.height));
	}

	private Rectangle getViewportBounds(Widget widget)
	{
		Rectangle viewport = null;
		for (Widget current = widget.getParent(); current != null; current = current.getParent())
		{
			Rectangle bounds = current.getBounds();
			if (bounds.width <= 0 || bounds.height <= 0)
			{
				continue;
			}

			viewport = viewport == null ? new Rectangle(bounds) : viewport.intersection(bounds);
			if (viewport.isEmpty())
			{
				return viewport;
			}
		}

		return viewport;
	}
}
