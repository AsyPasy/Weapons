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

    // ─── MID-GAME ────────────────────────────────────────────────────────────

    private ItemStack buildGreatsword() {
        return new ItemBuilder(Material.NETHERITE_SWORD)
            .name(text("⚔ Greatsword", NamedTextColor.RED))
            .lore(List.of(
                gray("Mid-Game Weapon"),
                Component.empty(),
                stat("Normal Hit", "7"),
                stat("Critical Hit", "10"),
                Component.empty(),
                abilityTitle("Reflective Guard"),
                hint("Right-Click to activate a 5s shield."),
                hint("Reflects 80% of incoming damage"),
                hint("back to the attacker."),
                cooldown("30 seconds")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.GREATSWORD.getId())
            .maxStackSize(1)
            .build();
    }

    // ─── LATE-GAME ────────────────────────────────────────────────────────────

    private ItemStack buildDominicanAxe() {
        return new ItemBuilder(Material.NETHERITE_AXE)
            .name(text("⚒ Dominican Axe", NamedTextColor.DARK_RED))
            .lore(List.of(
                gray("Late-Game Weapon"),
                Component.empty(),
                stat("Normal Hit", "10"),
                stat("Critical Hit", "13"),
                Component.empty(),
                abilityTitle("Ground Smash"),
                hint("Right-Click to slam the ground."),
                hint("Pushes all nearby enemies away"),
                hint("and deals 20 damage in the area."),
                cooldown("10 seconds")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.DOMINICAN_AXE.getId())
            .maxStackSize(1)
            .build();
    }

    private ItemStack buildArcanistStaff() {
        return new ItemBuilder(Material.BLAZE_ROD)
            .name(text("✦ Arcanist Staff", NamedTextColor.AQUA))
            .lore(List.of(
                gray("Late-Game Weapon  |  Wand"),
                Component.empty(),
                stat("Particle Shot Damage", "4"),
                Component.empty(),
                abilityTitle("Charged Shot"),
                hint("Sneak + Right-Click to charge."),
                hint("Your next particle shot deals 25 damage."),
                cooldown("30 seconds"),
                Component.empty(),
                hint("Right-Click to fire a flame particle bolt."),
                hint("0.5s shot cooldown.")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.ARCANIST_STAFF.getId())
            .maxStackSize(1)
            .build();
    }

    private ItemStack buildArchmagesWand() {
        return new ItemBuilder(Material.END_ROD)
            .name(text("✦ Archmage's Wand", NamedTextColor.LIGHT_PURPLE))
            .lore(List.of(
                gray("Late-Game Weapon  |  Wand"),
                Component.empty(),
                stat("Particle Shot Damage", "8"),
                Component.empty(),
                abilityTitle("Arcane Burst"),
                hint("Sneak + Right-Click to release arcane energy."),
                hint("Knocks back ALL nearby enemies."),
                hint("Heals you for 10% of max health."),
                hint("3 charges before entering cooldown."),
                cooldown("90 seconds (after 3 uses)"),
                Component.empty(),
                hint("Right-Click to fire an arcane particle bolt."),
                hint("0.5s shot cooldown.")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.ARCHMAGES_WAND.getId())
            .maxStackSize(1)
            .build();
    }

    private ItemStack buildShadowblade() {
        return new ItemBuilder(Material.IRON_SWORD)
            .name(text("☠ Shadowblade", NamedTextColor.DARK_PURPLE))
            .lore(List.of(
                gray("Mid-Game Weapon"),
                Component.empty(),
                stat("Normal Hit", "5.6"),
                stat("Critical Hit", "7"),
                Component.empty(),
                abilityTitle("Shadowstep"),
                hint("Right-Click to dash 5 blocks forward."),
                hint("Damages enemies in your path."),
                hint("Applies Bleeding: 1 dmg/sec for 10s."),
                cooldown("10 seconds")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.SHADOWBLADE.getId())
            .maxStackSize(1)
            .build();
    }

    private ItemStack buildAssassinsBlade() {
        return new ItemBuilder(Material.GOLDEN_SWORD)
            .name(text("☠ Assassin's Blade", NamedTextColor.GOLD))
            .lore(List.of(
                gray("Late-Game Weapon"),
                Component.empty(),
                stat("Normal Hit", "7.2"),
                stat("Critical Hit", "9"),
                Component.empty(),
                abilityTitle("The Shadow"),
                hint("Right-Click to vanish for 5 seconds."),
                hint("Grants Speed II. Enemies cannot"),
                hint("see or attack you."),
                hint("Natural healing disabled (potions work)."),
                cooldown("10 seconds")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.ASSASSINS_BLADE.getId())
            .maxStackSize(1)
            .build();
    }

    // ─── SUPPORT ──────────────────────────────────────────────────────────────

    private ItemStack buildHarmonyWand() {
        return new ItemBuilder(Material.STICK)
            .name(text("Harmony Wand", NamedTextColor.GREEN))
            .lore(List.of(
                gray("Support Weapon  |  Wand"),
                Component.empty(),
                abilityTitle("Harmony Bolt"),
                Component.empty(),
                Component.text(" ▸ Right-Click: ", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Damage bolt — 4 HP × 3 pulses (1s apart)", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)),
                Component.text(" ▸ Sneak + Right-Click: ", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Healing bolt — 4 HP × 3 pulses (1s apart)", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)),
                Component.empty(),
                hint("Can stack multiple bolts on the same target."),
                hint("Attack: 1s cooldown  |  Heal: 2s cooldown.")
            ))
            .enchant(Enchantment.UNBREAKING, 1)
            .flag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
            .unbreakable(true)
            .pdc(plugin.getItemKeys().WEAPON_ID, WeaponType.HARMONY_WAND.getId())
            .maxStackSize(1)
            .build();
    }

    // ─── Lore helpers ─────────────────────────────────────────────────────────

    private Component text(String s, NamedTextColor color) {
        return Component.text(s, color).decoration(TextDecoration.ITALIC, false);
    }
    private Component gray(String s) {
        return Component.text(s, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
    }
    private Component stat(String label, String value) {
        return Component.text("⚡ " + label + ": ", NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(value, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
    }
    private Component abilityTitle(String name) {
        return Component.text("✦ " + name, NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false).decorate(TextDecoration.BOLD);
    }
    private Component hint(String s) {
        return Component.text("  " + s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }
    private Component cooldown(String s) {
        return Component.text("  ⏱ Cooldown: ", NamedTextColor.DARK_AQUA)
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(s, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
    }
}
