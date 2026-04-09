package dev.hytalemodding;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.hytalemodding.commands.HealCommand;
import dev.hytalemodding.commands.MobWaveCommand;
import dev.hytalemodding.commands.ShowWaveHudCommand;
import dev.hytalemodding.commands.StartWaveCommand;
import dev.hytalemodding.events.WelcomeEvent;
import dev.hytalemodding.wave.TriggerWaveInteraction;

import javax.annotation.Nonnull;

public class CastleDefense extends JavaPlugin {

    public CastleDefense(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new MobWaveCommand());
        this.getCommandRegistry().registerCommand(new HealCommand());
        this.getCommandRegistry().registerCommand(new StartWaveCommand());
        this.getCommandRegistry().registerCommand(new ShowWaveHudCommand());

        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, WelcomeEvent::onPlayerReady);

        this.getCodecRegistry(Interaction.CODEC)
                .register("TriggerWave", TriggerWaveInteraction.class, TriggerWaveInteraction.CODEC);
    }
}