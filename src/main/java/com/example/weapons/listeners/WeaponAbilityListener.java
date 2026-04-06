package com.example.weapons.listeners;

import com.example.weapons.WeaponsPlugin;
import com.example.weapons.items.ItemKeys;
import com.example.weapons.items.WeaponType;
import com.example.weapons.managers.CooldownManager;
import com.example.weapons.managers.WeaponStateManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles PlayerInteractEvent (right-click) for all 7 custom weapon abilities.
 *
 *  Greatsword       → Reflective Guard (30 s CD)
 *  Dominican Axe    → Ground Smash 20 dmg AOE (10 s CD)
 *  Arcanist Staff   → Flame particle beam 4 dmg (0.5 s shot CD)
 *                      Sneak+RC: Charged Shot — next beam deals 25 dmg (30 s CD)
 *  Archmage's Wand  → Arcane particle beam 8 dmg (0.5 s shot CD)
 *                      Sneak+RC: Arcane Burst — knockback + 10 % heal (3 charges → 90 s)
 *  Shadowblade      → Shadowstep dash (10 s CD)
 *  Assassin's Blade → The Shadow invis (10 s CD)
 *  Harmony Wand     → RC: damage beam 4 HP×3 | Sneak+RC: heal beam 4 HP×3 (0.5 s CD)
 *
 * ALL wand shots are pure particle ray-casts — no projectile entities spawned.
 */
public final class WeaponAbilityListener implements Listener {

    // ── Cooldown keys ─────────────────────────────────────────────────────────
    private static final String CD_GREATSWORD       = "greatsword";
    private static final String CD_DOMINICAN        = "dominican_axe";
    private static final String CD_ARCANIST_CHARGE  = "arcanist_charge";   // charged-shot CD
    private static final String CD_ARCANIST_SHOT    = "arcanist_shot";     // 0.5 s per shot
    private static final String CD_ARCHMAGE_SHOT    = "archmage_shot";     // 0.5 s per shot
    private static final String CD_SHADOWBLADE      = "shadowblade";
    private static final String CD_ASSASSIN         = "assassins_blade";
    private static final String CD_HARMONY_DMG  = "harmony_dmg";   // 1s
    private static final String CD_HARMONY_HEAL = "harmony_heal";  // 2s

    private final WeaponsPlugin      plugin;
    private final ItemKeys            keys;
    private final CooldownManager    cooldowns;
    private final WeaponStateManager state;

    public WeaponAbilityListener(WeaponsPlugin plugin) {
        this.plugin    = plugin;
        this.keys      = plugin.getItemKeys();
        this.cooldowns = plugin.getCooldownManager();
        this.state     = plugin.getStateManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        WeaponType type = getWeaponType(held);
        if (type == null) return;

        event.setCancelled(true);

        switch (type) {
            case GREATSWORD      -> activateReflectShield(player);
            case DOMINICAN_AXE   -> activateGroundSmash(player);
            case ARCANIST_STAFF  -> {
                if (player.isSneaking()) activateChargedShot(player);
                else                     fireArcanistBeam(player);
            }
            case ARCHMAGES_WAND  -> {
                if (player.isSneaking()) activateArcaneBurst(player);
                else                     fireArcmageBeam(player);
            }
            case SHADOWBLADE     -> activateShadowstep(player);
            case ASSASSINS_BLADE -> activateShadow(player);
            case HARMONY_WAND    -> {
                if (player.isSneaking()) fireHarmonyHealBeam(player);
                else                     fireHarmonyDamageBeam(player);
            }
        }
    }

    // ═══ GREATSWORD — Reflective Guard ═══════════════════════════════════════

    private void activateReflectShield(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_GREATSWORD)) { sendCD(player, cooldowns.getRemainingSeconds(uid, CD_GREATSWORD)); return; }
        if (state.hasReflectShield(uid)) { player.sendActionBar(Component.text("⚔ Already active!", NamedTextColor.YELLOW)); return; }
        state.activateReflectShield(player, 5L * 20L);
        cooldowns.setCooldown(uid, CD_GREATSWORD, 30_000L);
    }

    // ═══ DOMINICAN AXE — Ground Smash ════════════════════════════════════════

    private void activateGroundSmash(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_DOMINICAN)) { sendCD(player, cooldowns.getRemainingSeconds(uid, CD_DOMINICAN)); return; }

        Location epicentre = player.getLocation();
        for (LivingEntity entity : epicentre.getNearbyLivingEntities(4.5)) {
            if (entity.equals(player)) continue;
            Vector kb = entity.getLocation().toVector().subtract(epicentre.toVector()).setY(0.5).normalize().multiply(2.0);
            entity.setVelocity(kb);
            // Ability damage — bypass weapon-damage override
            state.addAbilityBypass(uid);
            entity.damage(20.0, player);
            state.removeAbilityBypass(uid);
        }

        player.getWorld().spawnParticle(Particle.CRIT,  epicentre, 80, 3, 0.2, 3, 0.3);
        player.getWorld().spawnParticle(Particle.CLOUD, epicentre, 40, 3, 0.1, 3, 0.1);
        player.getWorld().playSound(epicentre, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 0.5f);
        player.getWorld().playSound(epicentre, Sound.ENTITY_GENERIC_EXPLODE,          1.0f, 1.5f);
        player.sendActionBar(Component.text("⚒ Ground Smash!", NamedTextColor.DARK_RED));
        cooldowns.setCooldown(uid, CD_DOMINICAN, 10_000L);
    }

    // ═══ ARCANIST STAFF — Charged Shot (Sneak+RC) ════════════════════════════

    private void activateChargedShot(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_ARCANIST_CHARGE)) { sendCD(player, cooldowns.getRemainingSeconds(uid, CD_ARCANIST_CHARGE)); return; }
        if (state.hasChargedShot(uid)) { player.sendActionBar(Component.text("✦ Already primed!", NamedTextColor.YELLOW)); return; }
        state.setChargedShot(uid, true);
        cooldowns.setCooldown(uid, CD_ARCANIST_CHARGE, 30_000L);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 30, 0.4, 0.4, 0.4, 0.05);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.5f);
        player.sendActionBar(Component.text("✦ Charged Shot primed! Fire when ready.", NamedTextColor.AQUA));
    }

    // ═══ ARCANIST STAFF — Flame Particle Beam (RC) ═══════════════════════════

    private void fireArcanistBeam(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_ARCANIST_SHOT)) return;   // silent — 0.5 s is fast

        boolean charged = state.hasChargedShot(uid);
        double  damage  = charged ? 25.0 : 4.0;
        if (charged) state.setChargedShot(uid, false);

        LivingEntity hit = fireParticleBeam(player, Particle.FLAME, 0.02);

        if (hit != null) {
            state.addAbilityBypass(uid);
            hit.damage(damage, player);
            state.removeAbilityBypass(uid);
            hit.getWorld().playSound(hit.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.7f, 1.4f);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.3f);
        if (charged) player.sendActionBar(Component.text("✦ Charged Shot! (25 dmg)", NamedTextColor.GOLD));
        cooldowns.setCooldown(uid, CD_ARCANIST_SHOT, 500L);
    }

    // ═══ ARCHMAGE'S WAND — Arcane Particle Beam (RC) ════════════════════════

    private void fireArcmageBeam(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_ARCHMAGE_SHOT)) return;

        LivingEntity hit = fireParticleBeam(player, Particle.END_ROD, 0.0);

        if (hit != null) {
            state.addAbilityBypass(uid);
            hit.damage(8.0, player);
            state.removeAbilityBypass(uid);
            hit.getWorld().spawnParticle(Particle.PORTAL,
                hit.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.2);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.8f);
        cooldowns.setCooldown(uid, CD_ARCHMAGE_SHOT, 500L);
    }

    // ═══ ARCHMAGE'S WAND — Arcane Burst (Sneak+RC) ═══════════════════════════

    private void activateArcaneBurst(Player player) {
        UUID uid = player.getUniqueId();
        int charges = state.getArcmageCharges(uid);
        if (charges <= 0) { player.sendActionBar(Component.text("✦ Arcane Burst recharging...", NamedTextColor.RED)); return; }

        if (!state.consumeArcmageCharge(player)) return;
        int remaining = state.getArcmageCharges(uid);

        Location origin = player.getLocation();
        for (LivingEntity entity : origin.getNearbyLivingEntities(6.0)) {
            if (entity.equals(player)) continue;
            Vector kb = entity.getLocation().toVector().subtract(origin.toVector()).setY(0.4).normalize().multiply(3.0);
            entity.setVelocity(kb);
        }

        double healAmount = player.getMaxHealth() * 0.10;   // 10 %
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + healAmount));

        player.getWorld().spawnParticle(Particle.PORTAL,  origin.add(0,1,0), 100, 1.5, 1.5, 1.5, 0.5);
        player.getWorld().spawnParticle(Particle.END_ROD, origin,            40,  1.5, 1.5, 1.5, 0.2);
        player.getWorld().playSound(origin, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f,  0.7f);
        player.getWorld().playSound(origin, Sound.BLOCK_BEACON_AMBIENT,         0.8f,1.5f);

        if (remaining > 0) {
            player.sendActionBar(Component.text("✦ Arcane Burst! (" + remaining + "/3 charges)", NamedTextColor.LIGHT_PURPLE));
        }
    }

    // ═══ SHADOWBLADE — Shadowstep ════════════════════════════════════════════

    private void activateShadowstep(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_SHADOWBLADE)) { sendCD(player, cooldowns.getRemainingSeconds(uid, CD_SHADOWBLADE)); return; }

        Vector direction = player.getLocation().getDirection().clone().setY(0);
        if (direction.lengthSquared() < 0.001) direction = new Vector(1, 0, 0);
        direction.normalize();

        Location origin  = player.getLocation().clone();
        Location dashEnd = origin.clone();
        Set<LivingEntity> hitEntities = new HashSet<>();

        // Trace 5 blocks in 0.5-block steps, stop at solid walls
        for (double step = 0.5; step <= 5.0; step += 0.5) {
            Location check = origin.clone().add(direction.clone().multiply(step));
            if (!check.getBlock().isPassable() || !check.clone().add(0,1,0).getBlock().isPassable()) break;
            dashEnd = check;
            for (LivingEntity nearby : check.getNearbyLivingEntities(0.9, 1.1, 0.9)) {
                if (!nearby.equals(player)) hitEntities.add(nearby);
            }
        }

        dashEnd.setYaw(origin.getYaw());
        dashEnd.setPitch(origin.getPitch());
        player.teleport(dashEnd);

        // Fixed 10 dmg dash impact — bypass so CombatListener doesn't use weapon damage
        for (LivingEntity entity : hitEntities) {
            state.addAbilityBypass(uid);
            entity.damage(10.0, player);
            state.removeAbilityBypass(uid);
            state.applyBleeding(entity, player);
        }

        player.getWorld().spawnParticle(Particle.PORTAL, origin.add(0,1,0),   30, 0.4, 0.6, 0.4, 0.4);
        player.getWorld().spawnParticle(Particle.PORTAL, dashEnd.clone().add(0,1,0), 30, 0.4, 0.6, 0.4, 0.4);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.4f);
        player.sendActionBar(Component.text("☠ Shadowstep! Hit " + hitEntities.size() + " enemy(ies).", NamedTextColor.DARK_PURPLE));
        cooldowns.setCooldown(uid, CD_SHADOWBLADE, 10_000L);
    }

    // ═══ ASSASSIN'S BLADE — The Shadow ═══════════════════════════════════════

    private void activateShadow(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_ASSASSIN)) { sendCD(player, cooldowns.getRemainingSeconds(uid, CD_ASSASSIN)); return; }
        if (state.isShadowInvisible(uid)) { player.sendActionBar(Component.text("☠ Already in shadow!", NamedTextColor.YELLOW)); return; }
        state.activateShadow(player);
        cooldowns.setCooldown(uid, CD_ASSASSIN, 45_000L);
    }

    // ═══ HARMONY WAND — Damage Beam (RC) ════════════════════════════════════

    private void fireHarmonyDamageBeam(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_HARMONY_DMG)) return;

        LivingEntity hit = fireParticleBeam(player, Particle.CRIT, 0.0);

        if (hit != null) {
            state.applyHarmonyDamage(hit, player);
            hit.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                hit.getLocation().add(0, 1.5, 0), 6, 0.2, 0.2, 0.2, 0);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 1.5f);
        cooldowns.setCooldown(uid, CD_HARMONY_DMG, 1000L);
    }

    // ═══ HARMONY WAND — Heal Beam (Sneak+RC) ════════════════════════════════

    private void fireHarmonyHealBeam(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_HARMONY_HEAL)) return;

        LivingEntity hit = fireParticleBeam(player, Particle.HEART, 0.0);

        if (hit instanceof Player targetPlayer && !targetPlayer.equals(player)) {
            state.applyHarmonyHeal(targetPlayer);
            targetPlayer.getWorld().spawnParticle(Particle.HEART,
                targetPlayer.getLocation().add(0, 2.3, 0), 6, 0.4, 0.3, 0.4, 0);
        }
        // Hitting a non-player entity with the heal beam has no effect

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.8f);
        cooldowns.setCooldown(uid, CD_HARMONY_HEAL, 2000L);
    }

    // ═══ PARTICLE BEAM UTILITY ═══════════════════════════════════════════════

    /**
     * Traces a ray from the player's eye, 10 blocks long, spawning particles.
     * Stops at solid blocks. Returns the FIRST LivingEntity hit, or null.
     *
     * @param particle visual particle to spawn along the ray
     * @param extra    particle extra/speed parameter (0 = stationary dots)
     */
    private LivingEntity fireParticleBeam(Player player, Particle particle, double extra) {
        Location start = player.getEyeLocation();
        Vector   dir   = start.getDirection().normalize();
        LivingEntity firstHit = null;

        for (double d = 0.3; d <= 10.0; d += 0.3) {
            Location point = start.clone().add(dir.clone().multiply(d));
            Block    block = point.getBlock();

            // Stop ray at solid blocks
            if (!block.isPassable()) break;

            // Spawn 2 particles per step for a solid beam appearance
            player.getWorld().spawnParticle(particle, point, 2, 0.05, 0.05, 0.05, extra);

            // Check for the first entity in the beam's path
            if (firstHit == null) {
                for (LivingEntity entity : point.getWorld().getNearbyLivingEntities(point, 0.6, 0.7, 0.6)) {
                    if (!entity.equals(player)) {
                        firstHit = entity;
                        break;
                    }
                }
            }
        }

        return firstHit;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void sendCD(Player player, long secondsLeft) {
        player.sendActionBar(Component.text("⏱ On cooldown: " + secondsLeft + "s", NamedTextColor.RED));
    }

    private WeaponType getWeaponType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta()
            .getPersistentDataContainer()
            .get(keys.WEAPON_ID, PersistentDataType.STRING);
        return WeaponType.fromId(id);
    }
}
