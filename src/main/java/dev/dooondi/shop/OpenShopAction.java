package dev.dooondi.shop;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;

/**
 * NPC role action {@code "Type": "OpenCastleSiegeShop", "Shop": "<id>"}: opens that shop's
 * page for the player who interacted with the NPC. Mirrors the vanilla OpenBarterShop action.
 */
public class OpenShopAction extends ActionBase {

    private final String shopId;

    public OpenShopAction(OpenShopActionBuilder builder) {
        super(builder);
        this.shopId = builder.getShopId();
    }

    @Override
    public boolean canExecute(Ref<EntityStore> ref, ExecutionSupport support, InfoProvider info,
                              double dt, Store<EntityStore> store) {
        return super.canExecute(ref, support, info, dt, store)
                && support.getStateSupport().getInteractionIterationTarget() != null;
    }

    @Override
    public boolean execute(Ref<EntityStore> ref, ExecutionSupport support, InfoProvider info,
                           double dt, Store<EntityStore> store) {
        super.execute(ref, support, info, dt, store);

        Ref<EntityStore> target = support.getStateSupport().getInteractionIterationTarget();
        if (target == null) return false;
        PlayerRef playerRef = store.getComponent(target, PlayerRef.getComponentType());
        Player player = store.getComponent(target, Player.getComponentType());
        if (playerRef == null || player == null) return false;

        player.getPageManager().openCustomPage(target, store, new ShopUI(playerRef, shopId));
        return true;
    }
}
