package com.example.weapons.items;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Central registry for all NamespacedKeys used by this plugin.
 * Initialised once in onEnable via the Plugin instance — never created per-call.
 */
public final class ItemKeys {

    /** Identifies which custom weapon an ItemStack is. Value: WeaponType#getId() */
    public final NamespacedKey WEAPON_ID;

    // ── Projectile tags (stored on Arrow/Snowball entities via PDC) ──────────
    /** Which weapon fired this projectile: "arcanist" or "harmony" */
    public final NamespacedKey PROJECTILE_TYPE;
    /** UUID string of the Player who fired this projectile */
    public final NamespacedKey PROJECTILE_SHOOTER;
    /** Double: the damage this projectile should deal on hit */
    public final NamespacedKey PROJECTILE_DAMAGE;

    public ItemKeys(Plugin plugin) {
        WEAPON_ID          = new NamespacedKey(plugin, "weapon_id");
        PROJECTILE_TYPE    = new NamespacedKey(plugin, "proj_type");
        PROJECTILE_SHOOTER = new NamespacedKey(plugin, "proj_shooter");
        PROJECTILE_DAMAGE  = new NamespacedKey(plugin, "proj_damage");
    }
}
