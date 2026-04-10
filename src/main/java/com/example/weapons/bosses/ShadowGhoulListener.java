package com.example.weapons.bosses;

import com.example.weapons.WeaponsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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

    // ── Enderman killed in The End → possible ghoul spawn ───────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEndermanDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.ENDERMAN) return;

        // Strictly The End only — overworld and nether are hard-blocked
        if (event.getEntity().getWorld().getEnvironment() != World.Environment.THE_END) return;

        Location deathLoc = event.getEntity().getLocation();

        // 1/80 chance: spawn a Shadow Ghoul
        if (rng.nextInt(80) != 0) return;

        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            manager.spawn(deathLoc);

            Component announcement = Component.text("☠ A Shadow Ghoul has emerged from the void!", NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false);

            for (Player nearby : deathLoc.getNearbyEntitiesByType(Player.class, 10.0)) {
                nearby.sendMessage(announcement);
            }
        });
    }

    // ── Ghoul melee damage is fixed at 12 ────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGhoulAttack(EntityDamageByEntityEvent event) {
        if (!manager.isGhoul(event.getDamager())) return;
        event.setDamage(12.0);
    }

    // ── Ghoul death drops ─────────────────────────────────────────────────────

    @EventHandler
    public void onGhoulDeath(EntityDeathEvent event) {
        if (!manager.isGhoul(event.getEntity())) return;

        event.getDrops().clear();
        event.setDroppedExp(0);

        // Always drop 1 Shadow
        event.getDrops().add(items.buildShadow());

        // 1/25 chance: drop 1-2 Assassin's Bones
        if (rng.nextInt(25) == 0) {
            int amount = rng.nextInt(2) + 1;
            ItemStack bone = items.buildAssassinsBone();
            bone.setAmount(amount);
            event.getDrops().add(bone);
        }

        manager.stopAI(event.getEntity().getUniqueId());
    }

    // ── Prevent ghoul from converting to drowned ─────────────────────────────

    @EventHandler
    public void onEntityTransform(EntityTransformEvent event) {
        if (event.getTransformReason() != EntityTransformEvent.TransformReason.DROWNED) return;
        if (manager.isGhoul(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
