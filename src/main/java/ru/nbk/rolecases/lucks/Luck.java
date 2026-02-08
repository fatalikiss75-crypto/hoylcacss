package ru.nbk.rolecases.lucks;

import org.bukkit.OfflinePlayer;
import ru.nbk.rolecases.HolyCases;
import ru.nbk.rolecases.database.dao.PlayerLuckDao;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class Luck {

    private final HolyCases plugin;
    private final String name;
    private final String history;
    private final String prizeMessage;

    public Luck(HolyCases plugin, String name, String history, String prizeMessage) {
        this.plugin = plugin;
        this.name = name;
        this.history = history;
        this.prizeMessage = prizeMessage;
    }

    public CompletableFuture<Integer> getLuckFor(OfflinePlayer player) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        if (plugin.isSQL()) {
            plugin.getExecutor().useExtension(PlayerLuckDao.class, daoLuck -> {
                int lucks = daoLuck.getLuck(player.getUniqueId(), name);
                future.complete(lucks);
            });
        } else {
            future.complete(plugin.getLucks(player, name));
        }

        return future;
    }

    public String getName() {
        return name;
    }

    public String getHistory() {
        return history;
    }

    public String getPrizeMessage() {
        return prizeMessage;
    }
}
