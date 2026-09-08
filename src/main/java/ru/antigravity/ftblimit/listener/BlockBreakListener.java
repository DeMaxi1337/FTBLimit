package ru.antigravity.ftblimit.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import ru.antigravity.ftblimit.config.ConfigManager;
import ru.antigravity.ftblimit.config.GroupConfig;
import ru.antigravity.ftblimit.data.DataManager;
import ru.antigravity.ftblimit.data.PlayerData;
import ru.antigravity.ftblimit.hook.FTBUltimineHook;
import ru.antigravity.ftblimit.util.TextUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlockBreakListener implements Listener {

    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final FTBUltimineHook ultimineHook;

    private final Map<UUID, Long> lastBreakTick = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> tickBlockCount = new ConcurrentHashMap<>();

    public BlockBreakListener(ConfigManager configManager, DataManager dataManager, FTBUltimineHook ultimineHook) {
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.ultimineHook = ultimineHook;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (dataManager.hasBypass(player)) {
            return;
        }

        PlayerData data = dataManager.getPlayerData(player);

        if (data.isBanned()) {
            if (!ultimineHook.isHooked()) {
                long currentTick = player.getWorld().getFullTime();
                long lastTick = lastBreakTick.getOrDefault(player.getUniqueId(), -1L);
                if (currentTick == lastTick) {
                    event.setCancelled(true);
                    if (data.shouldSendBannedNotification(configManager.getBannedMessageCooldownSeconds())) {
                        List<String> banMessages = configManager.getMessageList(player, "banned-attempt");
                        for (String msg : banMessages) {
                            String formatted = msg.replace("{player}", data.getName())
                                    .replace("{reason}", data.getBanReason())
                                    .replace("{banned_by}", data.getBannedBy())
                                    .replace("{ban_date}", data.getBanDate());
                            TextUtil.sendMessage(player, formatted);
                        }
                    }
                    return;
                }
                lastBreakTick.put(player.getUniqueId(), currentTick);
            }
            return;
        }

        if (!ultimineHook.isHooked()) {
            GroupConfig group = dataManager.resolveGroup(player, data);
            if (group.isUnlimited()) return;

            long currentTick = player.getWorld().getFullTime();
            long lastTick = lastBreakTick.getOrDefault(player.getUniqueId(), -1L);

            if (currentTick == lastTick) {
                int count = tickBlockCount.getOrDefault(player.getUniqueId(), 1) + 1;
                tickBlockCount.put(player.getUniqueId(), count);

                if (count == 2) {
                    if (data.isLockedOut()) {
                        event.setCancelled(true);
                        if (data.shouldSendCooldownNotification(configManager.getCooldownMessageCooldownSeconds())) {
                            boolean isRu = configManager.isRussian(player);
                            String remainingStr = TextUtil.formatDuration(data.getRemainingLockoutSeconds(), isRu);
                            String chatMsg = configManager.getMessage(player, "cooldown-chat", true)
                                    .replace("{remaining}", remainingStr);
                            TextUtil.sendMessage(player, chatMsg);
                        }
                        return;
                    }

                    int limit = data.getEffectiveDailyLimit(group);
                    data.recordExcavation(1);
                    if (limit > 0 && data.getDailyExcavations() >= limit) {
                        long lockoutMillis = configManager.getLockoutHours() * 3600L * 1000L;
                        data.setLockoutUntil(System.currentTimeMillis() + lockoutMillis);
                    }
                    dataManager.save(true);
                } else {
                    data.setTotalBlocks(data.getTotalBlocks() + 1);
                }
            } else {
                lastBreakTick.put(player.getUniqueId(), currentTick);
                tickBlockCount.put(player.getUniqueId(), 1);
            }
        }
    }
}
