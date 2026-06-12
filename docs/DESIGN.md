# OakStory — Game Design

A small 2D forest platformer with a crafting mechanic, built with libGDX for the
Mobile/Game Programming final assignment. Target: Android API 30, Pixel 2 (1080x1920).

## Concept

The player is a small adventurer in a forest. Run and jump through the world,
gather resources (wood, stone, fibre), and use a simple crafting menu to make
items that let you progress — for example a **Key** to open the door to the cave
level, or a **Bridge** to cross a gap. Avoid hazards and patrolling enemies.
Collect acorns for score. Fall in a pit or get hit and you die, then restart.

Target audience: casual players who enjoy light platformers (ages ~10+).

## Required features (assignment checklist)

| Requirement                  | How OakStory does it                                  |
|------------------------------|------------------------------------------------------|
| TileMaps                     | Tiled `.tmx` maps, 2 levels                           |
| Camera follows player        | OrthographicCamera that lerps toward the player       |
| Movement in a direction      | Left / right walking + jump                           |
| Collision detection          | Manual tile-based AABB vs. a Tiled collision layer    |
| At least two levels          | Forest (ground) + Cave                                |
| Sound effects                | jump, pickup, craft, hurt, death                      |
| Move between the two levels  | Craft a Key, open the door, load the cave level       |
| Start screen                 | Title screen with Play / Credits                      |
| Player death                 | Pit fall, enemy contact, hazard                       |
| Restart option               | Game-over screen -> restart current level             |
| **Crafting**                 | Collect materials -> craft menu -> Key / Bridge        |

## Bonus features we aim for

Enemies with basic patrol AI, background music, score + lives, interactive
elements (pickups, door, craftable bridge), functioning menus, and multi-
resolution support via a scaling viewport. A credits screen lists all asset
attributions.

## Crafting design

- Pickups scattered in the levels: **Wood**, **Stone**, **Fibre**.
- Press **C** to open the crafting menu. Recipes:
  - Wood x3            -> **Bridge** (placed to cross a gap)
  - Wood x2 + Stone x2 -> **Key** (opens the door to the cave level)
- Crafting gates progression, which also satisfies the "move between levels"
  requirement: you cannot reach level 2 without crafting the Key.

## Technical decisions

- **libGDX 1.13.x**, modules: `core` (game logic), `lwjgl3` (desktop, for fast
  development testing) and `android` (the required submission target).
- **Manual AABB collision** against a Tiled collision layer rather than Box2D.
  Reason: for a tight deadline this is the proven, stable libGDX platformer
  pattern (no physics jitter), and it still demonstrates collision-detection
  understanding for marking. Box2D may be added later if time allows.
- **Assets:** free / CC0-compatible sources only, each documented in
  `docs/ASSETS.md` and shown on an in-game credits screen.

## Screens / states

`TitleScreen` -> `GameScreen` (per level) -> `GameOverScreen` -> back to game.
A `CreditsScreen` is reachable from the title.
