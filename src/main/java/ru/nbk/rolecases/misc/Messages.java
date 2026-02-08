package ru.nbk.rolecases.misc;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.nbk.rolecases.HolyCases;
import ru.nbk.rolecases.misc.ColorUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Messages {
    private final Map<String, List<String>> messages = new HashMap<>();

    @SafeVarargs
    public Messages(Map.Entry<String, List<String>>... messages) {
        for (Map.Entry<String, List<String>> message : messages) {
            addMessage(message.getKey(), message.getValue());
        }
    }

    public void addMessage(String key, List<String> lines) {
        List<String> coloredLines = new ArrayList<>();
        for (String line : lines)
            coloredLines.add(ColorUtil.parse(line));
        messages.put(key, coloredLines);
    }

    public void sendMessage(Player player, String key) {
        if (messages.containsKey(key))
            messages.get(key).forEach(player::sendMessage);
    }
}
