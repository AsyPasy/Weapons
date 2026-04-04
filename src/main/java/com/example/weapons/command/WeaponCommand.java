package com.example.weapons.command;

import com.example.weapons.WeaponsPlugin;
import com.example.weapons.items.WeaponFactory;
import com.example.weapons.items.WeaponType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command: /weapon
 *
 *  /weapon give <weapon_id> [player]   — gives the specified weapon
 *  /weapon list                         — lists all available weapon IDs
 *
 * Requires permission: weapons.give
 */
public final class WeaponCommand implements CommandExecutor, TabCompleter {

    private final WeaponFactory factory;

    public WeaponCommand(WeaponsPlugin plugin) {
        this.factory = new WeaponFactory(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!sender.hasPermission("weapons.give")) {
            sender.sendMessage(Component.text("You don't have permission for this.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "list" -> {
                sender.sendMessage(Component.text("── Custom Weapons ──", NamedTextColor.GOLD));
                for (WeaponType type : WeaponType.values()) {
                    sender.sendMessage(Component.text("  • " + type.getId(), NamedTextColor.YELLOW));
                }
            }

            case "give" -> {
                if (args.length < 2) {
                    sendUsage(sender);
                    return true;
                }

                // Resolve weapon type
                WeaponType type = WeaponType.fromId(args[1].toLowerCase());
                if (type == null) {
                    sender.sendMessage(Component.text(
                        "Unknown weapon '" + args[1] + "'. Use /weapon list.", NamedTextColor.RED));
                    return true;
                }

                // Resolve target player
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayerExact(args[2]);
                    if (target == null) {
                        sender.sendMessage(Component.text(
                            "Player '" + args[2] + "' not found.", NamedTextColor.RED));
                        return true;
                    }
                } else if (sender instanceof Player p) {
                    target = p;
                } else {
                    sender.sendMessage(Component.text(
                        "Console must specify a player: /weapon give <weapon> <player>", NamedTextColor.RED));
                    return true;
                }

                // Give the item
                ItemStack item = factory.create(type);
                target.getInventory().addItem(item);
                target.sendMessage(Component.text(
                    "You received: ", NamedTextColor.GREEN)
                    .append(Component.text(type.getId(), NamedTextColor.YELLOW)));

                if (!target.equals(sender)) {
                    sender.sendMessage(Component.text(
                        "Gave " + type.getId() + " to " + target.getName(), NamedTextColor.GREEN));
                }
            }

            default -> sendUsage(sender);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String label,
                                      @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("give", "list");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Arrays.stream(WeaponType.values())
                .map(WeaponType::getId)
                .filter(id -> id.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }
        return List.of();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /weapon give <id> [player]", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  /weapon list", NamedTextColor.WHITE));
    }
}
