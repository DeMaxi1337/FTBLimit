package ru.antigravity.ftblimit.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.antigravity.ftblimit.config.ConfigManager;
import ru.antigravity.ftblimit.config.GroupConfig;
import ru.antigravity.ftblimit.data.DataManager;
import ru.antigravity.ftblimit.data.PlayerData;
import ru.antigravity.ftblimit.util.TextUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FTBLimitCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public FTBLimitCommand(JavaPlugin plugin, ConfigManager configManager, DataManager dataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "stats":
            case "info":
                return handleStats(sender, args);

            case "setgroup":
                return handleSetGroup(sender, args);

            case "cleardelay":
            case "resetdelay":
                return handleClearDelay(sender, args);

            case "enabledelay":
                return handleEnableDelay(sender, args);

            case "disabledelay":
                return handleDisableDelay(sender, args);

            case "addbonus":
            case "addlimit":
                return handleAddBonus(sender, args);

            case "setlimit":
                return handleSetLimit(sender, args);

            case "ban":
                return handleBan(sender, args);

            case "unban":
                return handleUnban(sender, args);

            case "banlist":
                return handleBanList(sender, args);

            case "reset":
                return handleReset(sender, args);

            case "reload":
                return handleReload(sender);

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        List<String> helpList = configManager.getMessageList(sender, "help");
        TextUtil.sendMessages(sender, helpList);
    }

    private boolean handleStats(CommandSender sender, String[] args) {
        PlayerData data;
        Player targetPlayer = null;

        if (args.length > 1) {
            if (!sender.hasPermission("ftblimit.stats.others") && !sender.hasPermission("ftblimit.admin")) {
                TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.no-permission"));
                return true;
            }
            String targetName = args[1];
            targetPlayer = Bukkit.getPlayerExact(targetName);
            data = dataManager.findPlayerDataByName(targetName);
            if (data == null) {
                TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.player-not-found").replace("{player}", targetName));
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.only-players"));
                return true;
            }
            if (!sender.hasPermission("ftblimit.stats")) {
                TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.no-permission"));
                return true;
            }
            targetPlayer = player;
            data = dataManager.getPlayerData(player);
        }

        GroupConfig group = dataManager.resolveGroup(targetPlayer, data);
        boolean isBypass = targetPlayer != null && dataManager.hasBypass(targetPlayer);
        boolean isRu = configManager.isRussian(sender);

        int limit = data.getEffectiveDailyLimit(group);
        String limitStr = limit < 0 ? "∞" : String.valueOf(limit);
        String bonusStr = data.getDailyBonus() > 0 ? configManager.getMessage(sender, "status.bonus-format", false).replace("{bonus}", String.valueOf(data.getDailyBonus())) : "";

        String cooldownStatus;
        if (data.isBanned()) {
            cooldownStatus = configManager.getMessage(sender, "status.banned", false).replace("{reason}", data.getBanReason());
        } else if (isBypass || group.isUnlimited()) {
            cooldownStatus = configManager.getMessage(sender, "status.unlimited", false);
        } else if (data.isLockedOut()) {
            String remainingStr = TextUtil.formatDuration(data.getRemainingLockoutSeconds(), isRu);
            cooldownStatus = configManager.getMessage(sender, "status.active-delay", false).replace("{remaining}", remainingStr);
        } else {
            cooldownStatus = configManager.getMessage(sender, "status.ready", false);
        }

        String banStatus = data.isBanned() ?
                configManager.getMessage(sender, "status.banned", false).replace("{reason}", data.getBanReason()) :
                configManager.getMessage(sender, "status.not-banned", false);

        String modeStr = data.hasPermanentLimitOverride() ?
                configManager.getMessage(sender, "status.mode-custom", false).replace("{amount}", String.valueOf(data.getPermanentLimitOverride())) :
                configManager.getMessage(sender, "status.mode-group", false);

        String delayStatus;
        if (!configManager.isDelaysEnabled()) {
            delayStatus = configManager.getMessage(sender, "status.delay-disabled-global", false);
        } else if (data.isDelayBypass()) {
            delayStatus = configManager.getMessage(sender, "status.delay-disabled-player", false);
        } else {
            delayStatus = configManager.getMessage(sender, "status.delay-enabled", false).replace("{seconds}", String.valueOf(group.getNormalDelay()));
        }

        TextUtil.sendMessage(sender, configManager.getMessage(sender, "stats.header", false).replace("{player}", data.getName()));
        TextUtil.sendMessage(sender, configManager.getMessage(sender, "stats.group", false)
                .replace("{group}", group.getDisplayName())
                .replace("{priority}", String.valueOf(group.getPriority())));
        TextUtil.sendMessage(sender, configManager.getMessage(sender, "stats.limit-mode", false).replace("{mode}", modeStr));
        TextUtil.sendMessage(sender, configManager.getMessage(sender, "stats.daily", false)
                .replace("{used}", String.valueOf(data.getDailyExcavations()))
                .replace("{limit}", limitStr)
                .replace("{bonus}", bonusStr));
        TextUtil.sendMessage(sender, configManager.getMessage(sender, "stats.cooldown", false).replace("{cooldown_status}", cooldownStatus));
        TextUtil.sendMessage(sender, configManager.getMessage(sender, "stats.delay-status", false).replace("{delay_status}", delayStatus));
        TextUtil.sendMessage(sender, configManager.getMessage(sender, "stats.total", false)
                .replace("{total_excavations}", String.valueOf(data.getTotalExcavations()))
                .replace("{total_blocks}", String.valueOf(data.getTotalBlocks())));
        TextUtil.sendMessage(sender, configManager.getMessage(sender, "stats.ban-status", false).replace("{ban_status}", banStatus));
        TextUtil.sendMessage(sender, configManager.getMessage(sender, "stats.footer", false));

        return true;
    }

    private boolean handleSetGroup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ftblimit.admin")) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.no-permission"));
            return true;
        }
        if (args.length < 3) {
            TextUtil.sendMessage(sender, "&cUsage: /ftblimit setgroup <player> <group>");
            return true;
        }

        String targetName = args[1];
        String groupName = args[2].toLowerCase();

        if (!configManager.getGroups().containsKey(groupName) && !groupName.equals("default") && !groupName.equals("reset")) {
            String available = String.join(", ", configManager.getGroups().keySet());
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.unknown-group")
                    .replace("{group}", groupName)
                    .replace("{available_groups}", available));
            return true;
        }

        PlayerData data = dataManager.findPlayerDataByName(targetName);
        if (data == null) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.player-not-found").replace("{player}", targetName));
            return true;
        }

        if (groupName.equals("reset")) {
            data.setCustomGroup(null);
        } else {
            data.setCustomGroup(groupName);
        }
        dataManager.save(true);

        GroupConfig targetGroup = configManager.getGroup(data.getCustomGroup());
        TextUtil.sendMessage(sender, configManager.getMessage(sender, "group-set")
                .replace("{player}", data.getName())
                .replace("{group}", targetGroup.getDisplayName()));
        return true;
    }

    private boolean handleClearDelay(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ftblimit.admin")) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.no-permission"));
            return true;
        }
        if (args.length < 2) {
            TextUtil.sendMessage(sender, "&cUsage: /ftblimit cleardelay <player>");
            return true;
        }

        String targetName = args[1];
        PlayerData data = dataManager.findPlayerDataByName(targetName);
        if (data == null) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.player-not-found").replace("{player}", targetName));
            return true;
        }

        data.clearDelay();
        dataManager.save(true);

        TextUtil.sendMessage(sender, configManager.getMessage(sender, "delay-cleared").replace("{player}", data.getName()));

        Player onlineTarget = Bukkit.getPlayerExact(data.getName());
        if (onlineTarget != null && !onlineTarget.equals(sender)) {
            TextUtil.sendMessage(onlineTarget, configManager.getMessage(onlineTarget, "delay-cleared-target"));
        }

        return true;
    }

    private boolean handleAddBonus(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ftblimit.admin")) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.no-permission"));
            return true;
        }
        if (args.length < 3) {
            TextUtil.sendMessage(sender, "&cUsage: /ftblimit addbonus <player> <amount>");
            return true;
        }

        String targetName = args[1];
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.invalid-number"));
            return true;
        }

        PlayerData data = dataManager.findPlayerDataByName(targetName);
        if (data == null) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.player-not-found").replace("{player}", targetName));
            return true;
        }

        data.addDailyBonus(amount);
        dataManager.save(true);

        Player onlineTarget = Bukkit.getPlayerExact(data.getName());
        GroupConfig group = dataManager.resolveGroup(onlineTarget, data);
        int newLimit = data.getEffectiveDailyLimit(group);

        TextUtil.sendMessage(sender, configManager.getMessage(sender, "bonus-added")
                .replace("{player}", data.getName())
                .replace("{amount}", String.valueOf(amount))
                .replace("{new_limit}", newLimit < 0 ? "∞" : String.valueOf(newLimit)));
        return true;
    }

    private boolean handleSetLimit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ftblimit.admin")) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.no-permission"));
            return true;
        }
        if (args.length < 3) {
            TextUtil.sendMessage(sender, "&cUsage: /ftblimit setlimit <player> <amount|reset>");
            return true;
        }

        String targetName = args[1];
        PlayerData data = dataManager.findPlayerDataByName(targetName);
        if (data == null) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.player-not-found").replace("{player}", targetName));
            return true;
        }

        String val = args[2];
        if (val.equalsIgnoreCase("reset") || val.equalsIgnoreCase("default")) {
            data.setPermanentLimitOverride(null);
            dataManager.save(true);

            Player onlineTarget = Bukkit.getPlayerExact(data.getName());
            GroupConfig group = dataManager.resolveGroup(onlineTarget, data);
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "limit-reset")
                    .replace("{player}", data.getName())
                    .replace("{group}", group.getDisplayName()));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(val);
            if (amount < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.invalid-number"));
            return true;
        }

        data.setPermanentLimitOverride(amount);
        dataManager.save(true);

        TextUtil.sendMessage(sender, configManager.getMessage(sender, "limit-set")
                .replace("{player}", data.getName())
                .replace("{amount}", String.valueOf(amount)));
        return true;
    }

    private boolean handleBan(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ftblimit.admin")) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.no-permission"));
            return true;
        }
        if (args.length < 2) {
            TextUtil.sendMessage(sender, "&cUsage: /ftblimit ban <player> [reason]");
            return true;
        }

        String targetName = args[1];
        PlayerData data = dataManager.findPlayerDataByName(targetName);
        if (data == null) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.player-not-found").replace("{player}", targetName));
            return true;
        }

        if (data.isBanned()) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.already-banned"));
            return true;
        }

        String reason = configManager.isRussian(sender) ? "Нарушение правил" : "Rule violation";
        if (args.length > 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (i > 2) sb.append(" ");
                sb.append(args[i]);
            }
            reason = sb.toString();
        }

        String staff = sender.getName();
        String date = LocalDateTime.now().format(DATE_FORMATTER);

        data.setBanned(true, reason, staff, date);
        dataManager.save(true);

        TextUtil.sendMessage(sender, configManager.getMessage(sender, "player-banned")
                .replace("{player}", data.getName())
                .replace("{reason}", reason));
        return true;
    }

    private boolean handleUnban(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ftblimit.admin")) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.no-permission"));
            return true;
        }
        if (args.length < 2) {
            TextUtil.sendMessage(sender, "&cUsage: /ftblimit unban <player>");
            return true;
        }

        String targetName = args[1];
        PlayerData data = dataManager.findPlayerDataByName(targetName);
        if (data == null) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.player-not-found").replace("{player}", targetName));
            return true;
        }

        if (!data.isBanned()) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.not-banned"));
            return true;
        }

        data.setBanned(false, null, null, null);
        dataManager.save(true);

        TextUtil.sendMessage(sender, configManager.getMessage(sender, "player-unbanned").replace("{player}", data.getName()));
        return true;
    }

    private boolean handleBanList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ftblimit.admin")) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.no-permission"));
            return true;
        }

        List<PlayerData> bannedList = new ArrayList<>();
        for (PlayerData data : dataManager.getAllPlayers()) {
            if (data.isBanned()) {
                bannedList.add(data);
            }
        }

        if (bannedList.isEmpty()) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "banlist-empty"));
            return true;
        }

        TextUtil.sendMessage(sender, configManager.getMessage(sender, "banlist-header", false));
        for (PlayerData b : bannedList) {
            String entry = configManager.getMessage(sender, "banlist-entry", false)
                    .replace("{player}", b.getName())
                    .replace("{reason}", b.getBanReason())
                    .replace("{banned_by}", b.getBannedBy())
                    .replace("{date}", b.getBanDate());
            TextUtil.sendMessage(sender, entry);
        }
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ftblimit.admin")) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.no-permission"));
            return true;
        }
        if (args.length < 2) {
            TextUtil.sendMessage(sender, "&cUsage: /ftblimit reset <player> [daily|stats|all]");
            return true;
        }

        String targetName = args[1];
        PlayerData data = dataManager.findPlayerDataByName(targetName);
        if (data == null) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.player-not-found").replace("{player}", targetName));
            return true;
        }

        String type = args.length > 2 ? args[2].toLowerCase() : "daily";

        if (type.equals("daily")) {
            data.setDailyExcavations(0);
            data.setDailyBonus(0);
            data.clearDelay();
        } else if (type.equals("stats")) {
            data.setTotalExcavations(0);
            data.setTotalBlocks(0);
        } else if (type.equals("all")) {
            data.setDailyExcavations(0);
            data.setDailyBonus(0);
            data.setPermanentLimitOverride(null);
            data.setTotalExcavations(0);
            data.setTotalBlocks(0);
            data.clearDelay();
            data.setCustomGroup(null);
        } else {
            TextUtil.sendMessage(sender, "&cInvalid reset type: use daily, stats or all.");
            return true;
        }

        dataManager.save(true);
        TextUtil.sendMessage(sender, configManager.getMessage(sender, "player-reset")
                .replace("{player}", data.getName())
                .replace("{type}", type));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("ftblimit.admin")) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.no-permission"));
            return true;
        }

        configManager.load();
        TextUtil.sendMessage(sender, configManager.getMessage(sender, "reload-success"));
        return true;
    }

    private boolean handleEnableDelay(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ftblimit.admin")) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.no-permission"));
            return true;
        }

        if (args.length == 1) {
            configManager.setDelaysEnabled(true);
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "delay-enabled-global"));
            return true;
        }

        String targetName = args[1];
        PlayerData data = dataManager.findPlayerDataByName(targetName);
        if (data == null) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.player-not-found").replace("{player}", targetName));
            return true;
        }

        data.setDelayBypass(false);
        dataManager.save(true);

        TextUtil.sendMessage(sender, configManager.getMessage(sender, "delay-enabled-player").replace("{player}", data.getName()));

        Player onlineTarget = Bukkit.getPlayerExact(data.getName());
        if (onlineTarget != null && !onlineTarget.equals(sender)) {
            TextUtil.sendMessage(onlineTarget, configManager.getMessage(onlineTarget, "delay-enabled-target"));
        }

        return true;
    }

    private boolean handleDisableDelay(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ftblimit.admin")) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.no-permission"));
            return true;
        }

        if (args.length == 1) {
            configManager.setDelaysEnabled(false);
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "delay-disabled-global"));
            return true;
        }

        String targetName = args[1];
        PlayerData data = dataManager.findPlayerDataByName(targetName);
        if (data == null) {
            TextUtil.sendMessage(sender, configManager.getMessage(sender, "errors.player-not-found").replace("{player}", targetName));
            return true;
        }

        data.setDelayBypass(true);
        dataManager.save(true);

        TextUtil.sendMessage(sender, configManager.getMessage(sender, "delay-disabled-player").replace("{player}", data.getName()));

        Player onlineTarget = Bukkit.getPlayerExact(data.getName());
        if (onlineTarget != null && !onlineTarget.equals(sender)) {
            TextUtil.sendMessage(onlineTarget, configManager.getMessage(onlineTarget, "delay-disabled-target"));
        }

        return true;
    }
}
