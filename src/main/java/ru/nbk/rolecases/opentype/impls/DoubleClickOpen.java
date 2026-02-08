package ru.nbk.rolecases.opentype.impls;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import ru.nbk.rolecases.cases.GameCase;
import ru.nbk.rolecases.opentype.Open;

public class DoubleClickOpen extends Open {

    private final ClickOpen leftClick;
    private final ClickOpen rightClick;

    public DoubleClickOpen(ClickOpen leftClick, ClickOpen rightClick) {
        super(Type.LEFTRIGHT);
        this.leftClick = leftClick;
        this.rightClick = rightClick;
    }

    @Override
    public void open(GameCase gameCase, ClickType type, Player whoClick) {
        if (type.isRightClick())
            rightClick.open(gameCase, type, whoClick);
        else if (type.isLeftClick())
            leftClick.open(gameCase, type, whoClick);
    }
}
