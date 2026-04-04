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
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles PlayerInteractEvent (right-click) for all 7 custom weapon abilities:
 *
 *  Greatsword      → Reflective Guard (5 s reflect shield, 30 s CD)
 *  Dominican Axe   → Ground Smash (AOE 50 dmg + knockback, 10 s CD)
 *  Arcanist Staff  → Fire bolt (15 dmg) | Sneak: Charged Shot (+50 dmg next bolt, 60 s CD)
 *  Archmage's Wand → Arcane Burst (knockback + 30% heal, 3 charges then 90 s CD)
 *  Shadowblade     → Shadowstep (5-block dash + 10 dmg + bleed, 10 s CD)
 *  Assassin's Blade→ The Shadow (5 s invis + SpeedII, 40 s CD)
 *  Harmony Wand    → Fire harmony bolt (DOT on mobs / HOT on players)
 */
public final class WeaponAbilityListener implements Listener {

    private final WeaponsPlugin plugin;
    private final ItemKeys keys;
    private final CooldownManager cooldowns;
    private final WeaponStateManager state;

    // Cooldown keys — string constants to avoid typos
    private static final String CD_GREATSWORD   = "greatsword";
    private static final String CD_DOMINICAN    = "dominican_axe";
    private static final String CD_ARCANIST_CD  = "arcanist_charge";   // charged-shot activation
    private static final String CD_ARCHMAGE     = "archmages_wand";
    private static final String CD_SHADOWBLADE  = "shadowblade";
    private static final String CD_ASSASSIN     = "assassins_blade";

    public WeaponAbilityListener(WeaponsPlugin plugin) {
        this.plugin   = plugin;
        this.keys     = plugin.getItemKeys();
        this.cooldowns = plugin.getCooldownManager();
        this.state    = plugin.getStateManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle right-click actions; ignore off-hand to avoid double-firing
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        WeaponType type = getWeaponType(held);
        if (type == null) return;

        // Always cancel default block/item interaction for our weapons
        event.setCancelled(true);

        switch (type) {
            case GREATSWORD      -> activateReflectShield(player);
            case DOMINICAN_AXE   -> activateGroundSmash(player);
            case ARCANIST_STAFF  -> {
                if (player.isSneaking()) activateChargedShot(player);
                else                     fireArcanistBolt(player);
            }
            case ARCHMAGES_WAND  -> activateArcaneBurst(player);
            case SHADOWBLADE     -> activateShadowstep(player);
            case ASSASSINS_BLADE -> activateShadow(player);
            case HARMONY_WAND    -> fireHarmonyBolt(player);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GREATSWORD — Reflective Guard
    // ═════════════════════════════════════════════════════════════════════════

    private void activateReflectShield(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_GREATSWORD)) {
            sendCooldown(player, cooldowns.getRemainingSeconds(uid, CD_GREATSWORD));
            return;
        }
        if (state.hasReflectShield(uid)) {
            player.sendActionBar(Component.text("⚔ Reflective Guard already active!", NamedTextColor.YELLOW));
            return;
        }

        // Activate 5-second shield
        state.activateReflectShield(player, 5L * 20L);
        cooldowns.setCooldown(uid, CD_GREATSWORD, 30_000L);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  DOMINICAN AXE — Ground Smash
    // ═════════════════════════════════════════════════════════════════════════

    private void activateGroundSmash(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_DOMINICAN)) {
            sendCooldown(player, cooldowns.getRemainingSeconds(uid, CD_DOMINICAN));
            return;
        }

        Location epicentre = player.getLocation();

        // Collect nearby living entities (4-block radius)
        for (LivingEntity entity : epicentre.getNearbyLivingEntities(4.5)) {
            if (entity.equals(player)) continue;

            // Push the entity away from the player
            Vector knockback = entity.getLocation().toVector()
                .subtract(epicentre.toVector())
                .setY(0.5)
                .normalize()
                .multiply(2.0);
            entity.setVelocity(knockback);

            // Deal 50 damage
            entity.damage(50.0, player);
        }

        // Visual & audio — ground shockwave effect
        player.getWorld().spawnParticle(Particle.CRIT,
            epicentre, 80, 3, 0.2, 3, 0.3);
        player.getWorld().spawnParticle(Particle.CLOUD,
            epicentre, 40, 3, 0.1, 3, 0.1);
        player.getWorld().playSound(epicentre, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 0.5f);
        player.getWorld().playSound(epicentre, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
        player.sendActionBar(Component.text("⚒ Ground Smash!", NamedTextColor.DARK_RED));

        cooldowns.setCooldown(uid, CD_DOMINICAN, 10_000L);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ARCANIST STAFF — Fire Bolt / Charged Shot activation
    // ═════════════════════════════════════════════════════════════════════════

    /** Activates the charged-shot buff. Next fired bolt will deal 50 dmg. */
    private void activateChargedShot(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_ARCANIST_CD)) {
            sendCooldown(player, cooldowns.getRemainingSeconds(uid, CD_ARCANIST_CD));
            return;
        }
        if (state.hasChargedShot(uid)) {
            player.sendActionBar(Component.text("✦ Charged Shot already primed!", NamedTextColor.YELLOW));
            return;
        }

        state.setChargedShot(uid, true);
        cooldowns.setCooldown(uid, CD_ARCANIST_CD, 60_000L);

        player.getWorld().spawnParticle(Particle.FLAME,
            player.getLocation().add(0, 1, 0), 30, 0.4, 0.4, 0.4, 0.05);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.5f);
        player.sendActionBar(Component.text("✦ Charged Shot primed! Fire when ready.", NamedTextColor.AQUA));
    }

    /** Fires an arrow with damage stored in its PDC. */
    private void fireArcanistBolt(Player player) {
        UUID uid = player.getUniqueId();
        boolean charged = state.hasChargedShot(uid);
        double damage = charged ? 50.0 : 15.0;

        // Consume charge
        if (charged) {
            state.setChargedShot(uid, false);
            player.sendActionBar(Component.text("✦ CHARGED bolt fired! (" + (int) damage + " dmg)", NamedTextColor.GOLD));
        }

        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setDamage(damage);  // override vanilla damage via the Arrow API
        arrow.setShooter(player);
        arrow.setVelocity(player.getLocation().getDirection().multiply(2.5));
        arrow.setGlowing(charged); // glowing if charged for visual feedback

        // Tag the projectile so WeaponCombatListener knows to override its damage
        PersistentDataContainer pdc = arrow.getPersistentDataContainer();
        pdc.set(keys.PROJECTILE_TYPE,   PersistentDataType.STRING, "arcanist");
        pdc.set(keys.PROJECTILE_SHOOTER, PersistentDataType.STRING, uid.toString());
        pdc.set(keys.PROJECTILE_DAMAGE,  PersistentDataType.DOUBLE, damage);

        // Launch particles from the staff tip
        player.getWorld().spawnParticle(Particle.FLAME,
            player.getEyeLocation(), 10, 0.1, 0.1, 0.1, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.6f, 1.3f);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ARCHMAGE'S WAND — Arcane Burst (3 charges → 90 s CD)
    // ═════════════════════════════════════════════════════════════════════════

    private void activateArcaneBurst(Player player) {
        UUID uid = player.getUniqueId();

        // Check if we have charges available
        int charges = state.getArcmageCharges(uid);
        if (charges <= 0) {
            // Still on recharge cooldown
            player.sendActionBar(Component.text("✦ Arcane Burst recharging...", NamedTextColor.RED));
            return;
        }

        // Consume a charge (this also starts recharge timer if we hit 0)
        boolean consumed = state.consumeArcmageCharge(player);
        if (!consumed) return;

        int remaining = state.getArcmageCharges(uid);
        Location origin = player.getLocation();

        // Knock back all nearby entities (6-block radius)
        for (LivingEntity entity : origin.getNearbyLivingEntities(6.0)) {
            if (entity.equals(player)) continue;
            Vector knockback = entity.getLocation().toVector()
                .subtract(origin.toVector())
                .setY(0.4)
                .normalize()
                .multiply(3.0);
            entity.setVelocity(knockback);
        }

        // Heal player 30% of max health
        double healAmount = player.getMaxHealth() * 0.30;
        double newHealth  = Math.min(player.getMaxHealth(), player.getHealth() + healAmount);
        player.setHealth(newHealth);

        // Visual & audio
        player.getWorld().spawnParticle(Particle.PORTAL,
            origin.add(0, 1, 0), 100, 1.5, 1.5, 1.5, 0.5);
        player.getWorld().spawnParticle(Particle.END_ROD,
            origin, 40, 1.5, 1.5, 1.5, 0.2);
        player.getWorld().playSound(origin, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 0.7f);
        player.getWorld().playSound(origin, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 1.5f);

        if (remaining > 0) {
            player.sendActionBar(Component.text("✦ Arcane Burst! (" + remaining + "/3 charges left)", NamedTextColor.LIGHT_PURPLE));
        }
        // If remaining == 0, the state manager already sent a "recharging" message
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SHADOWBLADE — Shadowstep
    // ═════════════════════════════════════════════════════════════════════════

    private void activateShadowstep(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_SHADOWBLADE)) {
            sendCooldown(player, cooldowns.getRemainingSeconds(uid, CD_SHADOWBLADE));
            return;
        }

        // Horizontal direction only (no vertical component) for a ground dash
        Vector direction = player.getLocation().getDirection().clone().setY(0);
        if (direction.lengthSquared() < 0.001) direction = new Vector(1, 0, 0); // fallback
        direction.normalize();

        Location origin   = player.getLocation().clone();
        Location dashEnd  = origin.clone();
        Set<LivingEntity> hitEntities = new HashSet<>();

        // Trace 5 blocks in 0.5-block increments — stop at solid walls
        for (double step = 0.5; step <= 5.0; step += 0.5) {
            Location check = origin.clone().add(direction.clone().multiply(step));
            Block feet = check.getBlock();
            Block head = check.clone().add(0, 1, 0).getBlock();

            // Stop if the player can't fit through
            if (!feet.isPassable() || !head.isPassable()) break;

            dashEnd = check;

            // Collect nearby entities along the dash path
            for (LivingEntity nearby : check.getNearbyLivingEntities(0.9, 1.1, 0.9)) {
                if (!nearby.equals(player)) hitEntities.add(nearby);
            }
        }

        // Preserve yaw/pitch (so player doesn't look weird after teleport)
        dashEnd.setYaw(origin.getYaw());
        dashEnd.setPitch(origin.getPitch());

        // Teleport
        player.teleport(dashEnd);

        // Damage and apply bleeding to everything hit along the path
        for (LivingEntity entity : hitEntities) {
            entity.damage(10.0, player);     // dash impact damage
            state.applyBleeding(entity, player);
        }

        // Smoke trail effect
        player.getWorld().spawnParticle(Particle.PORTAL, origin.add(0, 1, 0),
            30, 0.4, 0.6, 0.4, 0.4);
        player.getWorld().spawnParticle(Particle.PORTAL, dashEnd.clone().add(0, 1, 0),
            30, 0.4, 0.6, 0.4, 0.4);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.4f);
        player.sendActionBar(Component.text(
            "☠ Shadowstep! Hit " + hitEntities.size() + " enemy(ies).", NamedTextColor.DARK_PURPLE));

        cooldowns.setCooldown(uid, CD_SHADOWBLADE, 10_000L);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ASSASSIN'S BLADE — The Shadow
    // ═════════════════════════════════════════════════════════════════════════

    private void activateShadow(Player player) {
        UUID uid = player.getUniqueId();
        if (cooldowns.isOnCooldown(uid, CD_ASSASSIN)) {
            sendCooldown(player, cooldowns.getRemainingSeconds(uid, CD_ASSASSIN));
            return;
        }
        if (state.isShadowInvisible(uid)) {
            player.sendActionBar(Component.text("☠ Already in the shadow!", NamedTextColor.YELLOW));
            return;
        }

        state.activateShadow(player);
        cooldowns.setCooldown(uid, CD_ASSASSIN, 40_000L);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HARMONY WAND — Harmony Bolt
    // ═════════════════════════════════════════════════════════════════════════

    private void fireHarmonyBolt(Player player) {
        // Harmony wand has no cooldown — fire freely
        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.setShooter(player);
        snowball.setVelocity(player.getLocation().getDirection().multiply(2.0));

        // Tag the snowball so ProjectileListener / WeaponCombatListener recognise it
        PersistentDataContainer pdc = snowball.getPersistentDataContainer();
        pdc.set(keys.PROJECTILE_TYPE,    PersistentDataType.STRING, "harmony");
        pdc.set(keys.PROJECTILE_SHOOTER, PersistentDataType.STRING, player.getUniqueId().toString());

        // Particles at launch
        player.getWorld().spawnParticle(Particle.HEART,
            player.getEyeLocation(), 5, 0.2, 0.2, 0.2, 0.0);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.8f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILITIES
    // ─────────────────────────────────────────────────────────────────────────

    private void sendCooldown(Player player, long secondsLeft) {
        player.sendActionBar(Component.text(
            "⏱ On cooldown: " + secondsLeft + "s remaining.", NamedTextColor.RED));
    }

    private WeaponType getWeaponType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta()
                        .getPersistentDataContainer()
                        .get(keys.WEAPON_ID, PersistentDataType.STRING);
        return WeaponType.fromId(id);
    }
}
