package ru.nbk.menus.menu;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import ru.nbk.menus.menu.impl.version.v1_21_R1.MenuManagerImpl;
import ru.nbk.rolecases.HolyCases;

public class MenuManagerFactory {

    public MenuManager createMenuManager(HolyCases plugin) {
        switch (Bukkit.getServer().getBukkitVersion().split("-")[0]){
            case "1.21.1": return new MenuManagerImpl(plugin);
            case "1.21": return new MenuManagerImpl(plugin);
        }
        return null;
    }

}
