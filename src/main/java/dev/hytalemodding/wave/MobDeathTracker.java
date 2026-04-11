package dev.hytalemodding.wave;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.UUID;

public class MobDeathTracker extends DeathSystems.OnDeathSystem {

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void onComponentAdded(Ref<EntityStore> ref, DeathComponent deathComponent,
                                 Store<EntityStore> store,
                                 CommandBuffer<EntityStore> commandBuffer) {
        // Only count NPC deaths, not player deaths.
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }

        UUID killerUuid = resolveKillerUuid(deathComponent, store);
        WaveManager.recordMobDeath(ref, killerUuid, store);
    }

    private static UUID resolveKillerUuid(DeathComponent deathComponent, Store<EntityStore> store) {
        Damage deathInfo = deathComponent.getDeathInfo();
        if (deathInfo == null) return null;

        Damage.Source source = deathInfo.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) return null;

        Ref<EntityStore> killerRef = entitySource.getRef();
        if (killerRef == null) return null;

        PlayerRef killerPlayerRef = store.getComponent(killerRef, PlayerRef.getComponentType());
        if (killerPlayerRef == null) return null;

        return killerPlayerRef.getUuid();
    }

    @Override
    public void onComponentSet(Ref<EntityStore> ref, DeathComponent deathComponent,
                               DeathComponent old, Store<EntityStore> store,
                               CommandBuffer<EntityStore> commandBuffer) {
    }

    @Override
    public void onComponentRemoved(Ref<EntityStore> ref, DeathComponent deathComponent,
                                   Store<EntityStore> store,
                                   CommandBuffer<EntityStore> commandBuffer) {
    }
}
