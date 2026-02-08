package ru.nbk.menus.menu.impl.version.v1_21_R1;


import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ru.nbk.menus.menu.Menu;
import ru.nbk.menus.menu.MenuItem;
import ru.nbk.menus.menu.MenuManager;
import ru.nbk.menus.menu.impl.version.v1_21_R1.MenuListener;
import ru.nbk.rolecases.HolyCases;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MenuManagerImpl implements MenuManager {

    private Map<Inventory, Menu> menus = new HashMap<>();

    public MenuManagerImpl(HolyCases plugin){
        new MenuListener(plugin, menus);
    }

    @Override
    public Menu createMenu() {
        Menu menu = new MenuImpl();
        menus.put(menu.getInventory(), menu);
        return menu;
    }

    @Override
    public Menu createMenu(String name) {
        Menu menu = new MenuImpl(name);
        menus.put(menu.getInventory(), menu);
        return menu;
    }

    @Override
    public Menu createMenu(int size) {
        Menu menu = new MenuImpl(size);
        menus.put(menu.getInventory(), menu);
        return menu;
    }

    @Override
    public Menu createMenu(InventoryType type) {
        Menu menu = new MenuImpl(type);
        menus.put(menu.getInventory(), menu);
        return menu;
    }

    @Override
    public Menu createMenu(String name, int size) {
        Menu menu = new MenuImpl(size, name);
        menus.put(menu.getInventory(), menu);
        return menu;
    }

    @Override
    public Menu createMenu(String name, InventoryType type) {
        Menu menu = new MenuImpl(type, name);
        menus.put(menu.getInventory(), menu);
        return menu;
    }

    @Override
    public MenuItem createItem(ItemStack item) {
        return new MenuItemImpl(item);
    }

    @Override
    public MenuItem createItem(ItemStack item, BiConsumer<Player, ClickType> onClick) {
        return new MenuItemImpl(item, onClick);
    }
}
