package ru.antigravity.ftblimit.config;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    private final Map<String, FileConfiguration> languageConfigs = new HashMap<>();

    private String defaultLanguage;
    private boolean autoDetectPlayerLocale;
    private boolean opBypass;
    private String bypassPermission;
    private boolean delaysEnabled;
    private String defaultGroupId;
    private int lockoutHours;
    private int autosaveIntervalMinutes;
    private boolean actionbarEnabled;
    private boolean showOnExcavate;
    private boolean showOnCooldown;
    private int bannedMessageCooldownSeconds;
    private int cooldownMessageCooldownSeconds;

    private final Map<String, GroupConfig> groups = new LinkedHashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        this.defaultLanguage = config.getString("default-language", "en").toLowerCase();
        this.autoDetectPlayerLocale = config.getBoolean("auto-detect-player-locale", true);
        this.opBypass = config.getBoolean("op-bypass", true);
        this.bypassPermission = config.getString("bypass-permission", "ftblimit.bypass");
        this.delaysEnabled = config.getBoolean("delays-enabled", true);
        this.defaultGroupId = config.getString("default-group", "default");
        this.lockoutHours = Math.max(1, config.getInt("lockout-hours", 24));
        this.autosaveIntervalMinutes = Math.max(1, config.getInt("autosave-interval-minutes", 5));
        this.actionbarEnabled = config.getBoolean("actionbar.enabled", true);
        this.showOnExcavate = config.getBoolean("actionbar.show-on-excavate", true);
        this.showOnCooldown = config.getBoolean("actionbar.show-on-cooldown", true);
        this.bannedMessageCooldownSeconds = Math.max(1, config.getInt("banned-message-cooldown-seconds", 3));
        this.cooldownMessageCooldownSeconds = Math.max(1, config.getInt("cooldown-message-cooldown-seconds", 2));

        loadLanguages();
        loadGroups();
    }

    private void loadLanguages() {
        languageConfigs.clear();
        loadLanguageFile("en");
        loadLanguageFile("ru");
    }

    private void loadLanguageFile(String lang) {
        String fileName = "messages_" + lang + ".yml";
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        InputStream defStream = plugin.getResource(fileName);
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            yaml.setDefaults(defConfig);
        }
        languageConfigs.put(lang, yaml);
    }

    private void loadGroups() {
        groups.clear();
        ConfigurationSection section = config.getConfigurationSection("groups");
        if (section == null) {
            GroupConfig def = new GroupConfig("default", "&7Default", 500, 0, 3, 1);
            groups.put("default", def);
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection groupSec = section.getConfigurationSection(key);
            if (groupSec == null) continue;

            String displayName = groupSec.getString("display-name", key);
            int dailyLimit = groupSec.getInt("daily-limit", 500);
            int delayAfterLimit = groupSec.getInt("delay-after-limit", 0);
            int normalDelay = groupSec.getInt("normal-delay", 0);
            int priority = groupSec.getInt("priority", 1);

            GroupConfig groupConfig = new GroupConfig(key, displayName, dailyLimit, delayAfterLimit, normalDelay, priority);
            groups.put(key.toLowerCase(), groupConfig);
        }

        if (!groups.containsKey(defaultGroupId.toLowerCase())) {
            GroupConfig def = new GroupConfig(defaultGroupId, defaultGroupId, 500, 0, 3, 1);
            groups.put(defaultGroupId.toLowerCase(), def);
        }
    }

    public String resolveLanguage(CommandSender sender) {
        if (autoDetectPlayerLocale && sender instanceof Player player) {
            String locale = player.getLocale().toLowerCase();
            if (locale.startsWith("ru")) {
                return "ru";
            }
        }
        return defaultLanguage;
    }

    public boolean isRussian(CommandSender sender) {
        return "ru".equals(resolveLanguage(sender));
    }

    public FileConfiguration getLanguageConfig(String lang) {
        FileConfiguration cfg = languageConfigs.get(lang != null ? lang.toLowerCase() : defaultLanguage);
        if (cfg == null) {
            cfg = languageConfigs.get("en");
        }
        return cfg;
    }

    public String getPrefix(CommandSender sender) {
        String lang = resolveLanguage(sender);
        FileConfiguration cfg = getLanguageConfig(lang);
        return cfg != null ? cfg.getString("prefix", "&8[&6FTBLimit&8] &r") : "&8[&6FTBLimit&8] &r";
    }

    public String getMessage(CommandSender sender, String path) {
        return getMessage(sender, path, true);
    }

    public String getMessage(CommandSender sender, String path, boolean withPrefix) {
        String lang = resolveLanguage(sender);
        FileConfiguration cfg = getLanguageConfig(lang);
        if (cfg == null) return path;
        String msg = cfg.getString(path);
        if (msg == null) {
            FileConfiguration enCfg = languageConfigs.get("en");
            if (enCfg != null) {
                msg = enCfg.getString(path);
            }
        }
        if (msg == null) return path;
        return (withPrefix ? getPrefix(sender) : "") + msg;
    }

    public List<String> getMessageList(CommandSender sender, String path) {
        String lang = resolveLanguage(sender);
        FileConfiguration cfg = getLanguageConfig(lang);
        if (cfg == null) return Collections.emptyList();
        List<String> list = cfg.getStringList(path);
        if (list.isEmpty()) {
            FileConfiguration enCfg = languageConfigs.get("en");
            if (enCfg != null) {
                list = enCfg.getStringList(path);
            }
        }
        return list;
    }

    public GroupConfig getGroup(String id) {
        if (id == null) return getDefaultGroup();
        return groups.getOrDefault(id.toLowerCase(), getDefaultGroup());
    }

    public GroupConfig getDefaultGroup() {
        return groups.getOrDefault(defaultGroupId.toLowerCase(), new GroupConfig("default", "&7Default", 500, 0, 3, 1));
    }

    public Map<String, GroupConfig> getGroups() {
        return Collections.unmodifiableMap(groups);
    }

    public boolean isOpBypass() {
        return opBypass;
    }

    public String getBypassPermission() {
        return bypassPermission;
    }

    public boolean isDelaysEnabled() {
        return delaysEnabled;
    }

    public void setDelaysEnabled(boolean enabled) {
        this.delaysEnabled = enabled;
        config.set("delays-enabled", enabled);
        plugin.saveConfig();
    }

    public int getLockoutHours() {
        return lockoutHours;
    }

    public int getAutosaveIntervalMinutes() {
        return autosaveIntervalMinutes;
    }

    public boolean isActionbarEnabled() {
        return actionbarEnabled;
    }

    public boolean isShowOnExcavate() {
        return showOnExcavate;
    }

    public boolean isShowOnCooldown() {
        return showOnCooldown;
    }

    public int getBannedMessageCooldownSeconds() {
        return bannedMessageCooldownSeconds;
    }

    public int getCooldownMessageCooldownSeconds() {
        return cooldownMessageCooldownSeconds;
    }
}
