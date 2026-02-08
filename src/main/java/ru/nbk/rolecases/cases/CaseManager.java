package ru.nbk.rolecases.cases;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import ru.nbk.rolecases.configuration.CasesConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class CaseManager {

    private Plugin plugin;
    private CasesConfig casesConfig;
    private Map<String, CaseHolder> holders;

    public CaseManager(Plugin plugin, CasesConfig casesConfig) {
        this.plugin = plugin;
        this.casesConfig = casesConfig;
        this.holders = casesConfig.getCases();

        startParticlesTask();
    }

    public void loadCases() {
        this.holders = casesConfig.getCases();
        getHolders().forEach(CaseHolder::onReload);
    }

    public Collection<CaseHolder> getHolders() {
        return Collections.unmodifiableCollection(holders.values());
    }

    public Collection<String> getCaseNames() {
        return Collections.unmodifiableCollection(holders.keySet());
    }

    public CaseHolder getCaseHolder(String name) {
        return holders.get(name);
    }


    private void startParticlesTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                holders.values()
                        .forEach(caseHolder -> caseHolder.getCases()
                                .forEach(gameCase -> {

                                    if (gameCase.isOpening()) return;

                                    Location caseLocation = gameCase.getLocation();
                                    gameCase.getWrappedParticles().forEach(wrappedParticle -> {
                                        caseLocation.getWorld().spawnParticle(wrappedParticle.particle(),
                                                caseLocation.clone().add(0.5, 0.5, 0.5),
                                                wrappedParticle.count(),
                                                wrappedParticle.offX(),
                                                wrappedParticle.offY(),
                                                wrappedParticle.offZ(),
                                                wrappedParticle.speed(),
                                                wrappedParticle.getData());
                                    });
                                }));
            }
        }.runTaskTimer(plugin, 0, 20);
    }

}
