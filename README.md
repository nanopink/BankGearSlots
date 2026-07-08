# Bank Gear Slots

Adds equipment slot textures to your bank with ease!

[![Bank Gear Slots demo](https://img.youtube.com/vi/ttTm4U9lT4k/hqdefault.jpg)](https://www.youtube.com/watch?v=ttTm4U9lT4k)

# Slots

Shift + right-click a bank cell to add or remove slots.

<img width="516" height="306" alt="image" src="https://github.com/user-attachments/assets/bdf64be4-26c7-4a64-9a4d-6e34df1cbebc" />

Format:

```text
where:cells:slot                   Regular cell
where:cells:slot:#RRGGBBAA         Colored slot
```

Examples:

```text
mining:30:cape                           Cape slot in the mining tags tab on the 30th cell
mining:31:head                           Helmet slot in the mining tags tab on the 31st cell
BANK:12:empty_slot                       Empty slot in all bank tabs on the 12th cell
BANK_0:44:shield                         Shield slot in the All tab on the 44th cell
TAGS:42:legs                             Leggings slot in the tags tab on the 42nd cell
mining,combat:42:legs                    Leggings slot in the tags tabs for mining and combat on the 42nd cell
herblore:25-44,47-85:empty_slot          Empty slots in the herblore tabs from cell 25 to 44 and cells 47 to 85
BANK_2:22:weapon:#00ACED33               Weapon slot with a blue color in the second custom bank tab on the 22nd cell
farming:*:none                           Removes all slots in the farming tags tab
```

`BANK_0` is the All tab. `BANK_1` is the first custom bank tab, `BANK_2` is the second custom bank tab, and so on.

`BANK_n` cells are local to that tab. In the all-bank view, each bank tab section renders with its own local cell numbers.

# Cells

Cell numbers start at `0`.

```text
31              One cell
25-44           A range
25-44,47-85     More than one range
25-44,47        Ranges and single cells together
*               Every cell
```

Cells outside a tab's real range do not render.

# Slot Types

```text
head, cape, neck, weapon, two_handed, body, shield, legs, hands, feet, ring, ammunition, empty_slot, none
```

Assigned cells render as `empty_slot` while they contain a real bank item. While dragging that item, the assigned slot texture is shown again.

Invalid lines are ignored by the plugin and left unchanged in `Slots`.
