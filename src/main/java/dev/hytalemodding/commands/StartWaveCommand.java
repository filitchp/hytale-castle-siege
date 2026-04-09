package dev.hytalemodding.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.ui.WaveHUD;
import dev.hytalemodding.wave.WaveManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.concurrent.CompletableFuture;

public class StartWaveCommand extends AbstractTargetPlayerCommand {

    public StartWaveCommand() {
        super("startwave", "Start the next mob wave");
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

        // Ensure HUD is showing
        CompletableFuture.runAsync(() -> {
            if (!(player.getHudManager().getCustomHud() instanceof WaveHUD)) {
                player.getHudManager().setCustomHud(playerRef, new WaveHUD(playerRef));
            }
        }, world);

        int wave = WaveManager.spawnNextWave(store,
                msg -> commandContext.sendMessage(Message.raw(msg)));

        // Update HUD after spawning
        CompletableFuture.runAsync(() -> {
            if (player.getHudManager().getCustomHud() instanceof WaveHUD hud) {
                if (wave == -1) {
                    hud.setWaveLabel(WaveManager.getMaxWave(), WaveManager.getMaxWave());
                    hud.setStatus("All waves completed!");
                } else {
                    hud.setWaveLabel(wave, WaveManager.getMaxWave());
                    hud.setStatus("Wave in progress...");
                }
            }
        }, world);
    }
}
