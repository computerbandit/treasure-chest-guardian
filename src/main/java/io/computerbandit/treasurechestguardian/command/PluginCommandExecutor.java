package io.computerbandit.treasurechestguardian.command;

import io.computerbandit.treasurechestguardian.TreasureChestGuardian;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PluginCommandExecutor implements CommandExecutor {

    private final TreasureChestGuardian plugin;

    public PluginCommandExecutor(TreasureChestGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("treasureChestGuardian") || label.equalsIgnoreCase("tcg")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("help")) {
                    showHelp(sender);
                } else if (args[0].equalsIgnoreCase("create")) {
                    boolean openFlag = Arrays.asList(args).contains("-o");
                    List<String> filteredArgs = removeFlagsFromArgs(args, "-o");
                    return handleCreateCommand(sender, filteredArgs, openFlag);
                }
            } else {
                sender.sendMessage("Usage: /" + label + " <subcommand> [args]");
            }
        }
        // ... other commands ...
        return false;
    }

    private List<String> removeFlagsFromArgs(String[] args, String... flagsToRemove) {
        List<String> flagsList = Arrays.asList(flagsToRemove);
        return Arrays.stream(args).filter(arg -> !flagsList.contains(arg)).collect(Collectors.toList());
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6TreasureChestGuardian Commands:");
        sender.sendMessage("§e/treasureChestGuardian create [lootTableName] [-o] - Create a chest. '-o' to open immediately, '[lootTableName]' for specific loot.");
        sender.sendMessage("§e/treasureChestGuardian help - Show this help message.");
        // Add more command descriptions as needed
    }

    private boolean handleCreateCommand(CommandSender sender, List<String> args, boolean openFlag) {

        try {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (!player.hasPermission("treasureChestGuardian.command.create")) {
                    player.sendMessage("You do not have permission to use this command.");
                    return true;
                }

                Block block = player.getWorld().getBlockAt(player.getLocation());
                block.setType(Material.CHEST); // Set the block to a chest

                Chest chest = (Chest) block.getState();

                PersistentDataContainer container = chest.getPersistentDataContainer();

                LootTable lootTable;

                // Check for specified loot table
                if (!args.isEmpty()) {
                    NamespacedKey key = NamespacedKey.minecraft(args.get(1).toLowerCase());
                    lootTable = Bukkit.getLootTable(key);
                    if (lootTable == null) {
                        player.sendMessage("Invalid loot table name.");
                        return true;
                    }
                } else {
                    lootTable = Bukkit.getLootTable(LootTables.SIMPLE_DUNGEON.getKey()); // Default loot table
                }

                if (lootTable != null) {
                    chest.setLootTable(lootTable);
                    container.set(TreasureChestGuardian.LOOT_TABLE_KEY, PersistentDataType.STRING, lootTable.getKey().toString());
                    chest.update(); // Apply changes
                    player.sendMessage("Treasure chest create with lootTable: " + lootTable.getKey());
                }
                if (openFlag) {
                    player.openInventory(chest.getInventory()); // Open the chest inventory for the player
                }

                return true;
            }

        } catch (Exception e) {
            plugin.getLogger().severe(e.getMessage());
        }
        return false;

    }
}

