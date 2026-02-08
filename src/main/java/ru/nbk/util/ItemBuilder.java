package ru.nbk.util;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ItemBuilder {

    private ItemStack item;

    public ItemBuilder(ItemStack item){
        if (item == null){
            this.item = new ItemStack(Material.AIR);
        }else {
            this.item = item.clone();
        }
    }

    public ItemBuilder(Material material){
        this.item = new ItemStack(material);
    }

    public ItemMeta meta(){
        return item.getItemMeta();
    }

    public ItemBuilder meta(ItemMeta meta){
        item.setItemMeta(meta);
        return this;
    }

    public String name(){
        return meta().hasDisplayName() ? meta().getDisplayName() : "";
    }

    public ItemBuilder name(String name){
        ItemMeta meta = meta();

        if (meta == null) return this;

        meta.setDisplayName(name);
        meta(meta);

        return this;
    }

    public List<String> lore(){
        return meta().getLore() == null ? new ArrayList<>() : meta().getLore();
    }

    public ItemBuilder lore(List<String> lore){
        ItemMeta meta = meta();

        if (meta == null) return this;

        meta.setLore(lore);
        meta(meta);

        return this;
    }

    public ItemBuilder addLore(String... lore){
        ItemMeta meta = meta();

        if (meta == null) return this;

        List<String> old = lore();
        for (String line : lore) {
            old.add(line);
        }
        meta.setLore(old);
        meta(meta);

        return this;
    }

    public ItemBuilder addLoreAbove(String... lore){
        ItemMeta meta = meta();

        if (meta == null) return this;

        List<String> old = lore();
        List<String> toAdd = Arrays.asList(lore);
        Collections.reverse(toAdd);
        old.addAll(0, toAdd);
        meta.setLore(old);
        meta(meta);

        return this;
    }

    public ItemBuilder color(Color color) {
        ItemMeta meta = meta();

        if (meta == null) return this;
        if (!(meta instanceof LeatherArmorMeta)) return this;
        if (color == null) return this;

        LeatherArmorMeta armorMeta = (LeatherArmorMeta) meta;
        armorMeta.setColor(color);
        meta(armorMeta);

        return this;
    }

    public ItemBuilder removeAttributes() {
        ItemMeta meta = meta();

        if (meta == null) return this;

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE, ItemFlag.HIDE_POTION_EFFECTS);

        meta(meta);

        return this;
    }

    public ItemBuilder amount(int amount){
        item.setAmount(amount);
        return this;
    }

    public int amount(){
        return item.getAmount();
    }

    public ItemBuilder glow(boolean isGlow) {
        ItemMeta meta = meta();

        if (meta == null || !isGlow) return this;

        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta(meta);

        return this;
    }

    public ItemStack build(){
        return item;
    }

    public ItemBuilder save(ConfigurationSection section) {
        section.set("material", item.getType().name());
        ItemMeta meta = meta();
        if (meta != null) {
            section.set("name", meta.getDisplayName());
            section.set("lore", meta.getLore());
        }

        return this;
    }

    public static ItemBuilder fromConfig(ConfigurationSection section) {
        Material material = Material.valueOf(section.getString("material", "AIR"));
        String name = section.getString("name", "");

        List<String> lore = section.getStringList("lore");

        return new ItemBuilder(material)
                .name(name)
                .lore(lore);
    }

    public static ItemBuilder fromConfig(ConfigurationSection section, Function<String, String> textHandler) {
        Material material = Material.valueOf(section.getString("material", "AIR"));

        String name = textHandler.apply(section.getString("name", ""));
        boolean glow = section.getBoolean("glow");

        List<String> lore = new ArrayList<>();

        for (String line : section.getStringList("lore")) {
            if (!line.contains("%lucks%")) {
                lore.add(textHandler.apply(line));
            } else {
                String finalLine = textHandler.apply(line);
                List<String> lucks = List.of(finalLine.split("\n"));
                if (!lucks.isEmpty() && !finalLine.isBlank()) {
                    for (String luckLine : lucks) {
                        lore.add(textHandler.apply(luckLine));
                    }
                }
            }
        }

        return new ItemBuilder(material)
                .name(name)
                .lore(lore)
                .glow(glow);
    }
}
