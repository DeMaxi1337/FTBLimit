package ru.antigravity.ftblimit.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.antigravity.ftblimit.config.ConfigManager;
import ru.antigravity.ftblimit.config.GroupConfig;
import ru.antigravity.ftblimit.data.DataManager;
import ru.antigravity.ftblimit.data.PlayerData;
import ru.antigravity.ftblimit.util.TextUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FTBUltimineHook {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;

    private boolean hooked = false;

    private final Map<UUID, Integer> activeExcavationBlocks = new ConcurrentHashMap<>();

    private Class<?> canUltimineResultClass;
    private Object resultNoPermission;
    private Object resultOnCooldown;
    private Object resultAllowed;
    private Object resultOther;
    private Object passResult;

    public FTBUltimineHook(JavaPlugin plugin, ConfigManager configManager, DataManager dataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataManager = dataManager;
    }

    public boolean initHook() {
        try {
            Class<?> restrictionHandlerClass = Class.forName("dev.ftb.mods.ftbultimine.api.restriction.RestrictionHandler");
            Class<?> restrictionRegistryClass = Class.forName("dev.ftb.mods.ftbultimine.RestrictionHandlerRegistry");
            Class<?> blockBreakHandlerClass = Class.forName("dev.ftb.mods.ftbultimine.api.blockbreaking.BlockBreakHandler");
            Class<?> blockBreakingRegistryClass = Class.forName("dev.ftb.mods.ftbultimine.BlockBreakingRegistry");
            this.canUltimineResultClass = Class.forName("dev.ftb.mods.ftbultimine.api.util.CanUltimineResult");

            try {
                this.resultNoPermission = canUltimineResultClass.getField("NO_PERMISSION").get(null);
                this.resultOnCooldown = canUltimineResultClass.getField("ON_COOLDOWN").get(null);
                this.resultAllowed = canUltimineResultClass.getField("ALLOWED").get(null);
                this.resultOther = canUltimineResultClass.getField("OTHER_RESTRICTION").get(null);
            } catch (Throwable ignored) {}

            try {
                Class<?> resultEnumClass = Class.forName("dev.ftb.mods.ftbultimine.api.blockbreaking.BlockBreakHandler$Result");
                Method valueOf = resultEnumClass.getMethod("valueOf", String.class);
                this.passResult = valueOf.invoke(null, "PASS");
            } catch (Throwable ignored) {}

            Object restrictionProxy = Proxy.newProxyInstance(
                    restrictionHandlerClass.getClassLoader(),
                    new Class<?>[]{restrictionHandlerClass},
                    new RestrictionHandlerInvocationHandler()
            );

            Field instField = restrictionRegistryClass.getField("INSTANCE");
            Object registryInstance = instField.get(null);
            Method registerMethod = restrictionRegistryClass.getMethod("register", restrictionHandlerClass);
            registerMethod.invoke(registryInstance, restrictionProxy);

            Object breakHandlerProxy = Proxy.newProxyInstance(
                    blockBreakHandlerClass.getClassLoader(),
                    new Class<?>[]{blockBreakHandlerClass},
                    new BlockBreakHandlerInvocationHandler()
            );

            Method getBlockBreakingInst = blockBreakingRegistryClass.getMethod("getInstance");
            Object blockBreakingInstance = getBlockBreakingInst.invoke(null);
            Method registerBreakHandler = blockBreakingRegistryClass.getMethod("registerHandler", blockBreakHandlerClass);
            registerBreakHandler.invoke(blockBreakingInstance, breakHandlerProxy);

            this.hooked = true;
            plugin.getLogger().info("Successfully hooked into FTB Ultimine API (RestrictionHandler & BlockBreakHandler).");
            return true;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("FTB Ultimine classes not found on server classpath. Falling back to Bukkit event interception.");
            return false;
        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to hook into FTB Ultimine: " + t.getMessage());
            return false;
        }
    }

    public boolean isHooked() {
        return hooked;
    }

    private UUID getPlayerUuid(Object playerObj) {
        if (playerObj == null) return null;
        try {
            Method getUUIDMethod = playerObj.getClass().getMethod("getUUID");
            return (UUID) getUUIDMethod.invoke(playerObj);
        } catch (Throwable t) {
            try {
                Method getProfile = playerObj.getClass().getMethod("getGameProfile");
                Object profile = getProfile.invoke(playerObj);
                Method getId = profile.getClass().getMethod("getId");
                return (UUID) getId.invoke(profile);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private String getPlayerName(Object playerObj) {
        if (playerObj == null) return "Unknown";
        try {
            Method getScoreboardName = playerObj.getClass().getMethod("getScoreboardName");
            return (String) getScoreboardName.invoke(playerObj);
        } catch (Throwable t) {
            return "Unknown";
        }
    }

    private class RestrictionHandlerInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("canUltimine".equals(name) && args != null && args.length >= 1) {
                return handleCanUltimine(args[0]);
            }
            if ("ultimineBlockReason".equals(name) && args != null && args.length >= 1) {
                return handleUltimineBlockReason(args[0]);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(name)) {
                return "FTBLimitRestrictionProxy";
            }
            return false;
        }
    }

    private boolean handleCanUltimine(Object playerObj) {
        UUID uuid = getPlayerUuid(playerObj);
        if (uuid == null) return true;

        Player bukkitPlayer = Bukkit.getPlayer(uuid);
        String playerName = bukkitPlayer != null ? bukkitPlayer.getName() : getPlayerName(playerObj);

        PlayerData data = dataManager.getPlayerData(uuid, playerName);

        if (bukkitPlayer != null && dataManager.hasBypass(bukkitPlayer)) {
            return true;
        }

        if (data.isBanned()) {
            if (bukkitPlayer != null && data.shouldSendBannedNotification(configManager.getBannedMessageCooldownSeconds())) {
                List<String> banMessages = configManager.getMessageList(bukkitPlayer, "banned-attempt");
                for (String msg : banMessages) {
                    String formatted = msg.replace("{player}", data.getName())
                            .replace("{reason}", data.getBanReason())
                            .replace("{banned_by}", data.getBannedBy())
                            .replace("{ban_date}", data.getBanDate());
                    TextUtil.sendMessage(bukkitPlayer, formatted);
                }
            }
            return false;
        }

        GroupConfig group = dataManager.resolveGroup(bukkitPlayer, data);
        if (group.isUnlimited()) {
            return true;
        }

        if (data.isLockedOut()) {
            if (bukkitPlayer != null && data.shouldSendCooldownNotification(configManager.getCooldownMessageCooldownSeconds())) {
                boolean isRu = configManager.isRussian(bukkitPlayer);
                String formattedDuration = TextUtil.formatDuration(data.getRemainingLockoutSeconds(), isRu);

                if (configManager.isActionbarEnabled() && configManager.isShowOnCooldown()) {
                    String abMsg = configManager.getMessage(bukkitPlayer, "cooldown-actionbar", false)
                            .replace("{remaining}", formattedDuration);
                    TextUtil.sendActionBar(bukkitPlayer, abMsg);
                }
                String chatMsg = configManager.getMessage(bukkitPlayer, "cooldown-chat", true)
                        .replace("{remaining}", formattedDuration);
                TextUtil.sendMessage(bukkitPlayer, chatMsg);
            }
            return false;
        }

        long remainingDelay = data.getRemainingDelaySeconds(group, configManager.isDelaysEnabled());
        if (remainingDelay > 0) {
            if (bukkitPlayer != null && data.shouldSendCooldownNotification(configManager.getCooldownMessageCooldownSeconds())) {
                boolean isRu = configManager.isRussian(bukkitPlayer);
                String formattedDuration = TextUtil.formatDuration(remainingDelay, isRu);

                if (configManager.isActionbarEnabled() && configManager.isShowOnCooldown()) {
                    String abMsg = configManager.getMessage(bukkitPlayer, "delay-actionbar", false)
                            .replace("{remaining}", formattedDuration);
                    TextUtil.sendActionBar(bukkitPlayer, abMsg);
                }
                String chatMsg = configManager.getMessage(bukkitPlayer, "delay-chat", true)
                        .replace("{remaining}", formattedDuration);
                TextUtil.sendMessage(bukkitPlayer, chatMsg);
            }
            return false;
        }

        return true;
    }

    private Object handleUltimineBlockReason(Object playerObj) {
        UUID uuid = getPlayerUuid(playerObj);
        if (uuid != null) {
            PlayerData data = dataManager.getPlayerDataIfPresent(uuid);
            if (data != null) {
                if (data.isBanned()) {
                    return resultNoPermission != null ? resultNoPermission : resultOther;
                }
                Player bp = Bukkit.getPlayer(uuid);
                GroupConfig group = dataManager.resolveGroup(bp, data);
                if (data.isLockedOut() || data.getRemainingDelaySeconds(group, configManager.isDelaysEnabled()) > 0) {
                    return resultOnCooldown != null ? resultOnCooldown : resultOther;
                }
            }
        }
        return resultOther;
    }

    private class BlockBreakHandlerInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("breakBlock".equals(name) && args != null && args.length >= 1) {
                handleBreakBlock(args[0]);
                return passResult;
            }
            if ("postBreak".equals(name) && args != null && args.length >= 1) {
                handlePostBreak(args[0]);
                return null;
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(name)) {
                return "FTBLimitBlockBreakHandlerProxy";
            }
            return null;
        }
    }

    private void handleBreakBlock(Object playerObj) {
        UUID uuid = getPlayerUuid(playerObj);
        if (uuid == null) return;
        activeExcavationBlocks.compute(uuid, (k, count) -> count == null ? 1 : count + 1);
    }

    private void handlePostBreak(Object playerObj) {
        UUID uuid = getPlayerUuid(playerObj);
        if (uuid == null) return;

        int blocksBroken = activeExcavationBlocks.getOrDefault(uuid, 1);
        activeExcavationBlocks.remove(uuid);

        Player bukkitPlayer = Bukkit.getPlayer(uuid);
        String name = bukkitPlayer != null ? bukkitPlayer.getName() : getPlayerName(playerObj);

        PlayerData data = dataManager.getPlayerData(uuid, name);

        if (bukkitPlayer != null && dataManager.hasBypass(bukkitPlayer)) {
            data.setTotalExcavations(data.getTotalExcavations() + 1);
            data.setTotalBlocks(data.getTotalBlocks() + blocksBroken);
            if (configManager.isActionbarEnabled() && configManager.isShowOnExcavate()) {
                String msg = configManager.getMessage(bukkitPlayer, "actionbar-excavated-unlimited", false)
                        .replace("{blocks}", String.valueOf(blocksBroken));
                TextUtil.sendActionBar(bukkitPlayer, msg);
            }
            return;
        }

        GroupConfig group = dataManager.resolveGroup(bukkitPlayer, data);
        int limitBefore = data.getEffectiveDailyLimit(group);

        data.recordExcavation(blocksBroken);

        int usedNow = data.getDailyExcavations();
        int remaining = data.getRemainingDailyExcavations(group);

        if (limitBefore > 0 && usedNow >= limitBefore) {
            long lockoutMillis = configManager.getLockoutHours() * 3600L * 1000L;
            data.setLockoutUntil(System.currentTimeMillis() + lockoutMillis);

            if (bukkitPlayer != null) {
                boolean isRu = configManager.isRussian(bukkitPlayer);
                String lockoutStr = TextUtil.formatDuration(configManager.getLockoutHours() * 3600L, isRu);
                String limitMsg = configManager.getMessage(bukkitPlayer, "limit-reached", true)
                        .replace("{limit}", String.valueOf(limitBefore))
                        .replace("{lockout}", lockoutStr);
                TextUtil.sendMessage(bukkitPlayer, limitMsg);
            }
        }

        dataManager.save(true);

        if (bukkitPlayer != null && configManager.isActionbarEnabled() && configManager.isShowOnExcavate()) {
            if (group.isUnlimited()) {
                String msg = configManager.getMessage(bukkitPlayer, "actionbar-excavated-unlimited", false)
                        .replace("{blocks}", String.valueOf(blocksBroken));
                TextUtil.sendActionBar(bukkitPlayer, msg);
            } else if (data.isLockedOut()) {
                boolean isRu = configManager.isRussian(bukkitPlayer);
                String remainingStr = TextUtil.formatDuration(data.getRemainingLockoutSeconds(), isRu);
                String msg = configManager.getMessage(bukkitPlayer, "cooldown-actionbar", false)
                        .replace("{remaining}", remainingStr);
                TextUtil.sendActionBar(bukkitPlayer, msg);
            } else if (configManager.isDelaysEnabled() && !data.isDelayBypass() && group.getNormalDelay() > 0) {
                String msg = configManager.getMessage(bukkitPlayer, "actionbar-excavated-delayed", false)
                        .replace("{delay}", String.valueOf(group.getNormalDelay()));
                TextUtil.sendActionBar(bukkitPlayer, msg);
            } else {
                String msg = configManager.getMessage(bukkitPlayer, "actionbar-excavated", false)
                        .replace("{used}", String.valueOf(usedNow))
                        .replace("{limit}", String.valueOf(limitBefore))
                        .replace("{remaining}", String.valueOf(remaining));
                TextUtil.sendActionBar(bukkitPlayer, msg);
            }
        }
    }
}
