package ru.nbk.rolecases.nms;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.v1_21_R1.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R1.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.List;

public class CustomArmorStand extends EntityArmorStand {

    protected static List<CustomArmorStand> handlerList = new ArrayList<>();

    private boolean isSpawned;

    public CustomArmorStand(Location location) {
        super(EntityTypes.ARMOR_STAND, ((CraftWorld) location.getWorld()).getHandle());

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        float yaw = location.getYaw();
        float pitch = location.getPitch();
        setPositionRotation(x, y, z, yaw, pitch);
        setNoGravity(true);
        setInvisible(true);
        setOnGround(true);
        collides = false;

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
        setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        setHeadRotation(location.getYaw());

        //((WorldServer) world).chunkCheck(this);

        ClientboundTeleportEntityPacket teleportPacket = new ClientboundTeleportEntityPacket(this);
        sendPacket(teleportPacket);
    }

    public void teleport(Location location) {

//        byte xOffset = (byte)(MathHelper.floor(location.getX() * 32.0D) - MathHelper.floor(locX() * 32.0D));
//        byte yOffset = (byte)(MathHelper.floor(location.getY() * 32.0D) - MathHelper.floor(locY() * 32.0D));
//        byte zOffset = (byte)(MathHelper.floor(location.getZ() * 32.0D) - MathHelper.floor(locZ() * 32.0D));
//        byte yawOffset = (byte)MathHelper.d(location.getYaw() * 256.0F / 360.0F);
//        byte pitchOffset = (byte)MathHelper.d(location.getPitch() * 256.0F / 360.0F);

        //((WorldServer) world).chunkCheck(this);
//        setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
//        setHeadRotation(location.getYaw());

//        ClientboundRelEntityMoveLookPacket lookPacket = new ClientboundRelEntityMoveLookPacket(getId(), xOffset, yOffset, zOffset, yawOffset, pitchOffset, true);
//        ClientboundRelEntityMovePacket mov = new ClientboundRelEntityMovePacket(getId(), xOffset, yOffset, zOffset, false);
//        sendPacket(mov);

        pureTeleport(location);
    }

    public void setCustomName(String name) {
        setCustomName(CraftChatMessage.fromStringOrNull(name));
        setCustomNameVisible(true);

        sendMetadata();
    }

    public void setNameVisible(boolean visible) {
        setCustomNameVisible(visible);

        sendMetadata();
    }

    public Location getLocation() {
        return new Location(world.getWorld(), locX(), locY(), locZ(), yaw, pitch);

    }

    public void setHelmet(ItemStack item) {
        net.minecraft.server.v1_21_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        setSlot(EnumItemSlot.HEAD, nmsItem, false);

        ClientboundSetEquipmentPacket equipmentPacket = new ClientboundSetEquipmentPacket(getId(), List.of(Pair.of(EnumItemSlot.HEAD, nmsItem)));
        sendPacket(equipmentPacket);
    }

    public ItemStack getHelmet() {
        return CraftItemStack.asBukkitCopy(getEquipment(EnumItemSlot.HEAD));
    }

    public void setHeadPose(EulerAngle angle) {
        setHeadPose(toNMS(angle));

        ClientboundSetEntityDataPacket metadataPacket = new ClientboundSetEntityDataPacket(getId(), datawatcher, true);
        sendPacket(metadataPacket);
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

    public void setMarker(boolean marker) {
        super.setMarker(marker);
        sendMetadata();
    }

    public void setBaby(boolean baby) {
        setSmall(baby);

        sendMetadata();
    }

    private void sendMetadata() {
        ClientboundSetEntityDataPacket metadataPacket = new ClientboundSetEntityDataPacket(getId(), datawatcher, true);
        sendPacket(metadataPacket);
    }

    private void sendPacket(Packet<?> packet) {
        world.getPlayers().forEach(entityHuman -> ((EntityPlayer) entityHuman).playerConnection.sendPacket(packet));
    }

    private Vector3f toNMS(EulerAngle old) {
        return new Vector3f((float)Math.toDegrees(old.getX()), (float)Math.toDegrees(old.getY()), (float)Math.toDegrees(old.getZ()));
    }

    protected void spawnFor(Player player) {
        ClientboundAddEntityPacket spawnPacket = new ClientboundAddEntityPacket(this);
        ClientboundSetEntityDataPacket metadataPacket = new ClientboundSetEntityDataPacket(getId(), datawatcher, true);
        ClientboundSetEquipmentPacket equipmentPacket = new ClientboundSetEquipmentPacket(getId(), List.of(Pair.of(EnumItemSlot.HEAD, getEquipment(EnumItemSlot.HEAD))));
        ClientboundTeleportEntityPacket teleportPacket = new ClientboundTeleportEntityPacket(this);

        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
        connection.sendPacket(spawnPacket);
        connection.sendPacket(metadataPacket);
        connection.sendPacket(equipmentPacket);
        connection.sendPacket(teleportPacket);
    }
}
