package io.computerbandit.treasurechestguardian.listener;

import com.destroystokyo.paper.loottable.LootableInventory;
import io.computerbandit.treasurechestguardian.AutoReplenishManager;
import io.computerbandit.treasurechestguardian.TreasureChestGuardian;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class TreasureChestListener implements Listener {

    private final AutoReplenishManager autoReplenishManager;

    private final TreasureChestGuardian plugin;

    public TreasureChestListener(AutoReplenishManager autoReplenishManager) {
        this.autoReplenishManager = autoReplenishManager;
        plugin = autoReplenishManager.getPlugin();
    }

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
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();

        // Check if a chest is being placed
        if (placedBlock.getType() == Material.CHEST || placedBlock.getType() == Material.TRAPPED_CHEST) {
            // Check adjacent blocks for treasure chests
            for (BlockFace face : BlockFace.values()) {
                Block adjacentBlock = placedBlock.getRelative(face);

                if (adjacentBlock.getType() == Material.CHEST || adjacentBlock.getType() == Material.TRAPPED_CHEST) {
                    Chest adjacentChest = (Chest) adjacentBlock.getState();

                    if (isTreasureChest(adjacentChest)) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage("You cannot connect a chest to a treasure chest.");
                        break;
                    }
                }
            }
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

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        if (event.getInventoryHolder() instanceof Chest) {
            Chest chest = (Chest) event.getInventoryHolder();
            PersistentDataContainer container = chest.getPersistentDataContainer();
            container.set(TreasureChestGuardian.LOOT_TABLE_KEY, PersistentDataType.STRING, event.getLootTable().getKey().toString());
            container.set(TreasureChestGuardian.IS_TREASURE_CHEST_KEY, PersistentDataType.BOOLEAN, true);
            chest.update();
        }
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

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        InventoryHolder destinationHolder = event.getDestination().getHolder();

        if (destinationHolder instanceof Chest) {
            Chest chest = (Chest) destinationHolder;
            if (isTreasureChest(chest)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Chest && isTreasureChest((Chest) holder)) {
            // Check if any of the raw slots being dragged over are part of the chest inventory
            boolean dragsIntoChest = event.getRawSlots().stream()
                    .anyMatch(slot -> slot < event.getInventory().getSize());

            if (dragsIntoChest) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    event.getWhoClicked().sendMessage("You cannot add items to this treasure chest.");
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof Chest && isTreasureChest((Chest) holder)) {
            // Block actions that would place items into the chest
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                // Check if the items are being moved from the player's inventory to the chest
                if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getWhoClicked().getInventory())) {
                    event.setCancelled(true);
                    event.getWhoClicked().sendMessage("You cannot add items to this treasure chest.");
                    return;
                }
            }

            // Check other actions that would place items into the chest
            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof Chest) {
                switch (event.getAction()) {
                    case PLACE_ALL:
                    case PLACE_ONE:
                    case PLACE_SOME:
                    case SWAP_WITH_CURSOR:
                        event.setCancelled(true);
                        event.getWhoClicked().sendMessage("You cannot add items to this treasure chest.");
                        break;
                    // Allow other actions, including taking items out
                }
            }
        }
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



