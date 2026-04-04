package com.example.weapons.listeners;

import com.example.weapons.WeaponsPlugin;
import com.example.weapons.items.ItemKeys;
import com.example.weapons.items.WeaponType;
import com.example.weapons.managers.WeaponStateManager;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Handles all combat-phase weapon interactions:
 *
 *  1. Left-click melee → override damage for custom weapons.
 *  2. Arcanist Staff projectile hit → apply correct projectile damage.
 *  3. Harmony Wand projectile hit → cancel vanilla damage, apply DOT/HOT.
 *  4. Greatsword reflect shield → damage attacker by 80% when player is hit.
 *  5. Assassin's Blade shadow phase → cancel all incoming damage.
 *  6. Assassin's Blade shadow phase → cancel natural health regen.
 *  7. Player quit → clean up state.
 */
public final class WeaponCombatListener implements Listener {

    private final WeaponsPlugin plugin;
    private final ItemKeys keys;
    private final WeaponStateManager state;

    public WeaponCombatListener(WeaponsPlugin plugin) {
        this.plugin = plugin;
        this.keys   = plugin.getItemKeys();
        this.state  = plugin.getStateManager();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIMARY DAMAGE HANDLER
    // ─────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        Entity damager   = event.getDamager();
        Entity victim    = event.getEntity();

        // ── CASE 1: Player swings a custom weapon (melee) ────────────────────
        if (damager instanceof Player attacker) {
            ItemStack held = attacker.getInventory().getItemInMainHand();
            WeaponType type = getWeaponType(held);

            if (type != null) {
                // Override the damage to our configured value
                event.setDamage(type.getMeleeDamage());

                // Harmony Wand does 0 melee — cancel any accidental melee hit
                if (type == WeaponType.HARMONY_WAND) {
                    event.setCancelled(true);
                    return;
                }
            }
            return; // don't fall through to projectile checks
        }

        // ── CASE 2: A tagged projectile hit something ─────────────────────────
        if (damager instanceof Projectile proj) {
            PersistentDataContainer pdc = proj.getPersistentDataContainer();
            String projType = pdc.get(keys.PROJECTILE_TYPE, PersistentDataType.STRING);
            if (projType == null) return;

            switch (projType) {

                // Arcanist Staff bolt: override damage with stored value
                case "arcanist" -> {
                    Double dmg = pdc.get(keys.PROJECTILE_DAMAGE, PersistentDataType.DOUBLE);
                    if (dmg != null) event.setDamage(dmg);
                }

                // Harmony Wand bolt: cancel vanilla damage, apply DOT/HOT
                case "harmony" -> {
                    event.setCancelled(true);
                    if (!(victim instanceof LivingEntity target)) return;

                    // Identify shooter
                    ProjectileSource source = proj.getShooter();
                    Player shooter = (source instanceof Player p) ? p : null;

                    if (target instanceof Player targetPlayer) {
                        // Don't heal the shooter themselves (edge case: bolt comes back)
                        if (targetPlayer.equals(shooter)) return;
                        state.applyHarmonyHeal(targetPlayer);
                    } else {
                        // Mob / hostile entity — apply damage DOT
                        state.applyHarmonyDamage(target, shooter);
                    }
                }
            }
            return;
        }

        // ── CASE 3: Player is the VICTIM — check shield / shadow ─────────────
        if (!(victim instanceof Player player)) return;

        // Shadow phase: no one can attack this player
        if (state.isShadowInvisible(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Reflect shield: damage attacker 80% of what they dealt
        if (state.hasReflectShield(player.getUniqueId())) {
            // We use getFinalDamage() which includes armour reduction
            double reflectAmount = event.getFinalDamage() * 0.8;

            // Determine the actual living attacker to damage
            LivingEntity actualAttacker = null;
            if (damager instanceof LivingEntity le) {
                actualAttacker = le;
            } else if (damager instanceof Projectile proj2 &&
                       proj2.getShooter() instanceof LivingEntity le2) {
                actualAttacker = le2;
            }

            if (actualAttacker != null) {
                final LivingEntity finalAttacker = actualAttacker;
                // Must schedule so reflect damage fires AFTER this event resolves
                Bukkit.getScheduler().runTask(plugin, () ->
                    finalAttacker.damage(reflectAmount, player));

                // Particle feedback on the player
                player.getWorld().spawnParticle(Particle.END_ROD,
                    player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HEALTH REGEN — cancel food/natural regen during shadow phase
    //  Potions (RegainReason.MAGIC) are still allowed per weapon description
    // ─────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!state.isShadowInvisible(player.getUniqueId())) return;

        var reason = event.getRegainReason();
        // Cancel food saturation regen and natural regeneration effect
        if (reason == EntityRegainHealthEvent.RegainReason.SATIATED ||
            reason == EntityRegainHealthEvent.RegainReason.REGEN) {
            event.setCancelled(true);
        }
        // EntityRegainHealthEvent.RegainReason.MAGIC (potions) is allowed
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CLEANUP
    // ─────────────────────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        state.cleanupPlayer(event.getPlayer().getUniqueId());
        plugin.getCooldownManager().clearAll(event.getPlayer().getUniqueId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILITY
    // ─────────────────────────────────────────────────────────────────────────

    private WeaponType getWeaponType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta()
                        .getPersistentDataContainer()
                        .get(keys.WEAPON_ID, PersistentDataType.STRING);
        return WeaponType.fromId(id);
    }
}
