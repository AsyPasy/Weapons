package com.example.weapons;

import com.example.weapons.bosses.BossItems;
import com.example.weapons.items.WeaponFactory;
import com.example.weapons.items.WeaponType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CustomRecipes implements Listener {

    private static final String COMPACTED_ID = "compacted_iron_block";

    private final WeaponsPlugin plugin;
    private final BossItems     bossItems;
    private final NamespacedKey compactedKey;
    private final NamespacedKey compactedRecipeKey;

    private final Set<Location> placedCompacted = new HashSet<>();

    public CustomRecipes(WeaponsPlugin plugin) {
        this.plugin             = plugin;
        this.bossItems          = plugin.getBossItems();
        this.compactedKey       = new NamespacedKey(plugin, "compacted_iron_block");
        this.compactedRecipeKey = new NamespacedKey(plugin, "compacted_iron_block_recipe");
    }

    public void register() {
        ShapedRecipe compactedRecipe = new ShapedRecipe(compactedRecipeKey, buildCompacted());
        compactedRecipe.shape("III", "III", "III");
        compactedRecipe.setIngredient('I', Material.IRON_BLOCK);
        plugin.getServer().addRecipe(compactedRecipe);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CRAFTING
    // ─────────────────────────────────────────────────────────────────────────

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] m = inv.getMatrix();

        // ── Reverse: 1 compacted → 9 iron blocks ─────────────────────────
        int filled = 0;
        ItemStack only = null;
        for (ItemStack s : m) { if (s != null && s.getType() != Material.AIR) { filled++; only = s; } }
        if (filled == 1 && isCompacted(only)) {
            inv.setResult(new ItemStack(Material.IRON_BLOCK, 9));
            return;
        }

        // ── Greatsword ────────────────────────────────────────────────────
        // [ ][C][ ]
        // [S][C][S]
        // [ ][s][ ]
        if (isEmpty(m[0]) && isCompacted(m[1]) && isEmpty(m[2]) &&
            isShield(m[3]) && isCompacted(m[4]) && isShield(m[5]) &&
            isEmpty(m[6]) && isStick(m[7]) && isEmpty(m[8])) {
            inv.setResult(new WeaponFactory(plugin).create(WeaponType.GREATSWORD));
            return;
        }

        // ── Shadowblade ───────────────────────────────────────────────────
        // [ ][Sh][ ]
        // [ ][Sh][ ]
        // [ ][St][ ]
        // Sh = Shadow, St = stick
        if (isEmpty(m[0]) && isShadow(m[1]) && isEmpty(m[2]) &&
            isEmpty(m[3]) && isShadow(m[4]) && isEmpty(m[5]) &&
            isEmpty(m[6]) && isStick(m[7]) && isEmpty(m[8])) {
            inv.setResult(new WeaponFactory(plugin).create(WeaponType.SHADOWBLADE));
            return;
        }

        // ── Assassin's Blade ──────────────────────────────────────────────
        // [ ][GI][ ]
        // [AB][Sh][AB]
        // [ ][AB][ ]
        // GI = gold ingot, AB = assassin's bone, Sh = shadow
        if (isEmpty(m[0]) && isGoldIngot(m[1]) && isEmpty(m[2]) &&
            isAssassinsBone(m[3]) && isShadow(m[4]) && isAssassinsBone(m[5]) &&
            isEmpty(m[6]) && isAssassinsBone(m[7]) && isEmpty(m[8])) {
            inv.setResult(new WeaponFactory(plugin).create(WeaponType.ASSASSINS_BLADE));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PLACE / BREAK — return compacted block
    // ─────────────────────────────────────────────────────────────────────────

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isCompacted(event.getItemInHand()))
            placedCompacted.add(event.getBlock().getLocation());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (placedCompacted.remove(loc)) {
            event.setDropItems(false);
            loc.getWorld().dropItemNaturally(loc, buildCompacted());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  COMPACTED IRON BLOCK ITEM
    // ─────────────────────────────────────────────────────────────────────────

    public ItemStack buildCompacted() {
        ItemStack item = new ItemStack(Material.IRON_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Compacted Iron Block", NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text("Compressed iron.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Place in crafting alone to revert to 9 iron blocks.", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false)
        ));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setMaxStackSize(64);
        meta.getPersistentDataContainer().set(compactedKey, PersistentDataType.STRING, COMPACTED_ID);
        item.setItemMeta(meta);
        return item;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MATCHERS
    // ─────────────────────────────────────────────────────────────────────────

    public boolean isCompacted(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return COMPACTED_ID.equals(item.getItemMeta().getPersistentDataContainer()
            .get(compactedKey, PersistentDataType.STRING));
    }

    private boolean isShadow(ItemStack item)        { return bossItems.isShadow(item); }
    private boolean isAssassinsBone(ItemStack item) { return bossItems.isAssassinsBone(item); }
    private boolean isEmpty(ItemStack i)            { return i == null || i.getType() == Material.AIR; }
    private boolean isShield(ItemStack i)           { return i != null && i.getType() == Material.SHIELD; }
    private boolean isStick(ItemStack i)            { return i != null && i.getType() == Material.STICK; }
    private boolean isGoldIngot(ItemStack i)        { return i != null && i.getType() == Material.GOLD_INGOT; }
}
