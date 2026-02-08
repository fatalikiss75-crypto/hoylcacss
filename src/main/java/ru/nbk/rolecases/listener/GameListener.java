package ru.nbk.rolecases.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import ru.nbk.rolecases.HolyCases;
import ru.nbk.rolecases.cases.CaseHolder;
import ru.nbk.rolecases.cases.CaseManager;
import ru.nbk.rolecases.cases.GameCase;
import ru.nbk.rolecases.configuration.CasesConfig;
import ru.nbk.rolecases.misc.Messages;
import ru.nbk.util.ItemBuilder;

import java.util.Set;
import java.util.function.Supplier;

public class GameListener implements Listener {

    private HolyCases plugin;
    private CaseManager caseManager;
    private CasesConfig casesConfig;
    private final Object lock = new Object();

    public GameListener(HolyCases plugin, CaseManager caseManager, CasesConfig casesConfig) {
        this.plugin = plugin;
        this.caseManager = caseManager;
        this.casesConfig = casesConfig;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        GameCase.remove(player);
    }

    @EventHandler
    public void onInt(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null || !e.getClickedBlock().hasMetadata("case")) return;
        e.setCancelled(true);

        GameCase gameCase = (GameCase) e.getClickedBlock().getMetadata("case").get(0).value();

        if (gameCase != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        if (!gameCase.isOpening()) {
                            gameCase.openMenu(e.getPlayer());
                        } else if (!gameCase.getOpening().equals(e.getPlayer())) {
                            gameCase.getMessages().sendMessage(e.getPlayer(), "case-already-opening");
                        }
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler
    public void onCaseAdmin(PlayerInteractEvent e) {
        if (e.getItem() == null || e.getItem().getType() != Material.ENDER_CHEST) return;
        if (!e.getItem().hasItemMeta() || !e.getItem().getItemMeta().hasDisplayName()) return;
        if (e.getClickedBlock() == null) return;
        if (!e.getPlayer().hasPermission("rolecase.command.place")) return;

        String caseName = new ItemBuilder(e.getItem()).name();

        CaseHolder caseHolder = caseManager.getCaseHolder(caseName);
        if (caseHolder == null) return;

        Player player = e.getPlayer();

        e.setCancelled(true);

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {

            CaseHolder aleradyExists = caseManager.getHolders().stream()
                        .filter(holder -> {
                            for (GameCase gameCase : holder.getCases()) {
                                if (gameCase.getLocation().getBlock().equals(e.getClickedBlock())) return true;
                            }
                            return false;
                        }).findAny().orElse(null);

            if (aleradyExists != null) {
                player.sendMessage("На этом месте уже стоит кейс " + aleradyExists.getCaseName());
                return;
            }

            casesConfig.addCaseBlock(e.getClickedBlock(), caseName);
            player.sendMessage("Локация добавлена в конфиг");
            caseManager.loadCases();
        }

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            CaseHolder aleradyExists = caseManager.getHolders().stream()
                    .filter(holder -> {
                        for (GameCase gameCase : holder.getCases()) {
                            if (gameCase.getLocation().getBlock().equals(e.getClickedBlock()) && gameCase.getName().equalsIgnoreCase(caseName)){
                                return true;
                            }
                        }
                        return false;
                    }).findAny().orElse(null);

            if (aleradyExists == null) {
                player.sendMessage("На этом месте нет кейса " + caseName);
                return;
            }

            caseHolder.removeCase(caseHolder.getCaseBy(e.getClickedBlock().getLocation()));
            e.getClickedBlock().removeMetadata("case", plugin);

            casesConfig.removeCaseBlock(e.getClickedBlock().getLocation(), caseName);
            player.sendMessage("Локация удалена из конфига");
            caseManager.loadCases();
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        caseManager.getHolders().forEach(holder -> {
            Set<Supplier<GameCase>> creators = holder.getCreators(e.getWorld().getName());
            if (creators == null) return;

            creators.forEach(creator -> {
                holder.addCase(creator.get());
            });
        });
    }

}
