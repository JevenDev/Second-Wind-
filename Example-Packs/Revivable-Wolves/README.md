# Revivable Wolves

This example datapack opts tamed `minecraft:wolf` entities into Second Wind's
managed downed state. Wild wolves are unaffected. When a tamed wolf would
receive lethal damage, it is downed for up to 12 seconds instead. A player
within 2.5 blocks can hold the interact button for 2 seconds to revive it with
6 health (3 hearts).

While downed, the wolf is rendered on its side with the persistent red damage
overlay and announces `<wolf name> needs help!` in chat. The behavior JSON can
switch the pose to `secondwind:upright`, disable the announcement, or replace
its translation key and fallback text.

Revived wolves receive 3 seconds of regeneration and 2 seconds of
invulnerability. After revival, a wolf has a 5-minute Second Wind cooldown.

## Installation

Copy the `Revivable-Wolves` folder into the world's `datapacks` folder, then
run `/reload` or restart the world. Second Wind must be installed on the server.

The pack targets Minecraft 1.21.1. Its behavior definition is located at
`data/secondwind_examples/secondwind/entity_behaviors/wolf.json` and can be
edited to demonstrate different timers, revival settings, and presentation.
