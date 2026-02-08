package ru.nbk.rolecases.animation;

import org.bukkit.*;
import net.minecraft.server.v1_21_R1.*;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import ru.nbk.rolecases.animation.entity.AnimationStand;
import ru.nbk.rolecases.cases.GameCase;
import ru.nbk.rolecases.reward.Reward;

import java.util.*;

public class SpinningAnimation implements Animation {

    private Plugin plugin;
    private double liftHeight;
    private int liftDuration;
    private int spinDuration;
    private int changeInterval;
    private int dropDuration;
    private int dropDurationAfterWin;
    private Sound liftSound;
    private Sound dropSound;

    public SpinningAnimation(Plugin plugin, double liftHeight, int liftDuration, int spinDuration, int changeInterval, int dropDuration, Sound liftSound, Sound dropSound) {
        this.plugin = plugin;
        this.liftHeight = liftHeight;
        this.liftDuration = liftDuration;
        this.spinDuration = spinDuration;
        this.changeInterval = changeInterval;
        this.dropDuration = dropDuration;
        this.dropDurationAfterWin = 30;
        this.liftSound = liftSound;
        this.dropSound = dropSound;
    }

    @Override
    public void animate(GameCase gameCase) {
        org.bukkit.block.Block caseBlock = gameCase.getLocation().getBlock();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameCase.isOpening()) {
                    this.cancel();
                    return;
                }

                for (Entity nearbyEntity : gameCase.getLocation().getWorld().getNearbyEntities(gameCase.getLocation(), 3.3, 3.3, 3.3)) {
                    if (nearbyEntity.getType() == EntityType.PLAYER) {
                        Vector vector = nearbyEntity.getLocation().toVector().subtract(gameCase.getLocation().toVector()).normalize().multiply(0.7);
                        nearbyEntity.setVelocity(vector);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 5);
        if (caseBlock.getType() == Material.RESPAWN_ANCHOR) {
            RespawnAnchor anchor = (RespawnAnchor) caseBlock.getBlockData();
            if (anchor.getCharges() < 1) {
                anchor.setCharges(1);
                caseBlock.setBlockData(anchor);
            }
            chargeRespawnAnchor(caseBlock, 1, 4, () -> startAnimation(gameCase));
        } else {
            startAnimation(gameCase);
        }
    }

    private void startAnimation(GameCase gameCase) {
        Location caseLocation = gameCase.getLocation();
        List<Reward> rewards = gameCase.getRewards();
        Reward winnerReward = gameCase.getRandomReward();
        Location pivot = caseLocation.clone().add(0.5, 0.6, 0.5);

        AnimationStand animationStand = new AnimationStand();
        animationStand.spawn(pivot);
        animationStand.setItem(rewards.get(0).getIcon());

        new BukkitRunnable() {
            int counter = 0;
            int rewardCounter = 0;
            @Override
            public void run() {
                if (counter >= liftDuration) {
                    this.cancel();
                    animationStand.setItem(winnerReward.getIcon());
                    animationStand.setName(winnerReward.getName());
                    animationStand.getWorld().playSound(animationStand.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0F, 1.0F);
                    dropStand(animationStand, winnerReward, pivot, gameCase, dropDurationAfterWin);
                    Firework firework = animationStand.getWorld().spawn(animationStand.getLocation(), Firework.class);
                    FireworkMeta fireworkMeta = firework.getFireworkMeta();
                    fireworkMeta.setPower(0);
                    firework.setFireworkMeta(fireworkMeta);
                    firework.detonate();
                    animationStand.getWorld().playSound(animationStand.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0F, 1.0F);
                    return;
                }
                Reward newReward = rewards.get(rewardCounter % rewards.size());
                animationStand.setItem(newReward.getIcon());
                animationStand.setName(newReward.getName());
                animationStand.getWorld().playSound(animationStand.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0F, 1.0F);
                rewardCounter++;
                Location newLocation = pivot.clone().add(0, liftHeight * counter / liftDuration, 0);
                newLocation.setYaw(animationStand.getLocation().getYaw() + (360.0f / liftDuration) * 2);
                animationStand.teleport(newLocation);
                counter++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void dropStand(AnimationStand animationStand, Reward winnerReward, Location pivot, GameCase gameCase, int dropDuration) {
        new BukkitRunnable() {
            int counter = 0;
            @Override
            public void run() {
                if (counter >= dropDuration) {
                    this.cancel();
                    animationStand.remove();
                    gameCase.givePrize(gameCase.getOpening(), winnerReward);
                    org.bukkit.block.Block caseBlock = gameCase.getLocation().getBlock();
                    if (caseBlock.getType() == Material.RESPAWN_ANCHOR) {
                        drainRespawnAnchor(caseBlock, 4, 1, gameCase::onOpenEnd);
                    } else {
                        gameCase.onOpenEnd();
                    }
                    return;
                }
                animationStand.teleport(pivot.clone().add(0, liftHeight - (liftHeight * counter / dropDuration), 0));
                counter++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void chargeRespawnAnchor(org.bukkit.block.Block block, int fromLevel, int toLevel, Runnable callback) {
        new BukkitRunnable() {
            int level = fromLevel;
            @Override
            public void run() {
                if (level > toLevel) {
                    this.cancel();
                    callback.run();
                    return;
                }
                setRespawnAnchorLevel(block, level);
                block.getWorld().playSound(block.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0F, 1.0F);
                level++;
            }
        }.runTaskTimer(plugin, 0, 10);
    }

    private void drainRespawnAnchor(org.bukkit.block.Block block, int fromLevel, int toLevel, Runnable callback) {
        new BukkitRunnable() {
            int level = fromLevel;
            @Override
            public void run() {
                if (level < toLevel) {
                    this.cancel();
                    callback.run();
                    return;
                }
                setRespawnAnchorLevel(block, level);
                block.getWorld().playSound(block.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0F, 1.0F);
                level--;
            }
        }.runTaskTimer(plugin, 0, 10);
    }

    private void setRespawnAnchorLevel(org.bukkit.block.Block block, int level) {
        if (block.getType() == Material.RESPAWN_ANCHOR) {
            RespawnAnchor anchor = (RespawnAnchor) block.getBlockData();
            anchor.setCharges(level);
            block.setBlockData(anchor);
        }
    }
}