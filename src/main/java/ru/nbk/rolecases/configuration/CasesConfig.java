    package ru.nbk.rolecases.configuration;
    
    import com.mojang.datafixers.util.Pair;
    import me.clip.placeholderapi.PlaceholderAPI;
    import org.bukkit.*;
    import org.bukkit.block.Block;
    import org.bukkit.block.BlockFace;
    import org.bukkit.block.data.Directional;
    import org.bukkit.configuration.ConfigurationSection;
    import org.bukkit.configuration.file.YamlConfiguration;
    import org.bukkit.entity.Player;
    import org.bukkit.inventory.ItemStack;
    import org.jdbi.v3.core.async.JdbiExecutor;
    import ru.nbk.menus.menu.Menu;
    import ru.nbk.menus.menu.MenuItem;
    import ru.nbk.menus.menu.MenuManager;
    import ru.nbk.rolecases.HolyCases;
    import ru.nbk.rolecases.animation.Animation;
    import ru.nbk.rolecases.animation.CircleAnimation;
    import ru.nbk.rolecases.animation.SpinningAnimation;
    import ru.nbk.rolecases.cases.CaseHolder;
    import ru.nbk.rolecases.cases.ConfigurableCase;
    import ru.nbk.rolecases.cases.GameCase;
    import ru.nbk.rolecases.misc.Messages;
    import ru.nbk.rolecases.database.dao.PlayerKeysDao;
    import ru.nbk.rolecases.database.dao.PrizeHistoryDao;
    import ru.nbk.rolecases.database.dao.entity.PrizeHistoryEntry;
    import ru.nbk.rolecases.lucks.LuckManager;
    import ru.nbk.rolecases.misc.ColorUtil;
    import ru.nbk.rolecases.opentype.Open;
    import ru.nbk.rolecases.opentype.impls.ClickOpen;
    import ru.nbk.rolecases.opentype.impls.DoubleClickOpen;
    import ru.nbk.rolecases.opentype.impls.ItemOpen;
    import ru.nbk.rolecases.opentype.impls.KeyOpen;
    import ru.nbk.rolecases.particle.WrappedParticle;
    import ru.nbk.rolecases.reward.Prize;
    import ru.nbk.rolecases.reward.Reward;
    import ru.nbk.util.ItemBuilder;

    import java.io.File;
    import java.io.IOException;
    import java.text.SimpleDateFormat;
    import java.time.ZoneId;
    import java.util.*;
    import java.util.concurrent.CompletableFuture;
    import java.util.function.Function;
    import java.util.function.Supplier;
    import java.util.stream.Collectors;
    
    public class CasesConfig {

        private final HolyCases plugin;
        private final LuckManager luckManager;
        private final MenuManager menuManager;
        private JdbiExecutor executor;
        private final File rawConfig;
        private final YamlConfiguration config;

        public CasesConfig(HolyCases plugin, LuckManager luckManager, MenuManager menuManager, JdbiExecutor executor) {
            this.plugin = plugin;
            this.luckManager = luckManager;
            this.menuManager = menuManager;
            this.executor = executor;

            this.rawConfig = new File(plugin.getDataFolder(), "CasesConfig.yml");
            if (!rawConfig.exists()) plugin.saveResource("CasesConfig.yml", false);
            this.config = YamlConfiguration.loadConfiguration(rawConfig);
        }

        public CasesConfig(HolyCases plugin, LuckManager luckManager, MenuManager menuManager) {
            this.plugin = plugin;
            this.luckManager = luckManager;
            this.menuManager = menuManager;

            this.rawConfig = new File(plugin.getDataFolder(), "CasesConfig.yml");
            if (!rawConfig.exists()) plugin.saveResource("CasesConfig.yml", false);
            this.config = YamlConfiguration.loadConfiguration(rawConfig);
        }

        public Map<String, CaseHolder> getCases() {
            Map<String, CaseHolder> cases = new HashMap<>();

            ConfigurationSection casesSection = config.getConfigurationSection("cases");
            if (casesSection == null) {
                return cases;
            }

            for (String caseName : casesSection.getKeys(false)) {
                ConfigurationSection caseSection = config.getConfigurationSection("cases." + caseName);
                if (caseSection == null) {
                    continue;
                }

                Open open = null;

                ConfigurationSection openSection = caseSection.getConfigurationSection("open");
                if (openSection != null) {
                    try {
                        Open.Type openType = Open.Type.valueOf(openSection.getString("type").toUpperCase());
                        switch (openType) {
                            case KEY -> open = new KeyOpen(plugin, executor, luckManager);
                            case RIGHT, LEFT -> open = getClickOpen(openSection, openType);
                            case LEFTRIGHT -> {
                                ClickOpen leftClick = getClickOpen(openSection, Open.Type.LEFT);
                                ClickOpen rightClick = getClickOpen(openSection, Open.Type.RIGHT);

                                open = new DoubleClickOpen(leftClick, rightClick);
                            }
                            case ITEM -> {
                                int amount = openSection.getInt("amount");
                                String failedBuySlot = openSection.getString("item-failed-buy-slot");
                                open = new ItemOpen(plugin, luckManager, amount, failedBuySlot);
                            }
                        }
                    } catch (Exception ignored) {
                        continue;
                    }
                }

                if (open == null) continue;

                String titleAboveCase = ColorUtil.parse(caseSection.getString("title-above-case", ""));
                double titleAboveCaseYOffset = caseSection.getDouble("title-above-case-y-offset", 0.0);

                boolean luckWork = caseSection.getBoolean("luck_work", true);

                String animationType = caseSection.getString("animation.type", "circle");

                // CircleAnimation Configuration
                boolean randomRewards = caseSection.getBoolean("circle-animation.random-rewards", false);
                double rewardRadius = caseSection.getDouble("circle-animation.reward-radius", 0.0);
                Sound chestOpenSound = parseSound(caseSection.getString("circle-animation.chest-open-sound"));
                Sound chestCloseSound = parseSound(caseSection.getString("circle-animation.chest-close-sound"));
                int startDelay = caseSection.getInt("circle-animation.start.delay", 0);
                int spawnInterval = caseSection.getInt("circle-animation.start.spawn-interval", 0);
                int rewardUpTicks = caseSection.getInt("circle-animation.open.reward-up-ticks", 0);
                double openRotateIntoPlaceAngle = caseSection.getDouble("circle-animation.open.rotate-into-place-angle", 0.0);
                double spinStartAnglePerTick = caseSection.getDouble("circle-animation.spin.start-angle-per-tick", 0.0);
                double spinMinimalAnglePerTick = caseSection.getDouble("circle-animation.spin.minimal-angle-per-tick", 0.0);
                int minimumSlowdownTicks = caseSection.getInt("circle-animation.spin.minimum-slowdown-ticks", 0);
                int maximumSlowdownTicks = caseSection.getInt("circle-animation.spin.maximum-slowdown-ticks", 0);
                int minimumSpinIdleTicks = caseSection.getInt("circle-animation.spin.minimum-idle-ticks", 0);
                int maximumSpinIdleTicks = caseSection.getInt("circle-animation.spin.maximum-idle-ticks", 0);
                int endDelay = caseSection.getInt("circle-animation.end.delay", 0);
                int rewardDownTicks = caseSection.getInt("circle-animation.end.down-ticks", 0);
                double endRotateIntoPlaceAngle = caseSection.getDouble("circle-animation.end.rotate-into-place-angle", 0.0);

                CircleAnimation circleAnimation = new CircleAnimation(plugin,
                        randomRewards,
                        rewardRadius,
                        startDelay,
                        spawnInterval,
                        rewardUpTicks,
                        openRotateIntoPlaceAngle,
                        spinStartAnglePerTick,
                        spinMinimalAnglePerTick,
                        minimumSlowdownTicks,
                        maximumSlowdownTicks,
                        minimumSpinIdleTicks,
                        maximumSpinIdleTicks,
                        endDelay,
                        rewardDownTicks,
                        endRotateIntoPlaceAngle,
                        chestOpenSound,
                        chestCloseSound);

                // SpinningAnimation Configuration
                double liftHeight = caseSection.getDouble("spinning-animation.lift-height", 0.0);
                int liftDuration = caseSection.getInt("spinning-animation.lift-duration", 0);
                int spinDuration = caseSection.getInt("spinning-animation.spin-duration", 0);
                int changeInterval = caseSection.getInt("spinning-animation.change-interval", 0);
                int dropDuration = caseSection.getInt("spinning-animation.drop-duration", 0);
                Sound liftSound = parseSound(caseSection.getString("spinning-animation.lift-sound"));
                Sound dropSound = parseSound(caseSection.getString("spinning-animation.drop-sound"));

                SpinningAnimation spinningAnimation = new SpinningAnimation(plugin,
                        liftHeight,
                        liftDuration,
                        spinDuration,
                        changeInterval,
                        dropDuration,
                        liftSound,
                        dropSound);

                Animation animation;
                if ("circle".equalsIgnoreCase(animationType)) {
                    animation = circleAnimation;
                } else if ("spinning".equalsIgnoreCase(animationType)) {
                    animation = spinningAnimation;
                } else {
                    animation = circleAnimation;
                }

                Messages messages = new Messages();
                ConfigurationSection messagesSection = caseSection.getConfigurationSection("messages");
                if (messagesSection != null) {
                    for (String key : messagesSection.getKeys(false)) {
                        messages.addMessage(key, messagesSection.getStringList(key));
                    }
                }

                List<Reward> rewards = new ArrayList<>();

                List<String> prizeMessage = caseSection.getStringList("prize-messages").stream()
                        .map(ColorUtil::parse)
                        .collect(Collectors.toList());

                ConfigurationSection rewardsSection = caseSection.getConfigurationSection("rewards");
                if (rewardsSection != null) {
                    for (String rewardName : rewardsSection.getKeys(false)) {
                        Material material = parseMaterial(rewardsSection.getString(rewardName + ".material"));
                        Color color = rewardsSection.getColor(rewardName + ".color");
                        boolean glow = rewardsSection.getBoolean(rewardName + ".glow", false);
                        String name = ColorUtil.parse(rewardsSection.getString(rewardName + ".name", ""));
                        Map<String, Double> chances = new HashMap<>();
                        double firstWeight = 0;
                        if (rewardsSection.contains(rewardName + ".chances")) {
                            for (String luck : rewardsSection.getConfigurationSection(rewardName + ".chances").getKeys(false)) {
                                double weight = rewardsSection.getDouble(rewardName + ".chances." + luck);
                                if (firstWeight == 0)
                                    firstWeight = weight;
                                chances.put(luck, weight);
                            }
                        }
                        boolean sendTitle = rewardsSection.getBoolean(rewardName + ".send-title", false);
                        boolean addToHistory = rewardsSection.getBoolean(rewardName + ".add-to-history", false);

                        Reward reward = new Reward(name, chances, material, color, glow, sendTitle, addToHistory);

                        ConfigurationSection prizesSection = rewardsSection.getConfigurationSection(rewardName + ".prizes");
                        if (prizesSection != null) {
                            for (String prize : prizesSection.getKeys(false)) {
                                String prizeName = ColorUtil.parse(prizesSection.getString(prize + ".name", ""));
                                String title = ColorUtil.parse(prizesSection.getString(prize + ".title.title", ""));
                                String subtitle = ColorUtil.parse(prizesSection.getString(prize + ".title.subtitle", ""));
                                List<String> commands = prizesSection.getStringList(prize + ".commands");
                                Map<String, Double> prizeChances = new HashMap<>();
                                if (prizesSection.contains(prize + ".chances")) {
                                    for (String luck : prizesSection.getConfigurationSection(prize + ".chances").getKeys(false)) {
                                        double weight = prizesSection.getDouble(prize + ".chances." + luck);
                                        prizeChances.put(luck, weight);
                                    }
                                }

                                reward.addPrize(new Prize(prizeName, title, subtitle, commands), prizeChances);
                            }
                        }

                        rewards.add(reward);
                    }
                }

                List<WrappedParticle> particles = new ArrayList<>();

                ConfigurationSection particlesSection = caseSection.getConfigurationSection("particles");
                if (particlesSection != null) {
                    for (String particlesKey : particlesSection.getKeys(false)) {
                        Particle particle = parseParticle(particlesSection.getString(particlesKey + ".particle"));
                        int count = particlesSection.getInt(particlesKey + ".count", 0);
                        double speed = particlesSection.getDouble(particlesKey + ".speed", 0.0);
                        double xOffset = particlesSection.getDouble(particlesKey + ".xOffset", 0.0);
                        double yOffset = particlesSection.getDouble(particlesKey + ".yOffset", 0.0);
                        double zOffset = particlesSection.getDouble(particlesKey + ".zOffset", 0.0);
                        int red = particlesSection.getInt(particlesKey + ".r", 0);
                        int green = particlesSection.getInt(particlesKey + ".g", 0);
                        int blue = particlesSection.getInt(particlesKey + ".b", 0);

                        WrappedParticle wrappedParticle = new WrappedParticle(particle, count, xOffset, yOffset, zOffset, speed, red, green, blue);
                        particles.add(wrappedParticle);
                    }
                }

                CaseHolder caseHolder = new CaseHolder(plugin, caseName);

                Open finalOpen = open;
                caseSection.getStringList("locations").stream()
                        .map(wrapped -> {
                            String[] args = wrapped.split(" ");

                            Supplier<World> world = () -> Bukkit.getWorld(args[0]);
                            int x = Integer.parseInt(args[1]);
                            int y = Integer.parseInt(args[2]);
                            int z = Integer.parseInt(args[3]);
                            Supplier<Location> location = () -> new Location(world.get(), x, y, z);

                            BlockFace blockFace = BlockFace.valueOf(args[4]);

                            return Pair.of(args[0], Pair.of(location, blockFace));
                        }).forEach(position -> {
                            Function<Player, Menu> menuCreator = player -> {
                                ConfigurationSection menuSection = caseSection.getConfigurationSection("menu");
                                if (menuSection == null) {
                                    return null; // Если меню отсутствует, возвращаем null
                                }

                                String menuTitle = ColorUtil.parse(menuSection.getString("title", ""));
                                int menuSize = menuSection.getInt("size", 54);
                                String cooldownType = menuSection.getString("cooldown.type", "GLOBAL");
                                int cooldownTime = menuSection.getInt("cooldown.time", 10);
                                Menu caseMenu = menuManager.createMenu(menuTitle, menuSize);
                                caseMenu.setCooldown(cooldownType, cooldownTime);
                                caseMenu.onClick(e -> e.setCancelled(true));

                                Map<String, ItemStack> itemTypes = new HashMap<>();

                                if (plugin.isSQL()) {
                                    executor.useExtension(PlayerKeysDao.class, dao -> {
                                        ConfigurationSection itemTypesSection = menuSection.getConfigurationSection("item-types");
                                        if (itemTypesSection != null) {
                                            List<CompletableFuture<Void>> futures = new ArrayList<>();

                                            for (String typeKey : itemTypesSection.getKeys(false)) {
                                                UUID playerUUID = player.getUniqueId();

                                                CompletableFuture<Integer> keysFuture = CompletableFuture.supplyAsync(() -> dao.getKeys(playerUUID, caseName));
                                                CompletableFuture<String> luckFuture = CompletableFuture.supplyAsync(() -> luckManager.getLuckLines(player));

                                                CompletableFuture<Void> combinedFuture = keysFuture.thenCombine(luckFuture, (keys, lucks) -> {
                                                    ItemStack itemStack = ItemBuilder.fromConfig(itemTypesSection.getConfigurationSection(typeKey), text ->
                                                            PlaceholderAPI.setPlaceholders(player, ColorUtil.parse(
                                                            text.replace("%player_keys%", "" + keys).replace("%lucks%", lucks))
                                                    )).build();

                                                    itemTypes.put(typeKey, itemStack);
                                                    return null;
                                                });

                                                futures.add(combinedFuture);
                                            }

                                            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                                            allOf.join();
                                        }
                                    }).whenComplete((v, ex) -> {
                                        List<String> wrappedInventory = menuSection.getStringList("inventory");
                                        for (int inventoryRow = 0; inventoryRow < wrappedInventory.size(); inventoryRow++) {
                                            String[] wrappedSlotItem = wrappedInventory.get(inventoryRow).split(" ");
                                            for (int index = 0; index < wrappedSlotItem.length; index++) {
                                                int inventorySlot = index + (inventoryRow * 9);
                                                String itemType = wrappedSlotItem[index];
                                                ItemStack icon = itemTypes.get(itemType);

                                                MenuItem menuItem;
                                                if (itemType.equals("C")) {
                                                    menuItem = menuManager.createItem(icon, (whoClick, clickType) -> {
                                                        GameCase gameCase = (GameCase) ((Location)((Supplier<?>)((Pair<?, ?>)position.getSecond()).getFirst()).get()).getBlock().getMetadata("case").get(0).value();
                                                        if (gameCase != null) {
                                                            if (gameCase.isOpening()) {
                                                                whoClick.closeInventory();
                                                                gameCase.getMessages().sendMessage(whoClick, "case-already-opening");
                                                            } else {
                                                                finalOpen.open(gameCase, clickType, whoClick);
                                                            }
                                                        }
                                                    });
                                                } else if (itemType.equals("W")) {
                                                    caseMenu.setItem(inventorySlot, menuManager.createItem(icon));

                                                    int historyCount = (inventoryRow + 1) * 9 - inventorySlot - 1;
                                                    executor.withExtension(PrizeHistoryDao.class, dao -> dao.getLastPrizes(caseName, historyCount)).whenComplete((history, ex1) -> {
                                                        int counter = 1;
                                                        String prizeItemName = ColorUtil.parse(menuSection.getString("last-rewards.item-name", ""));
                                                        List<String> prizeItemLore = menuSection.getStringList("last-rewards.item-lore").stream()
                                                                .map(ColorUtil::parse)
                                                                .toList();

                                                        SimpleDateFormat fmt = new SimpleDateFormat("dd MMM HH:mm:ss", new Locale("ru"));
                                                        fmt.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Europe/Moscow")));

                                                        for (PrizeHistoryEntry prizeHistoryEntry : history) {
                                                            String[] args = prizeHistoryEntry.prizeMaterial().split(":");
                                                            Color color = getColor(args);
                                                            ItemStack prizeIcon = new ItemBuilder(Material.valueOf(args[0].toUpperCase()))
                                                                    .name(prizeItemName
                                                                            .replace("%player_name%", prizeHistoryEntry.opener())
                                                                            .replace("%prize_name%", prizeHistoryEntry.prizeName())
                                                                            .replace("%server_name%", prizeHistoryEntry.serverName())
                                                                            .replace("%date%", fmt.format(new Date(prizeHistoryEntry.timestamp())))
                                                                            .replace("%luck%", prizeHistoryEntry.luckName())
                                                                    )
                                                                    .lore(prizeItemLore.stream()
                                                                            .map(line -> line
                                                                                    .replace("%player_name%", prizeHistoryEntry.opener())
                                                                                    .replace("%prize_name%", prizeHistoryEntry.prizeName())
                                                                                    .replace("%server_name%", prizeHistoryEntry.serverName())
                                                                                    .replace("%date%", fmt.format(new Date(prizeHistoryEntry.timestamp())))
                                                                                    .replace("%luck%", prizeHistoryEntry.luckName())
                                                                            )
                                                                            .collect(Collectors.toList()))
                                                                    .color(color)
                                                                    .removeAttributes()
                                                                    .build();
                                                            caseMenu.setItem(inventorySlot + counter, menuManager.createItem(prizeIcon));
                                                            counter++;
                                                        }
                                                    });
                                                    continue;
                                                } else {
                                                    menuItem = menuManager.createItem(icon);
                                                }

                                                if (caseMenu.getItem(inventorySlot) == null)
                                                    caseMenu.setItem(inventorySlot, menuItem);
                                            }
                                        }
                                    });
                                } else {
                                    ConfigurationSection itemTypesSection = menuSection.getConfigurationSection("item-types");
                                    if (itemTypesSection != null) {
                                        for (String typeKey : itemTypesSection.getKeys(false)) {
                                            String keys = String.valueOf(plugin.getKeys(player, caseName));
                                            String lucks = luckManager.getLuckLines(player);
                                            ItemStack itemStack = ItemBuilder.fromConfig(itemTypesSection.getConfigurationSection(typeKey), text ->
                                                    PlaceholderAPI.setPlaceholders(player, ColorUtil.parse(
                                                    text.replace("%player_keys%", keys).replace("%lucks%", lucks))
                                            )).build();

                                            itemTypes.put(typeKey, itemStack);
                                        }
                                    }

                                    List<String> wrappedInventory = menuSection.getStringList("inventory");
                                    for (int inventoryRow = 0; inventoryRow < wrappedInventory.size(); inventoryRow++) {
                                        String[] wrappedSlotItem = wrappedInventory.get(inventoryRow).split(" ");
                                        for (int index = 0; index < wrappedSlotItem.length; index++) {
                                            int inventorySlot = index + (inventoryRow * 9);
                                            String itemType = wrappedSlotItem[index];
                                            ItemStack icon = itemTypes.get(itemType);

                                            MenuItem menuItem;
                                            if (itemType.equals("C")) {
                                                menuItem = menuManager.createItem(icon, (whoClick, clickType) -> {
                                                    GameCase gameCase = (GameCase) ((Location)((Supplier<?>)((Pair<?, ?>)position.getSecond()).getFirst()).get()).getBlock().getMetadata("case").get(0).value();
                                                    if (gameCase != null) {
                                                        if (gameCase.isOpening()) {
                                                            whoClick.closeInventory();
                                                            gameCase.getMessages().sendMessage(whoClick, "case-already-opening");
                                                        } else {
                                                            finalOpen.open(gameCase, clickType, whoClick);
                                                        }
                                                    }
                                                });
                                            } else if (itemType.equals("W")) {
                                                caseMenu.setItem(inventorySlot, menuManager.createItem(icon));

                                                int historyCount = (inventoryRow + 1) * 9 - inventorySlot - 1;
                                                Collection<PrizeHistoryEntry> history = plugin.getLastPrizes(caseName, historyCount);
                                                int counter = 1;
                                                String prizeItemName = ColorUtil.parse(menuSection.getString("last-rewards.item-name", ""));
                                                List<String> prizeItemLore = menuSection.getStringList("last-rewards.item-lore").stream()
                                                        .map(ColorUtil::parse)
                                                        .toList();

                                                SimpleDateFormat fmt = new SimpleDateFormat("dd MMM HH:mm:ss");
                                                fmt.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Europe/Moscow")));

                                                for (PrizeHistoryEntry prizeHistoryEntry : history) {
                                                    String[] args = prizeHistoryEntry.prizeMaterial().split(":");
                                                    Color color = getColor(args);
                                                    ItemStack prizeIcon = new ItemBuilder(Material.valueOf(args[0].toUpperCase()))
                                                            .name(prizeItemName
                                                                    .replace("%player_name%", prizeHistoryEntry.opener())
                                                                    .replace("%prize_name%", prizeHistoryEntry.prizeName())
                                                                    .replace("%server_name%", prizeHistoryEntry.serverName())
                                                                    .replace("%date%", fmt.format(new Date(prizeHistoryEntry.timestamp())))
                                                                    .replace("%luck%", prizeHistoryEntry.luckName())
                                                            )
                                                            .lore(prizeItemLore.stream()
                                                                    .map(line -> line
                                                                            .replace("%player_name%", prizeHistoryEntry.opener())
                                                                            .replace("%prize_name%", prizeHistoryEntry.prizeName())
                                                                            .replace("%server_name%", prizeHistoryEntry.serverName())
                                                                            .replace("%date%", fmt.format(new Date(prizeHistoryEntry.timestamp())))
                                                                            .replace("%luck%", prizeHistoryEntry.luckName())
                                                                    )
                                                                    .collect(Collectors.toList()))
                                                            .color(color)
                                                            .removeAttributes()
                                                            .build();
                                                    caseMenu.setItem(inventorySlot + counter, menuManager.createItem(prizeIcon));
                                                    counter++;
                                                }
                                                continue;
                                            } else {
                                                menuItem = menuManager.createItem(icon);
                                            }

                                            if (caseMenu.getItem(inventorySlot) == null)
                                                caseMenu.setItem(inventorySlot, menuItem);
                                        }
                                    }
                                }

                                return caseMenu;
                            };

                            Supplier<GameCase> configurableCase = () -> new ConfigurableCase(
                                    plugin,
                                    executor,
                                    caseName,
                                    titleAboveCase,
                                    titleAboveCaseYOffset,
                                    luckWork,
                                    position.getSecond().getFirst().get(),
                                    position.getSecond().getSecond(),
                                    animation,
                                    Material.ENDER_CHEST,
                                    particles,
                                    rewards,
                                    menuCreator,
                                    prizeMessage,
                                    messages
                            );
                            caseHolder.addCreator(position.getFirst(), configurableCase);
                        });

                cases.put(caseName, caseHolder);
            }
            return cases;
        }

        private Color getColor(String[] args) {
            Color color;
            if (args.length >= 4) {
                int red = Integer.parseInt(args[1]);
                int green = Integer.parseInt(args[2]);
                int blue = Integer.parseInt(args[3]);
                color = Color.fromRGB(red, green, blue);
            } else {
                color = null;
            }
            return color;
        }

        private Sound parseSound(String soundName) {
            if (soundName == null || soundName.isEmpty()) {
                return Sound.BLOCK_NOTE_BLOCK_PLING; // Значение по умолчанию, если звук не указан
            }
            try {
                return Sound.valueOf(soundName);
            } catch (IllegalArgumentException e) {
                return Sound.BLOCK_NOTE_BLOCK_PLING; // Значение по умолчанию, если звук некорректен
            }
        }

        private Material parseMaterial(String materialName) {
            if (materialName == null || materialName.isEmpty()) {
                return Material.STONE; // Значение по умолчанию, если материал не указан
            }
            try {
                return Material.valueOf(materialName);
            } catch (IllegalArgumentException e) {
                return Material.STONE; // Значение по умолчанию, если материал некорректен
            }
        }

        private Particle parseParticle(String particleName) {
            if (particleName == null || particleName.isEmpty()) {
                return Particle.CRIT; // Значение по умолчанию, если частица не указана
            }
            try {
                return Particle.valueOf(particleName);
            } catch (IllegalArgumentException e) {
                return Particle.CRIT; // Значение по умолчанию, если частица некорректна
            }
        }

        public void addCaseBlock(Block blockCase, String caseName) {
            Location pos = blockCase.getLocation();
            BlockFace dir = BlockFace.EAST;
            if (blockCase.getBlockData() instanceof Directional) {
                dir = ((Directional) blockCase.getBlockData()).getFacing();
            }

            String wrapped = pos.getWorld().getName()
                    + " " + ((int) pos.getX())
                    + " " + ((int) pos.getY())
                    + " " + ((int) pos.getZ())
                    + " " + dir.name();

            List<String> locs = config.getStringList("cases." + caseName + ".locations");
            if (!locs.contains(wrapped)) locs.add(wrapped);

            config.set("cases." + caseName + ".locations", locs);
            save();
        }

        public void removeCaseBlock(Location location, String caseName) {
            List<String> locations = config.getStringList("cases." + caseName + ".locations");
            for (String wrapped : new ArrayList<>(locations)) {
                String[] args = wrapped.split(" ");
                if (args[0].equalsIgnoreCase(location.getWorld().getName())
                        && args[1].equalsIgnoreCase(String.valueOf(((int) location.getX())))
                        && args[2].equalsIgnoreCase(String.valueOf(((int) location.getY())))
                        && args[3].equalsIgnoreCase(String.valueOf(((int) location.getZ()))))
                    locations.remove(wrapped);
            }

            config.set("cases." + caseName + ".locations", locations);
            save();
        }

        private void save() {
            try {
                config.save(rawConfig);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void sync(Runnable runnable) {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }

        private ClickOpen getClickOpen(ConfigurationSection openSection, Open.Type openType) {
            String click = openType.name().toLowerCase();
            int price = openSection.getInt(click + "-price");
            String priceType = openSection.getString(click + "-price-type");
            String commandChangePrice = openSection.getString(click + "-command-change-price");
            String failedBuySlot = openSection.getString(click + "-failed-buy-slot");
            return new ClickOpen(openType, plugin, luckManager, price, priceType, commandChangePrice, failedBuySlot);
        }
    }