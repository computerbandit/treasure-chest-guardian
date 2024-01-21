package io.computerbandit.treasurechestguardian.event;

import com.destroystokyo.paper.loottable.LootableInventory;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.NotNull;

public class TreasureChestReplenishedEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final LootableInventory lootableInventory;
    private final LootTable lootTable;

    public TreasureChestReplenishedEvent(LootableInventory lootableInventory) {
        this.lootableInventory = lootableInventory;
        this.lootTable = lootableInventory.getLootTable();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public LootableInventory getLootableInventory() {
        return lootableInventory;
    }

    public LootTable getLootTable() {
        return lootTable;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

}