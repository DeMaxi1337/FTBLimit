package ru.antigravity.ftblimit.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class TextUtil {

    private TextUtil() {}

    public static String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null && !message.isEmpty()) {
            sender.sendMessage(color(message));
        }
    }

    public static void sendMessages(CommandSender sender, List<String> messages) {
        if (sender == null || messages == null) return;
        for (String msg : messages) {
            sendMessage(sender, msg);
        }
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) return;
        try {
            player.sendActionBar('&', message);
        } catch (NoSuchMethodError | Exception e) {
            try {
                player.sendActionBar(color(message));
            } catch (Exception ignored) {
                player.sendMessage(color(message));
            }
        }
    }

    public static String formatDuration(long totalSeconds, boolean isRussian) {
        if (totalSeconds <= 0) {
            return isRussian ? "0 сек." : "0s";
        }
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append(isRussian ? " ч. " : "h ");
        }
        if (minutes > 0 || hours > 0) {
            sb.append(minutes).append(isRussian ? " мин. " : "m ");
        }
        sb.append(seconds).append(isRussian ? " сек." : "s");
        return sb.toString().trim();
    }
}
