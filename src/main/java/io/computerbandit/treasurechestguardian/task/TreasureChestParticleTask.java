package io.computerbandit.treasurechestguardian.task;

import com.destroystokyo.paper.ParticleBuilder;
import io.computerbandit.treasurechestguardian.TreasureChestGuardian;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TreasureChestParticleTask extends BukkitRunnable {
    private final TreasureChestGuardian plugin;

    public TreasureChestParticleTask(TreasureChestGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            // Skip players in spectator mode or creative mode
            if (player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }

            Block block = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);
            if (block != null && block.getState() instanceof Chest) {
                Chest chest = (Chest) block.getState();
                if (plugin.isTreasureChest(chest)) {
                    boolean willReplenished = willChestBeReplenishedForPlayer(chest, player);
                    Location location = chest.getLocation();
                    spawnSpiral(location, willReplenished, player);
                }
            }
        }
    }

    /**
     * Checks if a chest will be replenished for a given player.
     *
     * @param chest  The chest to check.
     * @param player The player to check for.
     * @return true if the chest will be replenished when opened by the player, false if waiting for replenishment.
     */
    public static boolean willChestBeReplenishedForPlayer(Chest chest, Player player) {
        if (chest == null) {
            return false; // Not a lootable inventory, so it can't be replenished.
        }

        // Check if the chest is enabled to auto-refill and if the player can loot this chest
        if (!chest.isRefillEnabled() || !chest.canPlayerLoot(player.getUniqueId())) {
            return false; // Replenishment is not enabled or the player can't loot this chest anymore.
        }

        // Check if the chest has a pending refill or if it's already been looted by this player
        long currentTime = System.currentTimeMillis();
        boolean isReplenishPending = chest.hasPendingRefill() && chest.getNextRefill() <= currentTime;
        boolean hasPlayerLooted = chest.hasPlayerLooted(player);

        return isReplenishPending && !hasPlayerLooted;
    }

    private void spawnSpiral(Location location, boolean willReplenished, Player playerLooking) {
        Color color = willReplenished ? Color.YELLOW : Color.RED;
        Location particleLocation = location.clone().add(0.5, 0, 0.5);// Center above the chest
        // Height of the helix
        int height = 1;
        // How many times it spirals
        double revolutions = 2.5;
        double yIncrement = height / (revolutions * Math.PI * 2);

        for (double t = 0; t < Math.PI * 2 * revolutions; t += Math.PI / 8) {
            // Radius of the helix
            double radius = 0.75;
            double x = radius * Math.cos(t);
            double z = radius * Math.sin(t);
            double y = yIncrement * t;

            new ParticleBuilder(Particle.REDSTONE)
                    .location(particleLocation.clone().add(x, y, z))
                    .color(color, 1.3f)
                    .receivers(playerLooking) // Send to all players in the world
                    .spawn();

        }
    }
}

