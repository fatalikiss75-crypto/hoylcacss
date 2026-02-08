package ru.nbk.rolecases.nms;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Rotations;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.List;

public class CustomArmorStand extends ArmorStand {

    protected static List<CustomArmorStand> handlerList = new ArrayList<>();

    private boolean isSpawned;

    public CustomArmorStand(Location location) {
        super(EntityType.ARMOR_STAND, ((CraftWorld) location.getWorld()).getHandle());

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        float yaw = location.getYaw();
        float pitch = location.getPitch();
        absMoveTo(x, y, z, yaw, pitch);
        setNoGravity(true);
        setInvisible(true);
        setOnGround(true);
        // collides = false; // In 1.21.1 it's different, ArmorStand doesn't have a simple 'collides' field like this

        handlerList.add(this);
    }

    private void addToWorld() {
        ClientboundAddEntityPacket spawnPacket = new ClientboundAddEntityPacket(this);
        sendPacket(spawnPacket);
    }

    public void spawn(Location location) {
        addToWorld();
        sendMetadata();
        pureTeleport(location);
        isSpawned = true;
    }

    private void pureTeleport(Location location) {
        absMoveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        setYHeadRot(location.getYaw());

        ClientboundTeleportEntityPacket teleportPacket = new ClientboundTeleportEntityPacket(this);
        sendPacket(teleportPacket);
    }

    public void teleport(Location location) {
        pureTeleport(location);
    }

    @Override
    public void setCustomName(net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        setCustomNameVisible(true);
        sendMetadata();
    }

    public void setCustomName(String name) {
        super.setCustomName(net.minecraft.network.chat.Component.literal(name));
        setCustomNameVisible(true);
        sendMetadata();
    }

    public void setNameVisible(boolean visible) {
        setCustomNameVisible(visible);
        sendMetadata();
    }

    public Location getLocation() {
        return new Location(level().getWorld(), getX(), getY(), getZ(), getYRot(), getXRot());
    }

    public void setHelmet(ItemStack item) {
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        setItemSlot(EquipmentSlot.HEAD, nmsItem);

        List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> equipment = new ArrayList<>();
        equipment.add(Pair.of(EquipmentSlot.HEAD, nmsItem));
        ClientboundSetEquipmentPacket equipmentPacket = new ClientboundSetEquipmentPacket(getId(), equipment);
        sendPacket(equipmentPacket);
    }

    public ItemStack getHelmet() {
        return CraftItemStack.asBukkitCopy(getItemBySlot(EquipmentSlot.HEAD));
    }

    public void setHeadPose(EulerAngle angle) {
        setHeadPose(toNMS(angle));
        sendMetadata();
    }

    public void destroy() {
        isSpawned = false;
        ClientboundRemoveEntitiesPacket destroyPacket = new ClientboundRemoveEntitiesPacket(getId());
        sendPacket(destroyPacket);
    }

    public void remove() {
        destroy();
        handlerList.remove(this);
    }

    public boolean isSpawned() {
        return isSpawned;
    }

    @Override
    public void setMarker(boolean marker) {
        super.setMarker(marker);
        sendMetadata();
    }

    public void setBaby(boolean baby) {
        setSmall(baby);
        sendMetadata();
    }

    private void sendMetadata() {
        ClientboundSetEntityDataPacket metadataPacket = new ClientboundSetEntityDataPacket(getId(), getEntityData().getNonDefaultValues());
        sendPacket(metadataPacket);
    }

    private void sendPacket(Packet<?> packet) {
        level().getPlayers(p -> true).forEach(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(packet);
            }
        });
    }

    private Rotations toNMS(EulerAngle old) {
        return new Rotations((float)Math.toDegrees(old.getX()), (float)Math.toDegrees(old.getY()), (float)Math.toDegrees(old.getZ()));
    }

    protected void spawnFor(Player player) {
        ClientboundAddEntityPacket spawnPacket = new ClientboundAddEntityPacket(this);
        ClientboundSetEntityDataPacket metadataPacket = new ClientboundSetEntityDataPacket(getId(), getEntityData().getNonDefaultValues());
        
        List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> equipment = new ArrayList<>();
        equipment.add(Pair.of(EquipmentSlot.HEAD, getItemBySlot(EquipmentSlot.HEAD)));
        ClientboundSetEquipmentPacket equipmentPacket = new ClientboundSetEquipmentPacket(getId(), equipment);
        
        ClientboundTeleportEntityPacket teleportPacket = new ClientboundTeleportEntityPacket(this);

        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        serverPlayer.connection.send(spawnPacket);
        serverPlayer.connection.send(metadataPacket);
        serverPlayer.connection.send(equipmentPacket);
        serverPlayer.connection.send(teleportPacket);
    }
}
