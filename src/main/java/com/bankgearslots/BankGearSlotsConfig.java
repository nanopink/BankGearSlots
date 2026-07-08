package com.bankgearslots;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(BankGearSlotsConfig.GROUP)
public interface BankGearSlotsConfig extends Config
{
	String GROUP = "bank-gear-slots";
	String SAVE_INPUT_TEXT = "CLICK HERE TO SAVE";

	@ConfigItem(
		position = 0,
		keyName = "slots",
		name = "Slots",
		description = "One rule per line: selector[,selector...]:cells:type[:#RRGGBB or #RRGGBBAA]. Use BANK, TAGS, BANK_0, *, or a Bank Tags tab name. Cells can be single values or ranges. Later rules override earlier rules."
	)
	default String slots()
	{
		return "";
	}

	@ConfigItem(
		position = 1,
		keyName = "saveInput",
		name = "Save",
		description = "Click this input after editing Slots to move focus out of the Slots field."
	)
	default String saveInput()
	{
		return SAVE_INPUT_TEXT;
	}
}
