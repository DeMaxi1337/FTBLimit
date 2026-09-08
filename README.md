# FTBLimit

A Bukkit/Paper 1.21.1 plugin to restrict the **FTB Ultimine** mod with daily limits, dynamic 24-hour lockouts, group permissions (LuckPerms), moderation bans, and EN/RU language support.

---

## ✨ Features

- **Daily quotas**: Restrict excavations per cycle (e.g. 500 uses/day).
- **Dynamic lockout**: When a player reaches their limit, FTB Ultimine is locked as default for 24 hours (configurable in `config.yml`).
- **OP and Bypass**: Server operators and players with `ftblimit.bypass` have unlimited usage.
- **LuckPerms and Groups**: Configure different limits for different ranks (`default`, `vip`, etc.).
- **Bonuses and personal limits**: Grant one-time bonuses for today or set permanent custom limits.
- **Moderation bans**: Completely ban abusive players from using FTB Ultimine.
- **EN and RU language support:**: Auto-detects client language (`messages_en.yml` by default, `messages_ru.yml` for Russian clients).

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
