package dev.hytalemodding.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.ui.WaveHUD;
import dev.hytalemodding.wave.WaveManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

public class ShowWaveHudCommand extends AbstractTargetPlayerCommand {

    public ShowWaveHudCommand() {
        super("wavehud", "Show or hide the wave HUD");
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext commandContext,
            @NullableDecl Ref<EntityStore> ref,
            @NonNullDecl Ref<EntityStore> ref1,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world,
            @NonNullDecl Store<EntityStore> store) {

        Player player = commandContext.senderAs(Player.class);

        CompletableFuture.runAsync(() -> {
            if (player.getHudManager().getCustomHud() instanceof WaveHUD) {
                // Already showing — hide it by setting an empty HUD
                player.getHudManager().setCustomHud(playerRef, new CustomUIHud(playerRef) {
                    @Override
                    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
                    }
                });
                playerRef.sendMessage(Message.raw("Wave HUD hidden."));
            } else {
                WaveHUD hud = new WaveHUD(playerRef);
                player.getHudManager().setCustomHud(playerRef, hud);

                int current = WaveManager.getCurrentWave();
                int max = WaveManager.getMaxWave();
                hud.setWaveLabel(current, max);
                if (current == 0) {
                    hud.setStatus("Type /startwave to begin");
                } else if (current >= max) {
                    hud.setStatus("All waves completed!");
                } else {
                    hud.setStatus("Type /startwave for next wave");
                }

                playerRef.sendMessage(Message.raw("Wave HUD shown."));
            }
        }, world);
    }
}
