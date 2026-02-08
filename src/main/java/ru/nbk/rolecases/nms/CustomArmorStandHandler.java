package ru.nbk.rolecases.nms;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;

public class CustomArmorStandHandler implements Listener {

    public CustomArmorStandHandler(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        CustomArmorStand.handlerList.forEach(stand -> {
            if (stand.getLocation().getWorld().equals(e.getPlayer().getWorld()) && stand.isSpawned()) stand.spawnFor(e.getPlayer());
        });
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        CustomArmorStand.handlerList.forEach(stand -> {
            if (stand.getLocation().getWorld().equals(e.getPlayer().getWorld()) && stand.isSpawned()) stand.spawnFor(e.getPlayer());
        });
    }

    public void onDisable() {
        for (CustomArmorStand customArmorStand : new ArrayList<>(CustomArmorStand.handlerList)) {
            customArmorStand.remove();
        }
    }

}
