package com.example.weapons.bosses;

import com.example.weapons.WeaponsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class ShadowGhoulManager {

    public static final String GHOUL_TAG = "shadow_ghoul";

    private final WeaponsPlugin plugin;
    private final NamespacedKey ghoulKey;
    private final Map<UUID, BukkitTask> aiTasks = new HashMap<>();

    public ShadowGhoulManager(WeaponsPlugin plugin) {
        this.plugin   = plugin;
        this.ghoulKey = new NamespacedKey(plugin, "shadow_ghoul");
    }

    public Zombie spawn(Location loc) {
        Zombie zombie = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);

        zombie.getPersistentDataContainer().set(ghoulKey, PersistentDataType.STRING, GHOUL_TAG);
        zombie.setRemoveWhenFarAway(true);
        zombie.setSilent(false);

        AttributeInstance maxHp = zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHp != null) maxHp.setBaseValue(100.0);
        zombie.setHealth(100.0);

        AttributeInstance dmg = zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(12.0);

        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,       Integer.MAX_VALUE, 2, false, false, false));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));

        zombie.customName(Component.text("Shadow Ghoul", NamedTextColor.BLACK)
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));
        zombie.setCustomNameVisible(true);

        // Delay equipment by 1 tick so the entity is fully initialised
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            EntityEquipment eq = zombie.getEquipment();
            eq.setHelmet(buildSkull());
            eq.setChestplate(coloredLeather(Material.LEATHER_CHESTPLATE));
            eq.setLeggings(coloredLeather(Material.LEATHER_LEGGINGS));
            eq.setBoots(coloredLeather(Material.LEATHER_BOOTS));
            eq.setItemInMainHand(new ItemStack(Material.STONE_HOE));
            eq.setHelmetDropChance(0f);
            eq.setChestplateDropChance(0f);
            eq.setLeggingsDropChance(0f);
            eq.setBootsDropChance(0f);
            eq.setItemInMainHandDropChance(0f);
            zombie.setCanPickupItems(false);
            zombie.setConversionTime(-1); // prevent drowned conversion
        }, 1L);

        startAI(zombie);
        return zombie;
    }

    private void startAI(Zombie zombie) {
        Random rng = new Random();
        long[] nextInvisAttack = { rng.nextInt(21) + 10 };
        long[] tickCounter     = { 0 };

        BukkitTask task = new BukkitRunnable() {
            @Override public void run() {
                if (!zombie.isValid() || zombie.isDead()) {
                    aiTasks.remove(zombie.getUniqueId());
                    cancel(); return;
                }
                tickCounter[0]++;

                Player target = getNearestPlayer(zombie, 30);
                if (target != null) ((Mob) zombie).setTarget(target);

                if (tickCounter[0] % 20 == 0) {
                    nextInvisAttack[0]--;
                    if (nextInvisAttack[0] <= 0 && target != null) {
                        triggerInvisAttack(zombie, target);
                        nextInvisAttack[0] = rng.nextInt(21) + 10;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        aiTasks.put(zombie.getUniqueId(), task);
    }

    private void triggerInvisAttack(Zombie zombie, Player target) {
        zombie.removePotionEffect(PotionEffectType.INVISIBILITY);

        Location behind = target.getLocation().clone();
        behind.add(behind.getDirection().multiply(-1.5));
        behind.setYaw(target.getLocation().getYaw() + 180f);
        zombie.teleport(behind);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (zombie.isValid() && !zombie.isDead()) {
                zombie.addPotionEffect(new PotionEffect(
                    PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
            }
        }, 60L);
    }

    public void stopAI(UUID id) {
        BukkitTask t = aiTasks.remove(id);
        if (t != null) t.cancel();
    }

    public void stopAll() {
        aiTasks.values().forEach(BukkitTask::cancel);
        aiTasks.clear();
    }

    public boolean isGhoul(org.bukkit.entity.Entity entity) {
        if (!(entity instanceof Zombie)) return false;
        return GHOUL_TAG.equals(entity.getPersistentDataContainer()
            .get(ghoulKey, PersistentDataType.STRING));
    }

    private Player getNearestPlayer(Zombie zombie, double radius) {
        return zombie.getWorld().getPlayers().stream()
            .filter(p -> !p.isDead() && p.getGameMode() == GameMode.SURVIVAL)
            .filter(p -> p.getLocation().distanceSquared(zombie.getLocation()) <= radius * radius)
            .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(zombie.getLocation())))
            .orElse(null);
    }

    private ItemStack buildSkull() {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        PlayerProfile profile = plugin.getServer()
            .createPlayerProfile(UUID.fromString("161c7d32-dd90-4b31-81a0-d9afe660565a"), "ShadowGhoul");
        meta.setOwnerProfile(profile);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack coloredLeather(Material material) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(Color.BLACK);
        item.setItemMeta(meta);
        return item;
    }
}
}
