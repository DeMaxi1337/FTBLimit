package ru.antigravity.ftblimit.data;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.antigravity.ftblimit.config.ConfigManager;
import ru.antigravity.ftblimit.config.GroupConfig;
import ru.antigravity.ftblimit.hook.LuckPermsHook;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final File dataFile;
    private final Map<UUID, PlayerData> players = new ConcurrentHashMap<>();
    private LuckPermsHook luckPermsHook;

    public DataManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataFile = new File(plugin.getDataFolder(), "players.yml");
    }

    public void setLuckPermsHook(LuckPermsHook luckPermsHook) {
        this.luckPermsHook = luckPermsHook;
    }

    public void load() {
        players.clear();
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = yaml.getConfigurationSection("players");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection pSec = section.getConfigurationSection(key);
                if (pSec == null) continue;

                String name = pSec.getString("name", "Unknown");
                PlayerData data = new PlayerData(uuid, name);
                data.setCustomGroup(pSec.getString("group", null));
                data.setDailyExcavations(pSec.getInt("daily-excavations", 0));
                data.setDailyBonus(pSec.getInt("daily-bonus", 0));
                if (pSec.contains("permanent-limit")) {
                    int permLimit = pSec.getInt("permanent-limit", -1);
                    if (permLimit >= 0) {
                        data.setPermanentLimitOverride(permLimit);
                    }
                }
                data.setLastExcavationTime(pSec.getLong("last-excavation-time", 0L));
                data.setLockoutUntil(pSec.getLong("lockout-until", 0L));
                data.setDelayBypass(pSec.getBoolean("delay-bypass", false));
                data.setTotalExcavations(pSec.getLong("total-excavations", 0L));
                data.setTotalBlocks(pSec.getLong("total-blocks", 0L));

                boolean banned = pSec.getBoolean("banned", false);
                if (banned) {
                    data.setBanned(true,
                            pSec.getString("ban-reason", "None"),
                            pSec.getString("banned-by", "Console"),
                            pSec.getString("ban-date", "Unknown"));
                }
                data.checkLockoutExpiration();

                players.put(uuid, data);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load player data for " + key + ": " + e.getMessage());
            }
        }
    }

    public void save(boolean async) {
        Runnable saveTask = () -> {
            YamlConfiguration yaml = new YamlConfiguration();
            ConfigurationSection root = yaml.createSection("players");

            for (PlayerData data : players.values()) {
                ConfigurationSection pSec = root.createSection(data.getUuid().toString());
                pSec.set("name", data.getName());
                if (data.getCustomGroup() != null) {
                    pSec.set("group", data.getCustomGroup());
                }
                pSec.set("daily-excavations", data.getDailyExcavations());
                pSec.set("daily-bonus", data.getDailyBonus());
                if (data.hasPermanentLimitOverride()) {
                    pSec.set("permanent-limit", data.getPermanentLimitOverride());
                }
                pSec.set("last-excavation-time", data.getLastExcavationTime());
                pSec.set("lockout-until", data.getLockoutUntil());
                pSec.set("delay-bypass", data.isDelayBypass());
                pSec.set("total-excavations", data.getTotalExcavations());
                pSec.set("total-blocks", data.getTotalBlocks());
                pSec.set("banned", data.isBanned());
                if (data.isBanned()) {
                    pSec.set("ban-reason", data.getBanReason());
                    pSec.set("banned-by", data.getBannedBy());
                    pSec.set("ban-date", data.getBanDate());
                }
            }

            try {
                yaml.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save players.yml: " + e.getMessage());
            }
        };

        if (async && plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, saveTask);
        } else {
            saveTask.run();
        }
    }

    public PlayerData getPlayerData(UUID uuid, String name) {
        PlayerData data = players.computeIfAbsent(uuid, u -> new PlayerData(u, name));
        if (name != null && (data.getName() == null || data.getName().equals("Unknown"))) {
            data.setName(name);
        }
        data.checkLockoutExpiration();
        return data;
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId(), player.getName());
    }

    public PlayerData getPlayerDataIfPresent(UUID uuid) {
        PlayerData data = players.get(uuid);
        if (data != null) {
            data.checkLockoutExpiration();
        }
        return data;
    }

    public PlayerData findPlayerDataByName(String name) {
        if (name == null) return null;
        for (PlayerData data : players.values()) {
            if (name.equalsIgnoreCase(data.getName())) {
                data.checkLockoutExpiration();
                return data;
            }
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return getPlayerData(online);
        }
        return null;
    }

    public Collection<PlayerData> getAllPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    public GroupConfig resolveGroup(Player player, PlayerData data) {
        if (data != null && data.getCustomGroup() != null) {
            GroupConfig cfg = configManager.getGroup(data.getCustomGroup());
            if (cfg != null) return cfg;
        }

        if (player == null) {
            return configManager.getDefaultGroup();
        }

        if (luckPermsHook != null && luckPermsHook.isAvailable()) {
            GroupConfig lpGroup = luckPermsHook.resolvePlayerGroup(player, configManager);
            if (lpGroup != null) return lpGroup;
        }

        GroupConfig highest = null;
        for (GroupConfig group : configManager.getGroups().values()) {
            if (player.hasPermission("ftblimit.group." + group.getId())) {
                if (highest == null || group.getPriority() > highest.getPriority()) {
                    highest = group;
                }
            }
        }

        return highest != null ? highest : configManager.getDefaultGroup();
    }

    public boolean hasBypass(Player player) {
        if (player == null) return false;
        if (configManager.isOpBypass() && player.isOp()) {
            return true;
        }
        return player.hasPermission(configManager.getBypassPermission());
    }

    public void checkAllLockouts() {
        for (PlayerData data : players.values()) {
            data.checkLockoutExpiration();
        }
    }
}
