package com.example.weapons;

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
    private final NamespacedKey compactedKey;
    private final NamespacedKey compactedRecipeKey;

    /** Tracks locations of placed compacted iron blocks (in-memory). */
    private final Set<Location> placedCompacted = new HashSet<>();

    public CustomRecipes(WeaponsPlugin plugin) {
        this.plugin             = plugin;
        this.compactedKey       = new NamespacedKey(plugin, "compacted_iron_block");
        this.compactedRecipeKey = new NamespacedKey(plugin, "compacted_iron_block_recipe");
    }

    public void register() {
        // ── Compacted Iron Block: 3x3 iron blocks ─────────────────────────
        ShapedRecipe compactedRecipe = new ShapedRecipe(compactedRecipeKey, buildCompacted());
        compactedRecipe.shape("III", "III", "III");
        compactedRecipe.setIngredient('I', Material.IRON_BLOCK);
        plugin.getServer().addRecipe(compactedRecipe);

        // ── Greatsword + reverse compacted recipe via PrepareItemCraftEvent ─
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CRAFTING LOGIC
    // ─────────────────────────────────────────────────────────────────────────

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        // ── Reverse: 1 compacted iron block → 9 iron blocks ──────────────
        int filledSlots = 0;
        ItemStack onlyItem = null;
        for (ItemStack s : matrix) {
            if (s != null && s.getType() != Material.AIR) {
                filledSlots++;
                onlyItem = s;
            }
        }
        if (filledSlots == 1 && isCompacted(onlyItem)) {
            inv.setResult(new ItemStack(Material.IRON_BLOCK, 9));
            return;
        }

        // ── Greatsword pattern ────────────────────────────────────────────
        // [ ][C][ ]
        // [S][C][S]
        // [ ][s][ ]
        // C = compacted iron, S = shield, s = stick
        if (!isEmpty(matrix[0])) return;
        if (!isCompacted(matrix[1])) return;
        if (!isEmpty(matrix[2])) return;
        if (!isShield(matrix[3])) return;
        if (!isCompacted(matrix[4])) return;
        if (!isShield(matrix[5])) return;
        if (!isEmpty(matrix[6])) return;
        if (!isStick(matrix[7])) return;
        if (!isEmpty(matrix[8])) return;

        inv.setResult(new WeaponFactory(plugin).create(WeaponType.GREATSWORD));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BLOCK PLACE / BREAK — return compacted block on break
    // ─────────────────────────────────────────────────────────────────────────

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isCompacted(event.getItemInHand())) {
            placedCompacted.add(event.getBlock().getLocation());
        }
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
    //  ITEM BUILDER
    // ─────────────────────────────────────────────────────────────────────────

    public ItemStack buildCompacted() {
        ItemStack item = new ItemStack(Material.IRON_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Compacted Iron Block", NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text("Compressed iron.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Place in crafting to revert to 9 iron blocks.", NamedTextColor.DARK_GRAY)
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
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    public boolean isCompacted(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return COMPACTED_ID.equals(item.getItemMeta()
            .getPersistentDataContainer()
            .get(compactedKey, PersistentDataType.STRING));
    }

    private boolean isEmpty(ItemStack i)  { return i == null || i.getType() == Material.AIR; }
    private boolean isShield(ItemStack i) { return i != null && i.getType() == Material.SHIELD; }
    private boolean isStick(ItemStack i)  { return i != null && i.getType() == Material.STICK; }
}
