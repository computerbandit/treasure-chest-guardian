package io.computerbandit.treasurechestguardian;

import com.destroystokyo.paper.loottable.LootableInventory;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class AutoReplenishManager {

    private final TreasureChestGuardian plugin;
    private final Random random;

    public AutoReplenishManager(TreasureChestGuardian plugin) {
        this.plugin = plugin;
        random = new Random();
    }

    public TreasureChestGuardian getPlugin() {
        return plugin;
    }

    //this is only called if the fallback is enabled and the paper version is disabled
    public void checkAndReplenish(Inventory inventory, LootableInventory lootableInventory) {

        PersistentDataContainer dataContainer = getDataContainer(inventory.getHolder());
        if (dataContainer == null) {
            // Handle error: inventory holder does not support persistent data
            return;
        }

        long nextReplenishTime = dataContainer.getOrDefault(TreasureChestGuardian.NEXT_REPLENISH_TIME_KEY, PersistentDataType.LONG, 0L);
        long currentTime = System.currentTimeMillis();
        getPlugin().getLogger().info("nextReplenishTime: " + nextReplenishTime);
        if (currentTime > nextReplenishTime) {
            if (inventory.isEmpty()) {
                replenishTreasureChest(inventory, lootableInventory);
                dataContainer.set(TreasureChestGuardian.LAST_REPLENISH_TIME_KEY, PersistentDataType.LONG, currentTime);
                long interval = getRandomReplenishInterval() * 1000L;      // Convert seconds to milliseconds
                dataContainer.set(TreasureChestGuardian.NEXT_REPLENISH_TIME_KEY, PersistentDataType.LONG, currentTime + interval);

            }
        }

    }


    public PersistentDataContainer getDataContainer(InventoryHolder holder) {
        if (holder instanceof Chest) {
            return ((Chest) holder).getPersistentDataContainer();
        } else if (holder instanceof StorageMinecart) {
            return ((StorageMinecart) holder).getPersistentDataContainer();
        }
        // Return null or throw an exception if the holder is neither a BlockState nor an Entity
        return null;
    }

    private int getRandomReplenishInterval() {
        int minSeconds = plugin.getReplenishIntervalMinSeconds();
        int maxSeconds = plugin.getReplenishIntervalMaxSeconds();

        if (minSeconds >= maxSeconds) {
            return minSeconds;
        }
        return minSeconds + random.nextInt(maxSeconds - minSeconds);
    }


    public boolean isPaperAutoReplenishDisabled(LootableInventory lootableInventory) {
        return lootableInventory == null || !lootableInventory.isRefillEnabled();
    }

    private void replenishTreasureChest(Inventory inventory, LootableInventory lootableInventory) {
        if (lootableInventory != null && inventory != null) {
            if (inventory.getHolder() instanceof Chest) {
                Chest chest = (Chest) inventory.getHolder();
                String location = chest.getPersistentDataContainer().getOrDefault(TreasureChestGuardian.LOOT_TABLE_KEY, PersistentDataType.STRING, LootTables.EMPTY.name());
                NamespacedKey key = NamespacedKey.fromString(location);
                if (key != null) {
                    LootTable lootTable = Bukkit.getLootTable(key);
                    if (lootTable != null) {
                        LootContext.Builder lootContextBuilder = new LootContext.Builder(Objects.requireNonNull(inventory.getLocation()));
                        ArrayList<ItemStack> itemStacks = (ArrayList<ItemStack>) lootTable.populateLoot(plugin.isSeedResetOnReplenish() ? new Random(random.nextLong()) : null, lootContextBuilder.build());
                        spreadItemsInInventory(inventory, itemStacks);
                        BlockState state = chest.getBlock().getState();
                        state.update();
                    }
                }
            }
        }
    }

    public void spreadItemsInInventory(Inventory inventory, ArrayList<ItemStack> items) {

        List<ItemStack> mergedItems = mergeCommonItems(items);
        sortItemsByQuantity(mergedItems);

        List<ItemStack> individualItems = new ArrayList<>();

        // Flatten the list: Break down the items into individual units
        for (ItemStack item : mergedItems) {
            for (int i = 0; i < item.getAmount(); i++) {
                ItemStack singleItem = item.clone();
                singleItem.setAmount(1);
                individualItems.add(singleItem);
            }
        }
        // Distribute each item into a random slot
        for (ItemStack singleItem : individualItems) {
            int slot;
            ItemStack slotItem;
            int maxTries = inventory.getSize();
            int i = 0;
            boolean cancelInsert = false;
            do {
                slot = random.nextInt(inventory.getSize());
                slotItem = inventory.getItem(slot);
                i++;
                if (i >= maxTries) {
                    cancelInsert = true;
                    break;
                }
            } while (slotItem != null && (!singleItem.isSimilar(slotItem) || slotItem.getAmount() + 1 > slotItem.getMaxStackSize()));

            if (cancelInsert) continue;

            if (slotItem == null) {
                slotItem = singleItem.clone();
            } else {
                slotItem.add(1);
            }

            inventory.setItem(slot, slotItem);

        }
    }

    private List<ItemStack> mergeCommonItems(List<ItemStack> items) {
        Map<String, ItemStack> mergedItems = new HashMap<>();

        for (ItemStack item : items) {
            String key = item.getType().toString();

            // Consider metadata/enchantments in the key if necessary
            if (item.hasItemMeta()) {
                key += item.getItemMeta().toString();
            }

            ItemStack existing = mergedItems.get(key);
            if (existing == null) {
                mergedItems.put(key, item.clone());
            } else {
                int combinedAmount = existing.getAmount() + item.getAmount();
                int maxStackSize = existing.getMaxStackSize();

                while (combinedAmount > maxStackSize) {
                    // Split into multiple stacks if exceeds max stack size
                    ItemStack newStack = existing.clone();
                    newStack.setAmount(maxStackSize);
                    mergedItems.put(key + "#" + combinedAmount, newStack);
                    combinedAmount -= maxStackSize;
                }

                existing.setAmount(combinedAmount);
            }
        }

        return new ArrayList<>(mergedItems.values());
    }

    public void sortItemsByQuantity(List<ItemStack> items) {
        items.sort(Comparator.comparingInt(ItemStack::getAmount));
    }
}

