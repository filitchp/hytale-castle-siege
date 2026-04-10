package dev.hytalemodding.events;

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;

public class InputListener {

    public static void onPlayerChat(PlayerChatEvent event) {
        if (event.getContent().contains("badword")) {
            event.setCancelled(true);  // Message won't be sent
        }
    }
}
