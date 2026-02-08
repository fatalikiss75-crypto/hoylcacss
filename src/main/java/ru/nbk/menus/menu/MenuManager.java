package ru.nbk.menus.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface MenuManager {

    Menu createMenu();

    Menu createMenu(String name);

    Menu createMenu(int size);

    Menu createMenu(InventoryType type);

    Menu createMenu(String name, int size);

    Menu createMenu(String name, InventoryType type);

    MenuItem createItem(ItemStack item);

    MenuItem createItem(ItemStack item, BiConsumer<Player, ClickType> onClick);
}
