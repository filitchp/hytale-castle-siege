package dev.dooondi.wave;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.dooondi.ui.WaveUI;

public class OpenWaveUIInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<OpenWaveUIInteraction> CODEC =
            BuilderCodec.builder(OpenWaveUIInteraction.class, OpenWaveUIInteraction::new,
                    SimpleInstantInteraction.CODEC).build();

    public OpenWaveUIInteraction() {
        super();
    }

    @Override
    protected void firstRun(InteractionType type, InteractionContext context,
                            CooldownHandler cooldownHandler) {
        Player player = context.getCommandBuffer()
                .getComponent(context.getEntity(), Player.getComponentType());
        if (player == null) return;

        PlayerRef playerRef = context.getCommandBuffer()
                .getComponent(context.getEntity(), PlayerRef.getComponentType());
        if (playerRef == null) return;

        // Defer store-touching work until the store is no longer processing.
        context.getCommandBuffer().run(store ->
                player.getPageManager().openCustomPage(context.getEntity(), store, new WaveUI(playerRef))
        );
    }
}
