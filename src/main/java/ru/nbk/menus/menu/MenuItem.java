package ru.nbk.menus.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface MenuItem {

    ItemStack item();

    void item(ItemStack itemStack);

    BiConsumer<Player, ClickType> onClick();

    void onClick(BiConsumer<Player, ClickType> consumer);
}
