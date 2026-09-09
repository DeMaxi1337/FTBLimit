package ru.antigravity.ftblimit.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.antigravity.ftblimit.config.ConfigManager;
import ru.antigravity.ftblimit.data.DataManager;
import ru.antigravity.ftblimit.data.PlayerData;

import java.util.*;

public class FTBLimitTabCompleter implements TabCompleter {

    private final ConfigManager configManager;
    private final DataManager dataManager;

    private static final List<String> ADMIN_SUBCOMMANDS = Arrays.asList(
            "stats", "setgroup", "cleardelay", "enabledelay", "disabledelay", "addbonus", "setlimit", "ban", "unban", "banlist", "reset", "reload", "help"
    );

    private static final List<String> RESET_TYPES = Arrays.asList("daily", "stats", "all");
    private static final List<String> LIMIT_SUGGESTIONS = Arrays.asList("reset", "100", "500", "1000", "2000");
    private static final List<String> BONUS_SUGGESTIONS = Arrays.asList("50", "100", "250", "500");

    public FTBLimitTabCompleter(ConfigManager configManager, DataManager dataManager) {
        this.configManager = configManager;
        this.dataManager = dataManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            boolean isAdmin = sender.hasPermission("ftblimit.admin");
            String prefix = args[0].toLowerCase();
            for (String sub : ADMIN_SUBCOMMANDS) {
                if (!isAdmin && !sub.equals("stats") && !sub.equals("info") && !sub.equals("help")) {
                    continue;
                }
                if (sub.startsWith(prefix)) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            String prefix = args[1].toLowerCase();

            if (sub.equals("unban")) {
                for (PlayerData p : dataManager.getAllPlayers()) {
                    if (p.isBanned() && p.getName() != null && p.getName().toLowerCase().startsWith(prefix)) {
                        completions.add(p.getName());
                    }
                }
                return completions;
            }

            if (sub.equals("stats") || sub.equals("info") || sub.equals("setgroup") || sub.equals("cleardelay")
                    || sub.equals("resetdelay") || sub.equals("enabledelay") || sub.equals("disabledelay")
                    || sub.equals("addbonus") || sub.equals("addlimit")
                    || sub.equals("setlimit") || sub.equals("ban") || sub.equals("reset")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(prefix)) {
                        completions.add(p.getName());
                    }
                }
                for (PlayerData p : dataManager.getAllPlayers()) {
                    if (p.getName() != null && p.getName().toLowerCase().startsWith(prefix) && !completions.contains(p.getName())) {
                        completions.add(p.getName());
                    }
                }
                return completions;
            }
        }

        if (args.length == 3) {
            String prefix = args[2].toLowerCase();

            if (sub.equals("setgroup")) {
                for (String group : configManager.getGroups().keySet()) {
                    if (group.startsWith(prefix)) {
                        completions.add(group);
                    }
                }
                if ("reset".startsWith(prefix)) {
                    completions.add("reset");
                }
                return completions;
            }

            if (sub.equals("reset")) {
                for (String t : RESET_TYPES) {
                    if (t.startsWith(prefix)) {
                        completions.add(t);
                    }
                }
                return completions;
            }

            if (sub.equals("setlimit")) {
                for (String s : LIMIT_SUGGESTIONS) {
                    if (s.startsWith(prefix)) {
                        completions.add(s);
                    }
                }
                return completions;
            }

            if (sub.equals("addbonus") || sub.equals("addlimit")) {
                for (String s : BONUS_SUGGESTIONS) {
                    if (s.startsWith(prefix)) {
                        completions.add(s);
                    }
                }
                return completions;
            }
        }

        return completions;
    }
}
