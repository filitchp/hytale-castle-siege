package dev.hytalemodding.ui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class WaveHUD extends CustomUIHud {

    public WaveHUD(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("WaveHUD.ui");
    }

    public void setWaveLabel(int currentWave, int maxWave) {
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#WaveLabel.TextSpans", Message.raw("Wave " + currentWave + " / " + maxWave));
        update(false, builder);
    }

    public void setStatus(String status) {
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#WaveStatus.TextSpans", Message.raw(status));
        update(false, builder);
    }
}
