package ru.nbk.rolecases.cases;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.Supplier;

public class CaseHolder {

    private Plugin plugin;
    private String caseName;
    private Set<GameCase> cases;
    private Map<String, Set<Supplier<GameCase>>> caseCreators;

    public CaseHolder(Plugin plugin, String caseName) {
        this.plugin = plugin;
        this.caseName = caseName;
        this.cases = new HashSet<>();
        this.caseCreators = new HashMap<>();
    }

    public String getCaseName() {
        return caseName;
    }

    public GameCase getCaseBy(Location location) {
        for (GameCase gameCase : getCases()) {
            Location loc = gameCase.getLocation();
            if (loc.getX() == location.getX()
             && loc.getY() == location.getY()
             && loc.getZ() == location.getZ())
                return gameCase;
        }

        return null;
    }

    public void addCase(GameCase gameCase) {
        cases.add(gameCase);
        gameCase.getLocation().getBlock().setMetadata("case", new FixedMetadataValue(plugin, gameCase));
    }

    public void removeCase(GameCase gameCase) {
        cases.remove(gameCase);
        gameCase.deleteTitle();
        gameCase.getLocation().getBlock().removeMetadata("case", plugin);
        caseCreators.forEach((worldName, creators) -> {
            if (Bukkit.getWorld(worldName) == null) return;
            creators.forEach(creator -> {
                if (creator.get() == gameCase)
                    creators.remove(creator);
            });
        });
    }

    public void addCreator(String worldName, Supplier<GameCase> creator) {
        if (!caseCreators.containsKey(worldName)) {
            caseCreators.put(worldName, new HashSet<>());
        }

        caseCreators.get(worldName).add(creator);
    }

    public Set<Supplier<GameCase>> getCreators(String worldName) {
        return caseCreators.get(worldName);
    }

    public Collection<GameCase> getCases() {
        return Collections.unmodifiableCollection(cases);
    }

    public void onReload() {
        caseCreators.forEach((worldName, creators) -> {
            if (Bukkit.getWorld(worldName) == null) return;
            long loaded = cases.stream()
                    .filter(gameCase -> gameCase.getLocation().getWorld().getName().equals(worldName))
                    .count();
            if (loaded == 0) {
                creators.forEach(creator -> addCase(creator.get()));
            }
        });
    }
}
