package ru.antigravity.ftblimit;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.antigravity.ftblimit.command.FTBLimitCommand;
import ru.antigravity.ftblimit.command.FTBLimitTabCompleter;
import ru.antigravity.ftblimit.config.ConfigManager;
import ru.antigravity.ftblimit.data.DataManager;
import ru.antigravity.ftblimit.hook.FTBUltimineHook;
import ru.antigravity.ftblimit.hook.LuckPermsHook;
import ru.antigravity.ftblimit.listener.BlockBreakListener;
import ru.antigravity.ftblimit.listener.PlayerListener;

public final class FTBLimitPlugin extends JavaPlugin {

    private static FTBLimitPlugin instance;

    private ConfigManager configManager;
    private DataManager dataManager;
    private LuckPermsHook luckPermsHook;
    private FTBUltimineHook ultimineHook;

    private BukkitTask autosaveTask;
    private BukkitTask lockoutCheckTask;

    public static FTBLimitPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("================================================");
        getLogger().info("  FTBLimit v" + getDescription().getVersion() + " loading...");
        getLogger().info("  Server software: " + Bukkit.getName() + " " + Bukkit.getVersion());
        getLogger().info("================================================");

        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.dataManager = new DataManager(this, configManager);
        this.dataManager.load();

        this.luckPermsHook = new LuckPermsHook();
        if (luckPermsHook.isAvailable()) {
            getLogger().info("LuckPerms detected! Group hook enabled.");
        }
        this.dataManager.setLuckPermsHook(luckPermsHook);

        this.ultimineHook = new FTBUltimineHook(this, configManager, dataManager);
        boolean hooked = this.ultimineHook.initHook();
        if (hooked) {
            getLogger().info("Direct integration with FTB Ultimine activated!");
        } else {
            getLogger().warning("FTB Ultimine API not directly available, event fallback mode enabled.");
        }

        Bukkit.getPluginManager().registerEvents(new BlockBreakListener(configManager, dataManager, ultimineHook), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(dataManager), this);

        PluginCommand cmd = getCommand("ftblimit");
        if (cmd != null) {
            FTBLimitCommand executor = new FTBLimitCommand(this, configManager, dataManager);
            FTBLimitTabCompleter completer = new FTBLimitTabCompleter(configManager, dataManager);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(completer);
        }

        startTasks();

        getLogger().info("FTBLimit enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
        }
        if (lockoutCheckTask != null) {
            lockoutCheckTask.cancel();
        }

        if (dataManager != null) {
            getLogger().info("Saving player data...");
            dataManager.save(false);
        }

        getLogger().info("FTBLimit disabled.");
        instance = null;
    }

    private void startTasks() {
        lockoutCheckTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (dataManager != null) {
                dataManager.checkAllLockouts();
            }
        }, 600L, 600L);

        long autosaveTicks = configManager.getAutosaveIntervalMinutes() * 60L * 20L;
        autosaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (dataManager != null) {
                dataManager.save(false);
            }
        }, autosaveTicks, autosaveTicks);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public FTBUltimineHook getUltimineHook() {
        return ultimineHook;
    }

    public LuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }
}
