package com.example.weapons;

import com.example.weapons.items.ItemBuilder;
import com.example.weapons.items.WeaponFactory;
import com.example.weapons.items.WeaponType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;

import java.util.List;

public final class CustomRecipes implements Listener {

    private static final String COMPACTED_ID = "compacted_iron_block";

    private final WeaponsPlugin plugin;
    private final NamespacedKey compactedKey;
    private final NamespacedKey compactedRecipeKey;
    private final NamespacedKey greatswordRecipeKey;

    public CustomRecipes(WeaponsPlugin plugin) {
        this.plugin             = plugin;
        this.compactedKey       = new NamespacedKey(plugin, "compacted_iron_block");
        this.compactedRecipeKey = new NamespacedKey(plugin, "compacted_iron_block_recipe");
        this.greatswordRecipeKey= new NamespacedKey(plugin, "greatsword_recipe");
    }

    public void register() {
        // ── Compacted Iron Block recipe (3x3 iron blocks) ─────────────────
        ItemStack compacted = buildCompacted();
        ShapedRecipe compactedRecipe = new ShapedRecipe(compactedRecipeKey, compacted);
        compactedRecipe.shape("III", "III", "III");
        compactedRecipe.setIngredient('I', Material.IRON_BLOCK);
        Bukkit.addRecipe(compactedRecipe);

        // ── Greatsword recipe registered via PrepareItemCraftEvent ────────
        // (vanilla ShapedRecipe can't match PDC ingredients, so we handle
        //  result-setting manually in onPrepare below)
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /** Builds a Compacted Iron Block ItemStack tagged with PDC. */
    public ItemStack buildCompacted() {
        ItemStack item = new ItemStack(Material.IRON_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Compacted Iron Block", NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Compressed iron.", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(compactedKey, PersistentDataType.STRING, COMPACTED_ID);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isCompacted(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return COMPACTED_ID.equals(item.getItemMeta()
            .getPersistentDataContainer()
            .get(compactedKey, PersistentDataType.STRING));
    }

    /**
     * Greatsword pattern:
     *   [ ][ C ][ ]
     *   [S][ C ][S]
     *   [ ][ st][ ]
     *  C = compacted iron block, S = shield, st = stick
     *  Slots: 0-8, top-left to bottom-right
     */
    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        // Pattern check
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

    private boolean isEmpty(ItemStack i)  { return i == null || i.getType() == Material.AIR; }
    private boolean isShield(ItemStack i) { return i != null && i.getType() == Material.SHIELD; }
    private boolean isStick(ItemStack i)  { return i != null && i.getType() == Material.STICK; }
}
