package com.example.weapons.bosses;

import com.example.weapons.WeaponsPlugin;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
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

        long time = world.getTime();
        if (time < 13000 || time > 23000) return;

        if (rng.nextInt(120) != 0) return;

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

        event.getDrops().add(items.buildShadow());

        if (rng.nextInt(30) == 0) {
            int amount = rng.nextInt(3) + 1;
            ItemStack bone = items.buildAssassinsBone();
            bone.setAmount(amount);
            event.getDrops().add(bone);
        }

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
