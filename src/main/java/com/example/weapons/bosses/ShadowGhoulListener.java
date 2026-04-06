package com.example.weapons.bosses;

import com.example.weapons.WeaponsPlugin;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Handles Shadow Ghoul spawning, damage fix, and drops.
 */
public final class ShadowGhoulListener implements Listener {

    private final WeaponsPlugin       plugin;
    private final ShadowGhoulManager  manager;
    private final BossItems           items;
    private final Random              rng = new Random();

    public ShadowGhoulListener(WeaponsPlugin plugin) {
        this.plugin  = plugin;
        this.manager = plugin.getShadowGhoulManager();
        this.items   = plugin.getBossItems();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SPAWN — 1/120 chance to replace a natural zombie spawn at night
    // ─────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.ZOMBIE) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;

        World world = event.getLocation().getWorld();
        if (world == null) return;

        // Night only: time between 13000 and 23000
        long time = world.getTime();
        if (time < 13000 || time > 23000) return;

        // 1/120 chance
        if (rng.nextInt(120) != 0) return;

        event.setCancelled(true);
        // Spawn on next tick so the cancelled event fully resolves first
        org.bukkit.Bukkit.getScheduler().runTask(plugin,
            () -> manager.spawn(event.getLocation()));
    }
        @EventHandler
public void onEntityTransform(org.bukkit.event.entity.EntityTransformEvent event) {
    if (event.getTransformReason() == org.bukkit.event.entity.EntityTransformEvent.TransformReason.DROWNED
        && manager.isGhoul(event.getEntity())) {
        event.setCancelled(true);
    }
}
    // ─────────────────────────────────────────────────────────────────────────
    //  DAMAGE — always 12, regardless of armor or effects
    // ─────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGhoulAttack(EntityDamageByEntityEvent event) {
        if (!manager.isGhoul(event.getDamager())) return;
        event.setDamage(12.0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DEATH — custom drops
    // ─────────────────────────────────────────────────────────────────────────

    @EventHandler
    public void onGhoulDeath(EntityDeathEvent event) {
        if (!manager.isGhoul(event.getEntity())) return;

        // Clear all vanilla drops
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Always drop 1 Shadow
        event.getDrops().add(items.buildShadow());

        // 1/30 chance: drop 1-3 Assassin's Bones
        if (rng.nextInt(30) == 0) {
            int amount = rng.nextInt(3) + 1;
            ItemStack bone = items.buildAssassinsBone();
            bone.setAmount(amount);
            event.getDrops().add(bone);
        }
    @EventHandler
public void onEntityTransform(org.bukkit.event.entity.EntityTransformEvent event) {
    if (event.getTransformReason() == org.bukkit.event.entity.EntityTransformEvent.TransformReason.DROWNED
        && manager.isGhoul(event.getEntity())) {
        event.setCancelled(true);
    }
}

        manager.stopAI(event.getEntity().getUniqueId());
    }
}
