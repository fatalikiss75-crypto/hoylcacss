package ru.nbk.rolecases.animation;

import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_21_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import ru.nbk.rolecases.animation.entity.AnimationStand;
import ru.nbk.rolecases.cases.GameCase;
import ru.nbk.rolecases.reward.Reward;

import java.util.*;
import java.util.stream.Collectors;

public class CircleAnimation implements Animation {

    private Plugin plugin;
    private boolean randomRewards;
    private double rewardRadius;
    private int animationStartDelay = 20;
    private int animationStartInterval = 20;
    private int showOpenUpTicks;
    private double showOpenAnglePerTick;
    private double showSpinStartAnglePerTick = 10;
    private double showSpinMinimalAnglePerTick = 2;
    private int showSpinMinSlowdownTime = 100;
    private int showSpinMaxSlowdownTime = 200;
    private int showSpinMinIdle = 40;
    private int showSpinMaxIdle = 80;
    private int animationEndDelay = 20;
    private int showEndDownTicks = 40;
    private double showEndAnglePerTick = 6;
    private Sound chestOpenSound;
    private Sound chestCloseSound;

    private List<AnimationStand> animationStands;

    public CircleAnimation(Plugin plugin, boolean randomRewards, double rewardRadius, int animationStartDelay, int animationStartInterval, int showOpenUpTicks, double showOpenAnglePerTick, double showSpinStartAnglePerTick, double showSpinMinimalAnglePerTick, int showSpinMinSlowdownTime, int showSpinMaxSlowdownTime, int showSpinMinIdle, int showSpinMaxIdle, int animationEndDelay, int showEndDownTicks, double showEndAnglePerTick, Sound chestOpenSound, Sound chestCloseSound) {
        this.plugin = plugin;
        this.randomRewards = randomRewards;
        this.rewardRadius = rewardRadius + 0.5;
        this.animationStartDelay = animationStartDelay;
        this.animationStartInterval = animationStartInterval;
        this.showOpenUpTicks = showOpenUpTicks;
        this.showOpenAnglePerTick = showOpenAnglePerTick;
        this.showSpinStartAnglePerTick = showSpinStartAnglePerTick;
        this.showSpinMinimalAnglePerTick = showSpinMinimalAnglePerTick;
        this.showSpinMinSlowdownTime = showSpinMinSlowdownTime;
        this.showSpinMaxSlowdownTime = showSpinMaxSlowdownTime;
        this.showSpinMinIdle = showSpinMinIdle;
        this.showSpinMaxIdle = showSpinMaxIdle;
        this.animationEndDelay = animationEndDelay;
        this.showEndDownTicks = showEndDownTicks;
        this.showEndAnglePerTick = showEndAnglePerTick;
        this.chestCloseSound = chestCloseSound;
        this.chestOpenSound = chestOpenSound;
    }

    @Override
    public void animate(GameCase gameCase) {
        Location caseLocation = gameCase.getLocation();

        List<Reward> rewards = getRewards(gameCase);
        Reward winnerReward = gameCase.getRandomReward();

        float caseYaw = directionToYaw(gameCase.getDirection());
        Location pivot = caseLocation.clone().add(0.5, 0.6, 0.5);
        pivot.setYaw(caseYaw);

        animationStands = spawnStands(rewards, pivot);
        caseLocation.getWorld().playSound(caseLocation, chestOpenSound, 1f, 1f);

        BlockFace direction = gameCase.getDirection();
        double rewardsAngle = 360.0 / animationStands.size();

        sendChestAnimation(pivot.getBlock(), true);

        new BukkitRunnable() {
            int counter = 0;

            @Override
            public void run() {
                if (counter >= animationStands.size()) {
                    this.cancel();
                    return;
                }

                AnimationStand animationStand = animationStands.get(counter);
                double offset = (counter + 1) * rewardsAngle;

                showOpen(animationStand, offset, showOpenUpTicks, pivot, direction);

                counter++;
            }
        }.runTaskTimer(plugin, animationStartDelay, animationStartInterval);

        int showOpenDurationTicks = animationStartDelay + showOpenUpTicks + animationStartInterval * animationStands.size();

        new BukkitRunnable() {
            int counter = showOpenDurationTicks;

            @Override
            public void run() {
                caseLocation.getWorld().spawnParticle(Particle.WITCH, pivot.clone().add(0, 0.2, 0), 5);
                counter--;
                if (counter < 0) {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);

        AnimationStand sword = createSword(pivot, direction);

        new BukkitRunnable() {
            @Override
            public void run() {
                int showSpinTicksRotate = showSpinMinSlowdownTime + new Random().nextInt(showSpinMaxSlowdownTime - showSpinMinSlowdownTime + 1);
                int idleTicks = showSpinMinIdle + new Random().nextInt(showSpinMaxIdle - showSpinMinIdle + 1);

                sendChestAnimation(pivot.getBlock(), false);
                pivot.getWorld().playSound(pivot, chestCloseSound, 1f, 1f);

                showSpin(animationStands, showSpinTicksRotate, idleTicks, pivot, direction, sword, winnerReward, gameCase);
            }
        }.runTaskLater(plugin, showOpenDurationTicks);

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
    }

    private void showOpen(AnimationStand animationStand, double offset, int upTicks, Location pivot, BlockFace direction) {
        Reward reward = (Reward) animationStand.getData("reward");
        animationStand.setItem(reward.getIcon());
        animationStand.setName(reward.getName());

        new BukkitRunnable() {
            int counter = 0;
            Location startUpLocation = pivot.clone();
            double upPerTick = rewardRadius / upTicks;
            Vector circleDirection = direction.getDirection().multiply(0.000001).setY(0);
            double currentAngle = 0;

            @Override
            public void run() {
                if (counter < upTicks) {
                    animationStand.teleport(startUpLocation.add(0, upPerTick, 0));
                } else {
                    if (currentAngle <= offset - 360) {
                        animationStand.setData("last_angle", currentAngle);
                        this.cancel();
                        return;
                    }

                    currentAngle = -showOpenAnglePerTick * (counter - upTicks);
                    if (currentAngle < offset - 360) currentAngle = offset - 360;

                    Location loc = circleDirection.clone()
                            .add(new Vector(0, rewardRadius, 0))
                            .rotateAroundAxis(circleDirection.clone(), -Math.toRadians(currentAngle))
                            .toLocation(startUpLocation.getWorld())
                            .add(pivot);

                    loc.setYaw(animationStand.getLocation().getYaw());
                    animationStand.teleport(loc);
                }

                counter++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void showSpin(List<AnimationStand> animationStands, int slowdownTicks, int idleTicks, Location pivot, BlockFace direction, AnimationStand sword, Reward winnerReward, GameCase gameCase) {
        new BukkitRunnable() {
            int counter = 0;
            double anglePerTick = showSpinStartAnglePerTick;
            double slowdownPerTick = (anglePerTick - showSpinMinimalAnglePerTick) / slowdownTicks;
            Vector circleDirection = direction.getDirection().multiply(0.000001).setY(0);
            double ticksRotate = slowdownTicks + idleTicks;

            @Override
            public void run() {
                if (sword.getItem().getType() == Material.AIR) sword.setItem(new ItemStack(Material.DIAMOND_SWORD));

                if (counter > ticksRotate) {
                    this.cancel();
                    showCloser(animationStands, pivot, direction, sword, winnerReward, gameCase);
                    return;
                }
                animationStands.forEach(animationStand -> {
                    double angle = ((double) animationStand.getData("last_angle")) - anglePerTick;
                    if (angle <= -360) {
                        angle = 360 + angle;
                        pivot.getWorld().playSound(pivot.clone().add(0, rewardRadius, 0), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    }
                    animationStand.setData("last_angle", angle);
                    Location loc = circleDirection.clone()
                            .add(new Vector(0, rewardRadius, 0))
                            .rotateAroundAxis(circleDirection.clone(), -Math.toRadians(angle))
                            .toLocation(animationStand.getWorld());
                    loc = pivot.clone().add(loc);

                    loc.setYaw(animationStand.getLocation().getYaw());
                    animationStand.teleport(loc);
                });

                if (counter <= slowdownTicks) anglePerTick -= slowdownPerTick;
                counter++;
            }
        }.runTaskTimer(plugin, 5, 1);
    }


    private void showCloser(List<AnimationStand> animationStands, Location pivot, BlockFace direction, AnimationStand sword, Reward winnerReward, GameCase gameCase) {
        AnimationStand winner = animationStands.stream()
                .filter(animationStand -> animationStand.getData("reward").equals(winnerReward))
                .findFirst().get();

        new BukkitRunnable() {
            double anglePerTick = showSpinMinimalAnglePerTick;
            double winnerAngle = (double) winner.getData("last_angle");
            Vector circleDirection = direction.getDirection().multiply(0.001).setY(0);

            @Override
            public void run() {
                winnerAngle -= anglePerTick;

                animationStands.forEach(entityReward -> {
                    double angle = ((double) entityReward.getData("last_angle")) - anglePerTick;
                    if (angle <= -360) {
                        angle = 360 + angle;
                        pivot.getWorld().playSound(pivot.clone().add(0, rewardRadius, 0), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    }
                    entityReward.setData("last_angle", angle);

                    Location loc = circleDirection.clone()
                            .add(new Vector(0, rewardRadius, 0))
                            .rotateAroundAxis(circleDirection.clone(), -Math.toRadians(angle))
                            .toLocation(entityReward.getWorld())
                            .add(pivot);

                    loc.setYaw(entityReward.getLocation().getYaw());
                    entityReward.teleport(loc);
                });

                if (winnerAngle <= -360) {
                    this.cancel();
                    gameCase.givePrize(gameCase.getOpening(), winnerReward);
                    startEnd(animationStands, pivot, direction, sword, gameCase);
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void startEnd(List<AnimationStand> animationStands, Location pivot, BlockFace direction, AnimationStand sword, GameCase gameCase) {
        List<AnimationStand> sorted = animationStands.stream()
                .sorted(Comparator.comparing(animationStand -> ((double) animationStand.getData("last_angle")), Comparator.reverseOrder()))
                .collect(Collectors.toList());

        new BukkitRunnable() {
            @Override
            public void run() {
                pivot.getWorld().playSound(pivot, chestOpenSound, 1f, 1f);

                for (int i = 0; i < sorted.size(); i++) {
                    sword.remove();
                    sendChestAnimation(pivot.getBlock(), true);

                    AnimationStand animationStand = sorted.get(i);
                    showEnd(animationStand, pivot, showEndDownTicks, showEndAnglePerTick, direction, i + 1 == sorted.size(), gameCase);
                }
            }
        }.runTaskLater(plugin, animationEndDelay);
    }

    private void showEnd(AnimationStand animationStand, Location pivot, int downTicks, double showCloseAnglePerTick, BlockFace direction, boolean isLast, GameCase gameCase) {
        new BukkitRunnable() {
            int counter = 0;
            Location startDownLocation = pivot.clone().add(0, rewardRadius, 0);
            double downPerTick = (rewardRadius - 0.5) / downTicks;
            double waitTicks = 2;
            Vector circleDirection = direction.getDirection().multiply(0.001).setY(0);
            double currentAngle = (double) animationStand.getData("last_angle");

            @Override
            public void run() {
                if (currentAngle != 0.0) {
                    currentAngle += showCloseAnglePerTick;
                    if (currentAngle > 0.0) {
                        currentAngle = 0.0;
                    }

                    Location loc = circleDirection.clone()
                            .add(new Vector(0, rewardRadius, 0))
                            .rotateAroundAxis(circleDirection.clone(), -Math.toRadians(currentAngle))
                            .toLocation(animationStand.getWorld());
                    loc = pivot.clone().add(loc);
                    animationStand.teleport(loc);
                } else {
                    if (counter >= downTicks - waitTicks) {
                        this.cancel();
                        animationStand.getWorld().playSound(pivot, Sound.ENTITY_CHICKEN_EGG, 1f, 1f);
                        animationStand.remove();

                        if (isLast) {
                            sendChestAnimation(pivot.getBlock(), false);
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    pivot.getWorld().playSound(pivot, chestCloseSound, 1f, 1f);
                                }
                            }.runTaskLater(plugin, 5);

                            for (AnimationStand stand : animationStands) {
                                stand.remove();
                            }

                            animationStands.clear();
                            gameCase.onOpenEnd();
                        }
                        return;
                    }

                    if (waitTicks != 0) {
                        waitTicks--;
                        return;
                    }

                    animationStand.teleport(startDownLocation.subtract(0, downPerTick, 0));
                    counter++;
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void sendChestAnimation(Block block, boolean open) {
        if (!(block.getType().name().contains("CHEST"))) return;

        net.minecraft.server.level.ServerLevel nmsWorld = ((org.bukkit.craftbukkit.v1_21_R1.CraftWorld) block.getWorld()).getHandle();
        net.minecraft.core.BlockPos blockPosition = new net.minecraft.core.BlockPos(block.getX(), block.getY(), block.getZ());

        nmsWorld.blockEvent(blockPosition, nmsWorld.getBlockState(blockPosition).getBlock(), 1, open ? 1 : 0);
    }

    private float directionToYaw(BlockFace direction) {
        return switch (direction) {
            case NORTH -> 180;
            case EAST -> -90;
            case WEST -> 90;
            default -> 0;
        };
    }

    private AnimationStand createSword(Location spawnLocation, BlockFace direction) {
        double xOffset = 0;
        double yOffset = 2.1;
        double zOffset = 0;

        switch (direction) {
            case EAST -> {
                xOffset = 0.20;
                zOffset = -0.372;
            }
            case WEST -> {
                xOffset = -0.20;
                zOffset = 0.372;
            }
            case SOUTH -> {
                xOffset = 0.372;
                zOffset = 0.20;
            }
            case NORTH -> {
                xOffset = -0.372;
                zOffset = -0.20;
            }
        }

        AnimationStand sword = new AnimationStand();
        sword.spawn(spawnLocation.clone().add(xOffset, yOffset, zOffset));
        sword.teleport(spawnLocation.clone().add(xOffset, yOffset, zOffset));
        sword.setHeadRotation(0, 0, -45);
        sword.setBaby(true);

        return sword;
    }

    private List<AnimationStand> spawnStands(List<Reward> rewards, Location pivot) {
        List<AnimationStand> animationStands = new ArrayList<>();

        rewards.forEach(reward -> {
            AnimationStand animationStand = new AnimationStand();
            animationStand.spawn(pivot);
            animationStand.teleport(pivot);
            animationStand.setData("reward", reward);

            animationStands.add(animationStand);
        });

        return animationStands;
    }

    private List<Reward> getRewards(GameCase gameCase) {
        if (!randomRewards) {
            return gameCase.getRewards();
        } else {
            List<Reward> result = new ArrayList<>(gameCase.getRewards());
            Collections.shuffle(result);
            return result;
        }
    }
}
