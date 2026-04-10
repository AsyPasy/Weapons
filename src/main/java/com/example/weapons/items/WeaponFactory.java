package com.example.weapons.items;

import com.example.weapons.WeaponsPlugin;
import com.google.common.collect.HashMultimap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class WeaponFactory {

    private final WeaponsPlugin plugin;

    public WeaponFactory(WeaponsPlugin plugin) { this.plugin = plugin; }

    public ItemStack create(WeaponType type) {
        return switch (type) {
            case GREATSWORD      -> buildGreatsword();
            case DOMINICAN_AXE   -> buildDominicanAxe();
            case ARCANIST_STAFF  -> buildArcanistStaff();
            case ARCHMAGES_WAND  -> buildArchmagesWand();
            case SHADOWBLADE     -> buildShadowblade();
            case ASSASSINS_BLADE -> buildAssassinsBlade();
            case HARMONY_WAND    -> buildHarmonyWand();
        };
    }

    // ─── WEAPONS ─────────────────────────────────────────────────────────────

    // RARE
    private ItemStack buildGreatsword() {
        return new ItemBuilder(Material.NETHERITE_SWORD)
            .name(name("⚔ Greatsword", "RARE"))
            .lore(List.of(
                stat("Normal Hit", "7"),
                stat("Critical Hit", "10"),
                Component.empty(),
                ability("Reflective Guard"),
                keybind("Right-Click"),
                desc("Activate a 5-second damage-absorbing shield."),
                desc("Reflects 80% of incoming damage back to the attacker."),
                cooldown("30s"),
                Component.empty(),
                rarity("RARE", "Sword")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .clearAttributes()   // wipes vanilla attack damage/speed — removes "When in Main Hand"
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.GREATSWORD.getId())
            .maxStackSize(1)
            .build();
    }

    // EPIC
    private ItemStack buildDominicanAxe() {
        return new ItemBuilder(Material.NETHERITE_AXE)
            .name(name("⚒ Dominican Axe", "EPIC"))
            .lore(List.of(
                stat("Normal Hit", "10"),
                stat("Critical Hit", "13"),
                Component.empty(),
                ability("Ground Smash"),
                keybind("Right-Click"),
                desc("Slam the ground, pushing all nearby enemies away."),
                desc("Deals 20 HP to all enemies in the area."),
                cooldown("10s"),
                Component.empty(),
                rarity("EPIC", "Axe")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .clearAttributes()   // wipes vanilla attack damage/speed — removes "When in Main Hand"
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.DOMINICAN_AXE.getId())
            .maxStackSize(1)
            .build();
    }

    // UNCOMMON
    private ItemStack buildArcanistStaff() {
        return new ItemBuilder(Material.BLAZE_ROD)
            .name(name("✦ Arcanist Staff", "UNCOMMON"))
            .lore(List.of(
                stat("Particle Shot", "4"),
                Component.empty(),
                ability("Charged Shot"),
                keybind("Sneak + Right-Click"),
                desc("Charge your next particle shot."),
                desc("Next bolt deals 25 HP damage."),
                cooldown("30s"),
                Component.empty(),
                rarity("UNCOMMON", "Staff")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            // BLAZE_ROD has no default weapon attributes — clearAttributes() not needed
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.ARCANIST_STAFF.getId())
            .maxStackSize(1)
            .build();
    }

    // LEGENDARY
    private ItemStack buildArchmagesWand() {
        return new ItemBuilder(Material.END_ROD)
            .name(name("✦ Archmage's Wand", "LEGENDARY"))
            .lore(List.of(
                stat("Particle Shot", "8"),
                Component.empty(),
                ability("Arcane Burst"),
                keybind("Sneak + Right-Click"),
                desc("Release arcane energy — knocks back ALL nearby enemies."),
                desc("Heals you for 10% of max HP. 3 charges before cooldown."),
                cooldown("90s (after 3 uses)"),
                Component.empty(),
                rarity("LEGENDARY", "Wand")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            // END_ROD has no default weapon attributes — clearAttributes() not needed
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.ARCHMAGES_WAND.getId())
            .maxStackSize(1)
            .build();
    }

    // VERY RARE
    private ItemStack buildShadowblade() {
        return new ItemBuilder(Material.IRON_SWORD)
            .name(name("☠ Shadowblade", "VERY RARE"))
            .lore(List.of(
                stat("Normal Hit", "5.6"),
                stat("Critical Hit", "7"),
                Component.empty(),
                ability("Shadowstep"),
                keybind("Right-Click"),
                desc("Dash 5 blocks forward, damaging enemies in your path."),
                desc("Applies Bleeding: 1 HP damage per second for 7 seconds."),
                cooldown("10s"),
                Component.empty(),
                rarity("VERY RARE", "Blade")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .clearAttributes()   // wipes vanilla attack damage/speed — removes "When in Main Hand"
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.SHADOWBLADE.getId())
            .maxStackSize(1)
            .build();
    }

    // LEGENDARY
    private ItemStack buildAssassinsBlade() {
        return new ItemBuilder(Material.GOLDEN_SWORD)
            .name(name("☠ Assassin's Blade", "LEGENDARY"))
            .lore(List.of(
                stat("Normal Hit", "7.2"),
                stat("Critical Hit", "9"),
                Component.empty(),
                ability("The Shadow"),
                keybind("Right-Click"),
                desc("Vanish for 10 seconds. Grants Speed III."),
                desc("Enemies cannot see or attack you."),
                desc("Natural healing disabled (potions still work)."),
                cooldown("10s"),
                Component.empty(),
                rarity("LEGENDARY", "Blade")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .clearAttributes()   // wipes vanilla attack damage/speed — removes "When in Main Hand"
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.ASSASSINS_BLADE.getId())
            .maxStackSize(1)
            .build();
    }

    // EPIC
    private ItemStack buildHarmonyWand() {
        return new ItemBuilder(Material.STICK)
            .name(name("Harmony Wand", "EPIC"))
            .lore(List.of(
                ability("Harmony Bolt"),
                keybind("Right-Click"),
                desc("Fire a damage bolt — deals 4 HP x 3 pulses (1s apart)."),
                Component.empty(),
                keybind("Sneak + Right-Click"),
                desc("Fire a healing bolt — restores 4 HP x 3 pulses (1s apart)."),
                desc("Multiple bolts can stack on the same target."),
                Component.empty(),
                cooldownMulti("Attack: 1s", "Heal: 2s"),
                Component.empty(),
                rarity("EPIC", "Wand")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            // STICK has no default weapon attributes — clearAttributes() not needed
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.HARMONY_WAND.getId())
            .maxStackSize(1)
            .build();
    }

    // ─── Lore helpers ─────────────────────────────────────────────────────────

    /**
     * Resolves the rarity label to its display color.
     *   COMMON    → WHITE
     *   UNCOMMON  → GREEN
     *   RARE      → BLUE
     *   VERY RARE → DARK_AQUA
     *   EPIC      → DARK_PURPLE
     *   LEGENDARY → GOLD
     */
    private NamedTextColor rarityColor(String rarityLabel) {
        return switch (rarityLabel) {
            case "UNCOMMON"  -> NamedTextColor.GREEN;
            case "RARE"      -> NamedTextColor.BLUE;
            case "VERY RARE" -> NamedTextColor.DARK_AQUA;
            case "EPIC"      -> NamedTextColor.DARK_PURPLE;
            case "LEGENDARY" -> NamedTextColor.GOLD;
            default          -> NamedTextColor.WHITE; // COMMON
        };
    }

    /**
     * Converts an ALL CAPS string to Title Case.
     * e.g. "VERY RARE" → "Very Rare", "LEGENDARY" → "Legendary"
     */
    private String toTitleCase(String s) {
        String[] words = s.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1))
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Item display name — colored by rarity, no italic.
     * e.g. name("⚔ Greatsword", "RARE") → blue "⚔ Greatsword"
     */
    private Component name(String displayName, String rarityLabel) {
        return Component.text(displayName, rarityColor(rarityLabel))
            .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Stat line — gray label, green value, no + prefix.
     * e.g. stat("Normal Hit", "7") → gray "Normal Hit: " + green "7 HP"
     */
    private Component stat(String label, String value) {
        return Component.text(label + ": ", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(value + " HP", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
    }

    /**
     * Ability header — dark purple "Ability: " + gold bold name.
     * e.g. "Ability: Reflective Guard"
     */
    private Component ability(String name) {
        return Component.text("Ability: ", NamedTextColor.DARK_PURPLE)
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(name, NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decorate(TextDecoration.BOLD));
    }

    /**
     * Keybind line — yellow.
     * e.g. "Right-Click" / "Sneak + Right-Click"
     */
    private Component keybind(String bind) {
        return Component.text(bind, NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Description line — gray, indented two spaces.
     */
    private Component desc(String s) {
        return Component.text("  " + s, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Single cooldown line — green.
     * e.g. "Cooldown: 30s"
     */
    private Component cooldown(String s) {
        return Component.text("Cooldown: ", NamedTextColor.GREEN)
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(s, NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
    }

    /**
     * Multi-cooldown line — green, entries joined by " | ".
     * e.g. "Cooldown: Attack: 1s  |  Heal: 2s"
     */
    private Component cooldownMulti(String... entries) {
        return Component.text("Cooldown: " + String.join("  |  ", entries), NamedTextColor.GREEN)
            .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Rarity footer — title case, rarity color, bold.
     * The second argument is the short weapon type noun (already in Title Case).
     * e.g. rarity("LEGENDARY", "Blade") → gold bold "Legendary Blade"
     *      rarity("VERY RARE",  "Blade") → teal bold "Very Rare Blade"
     */
    private Component rarity(String rarityLabel, String weaponType) {
        String display = toTitleCase(rarityLabel) + " " + weaponType;
        return Component.text(display, rarityColor(rarityLabel))
            .decoration(TextDecoration.ITALIC, false)
            .decorate(TextDecoration.BOLD);
    }
}
