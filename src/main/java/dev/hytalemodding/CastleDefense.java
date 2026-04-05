package dev.hytalemodding;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.hytalemodding.commands.MobWaveCommand;
import dev.hytalemodding.commands.HealCommand;
import dev.hytalemodding.events.WelcomeEvent;

import javax.annotation.Nonnull;

public class CastleDefense extends JavaPlugin {

    public CastleDefense(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new MobWaveCommand("mobwave", "Set the level mobs"));
        this.getCommandRegistry().registerCommand(new HealCommand());

        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, WelcomeEvent::onPlayerReady);
    }
}