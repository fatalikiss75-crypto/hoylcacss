package ru.nbk.rolecases.database.dao;

import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import ru.nbk.rolecases.database.dao.argument.UUIDArgumentFactory;

import java.util.UUID;

@RegisterArgumentFactory(value = UUIDArgumentFactory.class)
public interface PlayerKeysDao {
    @SqlUpdate("INSERT IGNORE INTO player_keys(player, key_name, `count`) VALUES (:player, :key_name, :count) ON DUPLICATE KEY UPDATE count = count + :count")
    void addKeys(@Bind("player") UUID player, @Bind("key_name") String key_name, @Bind("count") int count);

    @SqlUpdate("INSERT IGNORE INTO player_keys(player, key_name, `count`) VALUES (:player, :key_name, :count) ON DUPLICATE KEY UPDATE count = count - :count")
    void withdrawKeys(@Bind("player") UUID player, @Bind("key_name") String key_name, @Bind("count") int count);

    @SqlQuery("SELECT COALESCE(SUM(`count`), 0) FROM player_keys WHERE player = :player AND key_name = :key_name")
    int getKeys(@Bind("player") UUID player, @Bind("key_name") String key_name);
}
