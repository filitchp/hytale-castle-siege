package dev.dooondi.wave;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

public class TriggerWaveInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<TriggerWaveInteraction> CODEC =
            BuilderCodec.builder(TriggerWaveInteraction.class, TriggerWaveInteraction::new,
                    SimpleInstantInteraction.CODEC).build();

    public TriggerWaveInteraction() {
        super();
    }

    @Override
    protected void firstRun(InteractionType type, InteractionContext context,
                            CooldownHandler cooldownHandler) {
        Player player = context.getCommandBuffer()
                .getComponent(context.getEntity(), Player.getComponentType());

        if (player == null) {
            return;
        }

        // Use CommandBuffer.run to defer store writes until the store is ready
        context.getCommandBuffer().run(store ->
                WaveManager.spawnNextWave(store, msg -> player.sendMessage(Message.raw(msg)))
        );
    }
}
