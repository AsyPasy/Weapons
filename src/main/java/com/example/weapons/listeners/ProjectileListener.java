package com.example.weapons.listeners;

import com.example.weapons.WeaponsPlugin;
import com.example.weapons.items.ItemKeys;
import org.bukkit.Particle;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listens for projectile-hit events on custom weapon projectiles.
 *
 * When a tagged arrow/snowball hits a BLOCK (not an entity),
 * we remove it immediately so it doesn't litter the world or
 * bounce into unintended targets.
 *
 * Entity-hit logic is handled by WeaponCombatListener via
 * EntityDamageByEntityEvent (which fires after ProjectileHitEvent).
 */
public final class ProjectileListener implements Listener {

    private final ItemKeys keys;

    public ProjectileListener(WeaponsPlugin plugin) {
        this.keys = plugin.getItemKeys();
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        String projType = proj.getPersistentDataContainer()
            .get(keys.PROJECTILE_TYPE, PersistentDataType.STRING);

        if (projType == null) return; // not our projectile

        // If it hit a block (no entity), remove the projectile and spawn a small puff
        if (event.getHitEntity() == null) {
            proj.getWorld().spawnParticle(Particle.CLOUD,
                proj.getLocation(), 6, 0.2, 0.2, 0.2, 0.02);
            proj.remove();
        }
        // If it hit an entity, we leave it to EntityDamageByEntityEvent in WeaponCombatListener
        // The arrow will naturally be removed by the server after dealing damage
    }
}
