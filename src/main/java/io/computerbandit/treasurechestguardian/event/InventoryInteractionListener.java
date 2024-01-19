package io.computerbandit.treasurechestguardian.event;

import com.destroystokyo.paper.loottable.LootableInventory;
import io.computerbandit.treasurechestguardian.AutoReplenishManager;
import io.computerbandit.treasurechestguardian.TreasureChestGuardian;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class InventoryInteractionListener implements Listener {

    private final AutoReplenishManager autoReplenishManager;

    private final TreasureChestGuardian plugin;

    public InventoryInteractionListener(AutoReplenishManager autoReplenishManager) {
        this.autoReplenishManager = autoReplenishManager;
        plugin = autoReplenishManager.getPlugin();
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Chest) {
            Chest chest = (Chest) holder;
            if (autoReplenishManager.isPaperAutoReplenishDisabled(chest) && plugin.isAutoReplenishFallbackEnabled()) {
                if (isTreasureChest(chest)) {
                    autoReplenishManager.checkAndReplenish(chest);
                }
            }
        }
    }

    //TODO create event for when a minecraft with a chest is destroyed
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (!(block.getState() instanceof Chest)) return;

        Chest chest = (Chest) block.getState();
        if (isTreasureChest(chest)) {
            if (plugin.enforceBreakPermission() && !player.hasPermission("treasureChestGuardian.break")) {
                if (plugin.isNoPermMessageEnabled()) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getNoPermMessage()));
                }
                event.setCancelled(true);
                return;
            }
            if (!player.isSneaking()) {
                if (plugin.isWarningMessageEnabled()) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getWarningMessage()));
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        if (event.getInventoryHolder() instanceof Chest) {
            Chest chest = (Chest) event.getInventoryHolder();
            PersistentDataContainer container = chest.getPersistentDataContainer();
            container.set(TreasureChestGuardian.LOOT_TABLE_KEY, PersistentDataType.STRING, event.getLootTable().getKey().toString());
            chest.update();
        }
    }

    @EventHandler
    public void onExplosion(BlockExplodeEvent event) {
        preventAgainstExplosion(event.blockList());
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        preventAgainstExplosion(event.blockList());
    }

    private void preventAgainstExplosion(List<Block> blocksToBeExploded) {
        blocksToBeExploded.removeIf(block -> block.getState() instanceof Chest && isTreasureChest((Chest) block.getState()));
        blocksToBeExploded.removeIf(block -> block.getState() instanceof StorageMinecart && isTreasureChest((StorageMinecart) block.getState()));
    }

    private boolean isTreasureChest(LootableInventory lootableInventory) {
        if (lootableInventory.hasLootTable() || lootableInventory.hasBeenFilled()) return true;
        if (plugin.isAutoReplenishFallbackEnabled()) {
            if (lootableInventory instanceof Chest) {
                return ((Chest) lootableInventory).getPersistentDataContainer().has(TreasureChestGuardian.NEXT_REPLENISH_TIME_KEY);
            }
        } else {
            return lootableInventory.hasLootTable() || lootableInventory.hasPendingRefill();
        }
        return false;
    }
}



