<div align="center">

<h2><strong>You aren't dead yet.</strong></h2>

</div>

<div align="center">

<a href="https://modrinth.com/mod/second-wind-mod/settings/versions?l=neoforge"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/refs/heads/v3/assets/cozy/supported/neoforge_64h.png" alt="Available for NeoForge"></a>
<a href="https://modrinth.com/mod/second-wind-mod/versions?l=fabric"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/refs/heads/v3/assets/cozy/supported/fabric_64h.png" alt="Available for Fabric"></a>
<a href="https://modrinth.com/mod/toucan"><img src="https://raw.githubusercontent.com/JevenDev/toucanLib/refs/heads/1.21.1/docs/badges/toucanlib_toucanlib_cozy_64h.png" alt="Requires toucanLib"></a>
<br>
<a href="https://modrinth.com/mod/second-wind-mod" target="_blank" rel="noopener noreferrer"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/refs/heads/v3/assets/compact-minimal/available/modrinth_46h.png" alt="Available on Modrinth"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/secondwind" target="_blank" rel="nofollow"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/refs/heads/v3/assets/compact-minimal/available/curseforge_46h.png" alt="Available on CurseForge"></a>
<a href="https://github.com/JevenDev/Second-Wind-" target="_blank" rel="noopener noreferrer"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/refs/heads/v3/assets/compact-minimal/available/github_46h.png" alt="Available on GitHub"></a>

</div>

![second wind banner](https://cdn.modrinth.com/data/cached_images/5310e53f706adc88f05e634d4debe6e61b6bfcfc.png)

Second Wind! is a Vanilla+ NeoForge mod that replaces instant death with a final chance to survive.

Instead of dying immediately, players enter a downed state where they can crawl to safety, wait for help, or fight their way back up with a clutch kill.

This mod is obviously directly inspired by the Borderlands games' "Crippled"/"Fight For Your Life" states.

- Survive lethal damage through a final downed state
- Crawl, wait for help, or secure a kill to revive yourself
- Revive teammates before they bleed out
- Punish repeated downs with shorter survival windows
- Configure cooldowns, penalties, revive rules, and damage behavior
- Cinematic HUD effects, revive flashes, and screen distortion
- Fully multiplayer compatible

<div align="center">
  <img
    src="https://i.imgur.com/JFsUSO0.gif"
    alt='a player being downed by another player'
    width="49%"
  />
  <img
    src="https://i.imgur.com/Y0lkEIV.gif"
    alt='a player being revived by another player'
    width="49%"
  />
</div>

<br>

![features](https://cdn.modrinth.com/data/cached_images/ec0e4dc78ec1a652eb11b233dd2926f7461fe770.png)

## Main Features

### Last Stand

When Second Wind triggers, players are downed instead of dying instantly.

Downed players are forced into a low-mobility state with a visible countdown timer. They can still look around, crawl, and attempt to survive before their timer expires.

By default:

- Players enter a forced crawling/swimming pose
- Healing and eating are blocked while downed
- Incoming damage can shorten the remaining timer
- Repeated downs become increasingly dangerous

The default base downed timer is 10 seconds, but nearly every part of the system is configurable.

### Fight Back Up

A downed player is not helpless.

Players can revive themselves by securing a valid kill before their timer runs out, creating high-risk comeback moments during combat.

By default:

- Hostile mobs count
- Player kills count
- Passive mob kills do not count
- Pet kills for the owner do not count

Neutral mobs can still count if they are actively targeting the downed player.

Successful revives grant:

- Restored health
- Temporary regeneration
- Brief post-revive invulnerability

<div align="center">
  <img
    src="https://i.imgur.com/O3OMGKl.gif"
    alt='a player being finished while down by another player'
    width="49%"
  />
  <img
    src="https://i.imgur.com/c8IF0XG.gif"
    alt='a downed player reviving themselves by killing a mob'
    width="49%"
  />
</div>

<br>

### Multiplayer Revives

Teammates can pull each other back from the brink.

Players can revive downed allies by holding interaction nearby, creating tense rescue moments during boss fights, PvP encounters, caves, or large multiplayer battles.

Revives can optionally:

- Be interrupted by damage
- Require close range
- Use configurable channel times
- Trigger custom visual and sound effects

Default revive settings:

| Setting | Default |
| --- | --- |
| Revive Channel Time | `2.0 seconds` |
| Revive Distance | `2.5 blocks` |
| Damage Interrupts Revive | `true` |

### Repeat Downs

Repeated downs become more dangerous.

Every successful downed state can shorten the next survival window until the penalty resets, discouraging reckless play during extended fights.

### Data-Driven Entities

Datapacks can opt non-player living entities into Second Wind. Definitions live under
`data/<namespace>/secondwind/entity_behaviors/` and use `secondwind:entity_behavior/v1`.
Second Wind does not enable any vanilla mob by default.

```json
{
  "schema": "secondwind:entity_behavior/v1",
  "target": "minecraft:wolf",
  "priority": 0,
  "lifecycle": { "type": "managed" },
  "downed": {
    "timer_ticks": 240,
    "minimum_timer_ticks": 60,
    "penalty_per_down_ticks": 40,
    "damage_mode": "reduce_timer",
    "disable_ai": true,
    "block_healing": true
  },
  "revive": {
    "enabled": true,
    "channel_ticks": 40,
    "distance": 2.5,
    "health": 6.0,
    "cooldown_ticks": 6000
  },
  "presentation": {
    "show_timer": true,
    "announce": false,
    "poses": ["secondwind:crawl"]
  }
}
```

`target` accepts an entity ID or an entity-type tag prefixed with `#`. Higher-priority definitions
win; exact entity targets win equal-priority ties over tags. Managed entities keep their remaining
timer while unloaded and restore their captured AI, pickup, and pose state when revived.

Compatibility mods can instead declare an `external` lifecycle with an adapter ID. The owning mod
then controls downing and recovery while Second Wind supplies tracking and player revive channels;
external entities never receive a Second Wind bleedout timer.

Servers can configure:

- Minimum timer limits
- Penalty scaling
- Cooldown reset behavior
- Sleep/day-based recovery
- Damage interaction rules

### Cooldowns

Second Wind does not have to be permanently available.

The mod supports multiple cooldown systems:

| Mode | Description |
| --- | --- |
| `NONE` | No cooldown |
| `TIMED` | Real-time cooldown |
| `MC_DAY` | Resets once per Minecraft day |
| `ON_SLEEP` | Resets after sleeping |

Default behavior uses a 300 second cooldown.

## Client Feedback & Effects

Second Wind! includes dedicated client-side feedback to make downed states feel intense and readable.

Features include:

- Last-stand HUD timer bar
- Crosshair revive progress display
- Revive flash effect
- Optional vignette and desaturation effects
- Bloom-style screen effects
- Optional sound cues
- Default `R` keybind to give up while downed

Shader-style post-processing effects are powered through Lodestone.

## Commands

Requires operator permission level 2.

```mcfunction
/secondwind revive
/secondwind revive <player>
/secondwind down
/secondwind down <player>
````

`/secondwind revive` revives all currently downed players when no target is supplied.

`/secondwind down` forcibly downs all eligible players when no target is supplied.

## Advancements

The mod currently has 7 advancements.

<details>
<summary><strong>All Advancements (Click to Expand)</strong></summary>

<br>

| Advancement     | Type        | Criteria                                                       | Hidden |
| --------------- | ----------- | -------------------------------------------------------------- | ------ |
| Second Wind     | Task (Root) | Join the world                                                 | No     |
| Fight Back Up   | Goal        | Kill a valid target while downed to revive yourself            | No     |
| Medic!          | Task        | Revive another player                                          | No     |
| Not Today       | Task        | Be revived by another player                                   | No     |
| FINISH HIM!     | Goal        | Kill a player while they are downed                            | No     |
| Change of Heart | Goal        | Down a player, then revive them before their downed state ends | No     |
| Just in Time    | Challenge   | Revive with 0.1 seconds or less remaining                      | No     |

</details>

## Configuration

Main config file:

* Singleplayer/client: `config/secondwind-common.toml`
* Dedicated server: `<server root>/config/secondwind-common.toml`

Config categories include:

* `secondWind`
* `secondWind.multiplayerRevive`
* `secondWind.killRules`
* `secondWind.clientFeedback`

Notable defaults:

| Setting                     | Default |
| --------------------------- | ------- |
| `downedTimerSeconds`        | `10`    |
| `minimumDownedTimerSeconds` | `3`     |
| `timerPenaltyPerDown`       | `2`     |
| `forceCrawlingPose`         | `true`  |
| `blockHealingWhileDowned`   | `true`  |
| `blockEatingWhileDowned`    | `true`  |
| `cooldownMode`              | `TIMED` |
| `cooldownDurationSeconds`   | `300`   |
| `multiplayerRevive`         | `true`  |
| `allowPlayerKills`          | `true`  |
| `allowPassiveKills`         | `false` |

There is also an in-game NeoForge config screen on the client.

![compatibility](https://cdn.modrinth.com/data/cached_images/1252c11050b7daf8b8621712b58dd1005e7ba982.png)

## Compatibility

* Designed for multiplayer and singleplayer
* Highly configurable for servers and modpacks
* Eventually, will skip straight to 26.1/whatever the newest standard will be for modding.

Second Wind only triggers on supported lethal damage sources. Generic kill commands, invulnerability-bypassing damage, and void damage do not trigger it by default unless explicitly enabled in config.

## Version and Loaders

* NeoForge 1.21.1 - active development
* Fabric 1.21.1 - active development
* Forge - not planned anytime soon, don't ask please :)
* Older Minecraft versions - not planned

![credits & license](https://cdn.modrinth.com/data/cached_images/5fd3ad80e342e6985dd6ebda1f7afd9c48749fce.png)

## Modpacks

You may use this mod in modpacks, videos, servers, and other projects. A link back to the Modrinth page is appreciated.

## Credits

Created by me :D

## License

All Rights Reserved.

Feel free to use this mod in modpacks, videos, etc. Just provide a link back to this page if possible :)

Please don't port this mod without express permission from me.

For any general queries/unlisted questions, DM me on Twitter (@prodbyjvn) / Discord (ijvn).

<div align="center">

<p><strong>⚠ <em>This mod ONLY exists on Modrinth & CurseForge as of May 2026. Any sites hosting this mod outside of Modrinth/CurseForge are not official releases.</em> ⚠</strong></p>

</div>
