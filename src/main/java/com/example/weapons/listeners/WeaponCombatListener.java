package com.example.weapons.listeners;

import com.example.weapons.WeaponsPlugin;
import com.example.weapons.items.ItemKeys;
import com.example.weapons.items.WeaponType;
import com.example.weapons.managers.CooldownManager;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

public final class WeaponCombatListener implements Listener {

    private static final String CD_DOMINICAN_MELEE = "dominican_melee";

    private final WeaponsPlugin      plugin;
    private final ItemKeys           keys;
    private final WeaponStateManager state;
    private final CooldownManager    cooldowns;

    public WeaponCombatListener(WeaponsPlugin plugin) {
        this.plugin    = plugin;
        this.keys      = plugin.getItemKeys();
        this.state     = plugin.getStateManager();
        this.cooldowns = plugin.getCooldownManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim  = event.getEntity();

        // ── CASE 1: Player swings a custom weapon ─────────────────────────────
        if (damager instanceof Player) {
            Player attacker = (Player) damager;

            if (state.isAbilityDamage(attacker.getUniqueId())) return;

            ItemStack held = attacker.getInventory().getItemInMainHand();
            WeaponType type = getWeaponType(held);
            if (type == null) return;

            if (type.isWand()) {
                event.setCancelled(true);
                return;
            }

            if (type == WeaponType.DOMINICAN_AXE) {
                if (cooldowns.isOnCooldown(attacker.getUniqueId(), CD_DOMINICAN_MELEE)) {
                    event.setCancelled(true);
                    return;
                }
                cooldowns.setCooldown(attacker.getUniqueId(), CD_DOMINICAN_MELEE, 700L);
            }

            boolean isCrit = isCriticalHit(attacker);
            double  damage = isCrit ? type.getCritDamage() : type.getNormalDamage();
            event.setDamage(damage);

            if (isCrit) {
                victim.getWorld().spawnParticle(Particle.CRIT,
                    victim.getLocation().add(0, 1, 0), 18, 0.3, 0.5, 0.3, 0.1);
            }
            return;
        }

        // ── CASE 2: Victim is a Player — reflect / shadow checks ──────────────
        if (!(victim instanceof Player)) return;
        Player player = (Player) victim;

        if (state.isShadowInvisible(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (state.hasReflectShield(player.getUniqueId())) {
            LivingEntity actualAttacker = null;
            if (damager instanceof LivingEntity) {
                actualAttacker = (LivingEntity) damager;
            } else if (damager instanceof Projectile) {
                Projectile proj = (Projectile) damager;
                if (proj.getShooter() instanceof LivingEntity) {
                    actualAttacker = (LivingEntity) proj.getShooter();
                }
            }

            if (actualAttacker != null) {
                double raw = event.getDamage();
                event.setCancelled(true);

                double newHp = Math.max(0, player.getHealth() - raw * 0.2);
                player.setHealth(newHp);

                final LivingEntity finalAttacker = actualAttacker;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    state.addAbilityBypass(player.getUniqueId());
                    finalAttacker.damage(raw * 0.8, player);
                    state.removeAbilityBypass(player.getUniqueId());
                });
                player.getWorld().spawnParticle(Particle.END_ROD,
                    player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!state.isShadowInvisible(player.getUniqueId())) return;
        EntityRegainHealthEvent.RegainReason reason = event.getRegainReason();
        if (reason == EntityRegainHealthEvent.RegainReason.SATIATED ||
            reason == EntityRegainHealthEvent.RegainReason.REGEN) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        state.cleanupPlayer(event.getPlayer().getUniqueId());
        cooldowns.clearAll(event.getPlayer().getUniqueId());
    }

    private boolean isCriticalHit(Player player) {
        return player.getFallDistance() > 0.0f
            && !player.isOnGround()
            && !player.isInWater()
            && player.getActivePotionEffects().stream()
                   .noneMatch(e -> e.getType().equals(PotionEffectType.BLINDNESS));
    }

    private WeaponType getWeaponType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta()
            .getPersistentDataContainer()
            .get(keys.WEAPON_ID, PersistentDataType.STRING);
        return WeaponType.fromId(id);
    }
}
