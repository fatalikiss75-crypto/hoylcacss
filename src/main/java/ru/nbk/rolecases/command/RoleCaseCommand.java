package ru.nbk.rolecases.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jdbi.v3.core.async.JdbiExecutor;
import ru.nbk.rolecases.HolyCases;
import ru.nbk.rolecases.cases.CaseManager;
import ru.nbk.rolecases.database.dao.PlayerKeysDao;
import ru.nbk.rolecases.database.dao.PlayerLuckDao;
import ru.nbk.rolecases.lucks.LuckManager;
import ru.nbk.util.ItemBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RoleCaseCommand implements CommandExecutor {

    private JdbiExecutor executor;
    private CaseManager caseManager;
    private LuckManager luckManager;
    private HolyCases holyCases;

    public RoleCaseCommand(HolyCases plugin, CaseManager caseManager, LuckManager luckManager, JdbiExecutor executor) {
        this.executor = executor;
        this.caseManager = caseManager;
        this.luckManager = luckManager;
        this.holyCases = plugin;

        plugin.getCommand("holycases").setExecutor(this);
        plugin.getCommand("holycases").setTabCompleter(new RoleCaseCommandTabCompleter(caseManager, luckManager));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendUsage(sender);
            return true;
        } else if (args[0].equalsIgnoreCase("place")) {
            if (args.length < 2) {
                sender.sendMessage("§cНедостаточно аргументов!");
                return true;
            }

            if (!sender.hasPermission("holycases.command.place")) {
                sender.sendMessage("§cНет прав!");
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage("§cТолько для игроков!");
                return true;
            }

            if (caseManager.getCaseHolder(args[1]) == null) {
                sender.sendMessage("§cКейс §6" + args[1] + " §cне найден");
                return true;
            }

            ItemStack placer = new ItemBuilder(Material.ENDER_CHEST)
                    .name(args[1])
                    .addLore("§eПКМ по блоку, чтобы сделать его кейсом")
                    .addLore("§eЛКМ по сундуку, чтобы удалить его")
                    .build();
            ((Player) sender).getInventory().addItem(placer);
            sender.sendMessage("§aПлейсер добавлен в ваш инвентарь");
            return true;
        } else if (args[0].equalsIgnoreCase("item")) {
            if (args.length < 2) {
                sender.sendMessage("§cНедостаточно аргументов!");
                return true;
            }

            if (!sender.hasPermission("holycases.command.item")) {
                sender.sendMessage("§cНет прав!");
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cТолько для игроков!");
                return true;
            }

            if (caseManager.getCaseHolder(args[1]) == null) {
                sender.sendMessage("§cКейс §6" + args[1] + " §cне найден");
                return true;
            }

            ItemStack itemStack = player.getInventory().getItemInMainHand().clone();

            if (itemStack.getType().isAir()) {
                sender.sendMessage("§cВы должны взять предмет в руку!");
                return true;
            }

            itemStack.setAmount(1);
            holyCases.setItemStack(args[1], itemStack);

            sender.sendMessage("§aУстановлен предмет для кейса §6" + args[1]);
            return true;
        }

        if (args.length == 5 || args.length == 7) {
            if (args[0].equalsIgnoreCase("keys")) {
                if (!sender.hasPermission("holycases.command.keys")) {
                    sender.sendMessage("§cНет прав!");
                    return true;
                }

                if (caseManager.getCaseHolder(args[1]) == null) {
                    sender.sendMessage("§cКейс §6" + args[1] + " §cне найден");
                    return true;
                }

                if (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("withdraw")) {
                    OfflinePlayer offlinePlayer = Bukkit.getPlayer(args[3]);

                    if (offlinePlayer == null) {
                        offlinePlayer = Bukkit.getOfflinePlayer(args[3]);
                    }

                    if (!NumberUtils.isDigits(args[4])) {
                        sender.sendMessage("§6" + args[4] + " §7- §cне число");
                        return true;
                    }

                    if (Integer.parseInt(args[4]) <= 0) {
                        sender.sendMessage("&7Укажите положительное целое число!");
                        return true;
                    }

                    int count = (args[2].equalsIgnoreCase("withdraw")) ? -Integer.parseInt(args[4]) : Integer.parseInt(args[4]);

                    if (args.length == 7) {
                        if (!NumberUtils.isDigits(args[6])) {
                            sender.sendMessage("§6" + args[6] + " §7- §cне число");
                            return true;
                        }
                        if (Integer.parseInt(args[6]) <= 0) {
                            sender.sendMessage("&7Укажите положительное целое число!");
                            return true;
                        }
                    }

                    UUID uuid = offlinePlayer.getUniqueId();
                    if (holyCases.isSQL()) {
                        executor.useExtension(PlayerKeysDao.class, dao ->
                                dao.addKeys(uuid, args[1], count)
                        );
                    } else {
                        holyCases.addKeys(offlinePlayer, args[1], count);
                    }

                    sender.sendMessage("Изменили количество ключей от кейса " + args[1] + " у игрока " + args[3] + " на " + count);

                    return true;
                }
            }

            if (args[0].equalsIgnoreCase("luck")) {
                if (!sender.hasPermission("holycases.command.luck")) {
                    sender.sendMessage("§cНет прав!");
                    return true;
                }

                if (!luckManager.hasLuck(args[1])) {
                    sender.sendMessage("§cУдача §6" + args[1] + " §cне найдена");
                    return true;
                }

                if (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("withdraw")) {
                    OfflinePlayer offlinePlayer = Bukkit.getPlayer(args[3]);

                    if (offlinePlayer == null) {
                        offlinePlayer = Bukkit.getOfflinePlayer(args[3]);
                    }

                    if (!NumberUtils.isDigits(args[4])) {
                        sender.sendMessage("§6" + args[4] + " §7- §cне число");
                        return true;
                    }

                    if (Integer.parseInt(args[4]) <= 0) {
                        sender.sendMessage("&7Укажите положительное целое число!");
                        return true;
                    }

                    int count = (args[2].equalsIgnoreCase("withdraw")) ? -Integer.parseInt(args[4]) : Integer.parseInt(args[4]);

                    if (args.length == 7) {
                        if (!NumberUtils.isDigits(args[6])) {
                            sender.sendMessage("§6" + args[6] + " §7- §cне число");
                            return true;
                        }
                        if (Integer.parseInt(args[6]) <= 0) {
                            sender.sendMessage("&7Укажите положительное целое число!");
                            return true;
                        }
                    }

                    UUID uuid = offlinePlayer.getUniqueId();
                    if (holyCases.isSQL()) {
                        executor.useExtension(PlayerLuckDao.class, dao ->
                                dao.addLuck(uuid, args[1], count)
                        );
                    } else {
                        holyCases.addLuck(offlinePlayer, args[1], count);
                    }

                    sender.sendMessage("Изменили количество удачи " + args[1] + " у игрока " + args[3] + " на " + count);

                    return true;
                }
            }
        }

        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("keys")) {
                if (!sender.hasPermission("holycases.command.keys")) {
                    sender.sendMessage("§cНет прав!");
                    return true;
                }

                if (caseManager.getCaseHolder(args[1]) == null) {
                    sender.sendMessage("§cКейс §6" + args[1] + " §cне найден");
                    return true;
                }

                if (args[2].equalsIgnoreCase("count")) {
                    OfflinePlayer offlinePlayer = Bukkit.getPlayer(args[3]);

                    if (offlinePlayer == null) {
                        offlinePlayer = Bukkit.getOfflinePlayer(args[3]);
                    }

                    UUID uuid = offlinePlayer.getUniqueId();
                    if (holyCases.isSQL()) {
                        executor.withExtension(PlayerKeysDao.class, dao -> dao.getKeys(uuid, args[1])).whenComplete((count, ex) -> {
                            sender.sendMessage("У игрока " + args[3] + " - " + count + " ключей от кейса " + args[1]);
                        });
                    } else {
                        int count = holyCases.getKeys(offlinePlayer, args[1]);
                        sender.sendMessage("У игрока " + args[3] + " - " + count + " ключей от кейса " + args[1]);
                    }

                    return true;
                }
            }

            if (args[0].equalsIgnoreCase("luck")) {
                if (!sender.hasPermission("holycases.command.luck")) {
                    sender.sendMessage("§cНет прав!");
                    return true;
                }

                if (!luckManager.hasLuck(args[1])) {
                    sender.sendMessage("§cУдача §6" + args[1] + " §cне найдена");
                    return true;
                }

                if (args[2].equalsIgnoreCase("count")) {
                    OfflinePlayer offlinePlayer = Bukkit.getPlayer(args[3]);

                    if (offlinePlayer == null) {
                        offlinePlayer = Bukkit.getOfflinePlayer(args[3]);
                    }

                    UUID uuid = offlinePlayer.getUniqueId();
                    if (holyCases.isSQL()) {
                        executor.withExtension(PlayerLuckDao.class, dao -> dao.getLuck(uuid, args[1])).whenComplete((count, ex) -> {
                            sender.sendMessage("У игрока " + args[3] + " - " + count + " удачи " + args[1]);
                        });
                    } else {
                        int count = holyCases.getLucks(offlinePlayer, args[1]);
                        sender.sendMessage("У игрока " + args[3] + " - " + count + " удачи " + args[1]);
                    }

                    return true;
                }
            }
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§6§lКЕЙСЫ: \n");
        if (sender.hasPermission("holycases.command.item")) sender.sendMessage("§f/holycases §6item §f<§6название_кейса§f> - Установить предмет для кейса (для типа открытия item)");
        if (sender.hasPermission("holycases.command.keys")) sender.sendMessage("§f/holycases §6keys §f<§6название_кейса§f> §f<§6add§f/§6withdraw§f> <§6никнейм_игрока§f> <§6кол-во§f> - Добавить/Убрать ключи у игрока");
        if (sender.hasPermission("holycases.command.keys")) sender.sendMessage("§f/holycases §6keys §f<§6название_кейса§f> §6count §f<§6никнейм_игрока§f> - Получить количество ключей у игрока");
        if (sender.hasPermission("holycases.command.luck")) sender.sendMessage("§f/holycases §6luck §f<§6название_удачи§f> §f<§6add§f/§6withdraw§f> <§6никнейм_игрока§f> <§6кол-во§f> - Добавить/Убрать удачу у игрока");
        if (sender.hasPermission("holycases.command.luck")) sender.sendMessage("§f/holycases §6luck §f<§6название_удачи§f> §6count §f<§6никнейм_игрока§f> - Получить количество удачи у игрока");
        if (sender.hasPermission("holycases.command.place")) sender.sendMessage("§f/holycases §6place §f<§6название_кейса§f> - Получить блок для установки кейса");
        sender.sendMessage("§8Если вы ничего не получили, значит у вас нет прав на использование команд плагина.");
    }

    private static class RoleCaseCommandTabCompleter implements TabCompleter {

        private CaseManager caseManager;
        private LuckManager luckManager;

        public RoleCaseCommandTabCompleter(CaseManager caseManager, LuckManager luckManager) {
            this.caseManager = caseManager;
            this.luckManager = luckManager;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
            List<String> subs = new ArrayList<>();
            if (args.length == 1) {
                if (sender.hasPermission("holycases.command.item")) subs.add("item");
                if (sender.hasPermission("holycases.command.keys")) subs.add("keys");
                if (sender.hasPermission("holycases.command.luck")) subs.add("luck");
                if (sender.hasPermission("holycases.command.place")) subs.add("place");

                return StringUtil.copyPartialMatches(args[0], subs, new ArrayList<>());
            }else if (args.length == 2) {
                if (sender.hasPermission("holycases.command.keys") && args[0].equalsIgnoreCase("keys") ||
                    sender.hasPermission("holycases.command.place") && args[0].equalsIgnoreCase("place") ||
                    sender.hasPermission("holycases.command.item") && args[0].equalsIgnoreCase("item")) {
                    caseManager.getCaseNames().forEach(caseName -> subs.add(caseName));
                }
                if (sender.hasPermission("holycases.command.luck") && args[0].equalsIgnoreCase("luck")) {
                    luckManager.getLuckNames().forEach(luckName -> subs.add(luckName));
                }

                return StringUtil.copyPartialMatches(args[1], subs, new ArrayList<>());

            }else if (args.length == 3){
                if (args[0].equalsIgnoreCase("keys") && sender.hasPermission("holycases.command.keys") ||
                    args[0].equalsIgnoreCase("luck") && sender.hasPermission("holycases.command.luck")) {
                    subs.add("add");
                    subs.add("withdraw");
                    subs.add("count");
                }

                return StringUtil.copyPartialMatches(args[2], subs, new ArrayList<>());
            }else if (args.length == 6 || args.length == 7) {
                if (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("withdraw")) {

                    if (args.length == 7) {
                        return StringUtil.copyPartialMatches(args[6], Arrays.asList("1", "2", "3", "4", "5"), new ArrayList<>());
                    }

                    return StringUtil.copyPartialMatches(args[5], subs, new ArrayList<>());
                }
            }

            return null;
        }
    }
}
