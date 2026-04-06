package com.example.weapons.bosses;

import com.example.weapons.WeaponsPlugin;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public final class ShadowGhoulListener implements Listener {

    private final WeaponsPlugin      plugin;
    private final ShadowGhoulManager manager;
    private final BossItems          items;
    private final Random             rng = new Random();

    public ShadowGhoulListener(WeaponsPlugin plugin) {
        this.plugin  = plugin;
        this.manager = plugin.getShadowGhoulManager();
        this.items   = plugin.getBossItems();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.ZOMBIE) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;

        World world = event.getLocation().getWorld();
        if (world == null) return;

        // Night only
        long time = world.getTime();
        if (time < 13000 || time > 23000) return;

        // Dark Forest biome only
        Biome biome = event.getLocation().getBlock().getBiome();
        if (biome != Biome.DARK_FOREST) return;

        // 1/40 chance
        if (rng.nextInt(40) != 0) return;

        event.setCancelled(true);
        org.bukkit.Bukkit.getScheduler().runTask(plugin,
            () -> manager.spawn(event.getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGhoulAttack(EntityDamageByEntityEvent event) {
        if (!manager.isGhoul(event.getDamager())) return;
        event.setDamage(12.0);
    }

    @EventHandler
    public void onGhoulDeath(EntityDeathEvent event) {
        if (!manager.isGhoul(event.getEntity())) return;

        event.getDrops().clear();
        event.setDroppedExp(0);

        // Always drop 1 Shadow
        event.getDrops().add(items.buildShadow());

        // 1/25 chance: drop 1-2 Assassin's Bones
        if (rng.nextInt(25) == 0) {
            int amount = rng.nextInt(3) + 1;
            ItemStack bone = items.buildAssassinsBone();
            bone.setAmount(amount);
            event.getDrops().add(bone);
        }

        // Remove the TextDisplay passenger so it does not linger in the world.
        manager.removeDisplayPassengers((org.bukkit.entity.Zombie) event.getEntity());
        manager.stopAI(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onEntityTransform(EntityTransformEvent event) {
        if (event.getTransformReason() != EntityTransformEvent.TransformReason.DROWNED) return;
        if (manager.isGhoul(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
