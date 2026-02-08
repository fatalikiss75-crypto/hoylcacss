package me.eXo8_.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.nbk.rolecases.HolyCases;
import ru.nbk.rolecases.cases.CaseManager;
import ru.nbk.rolecases.lucks.Luck;
import ru.nbk.rolecases.lucks.LuckManager;

import java.util.concurrent.CompletableFuture;

public class CasePlaceholder extends PlaceholderExpansion
{
    private final LuckManager luckManager;
    private final CaseManager caseManager;
    private final HolyCases plugin;

    public CasePlaceholder(HolyCases plugin, LuckManager luckManager, CaseManager caseManager) {
        this.plugin = plugin;
        this.luckManager = luckManager;
        this.caseManager = caseManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cases";
    }

    @Override
    public @NotNull String getAuthor() {
        return "eXo8_";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "Игрок не найден";

        if (identifier.equalsIgnoreCase("luck15_amount"))
        {
            Luck luck15 = luckManager.getLuckByName("luck15");
            if (luck15 != null)
            {
                return String.valueOf(luck15.getLuckFor(player).join());
            }
            return "0";
        }

        if (identifier.equalsIgnoreCase("luck30_amount"))
        {
            Luck luck30 = luckManager.getLuckByName("luck30");
            if (luck30 != null) {
                return String.valueOf(luck30.getLuckFor(player).join());
            }
            return "0";
        }

        if (identifier.startsWith("all_amount"))
        {
            String allKeys = plugin.getKeysForAllCases(player);
            return allKeys != null ? allKeys : "0";
        }

        if (identifier.endsWith("_amount"))
        {
            String caseName = identifier.substring(0, identifier.length() - 7);
            if (caseManager.getCaseHolder(caseName) == null) {
                return "0";
            }

            CompletableFuture<Integer> keys = plugin.getKeysC(player, caseName);
            return String.valueOf(keys.join());
        }

        return "Плейсхлдер не найден";
    }


    public String replacePlaceholders(String input, OfflinePlayer player)
    {
        String result = input;

        result = replaceCaseKeys(result, player);
        result = replaceLuck(result, player);

        return result;
    }

    private String replaceCaseKeys(String input, OfflinePlayer player)
    {
        if (input.contains("_amount%")) {
            for (String caseName : caseManager.getCaseNames()) {
                if (input.contains("%cases_" + caseName + "_amount%")) {
                    int keys = plugin.getKeys(player, caseName);
                    input = input.replace("%cases_" + caseName + "_amount%", String.valueOf(keys));
                }
            }
        }

        return input;
    }

    private String replaceLuck(String input, OfflinePlayer player) {
        if (luckManager != null) {
            for (Luck luck : luckManager.getLucks()) {
                String placeholder = "%cases_" + luck.getName() + "_amount%";
                if (input.contains(placeholder)) {
                    int luckAmount = luck.getLuckFor(player.getPlayer()).join();
                    input = input.replace(placeholder, String.valueOf(luckAmount));
                }
            }
        }
        return input;
    }
}
