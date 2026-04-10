package dev.hytalemodding.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;

import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.ui.WaveUI;

public class InputListener {

    public static void onInteract(PlayerInteractEvent event) {
        if (event.getActionType() != InteractionType.Ability2) {
            return;
        }

        Player player = event.getPlayer();
        Ref<EntityStore> ref = event.getPlayerRef();
        Store<EntityStore> store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (playerRef == null) {
            return;
        }

        player.getPageManager().openCustomPage(ref, store,
                new WaveUI(playerRef));

        event.setCancelled(true);
    }

    public static void onClick(PlayerMouseButtonEvent event) {

        event.setCancelled(true);
    }

    public static void onPlayerChat(PlayerChatEvent event) {
        if (event.getContent().contains("badword")) {
            event.setCancelled(true);  // Message won't be sent
        }
    }
}
