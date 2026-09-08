package ru.antigravity.ftblimit.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.antigravity.ftblimit.data.DataManager;
import ru.antigravity.ftblimit.data.PlayerData;

public class PlayerListener implements Listener {

    private final DataManager dataManager;

    public PlayerListener(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = dataManager.getPlayerData(player);
        data.setName(player.getName());
        data.checkLockoutExpiration();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dataManager.save(true);
    }
}
