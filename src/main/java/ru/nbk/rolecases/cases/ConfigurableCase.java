package ru.nbk.rolecases.cases;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jdbi.v3.core.async.JdbiExecutor;
import ru.nbk.menus.menu.Menu;
import ru.nbk.rolecases.HolyCases;
import ru.nbk.rolecases.animation.Animation;
import ru.nbk.rolecases.lucks.Luck;
import ru.nbk.rolecases.misc.Messages;
import ru.nbk.rolecases.misc.RandomCollection;
import ru.nbk.rolecases.particle.WrappedParticle;
import ru.nbk.rolecases.reward.Reward;

import javax.swing.*;
import java.util.List;
import java.util.function.Function;

public class ConfigurableCase extends GameCase {

    public ConfigurableCase(HolyCases plugin, JdbiExecutor executor, String name, String titleAboveCase, double titleAboveCaseYOffset, boolean isLuckWork, Location location, BlockFace direction, Animation animation, Material blockMaterial, List<WrappedParticle> wrappedParticles, List<Reward> rewards, Function<Player, Menu> menuCreator, List<String> prizeMessage, Messages messages) {
        super(plugin, executor, name, titleAboveCase, titleAboveCaseYOffset, isLuckWork, location, direction, animation, blockMaterial, wrappedParticles, rewards, menuCreator, prizeMessage, messages);
    }

    @Override
    public void open(Player player, Luck luck) {
        this.opening = player;
        this.openingLuck = luck;
        removeAll(this);
        this.setTitleVisible(false);
        plugin.addPlayer(player, this);
        getAnimation().animate(this);
    }

    @Override
    public void onOpenEnd() {
        this.opening = null;
        this.openingLuck = null;
        this.setTitleVisible(true);
    }
}
