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
import org.bukkit.block.Biome;
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

public final class WeaponAbilityListener implements Listener {

    private static final String CD_GREATSWORD      = "greatsword";
    private static final String CD_DOMINICAN       = "dominican_axe";
    private static final String CD_ARCANIST_CHARGE = "arcanist_charge";
    private static final String CD_ARCANIST_SHOT   = "arcanist_shot";
    private static final String CD_ARCHMAGE_SHOT   = "archmage_shot";
    private static final String CD_SHADOWBLADE     = "shadowblade";
    private static final String CD_ASSASSIN        = "assassins_blade";
    private static final String CD_HARMONY_DMG     = "harmony_dmg";
    private static final String CD_HARMONY_HEAL    = "harmony_heal";

    private final WeaponsPlugin      plugin;
    private final ItemKeys           keys;
    private final CooldownManager   cooldowns;
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

    // ═══ GREATSWORD ══════════════════════════════════════════════════════════

    private void activateReflectShield(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_GREATSWORD)) { sendCD(player, cooldowns.getRemainingSeconds(uid, CD_GREATSWORD)); return; }
        if (state.hasReflectShield(uid)) { player.sendActionBar(Component.text("⚔ Already active!", NamedTextColor.YELLOW)); return; }
        state.activateReflectShield(player, 5L * 20L);
        cooldowns.setCooldown(uid, CD_GREATSWORD, 30_000L);
    }

    // ═══ DOMINICAN AXE ═══════════════════════════════════════════════════════

    private void activateGroundSmash(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_DOMINICAN)) { sendCD(player, cooldowns.getRemainingSeconds(uid, CD_DOMINICAN)); return; }

        Location epicentre = player.getLocation();
        for (LivingEntity entity : epicentre.getNearbyLivingEntities(4.5)) {
            if (entity.equals(player)) continue;
            Vector kb = entity.getLocation().toVector().subtract(epicentre.toVector()).setY(0.5).normalize().multiply(2.0);
            entity.setVelocity(kb);
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

    // ═══ ARCANIST STAFF ══════════════════════════════════════════════════════

    private void activateChargedShot(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_ARCANIST_CHARGE)) { sendCD(player, cooldowns.getRemainingSeconds(uid, CD_ARCANIST_CHARGE)); return; }
        if (state.hasChargedShot(uid)) { player.sendActionBar(Component.text("✦ Already primed!", NamedTextColor.YELLOW)); return; }
        state.setChargedShot(uid, true);
        cooldowns.setCooldown(uid, CD_ARCANIST_CHARGE, 30_000L);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 30, 0.4, 0.4, 0.4, 0.05);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.5f);
        player.sendActionBar(Component.text("✦ Charged Shot primed!", NamedTextColor.AQUA));
    }

    private void fireArcanistBeam(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_ARCANIST_SHOT)) return;

        boolean charged = state.hasChargedShot(uid);
        double  damage  = charged ? 25.0 : 4.0;
        if (charged) state.setChargedShot(uid, false);

        LivingEntity hit = fireParticleBeam(player, Particle.FLAME, 0.02);
        if (hit != null) {
            state.addAbilityBypass(uid);
            hit.damage(damage, player);
            state.removeAbilityBypass(uid);
            cancelKnockback(hit);
            hit.getWorld().playSound(hit.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.7f, 1.4f);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.3f);
        if (charged) player.sendActionBar(Component.text("✦ Charged Shot! (25 dmg)", NamedTextColor.GOLD));
        cooldowns.setCooldown(uid, CD_ARCANIST_SHOT, 500L);
    }

    // ═══ ARCHMAGE'S WAND ═════════════════════════════════════════════════════

    private void fireArcmageBeam(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_ARCHMAGE_SHOT)) return;

        LivingEntity hit = fireParticleBeam(player, Particle.END_ROD, 0.0);
        if (hit != null) {
            state.addAbilityBypass(uid);
            hit.damage(8.0, player);
            state.removeAbilityBypass(uid);
            cancelKnockback(hit);
            hit.getWorld().spawnParticle(Particle.PORTAL, hit.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.2);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.8f);
        cooldowns.setCooldown(uid, CD_ARCHMAGE_SHOT, 500L);
    }

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

        double healAmount = player.getMaxHealth() * 0.10;
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + healAmount));

        player.getWorld().spawnParticle(Particle.PORTAL,  origin.clone().add(0,1,0), 100, 1.5, 1.5, 1.5, 0.5);
        player.getWorld().spawnParticle(Particle.END_ROD, origin,                     40,  1.5, 1.5, 1.5, 0.2);
        player.getWorld().playSound(origin, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f,  0.7f);
        player.getWorld().playSound(origin, Sound.BLOCK_BEACON_AMBIENT,         0.8f,1.5f);

        if (remaining > 0) player.sendActionBar(Component.text("✦ Arcane Burst! (" + remaining + "/3 charges)", NamedTextColor.LIGHT_PURPLE));
    }

    // ═══ SHADOWBLADE ═════════════════════════════════════════════════════════

    private void activateShadowstep(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_SHADOWBLADE)) { sendCD(player, cooldowns.getRemainingSeconds(uid, CD_SHADOWBLADE)); return; }

        Vector direction = player.getLocation().getDirection().clone().setY(0);
        if (direction.lengthSquared() < 0.001) direction = new Vector(1, 0, 0);
        direction.normalize();

        Location origin  = player.getLocation().clone();
        Location dashEnd = origin.clone();
        Set<LivingEntity> hitEntities = new HashSet<>();

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

        for (LivingEntity entity : hitEntities) {
            state.addAbilityBypass(uid);
            entity.damage(10.0, player);
            state.removeAbilityBypass(uid);
            cancelKnockback(entity);
            state.applyBleeding(entity, player);
        }

        player.getWorld().spawnParticle(Particle.PORTAL, origin.clone().add(0,1,0), 30, 0.4, 0.6, 0.4, 0.4);
        player.getWorld().spawnParticle(Particle.PORTAL, dashEnd.clone().add(0,1,0), 30, 0.4, 0.6, 0.4, 0.4);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.4f);
        player.sendActionBar(Component.text("☠ Shadowstep! Hit " + hitEntities.size() + " enemy(ies).", NamedTextColor.DARK_PURPLE));
        cooldowns.setCooldown(uid, CD_SHADOWBLADE, 10_000L);
    }

    // ═══ ASSASSIN'S BLADE ════════════════════════════════════════════════════

    private void activateShadow(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_ASSASSIN)) { sendCD(player, cooldowns.getRemainingSeconds(uid, CD_ASSASSIN)); return; }
        if (state.isShadowInvisible(uid)) { player.sendActionBar(Component.text("☠ Already in shadow!", NamedTextColor.YELLOW)); return; }
        state.activateShadow(player);
        cooldowns.setCooldown(uid, CD_ASSASSIN, 45_000L);
    }

    // ═══ HARMONY WAND ════════════════════════════════════════════════════════

    private void fireHarmonyDamageBeam(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_HARMONY_DMG)) return;

        LivingEntity hit = fireParticleBeam(player, Particle.CRIT, 0.0);
        if (hit != null) {
            state.applyHarmonyDamage(hit, player);
            cancelKnockback(hit);
            hit.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, hit.getLocation().add(0, 1.5, 0), 6, 0.2, 0.2, 0.2, 0);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 1.5f);
        cooldowns.setCooldown(uid, CD_HARMONY_DMG, 1000L);
    }

    private void fireHarmonyHealBeam(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_HARMONY_HEAL)) return;

        LivingEntity hit = fireParticleBeam(player, Particle.HEART, 0.0);
        if (hit instanceof Player) {
            Player targetPlayer = (Player) hit;
            if (!targetPlayer.equals(player)) {
                state.applyHarmonyHeal(targetPlayer);
                targetPlayer.getWorld().spawnParticle(Particle.HEART, targetPlayer.getLocation().add(0, 2.3, 0), 6, 0.4, 0.3, 0.4, 0);
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.8f);
        cooldowns.setCooldown(uid, CD_HARMONY_HEAL, 2000L);
    }

    // ═══ PARTICLE BEAM ═══════════════════════════════════════════════════════

    private LivingEntity fireParticleBeam(Player player, Particle particle, double extra) {
        Location start = player.getEyeLocation();
        Vector   dir   = start.getDirection().normalize();
        LivingEntity firstHit = null;

        for (double d = 0.3; d <= 10.0; d += 0.3) {
            Location point = start.clone().add(dir.clone().multiply(d));
            Block    block = point.getBlock();
            if (!block.isPassable()) break;

            player.getWorld().spawnParticle(particle, point, 2, 0.05, 0.05, 0.05, extra);

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

    // ═══ KNOCKBACK CANCEL ════════════════════════════════════════════════════

    /**
     * Strictly zeroes horizontal (XZ) velocity on the very next server task,
     * which runs after the current tick's physics — including knockback — have
     * been applied.  Vertical velocity (gravity / jump arc) is preserved so the
     * entity doesn't snap to the ground unnaturally.
     *
     * Using runTask (0-tick delay) is critical: the old runTaskLater(1L) ran one
     * full tick too late, meaning the already-applied knockback impulse had
     * already moved the entity before it was cleared.
     */
    private void cancelKnockback(LivingEntity entity) {
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            if (entity.isValid() && !entity.isDead()) {
                entity.setVelocity(new Vector(0, entity.getVelocity().getY(), 0));
            }
        });
    }

    // ═══ HELPERS ═════════════════════════════════════════════════════════════

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
