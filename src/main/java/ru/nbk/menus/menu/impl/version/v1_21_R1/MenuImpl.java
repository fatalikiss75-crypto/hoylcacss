package ru.nbk.menus.menu.impl.version.v1_21_R1;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import ru.nbk.menus.menu.Menu;
import ru.nbk.menus.menu.MenuItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class MenuImpl implements Menu {

    private Inventory inventory;
    private String title;
    private Map<Integer, MenuItem> items = new HashMap<>();
    private String cooldownType = "LOCAL";
    private int cooldownTime = 10;
    private boolean removeOnClose;
    private Consumer<InventoryClickEvent> onClick = e -> e.setCancelled(true);
    private Consumer<InventoryDragEvent> onDrag = e -> e.setCancelled(true);
    private Consumer<InventoryCloseEvent> onClose = e -> {};


    private Map<String, Object> data = new HashMap<>();

    public MenuImpl(){
        this.inventory = Bukkit.createInventory(null, 9);
        this.title = "";
    }

    public MenuImpl(int size){
        this.inventory = Bukkit.createInventory(null, size);
        this.title = "";
    }

    public MenuImpl(String name){
        this.inventory = Bukkit.createInventory(null, 9, name);
        this.title = name;
    }

    public MenuImpl(InventoryType type){
        this.inventory = Bukkit.createInventory(null, type);
        this.title = "";
    }

    public MenuImpl cloneForPlayer(Player player) {
        MenuImpl clone = new MenuImpl(inventory.getSize(), getName());
        items.forEach(clone::setItem);
        return clone;
    }


    public MenuImpl(int size, String name){
        this.inventory = Bukkit.createInventory(null, size, name);
        this.title = name;
    }

    public MenuImpl(InventoryType type, String name){
        this.inventory = Bukkit.createInventory(null, type, name);
        this.title = name;
    }

    @Override
    public int getSize() {
        return inventory.getSize();
    }

    @Override
    public String getName() {
        return title;
    }

    @Override
    public MenuItem getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public void setItem(int slot, MenuItem menuItem) {
        if (menuItem == null) {
            inventory.setItem(slot, null);
            items.remove(slot);
        } else {
            inventory.setItem(slot, menuItem.item());
            items.put(slot, menuItem);
        }
    }


    @Override
    public void setCooldown(String type, int time) {
        if (type != null && List.of("global", "local").contains(type.toLowerCase()))
            this.cooldownType = type;
        if (time > 0)
            this.cooldownTime = time;
    }

    @Override
    public Map.Entry<String, Integer> getCooldown() {
        return Map.entry(cooldownType, cooldownTime);
    }

    @Override
    public void removeOnClose(boolean removeOnClose) {
        this.removeOnClose = removeOnClose;
    }

    @Override
    public boolean removeOnClose() {
        return removeOnClose;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(Consumer<InventoryClickEvent> onClick) {
        this.onClick = onClick;
    }

    @Override
    public Consumer<InventoryClickEvent> onClick() {
        return onClick;
    }

    @Override
    public void onDrag(Consumer<InventoryDragEvent> onDrag) {
        this.onDrag = onDrag;
    }

    @Override
    public Consumer<InventoryDragEvent> onDrag() {
        return onDrag;
    }

    @Override
    public void onClose(Consumer<InventoryCloseEvent> onClose) {
        this.onClose = onClose;
    }

    @Override
    public Consumer<InventoryCloseEvent> onClose() {
        return onClose;
    }

    @Override
    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public void refresh() {
        items.forEach((slot, item) -> inventory.setItem(slot, item.item()));
    }


    @Override
    public void setData(String key, Object data) {
        this.data.put(key, data);
    }

    @Override
    public Object getData(String key) {
        return data.get(key);
    }

    @Override
    public <T> T getData(String key, T def) {
        return (T) Optional.ofNullable(data.get(key))
                .orElse(def);
    }
}
