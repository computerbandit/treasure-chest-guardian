package io.computerbandit.treasurechestguardian;

import io.computerbandit.treasurechestguardian.command.PluginCommandExecutor;
import io.computerbandit.treasurechestguardian.command.PluginTabCompleter;
import io.computerbandit.treasurechestguardian.event.InventoryInteractionListener;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class TreasureChestGuardian extends JavaPlugin implements Listener {

    public static NamespacedKey LAST_REPLENISH_TIME_KEY;
    public static NamespacedKey NEXT_REPLENISH_TIME_KEY;
    public static NamespacedKey LOOT_TABLE_KEY;
    public static NamespacedKey LOOT_TABLE_SEED_KEY;

    private static final int CONFIG_VERSION = 1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        updateConfigIfNeeded();
        getServer().getPluginManager().registerEvents(new InventoryInteractionListener(new AutoReplenishManager(this)), this);

        //these are only used when the paper auto-rep is disabled and the fallback is enabled
        LAST_REPLENISH_TIME_KEY = new NamespacedKey(this, "last_replenish_time");
        NEXT_REPLENISH_TIME_KEY = new NamespacedKey(this, "next_replenish_time");
        LOOT_TABLE_KEY = new NamespacedKey(this, "loot_table_key");
        LOOT_TABLE_SEED_KEY = new NamespacedKey(this, "loot_table_seed_key");

        // Register the command and TabCompleter
        PluginCommand tcgCommand = this.getCommand("treasureChestGuardian");
        PluginCommandExecutor commandExecutor = new PluginCommandExecutor(this);
        PluginTabCompleter tabCompleter = new PluginTabCompleter();

        if (tcgCommand != null) {
            tcgCommand.setExecutor(commandExecutor);
            tcgCommand.setTabCompleter(tabCompleter);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void updateConfigIfNeeded() {
        int currentVersion = getConfig().getInt("config-version", 0);
        int latestVersion = CONFIG_VERSION; // Update this each time you change the config structure

        if (currentVersion < latestVersion) {
            // Update the config
            if (currentVersion < 1) {
                getConfig().addDefault("General.enable-fallback-auto-replenish", true);
                getConfig().addDefault("General.reset-seed-on-replenish", true);
                getConfig().addDefault("General.replenish-interval.min-seconds", 300);
                getConfig().addDefault("General.replenish-interval.max-seconds", 600);
            }

            // After all updates
            getConfig().set("config-version", latestVersion);
            getConfig().options().copyDefaults(true);
            saveConfig();
        }
    }

    public boolean isWarningMessageEnabled() {
        return getConfig().getBoolean("Notification.enable-warning-message", true);
    }

    public String getWarningMessage() {
        return getConfig().getString("Notification.warning-message", "<red>This is a treasure chest! (sneak to break)");
    }

    public boolean isNoPermMessageEnabled() {
        return getConfig().getBoolean("Notification.enable-no-perm-message", true);
    }

    public String getNoPermMessage() {
        return getConfig().getString("Notification.no-perm-message", "<red>You do NOT have permission to break this treasure chest!");
    }

    public boolean enforceBreakPermission() {
        return getConfig().getBoolean("Permission.enforce-break-permission", true);
    }

    public boolean isAutoReplenishFallbackEnabled() {
        return getConfig().getBoolean("General.enable-fallback-auto-replenish", true);
    }

    public int getReplenishIntervalMinSeconds() {
        return getConfig().getInt("General.replenish-interval.min-seconds", 300);
    }

    public int getReplenishIntervalMaxSeconds() {
        return getConfig().getInt("General.replenish-interval.max-seconds", 600);
    }

    public boolean isSeedResetOnReplenish() {
        return getConfig().getBoolean("General.reset-seed-on-replenish", true);
    }


}
