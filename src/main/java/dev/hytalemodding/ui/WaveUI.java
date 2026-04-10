package dev.hytalemodding.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.wave.WaveManager;

import javax.annotation.Nonnull;

public class WaveUI extends InteractiveCustomUIPage<WaveUI.Data> {

    public WaveUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder,
                      @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("WaveUI.ui");

        int current = WaveManager.getCurrentWave();
        int max = WaveManager.getMaxWave();
        int kills = WaveManager.getTotalKills();

        uiCommandBuilder.set("#WaveLabel.TextSpans", Message.raw("Wave " + current + " / " + max));
        uiCommandBuilder.set("#KillLabel.TextSpans", Message.raw("Mobs Killed: " + kills));

        if (current >= max) {
            uiCommandBuilder.set("#StartWaveBtn.Text", Message.raw("All Waves Complete!"));
            uiCommandBuilder.set("#StartWaveBtn.Disabled", true);
        }

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#StartWaveBtn");
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, Data data) {
        super.handleDataEvent(ref, store, data);

        int wave = WaveManager.spawnNextWave(store,
                msg -> playerRef.sendMessage(Message.raw(msg)));

        UICommandBuilder builder = new UICommandBuilder();
        int current = WaveManager.getCurrentWave();
        int max = WaveManager.getMaxWave();
        int kills = WaveManager.getTotalKills();

        builder.set("#WaveLabel.TextSpans", Message.raw("Wave " + current + " / " + max));
        builder.set("#KillLabel.TextSpans", Message.raw("Mobs Killed: " + kills));

        if (wave == -1) {
            builder.set("#StartWaveBtn.Text", Message.raw("All Waves Complete!"));
            builder.set("#StartWaveBtn.Disabled", true);
        }

        sendUpdate(builder);
    }

    public static class Data {
        public static final BuilderCodec<Data> CODEC = BuilderCodec.builder(Data.class, Data::new)
                .build();
    }
}
