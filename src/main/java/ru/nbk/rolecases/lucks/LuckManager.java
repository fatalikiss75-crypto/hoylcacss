package ru.nbk.rolecases.lucks;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jdbi.v3.core.async.JdbiExecutor;
import ru.nbk.rolecases.HolyCases;
import ru.nbk.rolecases.database.dao.PlayerKeysDao;
import ru.nbk.rolecases.database.dao.PlayerLuckDao;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public class LuckManager {
    private final List<Luck> lucks = new ArrayList<>();
    private HolyCases plugin;
    private FileConfiguration luckConfig;

    private static Luck NO_LUCK = null;

    public static Luck getNoLuck() {
        return NO_LUCK;
    }

    public LuckManager(HolyCases plugin) {
        this.plugin = plugin;

        loadLucks();

        NO_LUCK = new Luck(
                plugin,
                "no_luck",
                luckConfig.getString("lucks.no_luck.history"),
                luckConfig.getString("lucks.no_luck.prize_message")
        );
    }

    public void loadLucks() {
        lucks.clear();
        loadFile();

        if (luckConfig.contains("lucks")) {
            for (String name : luckConfig.getConfigurationSection("lucks").getKeys(false)) {
                if (name.equalsIgnoreCase("no_luck")) continue;

                ConfigurationSection luckSection = luckConfig.getConfigurationSection("lucks." + name);
                if (luckSection != null) {
                    Luck luck = new Luck(
                            plugin,
                            name,
                            luckSection.getString("history", name),
                            luckSection.getString("prize_message", name)
                    );
                    lucks.add(luck);
                }
            }
        }
    }

    public boolean hasLuck(String name) {
        return getLuckNames().stream().anyMatch(luckName -> luckName.equalsIgnoreCase(name));
    }

    public String getLuckLines(OfflinePlayer player) {
        ConcurrentHashMap<Integer, String> results = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < lucks.size(); i++) {
            final int index = i;
            Luck luck = lucks.get(index);
            String replace = getReplace(luck.getName());
            CompletableFuture<Void> future = luck.getLuckFor(player).thenAccept(lucksValue -> {
                if (lucksValue > 0) {
                    String resultLine = replace.replace("%count%", "" + lucksValue);
                    results.put(index, resultLine);
                }
            });
            futures.add(future);
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.join();

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lucks.size(); i++) {
            if (results.containsKey(i)) {
                if (!result.isEmpty()) {
                    result.append("\n");
                }
                result.append(results.get(i));
            }
        }

        if (result.isEmpty() && luckConfig.contains("lucks.no_luck.replace"))
            result.append(luckConfig.getString("lucks.no_luck.replace"));

        return result.toString();
    }

    public List<Luck> getLucks() {
        return lucks;
    }

    public List<String> getLuckNames() {
        List<String> result = new ArrayList<>();
        for (Luck luck : lucks) {
            result.add(luck.getName());
        }
        return result;
    }

    public Luck removeLast(OfflinePlayer player) {
        AtomicReference<Luck> result = new AtomicReference<>(null);
        AtomicReference<Integer> maxLuckValue = new AtomicReference<>(Integer.MIN_VALUE);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < lucks.size(); i++) {
            final int index = i;
            Luck luck = lucks.get(index);
            CompletableFuture<Void> future = luck.getLuckFor(player).thenAccept(luckValue -> {
                if (luckValue > 0 && index > maxLuckValue.get()) {
                    maxLuckValue.set(index);
                    result.set(luck);
                }
            });
            futures.add(future);
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.join();

        Luck highestLuck = result.get();
        if (highestLuck != null) {
            if (plugin.isSQL()) {
                plugin.getExecutor().useExtension(PlayerLuckDao.class, dao -> dao.withdrawLuck(player.getUniqueId(), highestLuck.getName(), 1));
            } else {
                plugin.removeLuck(player, highestLuck.getName(), 1);
            }
        }

        return highestLuck;
    }

    public Luck getLuckByName(String name) {
        for (Luck luck : lucks) {
            if (luck.getName().equalsIgnoreCase(name)) {
                return luck;
            }
        }
        return null;
    }


    private String getReplace(String luck) {
        return luckConfig.getString("lucks." + luck + ".replace");
    }

    private void loadFile() {
        File luckFile = new File(plugin.getDataFolder(), "LuckConfig.yml");
        if (!luckFile.exists()) plugin.saveResource("LuckConfig.yml", false);
        luckConfig = YamlConfiguration.loadConfiguration(luckFile);
    }


}
