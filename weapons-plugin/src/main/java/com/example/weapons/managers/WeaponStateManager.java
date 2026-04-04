package com.example.weapons.managers;

import com.example.weapons.WeaponsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Central store for all transient weapon ability states.
 *
 * Tracks:
 *  - Greatsword  : reflect shield active
 *  - Arcanist    : charged shot ready
 *  - Archmage    : charge count
 *  - Assassin    : shadow invisibility active
 *  - Shadowblade : bleeding entities (recurring damage task)
 *  - Harmony     : DOT / HOT tasks on entities/players
 */
public final class WeaponStateManager {

    private final WeaponsPlugin plugin;

    // ── Greatsword reflect shield ─────────────────────────────────────────────
    private final Set<UUID> reflectShieldActive = new HashSet<>();

    // ── Arcanist Staff charged shot ───────────────────────────────────────────
    private final Set<UUID> chargedShotReady = new HashSet<>();

    // ── Archmage's Wand charges ───────────────────────────────────────────────
    /** Default 3 charges; removed entry = full charges */
    private final Map<UUID, Integer> archmageCharges = new HashMap<>();

    // ── Assassin's Blade shadow invisibility ──────────────────────────────────
    private final Set<UUID> shadowInvisible = new HashSet<>();
    /** Tasks that expire the shadow invisibility after 5 s */
    private final Map<UUID, BukkitTask> shadowTasks = new HashMap<>();

    // ── Bleeding entities (Shadowblade) ───────────────────────────────────────
    /** entityUUID → repeating bleed task */
    private final Map<UUID, BukkitTask> bleedTasks = new HashMap<>();

    // ── Harmony Wand DOT / HOT ────────────────────────────────────────────────
    /** entityUUID → active harmony task */
    private final Map<UUID, BukkitTask> harmonyTasks = new HashMap<>();

    public WeaponStateManager(WeaponsPlugin plugin) {
        this.plugin = plugin;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GREATSWORD — Reflect Shield
    // ═════════════════════════════════════════════════════════════════════════

    public boolean hasReflectShield(UUID playerId) {
        return reflectShieldActive.contains(playerId);
    }

    /**
     * Activates the reflect shield for {@code durationTicks} ticks, then expires it.
     * Visual feedback via particles is spawned at activation.
     */
    public void activateReflectShield(Player player, long durationTicks) {
        UUID uid = player.getUniqueId();
        reflectShieldActive.add(uid);

        // Burst of particles on activation
        player.getWorld().spawnParticle(Particle.END_ROD,
            player.getLocation().add(0, 1, 0), 40, 0.6, 0.8, 0.6, 0.15);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.5f);
        player.sendActionBar(Component.text("⚔ Reflective Guard ACTIVE!", NamedTextColor.AQUA));

        // Schedule expiry
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            reflectShieldActive.remove(uid);
            if (player.isOnline()) {
                player.sendActionBar(Component.text("⚔ Reflective Guard expired.", NamedTextColor.GRAY));
                player.getWorld().spawnParticle(Particle.CLOUD,
                    player.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.05);
            }
        }, durationTicks);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ARCANIST STAFF — Charged Shot
    // ═════════════════════════════════════════════════════════════════════════

    public boolean hasChargedShot(UUID playerId) {
        return chargedShotReady.contains(playerId);
    }

    public void setChargedShot(UUID playerId, boolean charged) {
        if (charged) chargedShotReady.add(playerId);
        else chargedShotReady.remove(playerId);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ARCHMAGE'S WAND — Charge System (3 uses → 90 s cooldown)
    // ═════════════════════════════════════════════════════════════════════════

    /** Returns remaining charges (1–3). Defaults to 3 if never set. */
    public int getArcmageCharges(UUID playerId) {
        return archmageCharges.getOrDefault(playerId, 3);
    }

    /**
     * Consumes one charge.  Returns false if no charges remain (cooldown is active).
     * When charges reach zero, schedules a recharge after 90 s.
     */
    public boolean consumeArcmageCharge(Player player) {
        UUID uid = player.getUniqueId();
        int current = getArcmageCharges(uid);
        if (current <= 0) return false;  // cooldown active, no charges

        int remaining = current - 1;
        archmageCharges.put(uid, remaining);

        if (remaining <= 0) {
            // Out of charges — start recharge timer
            player.sendActionBar(Component.text("✦ Arcane Burst — recharging in 90s...", NamedTextColor.RED));
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                archmageCharges.put(uid, 3);
                if (player.isOnline()) {
                    player.sendActionBar(Component.text("✦ Arcane Burst — fully recharged! (3/3)", NamedTextColor.LIGHT_PURPLE));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);
                }
            }, 90L * 20L);
        }
        return true;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ASSASSIN'S BLADE — Shadow Invisibility
    // ═════════════════════════════════════════════════════════════════════════

    public boolean isShadowInvisible(UUID playerId) {
        return shadowInvisible.contains(playerId);
    }

    /**
     * Makes the player invisible for 5 seconds with Speed II.
     * During this phase: enemies cannot attack them, natural healing is cancelled.
     */
    public void activateShadow(Player player) {
        UUID uid = player.getUniqueId();

        // Cancel any previous shadow task
        BukkitTask existing = shadowTasks.remove(uid);
        if (existing != null) existing.cancel();

        shadowInvisible.add(uid);

        // Potion effects — invisibility + Speed II
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 5 * 20 + 10, 0, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 5 * 20 + 10, 1, false, true, true));

        player.getWorld().spawnParticle(Particle.PORTAL,
            player.getLocation().add(0, 1, 0), 50, 0.4, 0.8, 0.4, 0.3);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.5f);
        player.sendActionBar(Component.text("☠ The Shadow — you vanish...", NamedTextColor.DARK_PURPLE));

        // Schedule expiry after 5 s
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            shadowInvisible.remove(uid);
            shadowTasks.remove(uid);
            if (player.isOnline()) {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                player.getWorld().spawnParticle(Particle.PORTAL,
                    player.getLocation().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.3);
                player.sendActionBar(Component.text("☠ The Shadow — you reappear.", NamedTextColor.GRAY));
            }
        }, 5L * 20L);

        shadowTasks.put(uid, task);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SHADOWBLADE — Bleeding
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Applies a bleeding effect to an entity: 1 damage every 20 ticks for 10 seconds.
     * Refreshes if already bleeding.
     */
    public void applyBleeding(LivingEntity entity, Player attacker) {
        UUID entityId = entity.getUniqueId();

        // Cancel existing bleed task (refresh/reset)
        BukkitTask existing = bleedTasks.remove(entityId);
        if (existing != null) existing.cancel();

        BukkitRunnable bleedTask = new BukkitRunnable() {
            int pulses = 0;

            @Override
            public void run() {
                if (!entity.isValid() || entity.isDead() || pulses >= 10) {
                    bleedTasks.remove(entityId);
                    cancel();
                    return;
                }
                entity.damage(1.0, attacker);
                // Blood-like particles
                entity.getWorld().spawnParticle(Particle.CRIT,
                    entity.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.05);
                pulses++;
            }
        };

        BukkitTask task = bleedTask.runTaskTimer(plugin, 20L, 20L);
        bleedTasks.put(entityId, task);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HARMONY WAND — DOT on mobs / HOT on players
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Applies 5 damage × 3 pulses (every 20 ticks) to a living entity.
     * @param entity  the mob that was hit
     * @param shooter the player who fired (used as damage source)
     */
    public void applyHarmonyDamage(LivingEntity entity, Player shooter) {
        UUID entityId = entity.getUniqueId();

        BukkitTask existing = harmonyTasks.remove(entityId);
        if (existing != null) existing.cancel();

        BukkitRunnable task = new BukkitRunnable() {
            int pulses = 0;

            @Override
            public void run() {
                if (!entity.isValid() || entity.isDead() || pulses >= 3) {
                    harmonyTasks.remove(entityId);
                    cancel();
                    return;
                }
                entity.damage(5.0, shooter);
                entity.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                    entity.getLocation().add(0, 1.2, 0), 6, 0.2, 0.2, 0.2, 0);
                pulses++;
            }
        };

        BukkitTask t = task.runTaskTimer(plugin, 0L, 20L);
        harmonyTasks.put(entityId, t);
    }

    /**
     * Heals a player for 5 HP × 3 pulses (every 20 ticks).
     * @param target the allied player who was hit by the bolt
     */
    public void applyHarmonyHeal(Player target) {
        UUID targetId = target.getUniqueId();

        BukkitTask existing = harmonyTasks.remove(targetId);
        if (existing != null) existing.cancel();

        BukkitRunnable task = new BukkitRunnable() {
            int pulses = 0;

            @Override
            public void run() {
                if (!target.isOnline() || target.isDead() || pulses >= 3) {
                    harmonyTasks.remove(targetId);
                    cancel();
                    return;
                }
                double newHealth = Math.min(target.getMaxHealth(), target.getHealth() + 5.0);
                target.setHealth(newHealth);
                target.getWorld().spawnParticle(Particle.HEART,
                    target.getLocation().add(0, 2, 0), 4, 0.3, 0.3, 0.3, 0);
                target.sendActionBar(Component.text("❤ +5 healing pulse", NamedTextColor.GREEN));
                pulses++;
            }
        };

        BukkitTask t = task.runTaskTimer(plugin, 0L, 20L);
        harmonyTasks.put(targetId, t);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CLEANUP — called from onDisable and PlayerQuitEvent
    // ═════════════════════════════════════════════════════════════════════════

    /** Cancels all running tasks. Call from plugin onDisable(). */
    public void cleanup() {
        bleedTasks.values().forEach(BukkitTask::cancel);
        bleedTasks.clear();

        harmonyTasks.values().forEach(BukkitTask::cancel);
        harmonyTasks.clear();

        shadowTasks.values().forEach(BukkitTask::cancel);
        shadowTasks.clear();

        reflectShieldActive.clear();
        chargedShotReady.clear();
        shadowInvisible.clear();
        archmageCharges.clear();
    }

    /** Cleans up state for a specific player on quit. */
    public void cleanupPlayer(UUID playerId) {
        reflectShieldActive.remove(playerId);
        chargedShotReady.remove(playerId);
        shadowInvisible.remove(playerId);
        archmageCharges.remove(playerId);

        BukkitTask shadowTask = shadowTasks.remove(playerId);
        if (shadowTask != null) shadowTask.cancel();
    }
}
