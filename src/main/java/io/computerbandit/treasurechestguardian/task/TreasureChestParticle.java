package io.computerbandit.treasurechestguardian.task;

import io.computerbandit.treasurechestguardian.TreasureChestGuardian;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class TreasureChestParticle extends BukkitRunnable {
    private TreasureChestGuardian plugin;

    public TreasureChestParticle(TreasureChestGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        Set<Location> chestsCopy = new HashSet<>(plugin.getTreasureChests());

        for (Location location : chestsCopy) {
            World world = location.getWorld();
            if (world == null) continue;

            Particle particle = Particle.VILLAGER_HAPPY;
            Chest chest = (Chest) location.getBlock().getState();

            if (isWaitingReplenishment(chest)) {
                particle = Particle.SMOKE_NORMAL;
            }
            spawnSpiral(world, location, particle);
        }
    }

    private boolean isWaitingReplenishment(Chest chest) {
        return chest.hasPendingRefill() || chest.getPersistentDataContainer().getOrDefault(TreasureChestGuardian.NEXT_REPLENISH_TIME_KEY, PersistentDataType.LONG, 0L) > System.currentTimeMillis();
    }

    private static final int STRANDS = 2;
    private static final int PARTICLES = 170 / 5;
    private static final float RADIUS = 1.5F;
    private static final float CURVE = 2.0F;
    private static final double ROTATION = 0.7853981633974483D;

    private void spawnSpiral(World world, Location location, Particle particle) {
        // Center above the chest
        for (int boost = 0; boost < 3; boost++) {
            for (int strand = 1; strand <= STRANDS; ++strand) {
                float progress = 3 / (float) PARTICLES;
                double point = CURVE * progress * 2.0f * Math.PI / STRANDS + 6.283185307179586 * strand / STRANDS + ROTATION;
                double addX = Math.cos(point) * progress * RADIUS;
                double addZ = Math.sin(point) * progress * RADIUS;
                double addY = 3.5D - 0.02 * 5 * 3;
                Location particleLocation = location.clone().add(addX, addY, addZ);
                world.spawnParticle(particle, particleLocation, 1, 0.1f, 0.1f, 0.1f, 0.0f);
            }
        }
    }
}

