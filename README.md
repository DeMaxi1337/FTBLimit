# FTBLimit

[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://www.oracle.com/java/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://www.minecraft.net/)
[![Platform](https://img.shields.io/badge/Core-Youer%20%7C%20Paper%20%7C%20NeoForge-blue.svg)](https://github.com/FTBTeam/FTB-Ultimine)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A Bukkit/Paper plugin designed for hybrid Minecraft servers (such as **Youer 1.21.1**, Paper + NeoForge) to regulate and restrict the **FTB Ultimine** mod with daily limits, dynamic 24-hour lockouts, one-time bonuses, permanent personal overrides, and full moderation controls.

---

## ✨ Features

- **Immunity for Operators & Bypasses**:
  - Operators (OPs) and players with `ftblimit.bypass` have unlimited excavations with no delays or restrictions.
- **Configurable Limit Groups**:
  - Assign groups via config (`default` 500 excavations/cycle, `vip`, `premium`, `unlimited`).
  - Native **LuckPerms** integration: automatically detects the player's primary group or permission nodes (`ftblimit.group.<name>`).
  - Manual group assignment: `/ftblimit setgroup <player> <group>`.
- **Dynamic 24-Hour Lockout**:
  - Once a player exhausts their excavation limit (e.g. 500 excavations), an individual 24-hour lockout timer begins.
  - FTB Ultimine is fully blocked for that player until the 24 hours elapse.
  - Informative Action Bar and Chat messages display the remaining lockout time in real-time (`HH:mm:ss`).
- **Lockout Reset Command**:
  - `/ftblimit cleardelay <player>` immediately removes the lockout timer and resets their excavation count, giving them a fresh limit immediately.
- **One-Time Daily Bonuses & Permanent Overrides**:
  - `/ftblimit addbonus <player> <amount>`: Grants extra bonus excavations for the current cycle.
  - `/ftblimit setlimit <player> <amount|reset>`: Sets a permanent personal daily limit override for specific players.
- **Moderation & Bans**:
  - `/ftblimit ban <player> [reason]`: Blocks player from using FTB Ultimine entirely.
  - Displays a warning card with the reason, moderator name, and date when a banned player attempts to excavate (rate-limited to avoid chat spam).
  - `/ftblimit unban <player>`: Unbans the player.
  - `/ftblimit banlist`: Lists all banned players.
- **Detailed Statistics**:
  - `/ftblimit stats [player]` (alias: `/ftbl info`): View cycle usage, limits, bonuses, remaining lockout timer, total excavations, and mined blocks.
- **Bilingual Support (EN / RU)**:
  - Default language: **English** (`messages_en.yml`).
  - Russian localization included (`messages_ru.yml`).
  - Automatic player locale detection via `player.getLocale()` (Russian clients receive Russian messages, all others receive English).
- **Native FTB Ultimine Engine Integration**:
  - Injects dynamic proxies into FTB Ultimine's `RestrictionHandlerRegistry` to suppress client-side block outlines and packets before breaking.
  - Injects into `BlockBreakingRegistry` to accurately track excavation starts, completions, and block drops.
  - Includes fallback `BlockBreakEvent` listener for non-standard environments.

---

## 🚀 Installation

1. Download or build the latest **`FTBLimit.jar`**.
2. Place the JAR into your server's `plugins/` folder.
3. Ensure **FTB Ultimine** (NeoForge 1.21.1) is installed in your `mods/` directory.
4. Restart the server.
5. Configuration files will be generated in `plugins/FTBLimit/`:
   - `config.yml` — Group limits, lockout duration, bypass permissions.
   - `messages_en.yml` — English localization.
   - `messages_ru.yml` — Russian localization.

---

## 📜 Commands & Permissions

| Command | Description | Permission |
|---|---|---|
| `/ftbl stats [player]` | View excavation statistics | `ftblimit.stats` (own) / `ftblimit.stats.others` (others) |
| `/ftbl setgroup <player> <group>` | Assign a limit group | `ftblimit.admin` |
| `/ftbl cleardelay <player>` | Clear 24h lockout & reset counter | `ftblimit.admin` |
| `/ftbl addbonus <player> <amount>` | Add one-time bonus for current cycle | `ftblimit.admin` |
| `/ftbl setlimit <player> <num\|reset>` | Set permanent custom limit | `ftblimit.admin` |
| `/ftbl ban <player> [reason]` | Ban player from FTB Ultimine | `ftblimit.admin` |
| `/ftbl unban <player>` | Unban player | `ftblimit.admin` |
| `/ftbl banlist` | List banned players | `ftblimit.admin` |
| `/ftbl reset <player> [daily\|stats\|all]` | Reset player statistics | `ftblimit.admin` |
| `/ftbl reload` | Reload configuration & language files | `ftblimit.admin` |
| `/ftbl help` | Show help menu | Everyone |

### Permissions

- `ftblimit.admin` — Access to all management commands (default: OP).
- `ftblimit.bypass` — Immune to limits and lockouts (default: OP).
- `ftblimit.stats` — Access to view own stats (default: true).
- `ftblimit.stats.others` — Access to view other players' stats (default: OP).
- `ftblimit.group.<name>` — Assign limit group via permissions (e.g. via LuckPerms).

---

## 🛠 Building from Source

### Prerequisites
- JDK 21 or newer
- Maven (or PowerShell with JDK on Windows)

### Maven Build
```bash
mvn clean package
```
The compiled JAR will be in `target/FTBLimit-1.0.0.jar`.

### Windows PowerShell Build
```powershell
powershell -ExecutionPolicy Bypass -File build.ps1
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
