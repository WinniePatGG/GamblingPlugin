# WinniePat's Casino Plugin

A Minecraft casino/gambling plugin for Paper 1.16.3 - 26.2 with Vault economy support.

## Features
- Slot machine with configurable bets, multipliers, daily free spin, and win streak bonus.
- Double or Nothing (configurable entry cost).
- Roulette (configurable bet cost).
- CS:GO-style case opening (configurable cost).
- Daily reward claims.
- Modrinth update checker and downloader.
- Persistent player stats and win leaderboard.
- Particle/sound configuration.
- Unified `/casino` command.

## Commands
All commands are available through the unified command:

```text
/casino slots
/casino double
/casino roulette
/casino case
/casino daily
/casino update
/casino leaderboard
/casino hologram
/casino reload
```

Permission: most commands require players to be operators for admin actions such as `reload`, `update`, and `hologram`.

## Configuration
Edit `config.yml` to customize:

- Bet costs and multipliers
- Entry costs
- Daily reward amount
- Win streak bonus/cap
- Audio/particle names
- Update checking

Changes can be applied with:

```text
/casino reload
```

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
- Holograms are spawned as invisible ArmorStands near the player.
- Update downloads are placed in `plugins/update/` and applied on the next server restart.