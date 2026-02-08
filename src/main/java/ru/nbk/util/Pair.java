package ru.nbk.util;

public class Pair<L, R> {
    private final L first;
    private final R second;

    public Pair(L first, R second) {
        this.first = first;
        this.second = second;
    }

    public static <L, R> Pair<L, R> of(L first, R second) {
        return new Pair<>(first, second);
    }

    public L getFirst() {
        return first;
    }

    public R getSecond() {
        return second;
    }
}
