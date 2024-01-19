package io.computerbandit.treasurechestguardian.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.loot.LootTables;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PluginTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("treasureChestGuardian") || command.getAliases().contains("tcg")) {
            if (args.length == 1) {
                return List.of("create", "help");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
                return getAllLootTableNames(); // Completion for loot table names in 'create' subcommand
            } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
                return List.of("-o");
            }
        }
        return new ArrayList<>();
    }

    private List<String> getAllLootTableNames() {
        // Here, add all your loot table names or retrieve them dynamically
        return Arrays.stream(LootTables.values()).map(lootTable -> lootTable.getKey().getKey()).collect(Collectors.toList());
    }
}
