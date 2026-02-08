package ru.nbk.rolecases;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.eXo8_.placeholder.CasePlaceholder;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jdbi.v3.cache.caffeine.CaffeineCachePlugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.async.JdbiExecutor;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import ru.nbk.menus.menu.MenuManager;
import ru.nbk.menus.menu.MenuManagerFactory;
import ru.nbk.rolecases.cases.CaseHolder;
import ru.nbk.rolecases.cases.CaseManager;
import ru.nbk.rolecases.cases.GameCase;
import ru.nbk.rolecases.command.RoleCaseCommand;
import ru.nbk.rolecases.configuration.CasesConfig;
import ru.nbk.rolecases.database.ConnectionInfo;
import ru.nbk.rolecases.configuration.DatabaseConfig;
import ru.nbk.rolecases.database.dao.PlayerKeysDao;
import ru.nbk.rolecases.database.dao.entity.PrizeHistoryEntry;
import ru.nbk.rolecases.listener.GameListener;
import ru.nbk.rolecases.lucks.LuckManager;
import ru.nbk.rolecases.nms.CustomArmorStandHandler;
import ru.nbk.rolecases.reward.Reward;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HolyCases extends JavaPlugin {

    private Map<OfflinePlayer, GameCase> opens = new HashMap<>();

    private DatabaseConfig databaseConfig;
    private Jdbi jdbi;
    private JdbiExecutor jdbiExecutor;
    private CasesConfig casesConfig;
    private LuckManager luckManager;
    private MenuManager menuManager;
    private CaseManager caseManager;
    private CustomArmorStandHandler customArmorStandHandler;
    private YamlConfiguration config;
    private YamlConfiguration database;
    private static HolyCases instance;

    private boolean isSQL = false;

    private String url = "https://keyauth.win/api/1.1/";
    private String ownerid = "x6nHriHcAh"; // Owner ID
    private String appname = "Cases"; // Application Name
    private String version = "1.0"; // Application Version

    public void onEnable() {



        initConfig();
        // new Protect(appname, ownerid, version, url, this); //Защита, классы в util

        if (Objects.equals(config.getString("database"), "sql")) {
            this.databaseConfig = new DatabaseConfig(this);
            try {
                initJDBI(databaseConfig.getConnectionInfo());
            } catch (Exception ignored) {
                getLogger().severe("Настройте данные для подключения к базе данных в DatabaseConfig!");
                getLogger().severe("Отключение плагина.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            isSQL = true;
        } else if (Objects.equals(config.getString("database"), "yaml")) {
            initDatabaseYaml();
            isSQL = false;
        } else {
            getLogger().severe("Укажите верное значение в config! (sql или yaml)");
            getLogger().severe("Отключение плагина.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.menuManager = new MenuManagerFactory().createMenuManager(this);
        this.luckManager = new LuckManager(this);
        if (isSQL)
            this.casesConfig = new CasesConfig(this, luckManager, menuManager, jdbiExecutor);
        else
            this.casesConfig = new CasesConfig(this, luckManager, menuManager);
        this.caseManager = new CaseManager(this, casesConfig);

        new RoleCaseCommand(this, caseManager, luckManager, jdbiExecutor);
        this.customArmorStandHandler = new CustomArmorStandHandler(this);

        new GameListener(this, caseManager, casesConfig);

        caseManager.getHolders().forEach(CaseHolder::onReload);

        getLogger().info("Plugin " + getName() + " enabled!");



        // me.eXo8_
        instance = this;
        new CasePlaceholder(this, luckManager, caseManager).register();

    }

    public String getKeysForAllCases(Player player) {
        StringBuilder builder = new StringBuilder();

        caseManager.getCaseNames().forEach(caseName -> {
            int keys = getKeys(player, caseName);
            System.out.println("Кейс: " + caseName + ", Ключи: " + keys);
            builder.append(caseName).append(": ").append(keys).append(" ключей\n");
        });

        return builder.toString().trim();
    }

    public void onDisable() {
        for (OfflinePlayer player : opens.keySet().stream().toList()) {
            if (player != null) {
                GameCase gameCase = opens.get(player);
                if (gameCase != null) {
                    Reward winner = gameCase.getRandomReward();
                    gameCase.givePrize(player, winner);
                }
            }
        }

        try {
            if (customArmorStandHandler != null)
                customArmorStandHandler.onDisable();
            caseManager.getHolders().forEach(holder -> holder.getCases().forEach(gameCase -> {
                gameCase.getLocation().getBlock().removeMetadata("case", this);
            }));
        } catch (Exception ignored) {}
        getLogger().info("Plugin " + getName() + " disabled!");
    }

    public static HolyCases getInstance() {
        return instance;
    }


    private void initConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) saveResource("config.yml", false);
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void initDatabaseYaml() {
        File databaseFile = new File(getDataFolder(), "database.yml");
        if (!databaseFile.exists()) {
            getLogger().warning("Файл database.yml не найден! Создаю новый...");
            saveResource("database.yml", false); // Создаёт файл из ресурсов
        }
        database = YamlConfiguration.loadConfiguration(databaseFile);
    }



    private void initJDBI(ConnectionInfo info) {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(
                "jdbc:mysql://" + info.host() +
                        ":" + info.port() +
                        "/" + info.database() + "?autoReconnect=true&useSSL=false&characterEncoding=utf8"
        );
        config.setUsername(info.user());
        config.setPassword(info.password());

        HikariDataSource dataSource;
        try {
            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new CaffeineCachePlugin());
        jdbi.installPlugin(new SqlObjectPlugin());

        try (InputStream inputStream = this.getClass().getResourceAsStream("/template.sql")) {
            if (inputStream == null) {
                throw new RuntimeException("SQL template file not found: /template.sql");
            }
            jdbi.useHandle(handle -> handle.createScript(new String(inputStream.readAllBytes())).execute());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Executor executor = Executors.newFixedThreadPool(8);
        jdbiExecutor = JdbiExecutor.create(jdbi, executor);
    }

    @Override
    public void reloadConfig() {
        if (isSQL)
            this.casesConfig = new CasesConfig(this, luckManager, menuManager, jdbiExecutor);
        else
            this.casesConfig = new CasesConfig(this, luckManager, menuManager);
        initConfig();
    }

    @Override
    @Nonnull
    public YamlConfiguration getConfig() {
        return config;
    }

    public JdbiExecutor getExecutor() {
        return jdbiExecutor;
    }

    public void setItemStack(String caseName, ItemStack itemStack) {
        database.set("items." + caseName, itemStack);
        try {
            database.save(new File(getDataFolder(), "database.yml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ItemStack getItemStack(String caseName) {
        if (!database.contains("items." + caseName)) return null;
        return database.getItemStack("items." + caseName);
    }

    public void addKeys(OfflinePlayer player, String key, int value) {
        int keys = getKeys(player, key);
        database.set("keys." + key + "." + player.getUniqueId(), keys + value);
        try {
            database.save(new File(getDataFolder(), "database.yml"));
        } catch (IOException e) {
            getLogger().severe("Не удалось сохранить ключи в database.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void removeKeys(OfflinePlayer player, String key, int value) {
        int keys = getKeys(player, key);
        int newValue = Math.max(keys - value, 0);
        database.set("keys." + key + "." + player.getUniqueId(), newValue);
        try {
            database.save(new File(getDataFolder(), "database.yml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addLuck(OfflinePlayer player, String key, int value) {
        int lucks = getLucks(player, key);
        database.set("lucks." + key + "." + player.getUniqueId(), lucks + value);
        try {
            database.save(new File(getDataFolder(), "database.yml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void removeLuck(OfflinePlayer player, String key, int value) {
        if (GameCase.luckWorking)
        {
            int lucks = getLucks(player, key);
            int newValue = Math.max(lucks - value, 0);
            database.set("lucks." + key + "." + player.getUniqueId(), newValue);
            try {
                database.save(new File(getDataFolder(), "database.yml"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addPrizeHistory(String opener, String caseName, String luckName, String prizeName, String prizeMaterial, long timestamp, String serverName) {
        UUID uuid = UUID.randomUUID();
        database.set("history." + uuid + ".opener", opener);
        database.set("history." + uuid + ".caseName", caseName);
        database.set("history." + uuid + ".luckName", luckName);
        database.set("history." + uuid + ".prizeName", prizeName);
        database.set("history." + uuid + ".prizeMaterial", prizeMaterial);
        database.set("history." + uuid + ".timestamp", timestamp);
        database.set("history." + uuid + ".serverName", serverName);
        try {
            database.save(new File(getDataFolder(), "database.yml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<PrizeHistoryEntry> getLastPrizes(String caseName, int limit) {
        Collection<PrizeHistoryEntry> lastPrizes = new ArrayList<>();
        int amount = 0;
        if (database.contains("history")) {
            for (String uuid : database.getConfigurationSection("history").getKeys(false)) {
                ConfigurationSection section = database.getConfigurationSection("history." + uuid);
                if (Objects.equals(section.getString("caseName"), caseName)) {
                    lastPrizes.add(new PrizeHistoryEntry(
                            section.getString("opener"),
                            section.getString("caseName"),
                            section.getString("luckName"),
                            section.getString("prizeName"),
                            section.getString("prizeMaterial"),
                            section.getLong("timestamp"),
                            section.getString("serverName")
                    ));
                    amount++;
                    if (amount >= limit) break;
                }
            }
        }
        return lastPrizes;
    }

    public int getKeys(OfflinePlayer player, String key) {
        if (!isSQL)
        {
            if (database == null)
            {
                getLogger().severe("Ошибка: database.yml не загружен!");
                return 0;
            }
            return database.getInt("keys." + key + "." + player.getUniqueId(), 0);
        }
        return 0;
    }
    public CompletableFuture<Integer> getKeysC(OfflinePlayer player, String key) {
        if (!isSQL)
        {
            if (database == null) {
                getLogger().severe("Ошибка: database.yml не загружен!");
                return CompletableFuture.completedFuture(0); // Возвращаем значение 0 сразу
            }
            int keys = database.getInt("keys." + key + "." + player.getUniqueId(), 0);
            return CompletableFuture.completedFuture(keys);
        } else {
            // Асинхронная работа с SQL
            return jdbiExecutor.withExtension(PlayerKeysDao.class, dao -> dao.getKeys(player.getUniqueId(), key))
                    .toCompletableFuture()
                    .thenApply(count -> {
                        try {
                            return count;
                        } catch (NumberFormatException e) {
                            getLogger().warning("Некорректный формат данных в базе: " + count);
                            return 0; // Если значение в базе не число, возвращаем 0
                        }
                    });
        }
    }


    public int getLucks(OfflinePlayer player, String key) {
        return database.getInt("lucks." + key + "." + player.getUniqueId(), 0);
    }

    public CasesConfig getCasesConfig() {
        return casesConfig;
    }

    public boolean isSQL() {
        return isSQL;
    }

    public void addPlayer(Player player, GameCase gameCase) {
        opens.put(player, gameCase);
    }

    public void removePlayer(OfflinePlayer player) {
        opens.remove(player);
    }
}
