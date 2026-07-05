package dev.dooondi.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.dooondi.wave.WaveManager;

import javax.annotation.Nonnull;

public class WaveUI extends InteractiveCustomUIPage<WaveUI.Data> {

    public WaveUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder,
                      @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("WaveUI.ui");
        applyLabels(uiCommandBuilder);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#StartWaveBtn");
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ResetBtn");
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, Data data) {
        super.handleDataEvent(ref, store, data);

        if (isGameComplete()) {
            Player player = store.getComponent(ref, Player.getComponentType());
            World world = (player != null) ? player.getWorld() : null;
            if (world != null) {
                WaveManager.fullReset(world, store);
            }
        } else {
            WaveManager.spawnNextWave(store,
                    msg -> playerRef.sendMessage(Message.raw(msg)));
        }

        UICommandBuilder builder = new UICommandBuilder();
        applyLabels(builder);
        sendUpdate(builder);
    }

    private boolean isGameComplete() {
        return WaveManager.getCurrentWave() >= WaveManager.getMaxWave()
                && !WaveManager.isWaveInProgress();
    }

    private void applyLabels(UICommandBuilder builder) {
        int current = WaveManager.getCurrentWave();
        int max = WaveManager.getMaxWave();
        int waveKills = WaveManager.getCurrentWaveKills();
        int waveTotal = WaveManager.getCurrentWaveTotalMobs();
        int remaining = waveTotal - waveKills;
        int totalKills = WaveManager.getTotalKills();
        int myKills = WaveManager.getPlayerKills(playerRef.getUuid());
        int myDeaths = WaveManager.getPlayerDeaths(playerRef.getUuid());

        builder.set("#WaveLabel.TextSpans", Message.raw("Wave " + current + " / " + max));
        builder.set("#WaveKillLabel.TextSpans",
                Message.raw("Mobs Remaining: " + remaining + " out of " + waveTotal));
        builder.set("#PlayerKillLabel.TextSpans", Message.raw("Your Kills: " + myKills));
        builder.set("#DeathLabel.TextSpans", Message.raw("Your Deaths: " + myDeaths));
        builder.set("#TotalKillLabel.TextSpans", Message.raw("Mobs Killed: " + totalKills));

        boolean gameComplete = current >= max && !WaveManager.isWaveInProgress();
        String status;
        if (gameComplete) {
            status = "All waves complete!";
        } else if (WaveManager.isWaveInProgress()) {
            status = "Wave in progress! " + remaining + " out of " + waveTotal + "mobs remain.";
        } else {
            status = "Click to start the next wave";
        }
        builder.set("#StatusLabel.TextSpans", Message.raw(status));
        builder.set("#StartWaveBtnContainer.Visible", !gameComplete);
        builder.set("#ResetBtnContainer.Visible", gameComplete);
    }

    public static class Data {
        public static final BuilderCodec<Data> CODEC = BuilderCodec.builder(Data.class, Data::new)
                .build();
    }
}
