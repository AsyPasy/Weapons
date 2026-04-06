package com.example.weapons.bosses;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class BossItems {

    public static final String SHADOW_ID         = "shadow";
    public static final String ASSASSINS_BONE_ID = "assassins_bone";

    private final NamespacedKey shadowKey;
    private final NamespacedKey assassinsBoneKey;

    public BossItems(Plugin plugin) {
        this.shadowKey         = new NamespacedKey(plugin, SHADOW_ID);
        this.assassinsBoneKey  = new NamespacedKey(plugin, ASSASSINS_BONE_ID);
    }

    public ItemStack buildShadow() {
        ItemStack item = new ItemStack(Material.BLACK_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Shadow", NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("A fragment of darkness.", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(shadowKey, PersistentDataType.STRING, SHADOW_ID);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack buildAssassinsBone() {
        ItemStack item = new ItemStack(Material.BONE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Assassin's Bone", NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Remnant of a shadow killer.", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(assassinsBoneKey, PersistentDataType.STRING, ASSASSINS_BONE_ID);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isShadow(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return SHADOW_ID.equals(item.getItemMeta().getPersistentDataContainer()
            .get(shadowKey, PersistentDataType.STRING));
    }

    public boolean isAssassinsBone(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return ASSASSINS_BONE_ID.equals(item.getItemMeta().getPersistentDataContainer()
            .get(assassinsBoneKey, PersistentDataType.STRING));
    }

    public NamespacedKey getShadowKey()        { return shadowKey; }
    public NamespacedKey getAssassinsBoneKey() { return assassinsBoneKey; }
}
