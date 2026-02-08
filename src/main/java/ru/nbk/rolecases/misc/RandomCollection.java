package ru.nbk.rolecases.misc;


import java.util.*;

import org.apache.commons.lang.ArrayUtils;

public class RandomCollection<E> {
    private final NavigableMap<Double, E> map = new TreeMap<>();
    private final Random random;
    private double total;

    public RandomCollection() {
        this.random = new Random();
        this.total = 0;
    }

    public RandomCollection<E> add(double weight, E result) {
        if (weight <= 0) return this;
        total += weight;
        map.put(total, result);
        return this;
    }

    public E next() {
        double value = random.nextDouble() * total;
        return map.higherEntry(value).getValue();
    }

    public Set<Map.Entry<Double, E>> getEntries() {
        return map.entrySet();
    }

    public Collection<E> getValues() {
        return map.values();
    }

    public double getWeight(E element) {
        if (element == null || !map.containsValue(element)) return 0;
        double weight = 0;
        double lastKey = 0;
        for (Map.Entry<Double, E> entry : map.entrySet()) {
            if (entry.getValue().equals(element)) {
                weight = entry.getKey() - lastKey;
                break;
            }
            lastKey = entry.getKey();
        }
        return weight;
    }

    public double getTotal() {
        return total;
    }
}