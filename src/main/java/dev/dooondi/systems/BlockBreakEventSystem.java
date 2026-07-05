package dev.dooondi.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;

public class BlockBreakEventSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public BlockBreakEventSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index,
                       @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                       @NonNullDecl BreakBlockEvent event) {
        Ref<EntityStore> entityStoreRef = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(entityStoreRef, Player.getComponentType());
        if (player == null) return;

        if (player.getGameMode() == GameMode.Creative) {
            // Creative players bypass the restriction.
            return;
        }

        event.setCancelled(true);
        PlayerRef playerRef = store.getComponent(entityStoreRef, PlayerRef.getComponentType());
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw("You can't break this block in the minigame.")
                    .color(Color.RED).bold(true));
        }
    }

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }
}
