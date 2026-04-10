package com.example.weapons.items;

import com.example.weapons.WeaponsPlugin;
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
            .name(text("⚔ Greatsword", NamedTextColor.BLUE))
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
                rarity("RARE", "GREATSWORD")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.GREATSWORD.getId())
            .maxStackSize(1)
            .build();
    }

    // EPIC
    private ItemStack buildDominicanAxe() {
        return new ItemBuilder(Material.NETHERITE_AXE)
            .name(text("⚒ Dominican Axe", NamedTextColor.DARK_PURPLE))
            .lore(List.of(
                stat("Normal Hit", "10"),
                stat("Critical Hit", "13"),
                Component.empty(),
                ability("Ground Smash"),
                keybind("Right-Click"),
                desc("Slam the ground, pushing all nearby enemies away."),
                desc("Deals +20 HP to all enemies in the area."),
                cooldown("10s"),
                Component.empty(),
                rarity("EPIC", "DOMINICAN AXE")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.DOMINICAN_AXE.getId())
            .maxStackSize(1)
            .build();
    }

    // UNCOMMON
    private ItemStack buildArcanistStaff() {
        return new ItemBuilder(Material.BLAZE_ROD)
            .name(text("✦ Arcanist Staff", NamedTextColor.GREEN))
            .lore(List.of(
                stat("Particle Shot", "4"),
                Component.empty(),
                ability("Charged Shot"),
                keybind("Sneak + Right-Click"),
                desc("Charge your next particle shot."),
                desc("Next bolt deals +25 HP damage."),
                cooldown("30s"),
                Component.empty(),
                ability("Flame Bolt"),
                keybind("Right-Click"),
                desc("Fire a flame particle bolt."),
                desc("0.5s shot cooldown."),
                Component.empty(),
                rarity("UNCOMMON", "ARCANIST STAFF")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.ARCANIST_STAFF.getId())
            .maxStackSize(1)
            .build();
    }

    // LEGENDARY
    private ItemStack buildArchmagesWand() {
        return new ItemBuilder(Material.END_ROD)
            .name(text("✦ Archmage's Wand", NamedTextColor.GOLD))
            .lore(List.of(
                stat("Particle Shot", "8"),
                Component.empty(),
                ability("Arcane Burst"),
                keybind("Sneak + Right-Click"),
                desc("Release arcane energy — knocks back ALL nearby enemies."),
                desc("Heals you for 10% of max HP. 3 charges before cooldown."),
                cooldown("90s (after 3 uses)"),
                Component.empty(),
                ability("Arcane Bolt"),
                keybind("Right-Click"),
                desc("Fire an arcane particle bolt."),
                desc("0.5s shot cooldown."),
                Component.empty(),
                rarity("LEGENDARY", "ARCHMAGE'S WAND")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.ARCHMAGES_WAND.getId())
            .maxStackSize(1)
            .build();
    }

    // VERY RARE
    private ItemStack buildShadowblade() {
        return new ItemBuilder(Material.IRON_SWORD)
            .name(text("☠ Shadowblade", NamedTextColor.DARK_AQUA))
            .lore(List.of(
                stat("Normal Hit", "5.6"),
                stat("Critical Hit", "7"),
                Component.empty(),
                ability("Shadowstep"),
                keybind("Right-Click"),
                desc("Dash 5 blocks forward, damaging enemies in your path."),
                desc("Applies Bleeding: +1 HP damage per second for 7 seconds."),
                cooldown("10s"),
                Component.empty(),
                rarity("VERY RARE", "SHADOWBLADE")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.SHADOWBLADE.getId())
            .maxStackSize(1)
            .build();
    }

    // LEGENDARY
    private ItemStack buildAssassinsBlade() {
        return new ItemBuilder(Material.GOLDEN_SWORD)
            .name(text("☠ Assassin's Blade", NamedTextColor.GOLD))
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
                rarity("LEGENDARY", "ASSASSIN'S BLADE")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.ASSASSINS_BLADE.getId())
            .maxStackSize(1)
            .build();
    }

    // EPIC
    private ItemStack buildHarmonyWand() {
        return new ItemBuilder(Material.STICK)
            .name(text("Harmony Wand", NamedTextColor.DARK_PURPLE))
            .lore(List.of(
                ability("Harmony Bolt"),
                keybind("Right-Click"),
                desc("Fire a damage bolt — deals +4 HP × 3 pulses (1s apart)."),
                Component.empty(),
                keybind("Sneak + Right-Click"),
                desc("Fire a healing bolt — restores +4 HP × 3 pulses (1s apart)."),
                desc("Multiple bolts can stack on the same target."),
                Component.empty(),
                cooldownMulti("Attack: 1s", "Heal: 2s"),
                Component.empty(),
                rarity("EPIC", "HARMONY WAND")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.HARMONY_WAND.getId())
            .maxStackSize(1)
            .build();
    }

    // ─── Lore helpers ─────────────────────────────────────────────────────────

    /** White item name / rarity tag text. */
    private Component text(String s, NamedTextColor color) {
        return Component.text(s, color).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Stat line — yellow label, green value with +HP suffix.
     * Example: "⚡ Normal Hit: +7 HP"
     */
    private Component stat(String label, String value) {
        return Component.text("⚡ " + label + ": ", NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text("+" + value + " HP", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
    }

    /**
     * Ability header — "Ability:" in dark purple, name in gold, bold.
     * Example: "Ability: Reflective Guard"
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
     * Example: "Right-Click"  /  "Sneak + Right-Click"
     */
    private Component keybind(String bind) {
        return Component.text(bind, NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Description line — white, indented.
     * Example: "  Reflects 80% of incoming damage back to the attacker."
     */
    private Component desc(String s) {
        return Component.text("  " + s, NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Single cooldown line — green.
     * Example: "  Cooldown: 30s"
     */
    private Component cooldown(String s) {
        return Component.text("  Cooldown: ", NamedTextColor.GREEN)
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(s, NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
    }

    /**
     * Multi-cooldown line for weapons with multiple modes.
     * Example: "  Cooldown: Attack: 1s  |  Heal: 2s"
     */
    private Component cooldownMulti(String... entries) {
        return Component.text("  Cooldown: " + String.join("  |  ", entries), NamedTextColor.GREEN)
            .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Rarity footer tag — rarity color, all caps.
     * Rarity → color mapping:
     *   COMMON    → WHITE
     *   UNCOMMON  → GREEN
     *   RARE      → BLUE
     *   VERY RARE → DARK_AQUA
     *   EPIC      → DARK_PURPLE
     *   LEGENDARY → GOLD
     *
     * Example: rarity("LEGENDARY", "ARCHMAGE'S WAND") → "LEGENDARY ARCHMAGE'S WAND" in gold
     */
    private Component rarity(String rarityLabel, String weaponName) {
        NamedTextColor color = switch (rarityLabel) {
            case "UNCOMMON"  -> NamedTextColor.GREEN;
            case "RARE"      -> NamedTextColor.BLUE;
            case "VERY RARE" -> NamedTextColor.DARK_AQUA;
            case "EPIC"      -> NamedTextColor.DARK_PURPLE;
            case "LEGENDARY" -> NamedTextColor.GOLD;
            default          -> NamedTextColor.WHITE; // COMMON
        };
        return Component.text(rarityLabel + " " + weaponName, color)
            .decoration(TextDecoration.ITALIC, false)
            .decorate(TextDecoration.BOLD);
    }
}
