package com.bankgearslots;

import com.google.inject.Provides;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Bank Gear Slots",
	description = "Adds equipment slot textures to bank cells, including Bank Tags tabs",
	tags = {"bank gear slots", "bank slots", "bank tags", "tag tabs", "equipment", "gear", "loadout", "bank", "slot", "texture", "layout"}
)
public class BankGearSlotsPlugin extends Plugin
{
	private static final String CONFIG_SLOTS_KEY = "slots";
	private static final String CONFIG_SAVE_INPUT_KEY = "saveInput";
	private static final String BANK_TAGS_GROUP = "banktags";
	private static final String BANK_TAGS_TAB_KEY = "tagtabs";
	private static final String TAG_TAB_TAB_TITLE = "tag tab tab";
	private static final String TAG_TAB_PREFIX = "tag tab ";
	private static final String BANK_SELECTOR_PREFIX = "bank_";
	private static final int[] BANK_TAB_VARBITS = {
		VarbitID.BANK_TAB_1,
		VarbitID.BANK_TAB_2,
		VarbitID.BANK_TAB_3,
		VarbitID.BANK_TAB_4,
		VarbitID.BANK_TAB_5,
		VarbitID.BANK_TAB_6,
		VarbitID.BANK_TAB_7,
		VarbitID.BANK_TAB_8,
		VarbitID.BANK_TAB_9
	};
	private static final Pattern TAG_VALUE_SUFFIX = Pattern.compile("\\s+\\([0-9][0-9,.]*[kmb]?\\)$", Pattern.CASE_INSENSITIVE);

	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BankGearSlotsConfig config;

	@Inject
	private BankGearSlotsOverlay overlay;

	private List<BankSlotRule> rules = new ArrayList<>();
	private List<BankTabSignature> bankTabSignatures = new ArrayList<>();

	@Provides
	BankGearSlotsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankGearSlotsConfig.class);
	}

	@Override
	protected void startUp()
	{
		resetSaveInput(config.saveInput());
		loadRules(config.slots());
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		rules.clear();
		bankTabSignatures.clear();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (BankGearSlotsConfig.GROUP.equals(event.getGroup()) && CONFIG_SLOTS_KEY.equals(event.getKey()))
		{
			loadRules(config.slots());
		}
		else if (BankGearSlotsConfig.GROUP.equals(event.getGroup()) && CONFIG_SAVE_INPUT_KEY.equals(event.getKey()))
		{
			resetSaveInput(event.getNewValue());
		}
		else if (BANK_TAGS_GROUP.equals(event.getGroup()) && BANK_TAGS_TAB_KEY.equals(event.getKey()))
		{
			syncBankTagTabRename(event);
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.BANK.getId())
		{
			syncBankTabOrder(event.getItemContainer());
		}
	}

	@Subscribe
	public void onPostMenuSort(PostMenuSort event)
	{
		if (client.isMenuOpen() || !client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}

		BankSlotTarget target = findHoveredTarget();
		if (target == null)
		{
			return;
		}

		BankSlotStyle current = BankSlotRule.resolve(rules, target);
		MenuEntry parent = client.createMenuEntry(-2)
			.setOption("Add Slot")
			.setTarget("")
			.setType(MenuAction.RUNELITE);
		Menu submenu = parent.createSubMenu();

		if (current != null)
		{
			client.createMenuEntry(-2)
				.setOption("Remove Slot")
				.setTarget("")
				.setType(MenuAction.RUNELITE)
				.onClick(entry -> setRule(target, null));
		}

		for (BankSlotType type : BankSlotType.values())
		{
			if (current != null && type == current.getType())
			{
				continue;
			}

			submenu.createMenuEntry(0)
				.setOption(type.getDisplayName())
				.setType(MenuAction.RUNELITE)
				.onClick(entry -> setRule(target, type));
		}
	}

	Widget[] getBankItemWidgets(Widget container)
	{
		Widget[] children = container.getDynamicChildren();
		if (children == null || children.length == 0)
		{
			children = container.getChildren();
		}
		return children;
	}

	boolean isVisibleBankCell(Widget widget)
	{
		return widget != null && !widget.isHidden() && !widget.isSelfHidden() && !widget.getBounds().isEmpty();
	}

	BankSlotStyle getAssignedSlotStyle(Widget widget)
	{
		return getAssignedSlotStyle(widget, widget == null ? -1 : widget.getIndex());
	}

	BankSlotStyle getAssignedSlotStyle(Widget widget, int cell)
	{
		BankSlotTarget target = targetForWidget(widget, cell, createBankCellResolver());
		return target == null ? null : BankSlotRule.resolve(rules, target);
	}

	BankSlotStyle getRenderedSlotStyle(Widget widget)
	{
		return getRenderedSlotStyle(widget, widget == null ? -1 : widget.getIndex());
	}

	BankSlotStyle getRenderedSlotStyle(Widget widget, int cell)
	{
		return getRenderedSlotStyle(widget, cell, createBankCellResolver());
	}

	BankSlotStyle getRenderedSlotStyle(Widget widget, int cell, BankCellResolver resolver)
	{
		BankSlotTarget target = targetForWidget(widget, cell, resolver);
		BankSlotStyle assigned = target == null ? null : BankSlotRule.resolve(rules, target);
		return getRenderedSlotStyle(assigned, widget == null ? -1 : widget.getItemId(), isDraggedBankItemWidget(widget));
	}

	static BankSlotType getRenderedSlotType(BankSlotType assigned, int itemId)
	{
		return getRenderedSlotType(assigned, itemId, false);
	}

	static BankSlotType getRenderedSlotType(BankSlotType assigned, int itemId, boolean dragged)
	{
		BankSlotStyle rendered = getRenderedSlotStyle(BankSlotStyle.of(assigned), itemId, dragged);
		return rendered == null ? null : rendered.getType();
	}

	static BankSlotStyle getRenderedSlotStyle(BankSlotStyle assigned, int itemId, boolean dragged)
	{
		if (assigned == null)
		{
			return null;
		}

		return !dragged && isRealBankItemId(itemId) ? assigned.withType(BankSlotType.EMPTY_SLOT) : assigned;
	}

	static boolean isRealBankItemId(int itemId)
	{
		return itemId > 0 && itemId != ItemID.BANK_FILLER;
	}

	private void loadRules(String slots)
	{
		rules = new ArrayList<>(BankSlotRule.parseRules(slots));
	}

	private void resetSaveInput(String value)
	{
		if (!BankGearSlotsConfig.SAVE_INPUT_TEXT.equals(value))
		{
			configManager.setConfiguration(BankGearSlotsConfig.GROUP, CONFIG_SAVE_INPUT_KEY, BankGearSlotsConfig.SAVE_INPUT_TEXT);
		}
	}

	private void setRule(BankSlotTarget target, BankSlotType type)
	{
		String current = config.slots();
		String value = upsertSlotRule(current, target, BankSlotStyle.of(type));
		loadRules(value);
		configManager.setConfiguration(BankGearSlotsConfig.GROUP, CONFIG_SLOTS_KEY, value);
		refreshOpenSlotsConfigField(current, value);
	}

	private void syncBankTagTabRename(ConfigChanged event)
	{
		Map<String, String> rename = detectSingleRename(parseBankTagsTabList(event.getOldValue()), parseBankTagsTabList(event.getNewValue()));
		rewriteSlotSelectors(rename);
	}

	private void syncBankTabOrder(ItemContainer itemContainer)
	{
		List<BankTabSignature> nextSignatures = snapshotBankTabs(itemContainer);
		if (bankTabSignatures.isEmpty())
		{
			bankTabSignatures = nextSignatures;
			return;
		}
		if (nextSignatures.isEmpty())
		{
			bankTabSignatures = nextSignatures;
			return;
		}

		Map<String, String> tabMoves = findBankTabSelectorMoves(bankTabSignatures, nextSignatures);
		bankTabSignatures = nextSignatures;
		rewriteSlotSelectors(tabMoves);
	}

	private void rewriteSlotSelectors(Map<String, String> selectorRenames)
	{
		if (selectorRenames.isEmpty())
		{
			return;
		}

		String current = config.slots();
		String updated = rewriteSlotSelectors(current, selectorRenames);
		if (!updated.equals(current == null ? "" : current))
		{
			loadRules(updated);
			configManager.setConfiguration(BankGearSlotsConfig.GROUP, CONFIG_SLOTS_KEY, updated);
			refreshOpenSlotsConfigField(current, updated);
		}
	}

	static String upsertSlotRule(String current, String selector, int cell, BankSlotType type)
	{
		return upsertSlotRule(current, newTargetFromSelector(selector, cell), BankSlotStyle.of(type));
	}

	static String upsertSlotRule(String current, BankSlotTarget target, BankSlotType type)
	{
		return upsertSlotRule(current, target, BankSlotStyle.of(type));
	}

	static String upsertSlotRule(String current, BankSlotTarget target, BankSlotStyle style)
	{
		String selector = canonicalizeSelectorForConfig(target.getSelector());
		if (selector.isEmpty())
		{
			return current == null ? "" : current;
		}
		String normalizedSelector = BankSlotRule.normalize(selector);
		return upsertSlotRule(current, target, selector, normalizedSelector, target.getCell(), style);
	}

	private static String upsertSlotRule(String current, BankSlotTarget target, String selector, String normalizedSelector, int cell, BankSlotStyle style)
	{
		String newLine = BankSlotRule.formatLine(selector, BankSlotCells.single(cell), style);
		String currentValue = current == null ? "" : current;
		if (currentValue.trim().isEmpty())
		{
			return style == null ? "" : newLine;
		}

		List<String> lines = new ArrayList<>();
		for (String line : currentValue.split("\\R", -1))
		{
			for (String updated : removeExactSelectorCell(line, normalizedSelector, cell))
			{
				if (updated != null)
				{
					lines.add(updated);
				}
			}
		}

		if (style == null && BankSlotRule.resolve(BankSlotRule.parseRules(String.join("\n", lines)), target) == null)
		{
			return String.join("\n", lines);
		}

		lines.add(newLine);
		return String.join("\n", lines);
	}

	private static BankSlotTarget newTargetFromSelector(String selector, int cell)
	{
		String normalizedSelector = BankSlotRule.normalize(canonicalizeSelectorForConfig(selector));
		int bankTab = parseBankTabSelector(normalizedSelector);
		boolean tagTab = !BankSlotRule.BANK.equals(normalizedSelector) && bankTab < 0;
		return new BankSlotTarget(normalizedSelector, cell, tagTab, bankTab);
	}

	private static int parseBankTabSelector(String selector)
	{
		String selectorKey = selectorKey(selector);
		if (selectorKey.startsWith(BANK_SELECTOR_PREFIX))
		{
			return parseIntOrDefault(selectorKey.substring(BANK_SELECTOR_PREFIX.length()), -1);
		}

		return -1;
	}

	private static int parseIntOrDefault(String value, int fallback)
	{
		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException ex)
		{
			return fallback;
		}
	}

	private static List<String> removeExactSelectorCell(String line, String selector, int cell)
	{
		List<String> result = new ArrayList<>();
		if (BankSlotRule.parse(line) == null)
		{
			result.add(line);
			return result;
		}

		int firstSeparator = line.indexOf(':');
		if (firstSeparator < 0)
		{
			result.add(line);
			return result;
		}

		int secondSeparator = line.indexOf(':', firstSeparator + 1);
		if (secondSeparator < 0)
		{
			result.add(line);
			return result;
		}

		String cells = line.substring(firstSeparator + 1, secondSeparator);
		if (BankSlotCells.isWildcard(cells))
		{
			result.add(line);
			return result;
		}

		if (!BankSlotCells.contains(cells, cell))
		{
			result.add(line);
			return result;
		}

		String[] selectors = line.substring(0, firstSeparator).split(",", -1);
		List<String> remainingSelectors = new ArrayList<>();
		boolean removed = false;
		for (String rawSelector : selectors)
		{
			String normalized = normalizedConfigSelector(rawSelector);
			if (normalized.equals(selector))
			{
				removed = true;
				continue;
			}
			if (!normalized.isEmpty())
			{
				remainingSelectors.add(canonicalizeSelectorForConfig(normalized));
			}
		}

		if (!removed)
		{
			result.add(line);
			return result;
		}

		String suffix = line.substring(secondSeparator);
		String canonicalCells = BankSlotCells.canonicalize(cells);
		if (!remainingSelectors.isEmpty())
		{
			result.add(String.join(",", remainingSelectors) + ":" + canonicalCells + suffix);
		}

		String remainingCells = BankSlotCells.removeCell(cells, cell);
		if (remainingCells != null)
		{
			result.add(canonicalizeSelectorForConfig(selector) + ":" + remainingCells + suffix);
		}
		return result;
	}

	static String rewriteSlotSelectors(String value, Map<String, String> selectorRenames)
	{
		if (value == null || value.isEmpty() || selectorRenames.isEmpty())
		{
			return value == null ? "" : value;
		}

		String[] lines = value.split("\\R", -1);
		for (int i = 0; i < lines.length; i++)
		{
			lines[i] = rewriteSlotRuleSelectors(lines[i], selectorRenames);
		}
		return String.join("\n", lines);
	}

	private static String rewriteSlotRuleSelectors(String line, Map<String, String> selectorRenames)
	{
		if (BankSlotRule.parse(line) == null)
		{
			return line;
		}

		int separator = line.indexOf(':');
		if (separator < 0)
		{
			return line;
		}

		String[] selectors = line.substring(0, separator).split(",", -1);
		for (int i = 0; i < selectors.length; i++)
		{
			String replacement = selectorRenames.get(normalizedConfigSelector(selectors[i]));
			if (replacement != null)
			{
				selectors[i] = canonicalizeSelectorForConfig(replacement);
			}
		}
		return String.join(",", selectors) + line.substring(separator);
	}

	private static String normalizedConfigSelector(String selector)
	{
		String canonical = canonicalizeSelectorForConfig(selector);
		return BankSlotRule.normalize(canonical.isEmpty() ? selector : canonical);
	}

	static String canonicalizeSelectorForConfig(String selector)
	{
		String cleaned = normalizeActiveTagName(selector);
		String normalized = BankSlotRule.normalize(cleaned);
		if (normalized.isEmpty())
		{
			return "";
		}
		if (BankSlotRule.WILDCARD.equals(normalized))
		{
			return BankSlotRule.WILDCARD;
		}
		if (BankSlotRule.BANK.equals(normalized))
		{
			return "BANK";
		}
		if (BankSlotRule.TAGS.equals(normalized))
		{
			return "TAGS";
		}
		String selectorKey = selectorKey(normalized);
		if (selectorKey.startsWith(BANK_SELECTOR_PREFIX))
		{
			int bankTab = parseIntOrDefault(selectorKey.substring(BANK_SELECTOR_PREFIX.length()), -1);
			return bankTab < 0 ? normalized : "BANK_" + bankTab;
		}
		return normalized;
	}

	private static String selectorKey(String selector)
	{
		return selector == null ? "" : selector.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
	}

	static List<String> parseBankTagsTabList(String value)
	{
		List<String> tags = new ArrayList<>();
		if (value == null || value.trim().isEmpty())
		{
			return tags;
		}

		for (String tag : Text.fromCSV(value.toLowerCase(Locale.ROOT)))
		{
			String standardized = Text.standardize(tag);
			if (!standardized.isEmpty())
			{
				tags.add(standardized);
			}
		}
		return tags;
	}

	static Map<String, String> detectSingleRename(List<String> oldNames, List<String> newNames)
	{
		Map<String, String> rename = new HashMap<>();
		if (oldNames.size() == newNames.size())
		{
			int changedIndex = -1;
			for (int i = 0; i < oldNames.size(); i++)
			{
				if (!oldNames.get(i).equals(newNames.get(i)))
				{
					if (changedIndex >= 0)
					{
						changedIndex = -1;
						break;
					}
					changedIndex = i;
				}
			}

			if (changedIndex >= 0)
			{
				rename.put(oldNames.get(changedIndex), newNames.get(changedIndex));
				return rename;
			}
		}

		List<String> removed = new ArrayList<>(oldNames);
		removed.removeAll(newNames);
		List<String> added = new ArrayList<>(newNames);
		added.removeAll(oldNames);
		if (removed.size() == 1 && added.size() == 1)
		{
			rename.put(removed.get(0), added.get(0));
		}
		return rename;
	}

	private List<BankTabSignature> snapshotBankTabs(ItemContainer itemContainer)
	{
		List<BankTabSignature> signatures = new ArrayList<>();
		if (itemContainer == null)
		{
			return signatures;
		}

		Item[] items = itemContainer.getItems();
		int offset = Math.max(0, itemContainer.count() - totalConfiguredBankTabItems());
		for (int i = 0; i < BANK_TAB_VARBITS.length; i++)
		{
			int count = Math.max(0, client.getVarbitValue(BANK_TAB_VARBITS[i]));
			if (count <= 0)
			{
				continue;
			}

			signatures.add(new BankTabSignature(i + 1, itemSignature(items, offset, count)));
			offset += count;
		}
		return signatures;
	}

	private int totalConfiguredBankTabItems()
	{
		int total = 0;
		for (int bankTabVarbit : BANK_TAB_VARBITS)
		{
			total += Math.max(0, client.getVarbitValue(bankTabVarbit));
		}
		return total;
	}

	private static List<Long> itemSignature(Item[] items, int offset, int count)
	{
		List<Long> signature = new ArrayList<>();
		int end = Math.min(items.length, offset + count);
		for (int i = Math.max(0, offset); i < end; i++)
		{
			Item item = items[i];
			int id = item == null ? -1 : item.getId();
			int quantity = item == null ? 0 : item.getQuantity();
			signature.add(((long) id << 32) ^ (quantity & 0xFFFFFFFFL));
		}
		return signature;
	}

	static Map<String, String> findBankTabSelectorMoves(List<BankTabSignature> oldSignatures, List<BankTabSignature> newSignatures)
	{
		Map<String, String> moves = new HashMap<>();
		if (oldSignatures.isEmpty() || oldSignatures.size() != newSignatures.size() || !sameSignatureMultiset(oldSignatures, newSignatures))
		{
			return moves;
		}

		Map<BankTabSignature, Integer> oldCounts = signatureCounts(oldSignatures);
		Map<BankTabSignature, Integer> newCounts = signatureCounts(newSignatures);
		for (BankTabSignature oldSignature : oldSignatures)
		{
			if (oldCounts.get(oldSignature) != 1 || newCounts.get(oldSignature) != 1)
			{
				continue;
			}

			BankTabSignature newSignature = findSignature(newSignatures, oldSignature);
			if (newSignature != null && oldSignature.tab != newSignature.tab)
			{
				moves.put(BankSlotRule.normalize("BANK_" + oldSignature.tab), "BANK_" + newSignature.tab);
			}
		}
		return moves;
	}

	private static boolean sameSignatureMultiset(List<BankTabSignature> oldSignatures, List<BankTabSignature> newSignatures)
	{
		return signatureCounts(oldSignatures).equals(signatureCounts(newSignatures));
	}

	private static Map<BankTabSignature, Integer> signatureCounts(List<BankTabSignature> signatures)
	{
		Map<BankTabSignature, Integer> counts = new HashMap<>();
		for (BankTabSignature signature : signatures)
		{
			counts.put(signature, counts.getOrDefault(signature, 0) + 1);
		}
		return counts;
	}

	private static BankTabSignature findSignature(List<BankTabSignature> signatures, BankTabSignature target)
	{
		for (BankTabSignature signature : signatures)
		{
			if (signature.equals(target))
			{
				return signature;
			}
		}
		return null;
	}

	private void refreshOpenSlotsConfigField(String oldValue, String newValue)
	{
		String oldText = oldValue == null ? "" : oldValue;
		SwingUtilities.invokeLater(() ->
		{
			for (Window window : Window.getWindows())
			{
				refreshSlotsConfigField(window, oldText, newValue);
			}
		});
	}

	private static void refreshSlotsConfigField(Component component, String oldValue, String newValue)
	{
		if (!(component instanceof Container))
		{
			return;
		}

		Container container = (Container) component;
		if (isSlotsConfigRow(container))
		{
			JTextArea textArea = findSlotsTextArea(container);
			if (textArea != null && textArea.getText().equals(oldValue))
			{
				textArea.setText(newValue);
			}
		}

		for (Component child : container.getComponents())
		{
			refreshSlotsConfigField(child, oldValue, newValue);
		}
	}

	private static boolean isSlotsConfigRow(Container container)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JLabel && "Slots".equals(((JLabel) child).getText()))
			{
				return true;
			}
		}
		return false;
	}

	private static JTextArea findSlotsTextArea(Container container)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JTextArea)
			{
				return (JTextArea) child;
			}
		}
		return null;
	}

	private BankSlotTarget findHoveredTarget()
	{
		Widget root = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (root == null || root.isHidden() || container == null || container.isHidden())
		{
			return null;
		}

		Point mousePosition = client.getMouseCanvasPosition();
		Widget[] children = getBankItemWidgets(container);
		if (children == null)
		{
			return null;
		}

		BankCellResolver resolver = createBankCellResolver();
		int cell = 0;
		for (Widget widget : children)
		{
			if (!isVisibleBankCell(widget))
			{
				continue;
			}

			Rectangle bounds = widget.getBounds();
			if (bounds.contains(mousePosition.getX(), mousePosition.getY()))
			{
				return targetForWidget(widget, cell, resolver);
			}
			cell++;
		}

		return null;
	}

	private boolean isDraggedBankItemWidget(Widget widget)
	{
		if (widget == null || !client.isDraggingWidget())
		{
			return false;
		}

		Widget dragged = client.getDraggedWidget();
		if (dragged == null)
		{
			return false;
		}

		return dragged == widget || dragged.getId() == widget.getId() && dragged.getIndex() == widget.getIndex();
	}

	private BankSlotTarget targetForWidget(Widget widget)
	{
		return targetForWidget(widget, widget == null ? -1 : widget.getIndex(), createBankCellResolver());
	}

	private BankSlotTarget targetForWidget(Widget widget, int cell)
	{
		return targetForWidget(widget, cell, createBankCellResolver());
	}

	private BankSlotTarget targetForWidget(Widget widget, int cell, BankCellResolver resolver)
	{
		if (!isVisibleBankCell(widget))
		{
			return null;
		}

		if (cell < 0)
		{
			return null;
		}

		return resolver.targetForCell(cell);
	}

	BankCellResolver createBankCellResolver()
	{
		if (isTagTabTabOpen())
		{
			return BankCellResolver.none();
		}

		String activeTag = getActiveTagFromTitle();
		if (activeTag != null)
		{
			String tag = Text.standardize(activeTag);
			return BankCellResolver.tag(tag);
		}

		int bankTab = client.getVarbitValue(VarbitID.BANK_CURRENTTAB);
		return BankCellResolver.bank(bankTab, getBankItemCount(), getBankTabItemCounts());
	}

	private int getBankItemCount()
	{
		ItemContainer itemContainer = client.getItemContainer(InventoryID.BANK);
		return itemContainer == null ? -1 : Math.max(0, itemContainer.count());
	}

	private int[] getBankTabItemCounts()
	{
		int[] counts = new int[BANK_TAB_VARBITS.length];
		for (int i = 0; i < BANK_TAB_VARBITS.length; i++)
		{
			counts[i] = Math.max(0, client.getVarbitValue(BANK_TAB_VARBITS[i]));
		}
		return counts;
	}

	private boolean isTagTabTabOpen()
	{
		String title = getCleanBankTitle();
		return TAG_TAB_TAB_TITLE.equals(title);
	}

	private String getActiveTagFromTitle()
	{
		String title = getCleanBankTitle();
		if (title == null || TAG_TAB_TAB_TITLE.equals(title) || !title.startsWith(TAG_TAB_PREFIX))
		{
			return null;
		}

		String tag = normalizeActiveTagName(title.substring(TAG_TAB_PREFIX.length()));
		return tag.isEmpty() ? null : tag;
	}

	static String normalizeActiveTagName(String tag)
	{
		if (tag == null)
		{
			return "";
		}

		return TAG_VALUE_SUFFIX.matcher(tag.trim()).replaceFirst("").trim();
	}

	private String getCleanBankTitle()
	{
		Widget title = client.getWidget(InterfaceID.Bankmain.TITLE);
		if (title == null || title.getText() == null)
		{
			return null;
		}

		return Text.removeTags(title.getText()).trim().toLowerCase(Locale.ROOT);
	}

	static final class BankTabSignature
	{
		private final int tab;
		private final List<Long> items;

		BankTabSignature(int tab, List<Long> items)
		{
			this.tab = tab;
			this.items = items;
		}

		@Override
		public boolean equals(Object other)
		{
			if (this == other)
			{
				return true;
			}
			if (!(other instanceof BankTabSignature))
			{
				return false;
			}

			BankTabSignature that = (BankTabSignature) other;
			return items.equals(that.items);
		}

		@Override
		public int hashCode()
		{
			return items.hashCode();
		}
	}

	static final class BankCellResolver
	{
		private final String tag;
		private final int currentBankTab;
		private final int bankItemCount;
		private final int[] bankTabItemCounts;

		private BankCellResolver(String tag, int currentBankTab, int bankItemCount, int[] bankTabItemCounts)
		{
			this.tag = tag;
			this.currentBankTab = currentBankTab;
			this.bankItemCount = bankItemCount;
			this.bankTabItemCounts = bankTabItemCounts;
		}

		static BankCellResolver none()
		{
			return new BankCellResolver("", -1, -1, new int[0]);
		}

		static BankCellResolver tag(String tag)
		{
			return new BankCellResolver(BankSlotRule.normalize(tag), -1, -1, new int[0]);
		}

		static BankCellResolver bank(int currentBankTab, int bankItemCount, int[] bankTabItemCounts)
		{
			return new BankCellResolver(null, currentBankTab, bankItemCount, bankTabItemCounts == null ? new int[0] : bankTabItemCounts.clone());
		}

		BankSlotTarget targetForCell(int cell)
		{
			if (cell < 0)
			{
				return null;
			}
			if (tag != null)
			{
				return tag.isEmpty() ? null : new BankSlotTarget(tag, cell, true, -1);
			}
			return resolveBankTarget(currentBankTab, cell, bankItemCount, bankTabItemCounts);
		}
	}

	static BankSlotTarget resolveBankTarget(int currentBankTab, int cell, int bankItemCount, int[] bankTabItemCounts)
	{
		if (cell < 0)
		{
			return null;
		}
		int[] counts = bankTabItemCounts == null ? new int[0] : bankTabItemCounts;
		if (currentBankTab != 0)
		{
			if (currentBankTab > 0 && currentBankTab <= counts.length && cell >= counts[currentBankTab - 1])
			{
				return null;
			}
			return new BankSlotTarget("BANK_" + currentBankTab, cell, false, currentBankTab);
		}
		if (bankItemCount < 0)
		{
			return new BankSlotTarget("BANK_0", cell, false, 0);
		}

		int totalTabbedItems = 0;
		for (int count : counts)
		{
			totalTabbedItems += Math.max(0, count);
		}

		int offset = 0;
		int mainTabItems = Math.max(0, bankItemCount - totalTabbedItems);
		if (cell < mainTabItems)
		{
			return new BankSlotTarget("BANK_0", cell, false, 0);
		}
		offset += mainTabItems;

		for (int i = 0; i < counts.length; i++)
		{
			int remainingItems = bankItemCount - offset;
			if (remainingItems <= 0)
			{
				break;
			}

			int tabItems = Math.min(Math.max(0, counts[i]), remainingItems);
			if (cell < offset + tabItems)
			{
				return new BankSlotTarget("BANK_" + (i + 1), cell - offset, false, i + 1);
			}
			offset += tabItems;
		}

		return null;
	}
}
