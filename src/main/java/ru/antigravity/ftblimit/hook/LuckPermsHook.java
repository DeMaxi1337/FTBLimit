package ru.antigravity.ftblimit.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.antigravity.ftblimit.config.ConfigManager;
import ru.antigravity.ftblimit.config.GroupConfig;

import java.lang.reflect.Method;

public class LuckPermsHook {

    private boolean available = false;
    private Object luckPermsApi = null;
    private Method getUserManagerMethod = null;
    private Method getUserMethod = null;
    private Method getPrimaryGroupMethod = null;

    public LuckPermsHook() {
        init();
    }

    private void init() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return;
        }

        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Method getMethod = providerClass.getMethod("get");
            luckPermsApi = getMethod.invoke(null);

            Class<?> apiClass = luckPermsApi.getClass();
            getUserManagerMethod = apiClass.getMethod("getUserManager");
            Object userManager = getUserManagerMethod.invoke(luckPermsApi);

            Class<?> userManagerClass = userManager.getClass();
            getUserMethod = userManagerClass.getMethod("getUser", java.util.UUID.class);

            Class<?> userClass = Class.forName("net.luckperms.api.model.user.User");
            getPrimaryGroupMethod = userClass.getMethod("getPrimaryGroup");

            available = true;
        } catch (Throwable t) {
            available = false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public GroupConfig resolvePlayerGroup(Player player, ConfigManager configManager) {
        if (!available || player == null) return null;

        try {
            Object userManager = getUserManagerMethod.invoke(luckPermsApi);
            Object user = getUserMethod.invoke(userManager, player.getUniqueId());
            if (user == null) return null;

            String primaryGroup = (String) getPrimaryGroupMethod.invoke(user);
            if (primaryGroup != null) {
                GroupConfig cfg = configManager.getGroups().get(primaryGroup.toLowerCase());
                if (cfg != null) {
                    return cfg;
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }
}
