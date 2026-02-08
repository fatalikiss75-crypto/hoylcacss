package ru.nbk.rolecases.database.dao;

import org.bukkit.Color;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import ru.nbk.rolecases.database.dao.entity.PrizeHistoryEntry;

import java.util.Collection;

public interface PrizeHistoryDao {

    @SqlUpdate("insert ignore into prize_history(opener, case_name, luck_name, prize_name, prize_material, timestamp, server_name) values(?,?,?,?,?,?,?)")
    void addEntry(String opener, String caseName, String luckName, String prizeName, String prizeMaterial, long timestamp, String serverName);

    @SqlQuery("select * from prize_history where case_name = :caseName order by timestamp desc limit :limit")
    @RegisterConstructorMapper(value = PrizeHistoryEntry.class)
    Collection<PrizeHistoryEntry> getLastPrizes(@Bind("caseName") String caseName, @Bind("limit") int limit);

}
