package ru.nbk.rolecases.database.dao.entity;

import org.bukkit.Color;

import java.beans.ConstructorProperties;

public record PrizeHistoryEntry(String opener, String caseName, String luckName, String prizeName, String prizeMaterial, long timestamp, String serverName) {

    @ConstructorProperties({"opener", "case_name", "luck_name", "prize_name", "prize_material", "timestamp", "server_name"})
    public PrizeHistoryEntry {}

}
