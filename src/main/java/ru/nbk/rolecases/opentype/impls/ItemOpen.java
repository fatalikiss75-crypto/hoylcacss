package ru.nbk.rolecases.opentype.impls;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import ru.nbk.rolecases.HolyCases;
import ru.nbk.rolecases.cases.GameCase;
import ru.nbk.rolecases.lucks.LuckManager;
import ru.nbk.rolecases.misc.ColorUtil;
import ru.nbk.rolecases.opentype.Open;

import java.util.Arrays;
import java.util.Objects;

public class ItemOpen extends Open {
    private final HolyCases plugin;
    private final LuckManager luckManager;
    private final int amount;
    private final String failedBuySlot;

    public ItemOpen(HolyCases plugin, LuckManager luckManager, int amount, String failedBuySlot) {
        super(Type.ITEM);
        this.plugin = plugin;
        this.luckManager = luckManager;
        this.amount = amount;
        this.failedBuySlot = failedBuySlot;
    }

    @Override
    public void open(GameCase gameCase, ClickType type, Player whoClick) {
        ItemStack itemStack = plugin.getItemStack(gameCase.getName());

        whoClick.closeInventory();

        if (itemStack == null) {
            plugin.getLogger().severe(ColorUtil.parse("&cНе удалось получить предмет для кейса: " + gameCase.getName()));
            if (!whoClick.isOp())
                whoClick.sendMessage(ColorUtil.parse("&cПроизошла ошибка! Обратитесь к администрации сервера!"));
            else
                whoClick.sendMessage(ColorUtil.parse("&cНе удалось получить предмет для кейса: " + gameCase.getName()));
            return;
        }

        int value = 0;
        ItemStack finalItemStack = itemStack.clone();
        finalItemStack.setAmount(1);

        for (ItemStack item : whoClick.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                if (item.isSimilar(finalItemStack))
                    value += item.getAmount();
            }
        }

        if (amount > value) {
            whoClick.sendMessage(ColorUtil.parse(failedBuySlot.replace("%amount%", "" + amount)));
            return;
        }

        finalItemStack.setAmount(amount);

        whoClick.getInventory().removeItem(finalItemStack);

        gameCase.open(whoClick, luckManager.removeLast(whoClick));
    }
}
