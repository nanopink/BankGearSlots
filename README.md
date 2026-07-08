# Bank Gear Slots

Adds equipment slot textures to bank cells, including cells shown inside Bank Tags tabs.

[![Bank Gear Slots demo](https://img.youtube.com/vi/ttTm4U9lT4k/hqdefault.jpg)](https://www.youtube.com/watch?v=ttTm4U9lT4k)

# Slots

Hold shift and right-click a bank cell to add or remove a slot.

Manual rules can be edited in the `Slots` setting. Click the `CLICK HERE TO SAVE` input after editing.

Format:

```text
where:cells:slot
where:cells:slot:#RRGGBB
where:cells:slot:#RRGGBBAA
```

Examples:

```text
mining:30:cape
mining:31:head
BANK:12:empty_slot
TAGS:42:legs
mining,combat:42:legs
herblore:25-44,47-85:empty_slot:#52530095
BANK_0:0:weapon
farming:*:none
```

# Where

```text
mining          Bank Tags tab named mining
mining,combat   More than one Bank Tags tab
BANK_0          Main bank section
BANK_1          Bank tab 1
BANK            Every normal bank tab
TAGS            Every Bank Tags tab
*               Every bank tab and Bank Tags tab
```

Bank Tags tab values shown in the title are ignored. If the bank title says `Herblore (158k)`, use `herblore`.

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
head
cape
neck
weapon
two_handed
body
shield
legs
hands
feet
ring
ammunition
empty_slot
none
```

Assigned cells render as `empty_slot` while they contain a real bank item. While dragging that item, the assigned slot texture is shown again.

Invalid lines are ignored by the plugin and left unchanged in `Slots`.
