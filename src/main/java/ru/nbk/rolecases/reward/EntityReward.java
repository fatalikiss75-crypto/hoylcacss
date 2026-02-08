package ru.nbk.rolecases.reward;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class EntityReward {

    private Reward reward;
    private ArmorStand entity;
    private ArmorStand name;
    private Map<String, Object> data = new HashMap<>();

    public EntityReward(Reward reward) {
        this.reward = reward;
    }

    public void spawn(Location location) {
        entity = location.getWorld().spawn(location, ArmorStand.class);
        entity.setInvisible(true);
        entity.setGravity(false);

        name = location.getWorld().spawn(location, ArmorStand.class);
        name.setInvisible(true);
        name.setGravity(false);
    }

    public void teleport(Location location) {
        name.teleport(location.clone().add(0, -getHeight(), 0));
        entity.teleport(((getItem() != null && !getItem().getType().isBlock()) ? location.clone().add(0, -0.45, 0) : location).clone().add(0, -getHeight(), 0));
    }

    public World getWorld() {
        return entity.getWorld();
    }

    public Location getLocation() {
        return entity.getLocation();
    }

    public double getHeight() {
        return name.getHeight();
    }

    public Reward getReward() {
        return reward;
    }

    public void setItem(ItemStack item) {
        if (getItem() != null && !getItem().getType().isBlock()) {
            entity.teleport(entity.getLocation().clone().add(0, 0.45, 0));
        }

        if (item != null && !item.getType().isBlock()) {
            entity.teleport(entity.getLocation().clone().add(0, -0.45, 0));
        }

        entity.getEquipment().setHelmet(item);
    }

    public ItemStack getItem() {
        return entity.getEquipment().getHelmet();
    }

    public void setName(String name) {
        this.name.setCustomName(name);
        this.name.setCustomNameVisible(true);
    }

    public void show() {

    }

    public void setData(String path, Object o) {
        data.put(path, o);
    }

    public Object getData(String path) {
        return data.get(path);
    }
}
