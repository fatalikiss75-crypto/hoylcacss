package ru.nbk.rolecases.database.dao.argument;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.sql.Types;
import java.util.UUID;

public class UUIDArgumentFactory extends AbstractArgumentFactory<UUID> {

    public UUIDArgumentFactory() {
        super(Types.VARCHAR);
    }

    @Override
    protected Argument build(UUID value, ConfigRegistry config) {
        return new UUIDArgument(value);
    }
}
