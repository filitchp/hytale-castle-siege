package dev.hytalemodding.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class WelcomeEvent {

    public static final String HAMMER_ID = "CastleDefense_WaveHammer";

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        player.sendMessage(Message.raw("Welcome " + player.getDisplayName()));

        Ref<EntityStore> ref = event.getPlayerRef();
        Store<EntityStore> store = ref.getStore();

        // Check the player's hotbar/storage/backpack for the hammer.
        CombinedItemContainer container = InventoryComponent.getCombined(
                store, ref, InventoryComponent.HOTBAR_STORAGE_BACKPACK);

        ItemStack hammer = new ItemStack(HAMMER_ID, 1);
        if (!container.containsItemStacksStackableWith(hammer)) {
            player.giveItem(hammer, ref, store);
            player.sendMessage(Message.raw("You received the Iron Hammer. Click it to open the Wave UI."));
        }
    }
}
