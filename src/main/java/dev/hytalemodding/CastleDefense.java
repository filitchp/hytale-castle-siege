package dev.hytalemodding;

import com.hypixel.hytale.server.core.event.events.player.*;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.hytalemodding.commands.HealCommand;
import dev.hytalemodding.commands.MobWaveCommand;
import dev.hytalemodding.commands.ShowWaveHudCommand;
import dev.hytalemodding.commands.StartWaveCommand;
import dev.hytalemodding.events.InputListener;
import dev.hytalemodding.events.WelcomeEvent;
import dev.hytalemodding.systems.BlockBreakEventSystem;
import dev.hytalemodding.systems.PreventHammerDropSystem;
import dev.hytalemodding.wave.MobDeathTracker;
import dev.hytalemodding.wave.OpenWaveUIInteraction;
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



        // Receive all keyed events regardless of key
        getEventRegistry().registerGlobal(
                PlayerChatEvent.class,
                InputListener::onPlayerChat
        );

        this.getEntityStoreRegistry().registerSystem(new MobDeathTracker());


        // Initialize Event Systems
        this.getEntityStoreRegistry().registerSystem(new BlockBreakEventSystem());
        this.getEntityStoreRegistry().registerSystem(new PreventHammerDropSystem());

        this.getCodecRegistry(Interaction.CODEC)
                .register("TriggerWave", TriggerWaveInteraction.class, TriggerWaveInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC)
                .register("OpenWaveUI", OpenWaveUIInteraction.class, OpenWaveUIInteraction.CODEC);
    }
}