package ru.nbk.rolecases.opentype.impls;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import ru.nbk.rolecases.HolyCases;
import ru.nbk.rolecases.cases.GameCase;
import ru.nbk.rolecases.lucks.Luck;
import ru.nbk.rolecases.lucks.LuckManager;
import ru.nbk.rolecases.misc.ColorUtil;
import ru.nbk.rolecases.opentype.Open;

import javax.swing.plaf.basic.BasicSplitPaneUI;

public class ClickOpen extends Open {
    private final HolyCases plugin;
    private final LuckManager luckManager;
    private final int price;
    private final String priceType;
    private final String commandChangePrice;
    private final String failedBuySlot;

    public ClickOpen(Type type, HolyCases plugin, LuckManager luckManager, int price, String priceType, String commandChangePrice, String failedBuySlot) {
        super(type);
        this.plugin = plugin;
        this.luckManager = luckManager;
        this.price = price;
        this.priceType = priceType;
        this.commandChangePrice = commandChangePrice;
        this.failedBuySlot = failedBuySlot;
    }

    @Override
    public void open(GameCase gameCase, ClickType clickType, Player whoClick) {
        if (!(clickType.isLeftClick() && getType() == Type.LEFT || clickType.isRightClick() && getType() == Type.RIGHT)) return;

        whoClick.closeInventory();

        int value;
        try {
            value = Integer.parseInt(PlaceholderAPI.setPlaceholders(whoClick, priceType));
        } catch (NumberFormatException e) {
            plugin.getLogger().severe(ColorUtil.parse("&cНе удалось получить число из плейсхолдера: " + priceType));
            if (!whoClick.isOp())
                whoClick.sendMessage(ColorUtil.parse("&cПроизошла ошибка! Обратитесь к администрации сервера!"));
            else
                whoClick.sendMessage(ColorUtil.parse("&cНе удалось получить число из плейсхолдера: " + priceType));
            return;
        }

        if (price > value) {
            whoClick.sendMessage(ColorUtil.parse(failedBuySlot.replace("%amount%", "" + price)));
            return;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                commandChangePrice.replace("%player%", whoClick.getName()).replace("%count%", "" + price));

        gameCase.open(whoClick, luckManager.removeLast(whoClick));
    }
}
