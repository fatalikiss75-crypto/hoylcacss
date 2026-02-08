package ru.nbk.rolecases.database.dao;

import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import ru.nbk.rolecases.database.dao.argument.UUIDArgumentFactory;

import java.util.UUID;

@RegisterArgumentFactory(value = UUIDArgumentFactory.class)
public interface PlayerLuckDao {
    @SqlUpdate("INSERT IGNORE INTO player_luck(player, luck_name, `count`) VALUES (:player, :luck_name, :count) ON DUPLICATE KEY UPDATE count = count + :count")
    void addLuck(@Bind("player") UUID player, @Bind("luck_name") String luck_name, @Bind("count") int count);

    @SqlUpdate("INSERT IGNORE INTO player_luck(player, luck_name, `count`) VALUES (:player, :luck_name, :count) ON DUPLICATE KEY UPDATE count = count - :count")
    void withdrawLuck(@Bind("player") UUID player, @Bind("luck_name") String luck_name, @Bind("count") int count);

    @SqlQuery("SELECT COALESCE(SUM(`count`), 0) FROM player_luck WHERE player = :player AND luck_name = :luck_name")
    int getLuck(@Bind("player") UUID player, @Bind("luck_name") String luck_name);
}