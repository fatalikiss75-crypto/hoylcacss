package ru.nbk.menus.menu.impl.version.v1_21_R1;


import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import ru.nbk.menus.menu.MenuItem;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MenuItemImpl implements MenuItem {

    private ItemStack item;
    private BiConsumer<Player, ClickType> consumer;

    public MenuItemImpl(ItemStack item){
        this.item = item;
        this.consumer = (player, clickType) -> {};
    }

    public MenuItemImpl(ItemStack item, BiConsumer<Player, ClickType> onClick){
        this.item = item;
        this.consumer = onClick;
    }

    @Override
    public ItemStack item() {
        return item;
    }

    @Override
    public void item(ItemStack item) {
        this.item = item;
    }

    @Override
    public BiConsumer<Player, ClickType> onClick() {
        return consumer;
    }

    @Override
    public void onClick(BiConsumer<Player, ClickType> consumer) {
        this.consumer = consumer;
    }
}
