package dev.hytalemodding.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.events.WelcomeEvent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

// Cancels any DropItemEvent.Drop carrying the iron hammer.
public class PreventHammerDropSystem extends EntityEventSystem<EntityStore, DropItemEvent.Drop> {

    public PreventHammerDropSystem() {
        super(DropItemEvent.Drop.class);
    }

    @Override
    public void handle(int index,
                       @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                       @NonNullDecl DropItemEvent.Drop event) {
        ItemStack stack = event.getItemStack();
        if (stack == null) return;

        if (WelcomeEvent.HAMMER_ID.equals(stack.getItemId())) {
            event.setCancelled(true);
        }
    }

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }
}
