package dev.hytalemodding.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.concurrent.CompletableFuture;

public class WelcomeEvent {

    public static final String HAMMER_ID = "CastleDefense_WaveHammer";

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        player.sendMessage(Message.raw("Welcome " + player.getDisplayName()));

        Ref<EntityStore> ref = event.getPlayerRef();
        Store<EntityStore> store = ref.getStore();

        // Defer teleport to the next tick so the game finishes restoring
        // the player's saved position before we override it.
        World world = player.getWorld();
        if (world != null) {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                ISpawnProvider spawnProvider = world.getWorldConfig().getSpawnProvider();
                Transform spawn = spawnProvider.getSpawnPoint(world, playerRef.getUuid());
                if (spawn != null) {
                    Vector3d pos = spawn.getPosition();
                    CompletableFuture.runAsync(
                            () -> player.moveTo(ref, pos.x, pos.y, pos.z, store), world);
                }
            }
        }

        // Clear all inventory sections before giving starter items.
        CombinedItemContainer everything = InventoryComponent.getCombined(
                store, ref, InventoryComponent.EVERYTHING);
        everything.clear();

        // Give starter items.
        player.giveItem(new ItemStack(HAMMER_ID, 1), ref, store);
        player.giveItem(new ItemStack("Weapon_Axe_Crude", 1), ref, store);

        player.sendMessage(Message.raw("You received the Iron Hammer and a Crude Axe. Click the hammer to open the Wave UI."));
    }
}
