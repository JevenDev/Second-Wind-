# Changelog

## 1.1.0 - 2026-07-13

### Added

- Added data-driven Second Wind support for non-player living entities through datapack definitions
- Added managed entity lifecycles with configurable downed timers, penalties, damage behavior, AI disabling, healing restrictions, revives, cooldowns, announcements, and poses
- Added persistence for managed downed entities, including timers and restoration of their previous AI, item pickup, and pose state
- Added external lifecycle adapters, allowing compatibility mods to control an entity's downed and recovery behavior while Second Wind provides tracking and player revive channels
- Added client APIs for accessing tracked entity timers, revive state, distance, and presentation pose
- Added client pose-renderer registration so compatibility mods can provide custom downed presentation poses
- Added built-in `secondwind:sideways`, `secondwind:upright`, and `secondwind:swimming` entity presentation poses
- Added persistent red damage overlays for tracked downed living entities (just like downed players :D)
- Added data-driven player downed and revive announcement pools with priorities, random selection, and server-supplied text
- Added configurable datapack-driven downed announcements for non-player entities
- Added the `forceCrawlingPose` config option, enabled by default, for compatibility with movement and animation mods that may add crawling
- Added a Revivable Wolves example datapack demonstrating conditional, managed Second Wind support for tamed wolves

### Changed

- Bumped the ToucanLib dependency to version 0.3.1
- Changed downed-state tracking, networking, timer rendering, revive channels, and pose presentation to support arbitrary living entities instead of only players
- Changed managed non-player entities to render sideways while downed by default instead of forcing the vanilla swimming pose (some entities looked odd in crawl pose so I defaulted to a sideways pose)
- Changed player and entity announcements to support either server-data text or optional translation keys with fallback text
