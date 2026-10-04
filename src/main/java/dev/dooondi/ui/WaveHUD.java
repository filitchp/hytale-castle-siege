package dev.dooondi.ui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.dooondi.wave.TeamBank;

import javax.annotation.Nonnull;

public class WaveHUD extends CustomUIHud {

    public static final String KEY = "CastleSiege:WaveHUD";

    public WaveHUD(@Nonnull PlayerRef playerRef) {
        super(playerRef, KEY);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("WaveHUD.ui");
    }

    public void setValues(int currentWave, int maxWave, int mobsRemaining, int money) {
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#WaveLabel.TextSpans", Message.raw(currentWave + " / " + maxWave));
        builder.set("#MobsLabel.TextSpans", Message.raw(Integer.toString(mobsRemaining)));
        builder.set("#MoneyLabel.TextSpans", Message.raw(TeamBank.format(money)));
        update(false, builder);
    }
}
