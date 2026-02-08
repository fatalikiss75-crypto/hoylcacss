package ru.nbk.rolecases.configuration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import ru.nbk.rolecases.database.ConnectionInfo;

import java.io.File;
import java.io.IOException;

public class DatabaseConfig {

    private File rawConfig;
    private YamlConfiguration config;

    public DatabaseConfig(Plugin plugin) {
        this.rawConfig = new File(plugin.getDataFolder(), "DatabaseConfig.yml");
        if (!rawConfig.exists()) {
            try {
                rawConfig.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        this.config = YamlConfiguration.loadConfiguration(rawConfig);

        checkDefault();
        save();
    }

    private void save() {
        try {
            config.save(rawConfig);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setIfNotExists(String path, Object o) {
        if (!config.contains(path)) {
            config.set(path, o);
        }
    }

    private void checkDefault() {
        setIfNotExists("mysql.host", "localhost");
        setIfNotExists("mysql.port", 3306);
        setIfNotExists("mysql.database", "db");
        setIfNotExists("mysql.user", "root");
        setIfNotExists("mysql.password", "root");
        setIfNotExists("mysql.group-concat-length", 1000000);
    }

    public ConnectionInfo getConnectionInfo() {
        String host = config.getString("mysql.host");
        int port = config.getInt("mysql.port");
        String database = config.getString("mysql.database");
        String user = config.getString("mysql.user");
        String password = config.getString("mysql.password");
        int groupConcatLength = config.getInt("mysql.group-concat-length");

        return new ConnectionInfo(host, port, database ,user, password, groupConcatLength);
    }
}
