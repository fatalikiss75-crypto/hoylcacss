package ru.nbk.rolecases.animation.entity;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import ru.nbk.rolecases.nms.CustomArmorStand;

import java.util.HashMap;
import java.util.Map;

public class AnimationStand {

    private CustomArmorStand itemNoBlock;
    private CustomArmorStand name;
    private Map<String, Object> data = new HashMap<>();

    public void spawn(Location location) {
        Location fixed = location.clone();
        fixed.setYaw(location.getYaw());

        itemNoBlock = new CustomArmorStand(fixed) {
            @Override
            public void teleport(Location location) {
                super.teleport(location.clone().add(0, -0.45, 0));
            }

            @Override
            public Location getLocation() {
                return super.getLocation().add(0, -0.45, 0);
            }
        };
        itemNoBlock.spawn(fixed);
        name = new CustomArmorStand(fixed);
        name.spawn(fixed);
    }

    public void teleport(Location location) {
        Location fixed = location.clone().add(0, -getHeight(), 0);
        name.teleport(fixed);
        itemNoBlock.teleport(fixed);
    }

    public World getWorld() {
        return name.getLocation().getWorld();
    }

    public Location getLocation() {
        return name.getLocation();
    }

    public double getHeight() {
        return name.getHeight();
    }

    public void setItem(ItemStack item) {
        if (item.getType().isBlock()) {
            if (itemNoBlock.getHelmet() != null) {
                itemNoBlock.setHelmet(null);
            }
            name.setHelmet(item);
        }else {
            if (name.getHelmet() != null) {
                name.setHelmet(null);
            }
            itemNoBlock.setHelmet(item);
        }

    }

    public ItemStack getItem() {
        if (itemNoBlock.getHelmet().getType() != Material.AIR) return itemNoBlock.getHelmet();
        return name.getHelmet();
    }

    public void setBaby(boolean baby) {
        this.name.setBaby(baby);
        this.itemNoBlock.setBaby(baby);
    }

    public void setName(String name) {
        this.name.setCustomName(name);
        this.name.setCustomNameVisible(true);
    }


    public void setHeadRotation(double x, double y, double z) {
        name.setHeadPose(new EulerAngle(Math.toRadians(x), Math.toRadians(y), Math.toRadians(z)));
        itemNoBlock.setHeadPose(new EulerAngle(Math.toRadians(x), Math.toRadians(y), Math.toRadians(z)));
    }

    public void setData(String path, Object o) {
        data.put(path, o);
    }

    public Object getData(String path) {
        return data.get(path);
    }

    public void remove() {
        name.remove();
        itemNoBlock.remove();
    }
}
