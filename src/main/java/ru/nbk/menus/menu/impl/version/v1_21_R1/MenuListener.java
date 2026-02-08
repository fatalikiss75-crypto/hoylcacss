package ru.nbk.menus.menu.impl.version.v1_21_R1;


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.nbk.menus.menu.Menu;
import ru.nbk.menus.menu.MenuItem;
import ru.nbk.rolecases.HolyCases;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuListener implements Listener {

    private HolyCases plugin;
    private Map<Inventory, Menu> menus;
    private Map<Player, List<MenuItem>> localCooldown = new HashMap<>();
    private List<MenuItem> globalCooldown = new ArrayList<>();

    public MenuListener(HolyCases plugin, Map<Inventory, Menu> menus){
        this.plugin = plugin;
        this.menus = menus;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e){
        Menu menu = menus.get(e.getView().getTopInventory());
        Player player = (Player) e.getWhoClicked();
        if (menu != null) {
            menu.onClick().accept(e);

            MenuItem menuItem = menu.getItem(e.getRawSlot());
            Map.Entry<String, Integer> cooldown = menu.getCooldown();
            if (menuItem != null) {
                if (!hasCooldown(player, menuItem, cooldown.getKey())) {
                    addCooldown(player, menuItem, cooldown);
                    menuItem.onClick().accept(player, e.getClick());
                }
            }
        }
    }

    @EventHandler
    public void onInvDrag(InventoryDragEvent e){
        Menu menu = menus.get(e.getView().getTopInventory());
        if (menu != null) menu.onDrag().accept(e);
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent e){
        Menu menu = menus.get(e.getView().getTopInventory());
        if (menu != null) {
            menu.onClose().accept(e);
            if (menu.removeOnClose()) {
                if (e.getInventory().getViewers().isEmpty()) {
                    menus.remove(e.getView().getTopInventory());
                }
            }
        }
    }


    private boolean hasCooldown(Player player, MenuItem menuItem, String cooldownType) {
        if (cooldownType.equalsIgnoreCase("global"))
            return globalCooldown.contains(menuItem);
        else if (cooldownType.equalsIgnoreCase("local"))
            return localCooldown.containsKey(player) && localCooldown.get(player).contains(menuItem);

        return false;
    }

    private void addCooldown(Player player, MenuItem menuItem, Map.Entry<String, Integer> cooldown) {
//        Bukkit.getLogger().info("Adding cooldown for player " + player.getName() + " on " + menuItem + " with cooldown type " + cooldown.getKey());

        if (cooldown.getKey().equalsIgnoreCase("global")) {
            globalCooldown.add(menuItem);
            new BukkitRunnable() {
                @Override
                public void run() {
                    globalCooldown.remove(menuItem);
                }
            }.runTaskLater(plugin, cooldown.getValue());
        } else if (cooldown.getKey().equalsIgnoreCase("local")) {
            List<MenuItem> menuItems = localCooldown.getOrDefault(player, new ArrayList<>());
            menuItems.add(menuItem);
            localCooldown.put(player, menuItems);
            new BukkitRunnable() {
                @Override
                public void run() {
                    removeCooldown(player, menuItem);
                }
            }.runTaskLater(plugin, 1);
        }
    }


    private void removeCooldown(Player player, MenuItem menuItem) {
        List<MenuItem> menuItems = localCooldown.getOrDefault(player, new ArrayList<>());
        menuItems.remove(menuItem);
        localCooldown.put(player, menuItems);
    }
}
