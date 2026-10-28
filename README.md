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
/casino leaderboard        aliases: top, lb
/casino hologram           spawn a top-winners hologram where you stand
/casino reload             admin only
/casino update             admin only
```

Aliases: `/games`, `/gamble`. Admin actions (`reload`, `update`, `hologram`) are operator-only.

## Configuration
Everything is configured in `config.yml` (apply changes with `/casino reload`):

- Bet costs and multipliers for every game.
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

## Notes
- House edges by default: Mines 3%, Crash 1%, Coinflip 2.5%, Wheel ~5% — check the numbers in `config.yml` before going live.
- Holograms are spawned as invisible ArmorStands near the player.
- Update downloads are placed in `plugins/update/` and applied on the next server restart.
