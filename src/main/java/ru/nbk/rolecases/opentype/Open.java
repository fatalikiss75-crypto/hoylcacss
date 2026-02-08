package ru.nbk.rolecases.opentype;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import ru.nbk.rolecases.cases.GameCase;

public abstract class Open {
    private final Type type;

    public Open(Type type) {
        this.type = type;
    }

    public abstract void open(GameCase gameCase, ClickType type, Player whoClick);

    public Type getType() {
        return type;
    }

    public enum Type {
        KEY,
        LEFT,
        RIGHT,
        LEFTRIGHT,
        ITEM;
    }
}
