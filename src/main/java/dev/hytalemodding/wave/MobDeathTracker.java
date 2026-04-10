package dev.hytalemodding.wave;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class MobDeathTracker extends DeathSystems.OnDeathSystem {

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void onComponentAdded(Ref<EntityStore> ref, DeathComponent deathComponent,
                                 Store<EntityStore> store,
                                 CommandBuffer<EntityStore> commandBuffer) {
        // Only count NPC deaths, not player deaths
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc != null) {
            WaveManager.incrementKills();
        }
    }

    @Override
    public void onComponentSet(Ref<EntityStore> ref, DeathComponent deathComponent,
                               DeathComponent old, Store<EntityStore> store,
                               CommandBuffer<EntityStore> commandBuffer) {
        // No action needed on component update
    }

    @Override
    public void onComponentRemoved(Ref<EntityStore> ref, DeathComponent deathComponent,
                                   Store<EntityStore> store,
                                   CommandBuffer<EntityStore> commandBuffer) {
        // No action needed on respawn
    }
}
