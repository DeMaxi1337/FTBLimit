# FTBLimit
paper 1.21.1 plugin to restrict FTB Ultimine mod

[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://www.oracle.com/java/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://www.minecraft.net/)
[![Platform](https://img.shields.io/badge/Core-Youer%20%7C%20Paper%20%7C%20NeoForge-blue.svg)](https://github.com/FTBTeam/FTB-Ultimine)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A Paper/Youer 1.21.1 plugin designed for hybrid Minecraft servers (Paper + NeoForge) to restrict the **FTB Ultimine** mod with daily limits, dynamic 24-hour lockouts, group permissions (LuckPerms), moderation bans, and bilingual EN/RU support.

---

## ✨ Features

- **Daily Quotas**: Restrict excavations per cycle (e.g. 500 uses/day).
- **Dynamic Lockout**: When a player reaches their limit, FTB Ultimine is locked for 24 hours (configurable in `config.yml`).
- **OP & Bypass**: Server operators and players with `ftblimit.bypass` have unlimited usage.
- **LuckPerms & Groups**: Configure different limits for different ranks (`default`, `vip`, etc.).
- **Bonuses & Personal Limits**: Grant one-time bonuses for today or set permanent custom limits.
- **Moderation Bans**: Completely ban abusive players from using FTB Ultimine.
- **Bilingual (EN / RU)**: Auto-detects client language (`messages_en.yml` by default, `messages_ru.yml` for Russian clients).

---

## 📋 Commands

| Command | Description | Permission |
|---|---|---|
| `/ftbl stats [player]` | View excavation statistics | `ftblimit.stats` |
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

---

## ⚙️ Configuration (`config.yml`)

```yaml
default-language: "en"
auto-detect-player-locale: true

op-bypass: true
bypass-permission: "ftblimit.bypass"

default-group: "default"
lockout-hours: 24

groups:
  default:
    display-name: "&7Default"
    daily-limit: 500
    priority: 1
  vip:
    display-name: "&aVIP"
    daily-limit: 1500
    priority: 10
  unlimited:
    display-name: "&dUnlimited"
    daily-limit: -1
    priority: 100
```

---

## 📥 Installation

1. Place `FTBLimit.jar` into your server's `plugins/` folder.
2. Make sure **FTB Ultimine** is installed in your `mods/` directory.
3. Restart your server.

## 📄 License
Licensed under the [MIT License](LICENSE).
