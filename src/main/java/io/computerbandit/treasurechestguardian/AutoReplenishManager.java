package io.computerbandit.treasurechestguardian;

import com.destroystokyo.paper.loottable.LootableInventory;
import io.computerbandit.treasurechestguardian.event.TreasureChestReplenishedEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
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
    public void checkAndReplenish(LootableInventory lootableInventory) {
        if (lootableInventory instanceof Chest) {
            Chest chest = (Chest) lootableInventory;
            PersistentDataContainer container = chest.getPersistentDataContainer();

            long nextReplenishTime = container.getOrDefault(TreasureChestGuardian.NEXT_REPLENISH_TIME_KEY, PersistentDataType.LONG, 0L);
            long currentTime = System.currentTimeMillis();
            getPlugin().getLogger().info("checking nextReplenishTime: " + nextReplenishTime);
            if (currentTime > nextReplenishTime) {
                if (chest.getInventory().isEmpty()) {
                    container.set(TreasureChestGuardian.LAST_REPLENISH_TIME_KEY, PersistentDataType.LONG, currentTime);
                    long newNextReplenishTime = currentTime + (getRandomReplenishInterval() * 1000L);      // Convert seconds to milliseconds
                    container.set(TreasureChestGuardian.NEXT_REPLENISH_TIME_KEY, PersistentDataType.LONG, newNextReplenishTime);
                    chest.update();
                    boolean didReplenish = replenishTreasureChest(chest);
                    if (didReplenish) {
                        TreasureChestReplenishedEvent event = new TreasureChestReplenishedEvent(chest);
                        Bukkit.getServer().getPluginManager().callEvent(event);
                    }
                }
            }
        }
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

    private boolean replenishTreasureChest(LootableInventory lootableInventory) {
        if (lootableInventory != null) {
            if (lootableInventory instanceof Chest) {
                Chest chest = (Chest) lootableInventory;
                PersistentDataContainer container = chest.getPersistentDataContainer();
                String location = container.getOrDefault(TreasureChestGuardian.LOOT_TABLE_KEY, PersistentDataType.STRING, LootTables.EMPTY.name());
                NamespacedKey key = NamespacedKey.fromString(location);
                if (key != null) {
                    LootTable lootTable = Bukkit.getLootTable(key);
                    if (lootTable != null) {
                        LootContext.Builder lootContextBuilder = new LootContext.Builder(Objects.requireNonNull(chest.getLocation()));
                        Random randomReplenish;
                        if (plugin.isSeedResetOnReplenish()) {
                            randomReplenish = new Random(random.nextLong());
                        } else {
                            long seed = container.getOrDefault(TreasureChestGuardian.LOOT_TABLE_SEED_KEY, PersistentDataType.LONG, 0L);
                            if (seed == 0L) {
                                chest.setSeed(random.nextLong());
                                container.set(TreasureChestGuardian.LOOT_TABLE_SEED_KEY, PersistentDataType.LONG, chest.getSeed());

                            } else {
                                chest.setSeed(seed);
                            }
                            randomReplenish = new Random(chest.getSeed());
                        }
                        ArrayList<ItemStack> itemStacks = (ArrayList<ItemStack>) lootTable.populateLoot(randomReplenish, lootContextBuilder.build());
                        spreadItemsInInventory(chest.getInventory(), itemStacks);
                        BlockState state = chest.getBlock().getState();
                        state.update();
                        return true;
                    }
                }
            }
        }
        return false;
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

