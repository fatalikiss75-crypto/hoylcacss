package ru.nbk.rolecases.cases;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jdbi.v3.core.async.JdbiExecutor;
import ru.nbk.menus.menu.Menu;
import ru.nbk.rolecases.HolyCases;
import ru.nbk.rolecases.animation.Animation;
import ru.nbk.rolecases.database.dao.PrizeHistoryDao;
import ru.nbk.rolecases.lucks.Luck;
import ru.nbk.rolecases.lucks.LuckManager;
import ru.nbk.rolecases.misc.ColorUtil;
import ru.nbk.rolecases.misc.Messages;
import ru.nbk.rolecases.misc.RandomCollection;
import ru.nbk.rolecases.nms.CustomArmorStand;
import ru.nbk.rolecases.particle.WrappedParticle;
import ru.nbk.rolecases.reward.Prize;
import ru.nbk.rolecases.reward.Reward;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public abstract class GameCase {
    private static final Map<Player, GameCase> openedCases = new ConcurrentHashMap<>();


    protected HolyCases plugin;
    protected JdbiExecutor executor;
    protected String name;
    protected boolean isLuckWork;
    protected Location location;
    protected BlockFace direction;
    protected Animation animation;
    protected Material blockMaterial;
    protected List<WrappedParticle> wrappedParticles;
    protected List<Reward> rewards;
    protected Player opening;
    protected Luck openingLuck;
    protected CustomArmorStand titleAboveCase;
    protected Function<Player, Menu> menuCreator;
    protected List<String> prizeMessage;
    protected Messages messages;

    public static boolean luckWorking;

    public GameCase()
    {
        luckWorking = isLuckWork;
    }


    public GameCase(HolyCases plugin, JdbiExecutor executor, String name, String titleAboveCase, double titleAboveCaseYOffset, boolean isLuckWork, Location location, BlockFace direction, Animation animation, Material blockMaterial, List<WrappedParticle> wrappedParticles, List<Reward> rewards, Function<Player, Menu> menuCreator, List<String> prizeMessage, Messages messages) {
        this.plugin = plugin;
        this.executor = executor;
        this.name = name;
        this.isLuckWork = isLuckWork;
        this.location = location;
        this.direction = direction;
        this.animation = animation;
        this.blockMaterial = blockMaterial;
        this.wrappedParticles = wrappedParticles;
        this.rewards = rewards;
        this.menuCreator = menuCreator;
        this.prizeMessage = prizeMessage;
        this.messages = messages;

        this.titleAboveCase = new CustomArmorStand(location);
        this.titleAboveCase.spawn(location.clone().add(0.5, titleAboveCaseYOffset, 0.5));
        this.titleAboveCase.setCustomName(titleAboveCase);
        this.titleAboveCase.setMarker(true);
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public BlockFace getDirection() {
        return direction;
    }

    public Animation getAnimation() {
        return animation;
    }

    public List<WrappedParticle> getWrappedParticles() {
        return wrappedParticles;
    }

    public List<Reward> getRewards() {
        return rewards;
    }

    public Luck getLuck()
    {
        return openingLuck;
    }

    public Reward getRandomReward() {
        RandomCollection<Reward> newRewards = new RandomCollection<>();

        if (isLuckWork) {
            String luckName = (openingLuck != null) ? openingLuck.getName() : "no_luck";

            for (Reward reward : rewards)
                newRewards.add(reward.getChance(luckName), reward);
        } else {
            for (Reward reward : rewards)
                newRewards.add(reward.getChance("no_luck"), reward);
        }

        return newRewards.next();
    }

    public abstract void open(Player player, Luck luck);

    public Player getOpening() {
        return opening;
    }

    public Luck getOpeningLuck() {
        return openingLuck;
    }

    public Messages getMessages() {
        return messages;
    }

    public boolean isOpening() {
        return opening != null;
    }

    public abstract void onOpenEnd();

    public void setTitleVisible(boolean visible) {
        titleAboveCase.setNameVisible(visible);
    }

    public void deleteTitle() {
        setTitleVisible(false);
        titleAboveCase.remove();
    }

    public void openMenu(Player player) {
        menuCreator.apply(player).open(player);
        put(player, this);
    }

    public void givePrize(OfflinePlayer player, Reward reward) {
        Prize prize;
        if (isLuckWork)
            prize = reward.getRandomPrize(openingLuck);
        else
            prize = reward.getRandomPrize(null);

        if (reward.isSendTitle() && player.isOnline())
            player.getPlayer().sendTitle(prize.title(), prize.subtitle(), 20, 100, 10);

        String prizeLuck;
        if (isLuckWork) {
            if (this.getOpeningLuck() != null) {
                prizeLuck = this.getOpeningLuck().getPrizeMessage();
            } else {
                prizeLuck = (LuckManager.getNoLuck() == null) ? "" : LuckManager.getNoLuck().getPrizeMessage();
            }
        } else
            prizeLuck = "";

        prizeMessage.forEach(prizeLine -> {
            String message = prizeLine
                    .replace("%player_name%", player.getName())
                    .replace("%prize_name%", prize.name())
                    .replace("%luck%", ColorUtil.parse(prizeLuck));
            
            Bukkit.broadcastMessage(message);
        });

        prize.commands().forEach(cmd -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%luck%", prizeLuck).replace("%player_name%", player.getName()));
        });

        plugin.removePlayer(player);

        if (reward.isAddInHistory()) {
            String color;
            if (reward.getIcon().getItemMeta() instanceof LeatherArmorMeta armorMeta) {
                color = ":" + armorMeta.getColor().getRed() + ":" + armorMeta.getColor().getGreen() + ":" + armorMeta.getColor().getBlue();
            } else {
                color = "";
            }
            String luck;
            if (isLuckWork) {
                if (this.getOpeningLuck() != null) {
                    luck = this.getOpeningLuck().getHistory();
                } else {
                    luck = (LuckManager.getNoLuck() == null) ? "Без удачи" : LuckManager.getNoLuck().getHistory();
                }
            } else
                luck = (LuckManager.getNoLuck() == null) ? "Без удачи" : LuckManager.getNoLuck().getHistory();
            String serverName = plugin.getConfig().getString("server_name");
            if (plugin.isSQL()) {
                executor.useExtension(PrizeHistoryDao.class, dao -> {
                    dao.addEntry(player.getName(), name, luck, prize.name(), reward.getIcon().getType().name() + color, System.currentTimeMillis(), serverName);
                });
            } else {
                plugin.addPrizeHistory(player.getName(), name, luck, prize.name(), reward.getIcon().getType().name() + color, System.currentTimeMillis(), serverName);
            }
        }
    }

    public static void put(Player player, GameCase gameCase) {
        openedCases.put(player, gameCase);
    }

    public static void remove(Player player) {
        openedCases.remove(player);
    }

    public static void removeAll(GameCase gameCase) {
        for (Player player : openedCases.keySet().stream().toList()) {
            if (player != null) {
                if (openedCases.get(player) == gameCase)
                    player.closeInventory();
            }
        }
    }
}
