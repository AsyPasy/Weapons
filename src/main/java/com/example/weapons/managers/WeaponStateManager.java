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

import java.util.*;

/**
 * Central store for all transient weapon ability states.
 *
 *  abilityDamageBypass  — players whose outgoing damage should NOT be
 *                         overridden by WeaponCombatListener (e.g. DOT ticks,
 *                         ability impacts like Shadowstep or Ground Smash).
 *  reflectShieldActive  — Greatsword reflect shield
 *  chargedShotReady     — Arcanist Staff charged-shot flag
 *  archmageCharges      — Archmage's Wand remaining charges
 *  shadowInvisible      — Assassin's Blade shadow phase
 *  bleedTasks           — Shadowblade bleeding DOT
 *  harmonyDmgTasks      — Harmony Wand damage DOT (stackable)
 *  harmonyHealTasks     — Harmony Wand heal HOT (stackable)
 */
public final class WeaponStateManager {

    private final WeaponsPlugin plugin;

    // ── Ability-damage bypass ─────────────────────────────────────────────────
    /** Players whose next EntityDamageByEntity event must NOT be damage-overridden. */
    private final Set<UUID> abilityDamageBypass = new HashSet<>();

    // ── Greatsword ────────────────────────────────────────────────────────────
    private final Set<UUID> reflectShieldActive = new HashSet<>();

    // ── Arcanist Staff ────────────────────────────────────────────────────────
    private final Set<UUID> chargedShotReady = new HashSet<>();

    // ── Archmage's Wand ───────────────────────────────────────────────────────
    private final Map<UUID, Integer> archmageCharges = new HashMap<>();

    // ── Assassin's Blade ──────────────────────────────────────────────────────
    private final Set<UUID> shadowInvisible = new HashSet<>();
    private final Map<UUID, BukkitTask> shadowTasks = new HashMap<>();

    // ── Shadowblade bleed ─────────────────────────────────────────────────────
    private final Map<UUID, BukkitTask> bleedTasks = new HashMap<>();

    // ── Harmony Wand (stackable) ──────────────────────────────────────────────
    private final Map<UUID, List<BukkitTask>> harmonyDmgTasks  = new HashMap<>();
    private final Map<UUID, List<BukkitTask>> harmonyHealTasks = new HashMap<>();

    public WeaponStateManager(WeaponsPlugin plugin) { this.plugin = plugin; }

    // ═══ ABILITY BYPASS ══════════════════════════════════════════════════════

    /** Mark that the next entity.damage() call from this player is an ability
     *  (DOT tick, dash, smash, etc.) and must skip the weapon-damage override. */
    public void addAbilityBypass(UUID playerId)    { abilityDamageBypass.add(playerId); }
    public void removeAbilityBypass(UUID playerId) { abilityDamageBypass.remove(playerId); }
    public boolean isAbilityDamage(UUID playerId)  { return abilityDamageBypass.contains(playerId); }

    // ═══ GREATSWORD — Reflect Shield ═════════════════════════════════════════

    public boolean hasReflectShield(UUID playerId) { return reflectShieldActive.contains(playerId); }

    public void activateReflectShield(Player player, long durationTicks) {
        UUID uid = player.getUniqueId();
        reflectShieldActive.add(uid);
        player.getWorld().spawnParticle(Particle.END_ROD,
            player.getLocation().add(0, 1, 0), 40, 0.6, 0.8, 0.6, 0.15);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.5f);
        player.sendActionBar(Component.text("⚔ Reflective Guard ACTIVE!", NamedTextColor.AQUA));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            reflectShieldActive.remove(uid);
            if (player.isOnline()) {
                player.sendActionBar(Component.text("⚔ Reflective Guard expired.", NamedTextColor.GRAY));
                player.getWorld().spawnParticle(Particle.CLOUD,
                    player.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.05);
            }
        }, durationTicks);
    }

    // ═══ ARCANIST STAFF — Charged Shot ═══════════════════════════════════════

    public boolean hasChargedShot(UUID playerId)              { return chargedShotReady.contains(playerId); }
    public void setChargedShot(UUID playerId, boolean charged) {
        if (charged) chargedShotReady.add(playerId); else chargedShotReady.remove(playerId);
    }

    // ═══ ARCHMAGE'S WAND — Charges ═══════════════════════════════════════════

    public int getArcmageCharges(UUID playerId) { return archmageCharges.getOrDefault(playerId, 3); }

    /**
     * Consumes one charge. Returns false if no charges remain (recharging).
     * Schedules 90-second recharge when charges hit zero.
     */
    public boolean consumeArcmageCharge(Player player) {
        UUID uid = player.getUniqueId();
        int current = getArcmageCharges(uid);
        if (current <= 0) return false;
        int remaining = current - 1;
        archmageCharges.put(uid, remaining);
        if (remaining <= 0) {
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

    // ═══ ASSASSIN'S BLADE — Shadow Invisibility ══════════════════════════════

    public boolean isShadowInvisible(UUID playerId) { return shadowInvisible.contains(playerId); }

    public void activateShadow(Player player) {
        UUID uid = player.getUniqueId();
        BukkitTask existing = shadowTasks.remove(uid);
        if (existing != null) existing.cancel();
        shadowInvisible.add(uid);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 5 * 20 + 10, 0, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 5 * 20 + 10, 1, false, true, true));
        player.getWorld().spawnParticle(Particle.PORTAL,
            player.getLocation().add(0, 1, 0), 50, 0.4, 0.8, 0.4, 0.3);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.5f);
        player.sendActionBar(Component.text("☠ The Shadow — you vanish...", NamedTextColor.DARK_PURPLE));
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

    // ═══ SHADOWBLADE — Bleeding DOT ══════════════════════════════════════════

    /**
     * 1 damage every 20 ticks for 10 seconds.
     * Uses ability bypass so CombatListener doesn't override the tick damage.
     */
    public void applyBleeding(LivingEntity entity, Player attacker) {
        UUID entityId = entity.getUniqueId();
        BukkitTask existing = bleedTasks.remove(entityId);
        if (existing != null) existing.cancel();

        BukkitTask task = new BukkitRunnable() {
            int pulses = 0;
            @Override public void run() {
                if (!entity.isValid() || entity.isDead() || pulses >= 10) {
                    bleedTasks.remove(entityId);
                    cancel(); return;
                }
                // Bypass weapon-damage override for this tick
                addAbilityBypass(attacker.getUniqueId());
                entity.damage(1.0, attacker);
                removeAbilityBypass(attacker.getUniqueId());
                entity.getWorld().spawnParticle(Particle.CRIT,
                    entity.getLocation().add(0, 1, 0), 4, 0.3, 0.3, 0.3, 0.05);
                pulses++;
            }
        }.runTaskTimer(plugin, 20L, 20L);
        bleedTasks.put(entityId, task);
    }

    // ═══ HARMONY WAND — Stackable DOT / HOT ══════════════════════════════════

    /**
     * Applies 4 HP damage × 3 pulses (every 20 ticks) to a living entity.
     * Stacks: calling again does NOT cancel previous instances.
     */
    public void applyHarmonyDamage(LivingEntity entity, Player shooter) {
        List<BukkitTask> tasks = harmonyDmgTasks.computeIfAbsent(
            entity.getUniqueId(), k -> new ArrayList<>());

        BukkitTask task = new BukkitRunnable() {
            int pulses = 0;
            @Override public void run() {
                if (!entity.isValid() || entity.isDead() || pulses >= 3) {
                    tasks.remove(this); cancel(); return;  // note: this works because BukkitRunnable IS the task
                }
                addAbilityBypass(shooter.getUniqueId());
                entity.damage(4.0, shooter);   // 2 hearts
                removeAbilityBypass(shooter.getUniqueId());
                entity.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                    entity.getLocation().add(0, 1.2, 0), 5, 0.2, 0.2, 0.2, 0);
                pulses++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        tasks.add(task);
    }

    /**
     * Heals a player for 4 HP × 3 pulses (every 20 ticks).
     * Stacks: calling again does NOT cancel previous instances.
     */
    public void applyHarmonyHeal(Player target) {
        List<BukkitTask> tasks = harmonyHealTasks.computeIfAbsent(
            target.getUniqueId(), k -> new ArrayList<>());

        BukkitTask task = new BukkitRunnable() {
            int pulses = 0;
            @Override public void run() {
                if (!target.isOnline() || target.isDead() || pulses >= 3) {
                    tasks.remove(this); cancel(); return;
                }
                double newHp = Math.min(target.getMaxHealth(), target.getHealth() + 4.0);
                target.setHealth(newHp);
                target.getWorld().spawnParticle(Particle.HEART,
                    target.getLocation().add(0, 2.2, 0), 4, 0.4, 0.3, 0.4, 0);
                target.sendActionBar(Component.text("❤ +4 healing pulse", NamedTextColor.GREEN));
                pulses++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        tasks.add(task);
    }

    // ═══ CLEANUP ═════════════════════════════════════════════════════════════

    public void cleanup() {
        bleedTasks.values().forEach(BukkitTask::cancel);
        bleedTasks.clear();
        harmonyDmgTasks.values().forEach(list -> list.forEach(BukkitTask::cancel));
        harmonyDmgTasks.clear();
        harmonyHealTasks.values().forEach(list -> list.forEach(BukkitTask::cancel));
        harmonyHealTasks.clear();
        shadowTasks.values().forEach(BukkitTask::cancel);
        shadowTasks.clear();
        reflectShieldActive.clear();
        chargedShotReady.clear();
        shadowInvisible.clear();
        archmageCharges.clear();
        abilityDamageBypass.clear();
    }

    public void cleanupPlayer(UUID playerId) {
        reflectShieldActive.remove(playerId);
        chargedShotReady.remove(playerId);
        shadowInvisible.remove(playerId);
        archmageCharges.remove(playerId);
        abilityDamageBypass.remove(playerId);
        BukkitTask st = shadowTasks.remove(playerId);
        if (st != null) st.cancel();
    }
}
