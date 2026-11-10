# WinniePat's Casino Plugin

A Minecraft casino/gambling plugin for Paper 1.16.3 - 26.2 with Vault economy support.

## Features
- **9 games** in one plugin, all playable from the new Casino Lobby menu:
  - Slot machine with configurable bets, multipliers, daily free spin and win streak bonus.
  - **Blackjack** vs the dealer — Hit / Double / Stand, animated dealer draws, configurable payouts, forced stand if you close mid-hand.
  - **Mines** (Stake-style 5x5 grid) — reveal gems, dodge mines, cash out any time, fair house-edged multipliers.
  - **Crash** — ride a rising multiplier and cash out before it crashes; auto cash-out if you leave.
  - **Wheel of Fortune** — weighted segments spread across a spinning ring with deceleration.
  - **Coinflip** — heads or tails with a flipping animation.
  - Roulette (red/green/black on a spinning ring).
  - Double or Nothing.
  - CS:GO-style case opening.
- **Casino Lobby**: `/casino` with no arguments opens a clickable menu with every game, your balance, the daily reward and the leaderboard.
- **Station blocks**: right-click configurable block types in the world to open games (right-click a structure block = roulette table, as before). Crouch to use the block normally; operators may break stations.
- Tab completion on `/casino` and aliases `/games`, `/gamble`.
- Session guard: you can't open a second interactive game while one is running.
- Daily reward claims.
- Modrinth update checker and downloader.
- Persistent player stats and win leaderboard.
- Per-player casino history with games played, total wagered, total returned, net profit/loss and largest win.
- Particle/sound configuration.

## Commands
```text
/casino                    opens the Casino Lobby menu
/casino menu               same thing
/casino slots
/casino roulette
/casino blackjack
/casino mines
/casino crash
/casino wheel
/casino coinflip
/casino double
/casino case
/casino daily
/casino stats              show your casino history and net profit/loss
/casino stats <player>     show another player's casino history
/casino leaderboard        aliases: top, lb
/casino hologram           spawn a top-winners hologram where you stand
/casino reload             admin only
/casino update             admin only
```

Aliases: `/games`, `/gamble`. Admin actions (`reload`, `update`, `hologram`) are operator-only.

## Configuration
Everything is configured in `config.yml` (apply changes with `/casino reload`):

- Shared bet-selection amounts are configured under `betting.amounts` (positive finite numbers); invalid or empty values log a warning and use the built-in defaults. Changes apply with `/casino reload`.
- Game-specific `bet-cost`/`entry-cost` values remain the default payout scale for slots, cases and game-specific displays.
- Blackjack payout tiers (`win`, `blackjack`, `push`) and soft-17 behavior.
- Mines count (1-24) and house edge.
- Crash house edge, growth speed and maximum multiplier.
- Coinflip payout.
- Wheel segments: multiplier, relative weight and color per tier.
- Station blocks under `stations.<game>: BLOCK_MATERIAL` (empty string disables).
- Daily reward amount, audio and particles.

## Requirements
- Paper/Spigot 1.16.3 - 26.2
- Vault
- An economy plugin (EssentialsX, etc.)
- Java 11+ recommended (README build target uses Java 21)

## Build

```bash
./gradlew build
```

Run tests:

```bash
./gradlew test
```

## Resource pack

The repository includes a pixel-art casino resource pack under `resourcepack/`.
It adds custom icons to the lobby and wager screens and renders every blackjack
card as a distinct card face. The plugin keeps vanilla item fallbacks when a
player has not loaded the pack.

Regenerate the assets after changing the pack generator:

```bash
python3 tools/generate_resource_pack.py
./gradlew resourcePack
```

The ZIP is written to `build/resource-pack/gambling-plugin-resource-pack.zip`.
Host that ZIP from a public HTTPS URL and configure it as the server resource
pack for clients.

## Notes
- House edges by default: Mines 3%, Crash 1%, Coinflip 2.5%, Wheel ~5% — check the numbers in `config.yml` before going live.
- Holograms are spawned as invisible ArmorStands near the player.
- Update downloads are placed in `plugins/update/` and applied on the next server restart.
