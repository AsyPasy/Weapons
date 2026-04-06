package com.example.weapons;

import com.example.weapons.bosses.BossItems;
import com.example.weapons.bosses.ShadowGhoulListener;
import com.example.weapons.bosses.ShadowGhoulManager;
import com.example.weapons.command.WeaponCommand;
import com.example.weapons.items.ItemKeys;
import com.example.weapons.listeners.ProjectileListener;
import com.example.weapons.listeners.WeaponAbilityListener;
import com.example.weapons.listeners.WeaponCombatListener;
import com.example.weapons.managers.CooldownManager;
import com.example.weapons.managers.WeaponStateManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class WeaponsPlugin extends JavaPlugin {

    private static WeaponsPlugin instance;
    private ItemKeys           itemKeys;
    private CooldownManager    cooldownManager;
    private WeaponStateManager stateManager;
    private ShadowGhoulManager shadowGhoulManager;
    private BossItems          bossItems;

    @Override
    public void onEnable() {
        instance = this;

        this.itemKeys           = new ItemKeys(this);
        this.cooldownManager    = new CooldownManager();
        this.stateManager       = new WeaponStateManager(this);
        this.bossItems          = new BossItems(this);
        this.shadowGhoulManager = new ShadowGhoulManager(this);

        getServer().getPluginManager().registerEvents(new WeaponCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new WeaponAbilityListener(this), this);
        getServer().getPluginManager().registerEvents(new ProjectileListener(this), this);
        getServer().getPluginManager().registerEvents(new ShadowGhoulListener(this), this);

        new CustomRecipes(this).register();

        Objects.requireNonNull(getCommand("weapon")).setExecutor(new WeaponCommand(this));

        getLogger().info("WeaponsPlugin enabled.");
    }

    @Override
    public void onDisable() {
        if (stateManager       != null) stateManager.cleanup();
        if (shadowGhoulManager != null) shadowGhoulManager.stopAll();
    }

    public static WeaponsPlugin getInstance()           { return instance; }
    public ItemKeys            getItemKeys()            { return itemKeys; }
    public CooldownManager     getCooldownManager()     { return cooldownManager; }
    public WeaponStateManager  getStateManager()        { return stateManager; }
    public ShadowGhoulManager  getShadowGhoulManager()  { return shadowGhoulManager; }
    public BossItems           getBossItems()           { return bossItems; }
}
