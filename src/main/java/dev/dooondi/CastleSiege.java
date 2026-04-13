package dev.dooondi;

import com.hypixel.hytale.server.core.event.events.player.*;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.dooondi.commands.DebugPathsCommand;
import dev.dooondi.commands.HealCommand;
import dev.dooondi.commands.MobWaveCommand;
import dev.dooondi.commands.ShowWaveHudCommand;
import dev.dooondi.commands.StartWaveCommand;
import dev.dooondi.events.InputListener;
import dev.dooondi.events.WelcomeEvent;
import dev.dooondi.systems.BlockBreakEventSystem;
import dev.dooondi.systems.BlockPlaceEventSystem;
import dev.dooondi.wave.MobDeathTracker;
import dev.dooondi.wave.OpenWaveUIInteraction;
import dev.dooondi.wave.TriggerWaveInteraction;

import javax.annotation.Nonnull;

public class CastleSiege extends JavaPlugin {

    public CastleSiege(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new MobWaveCommand());
        this.getCommandRegistry().registerCommand(new HealCommand());
        this.getCommandRegistry().registerCommand(new StartWaveCommand());
        this.getCommandRegistry().registerCommand(new ShowWaveHudCommand());
        this.getCommandRegistry().registerCommand(new DebugPathsCommand());

        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, WelcomeEvent::onPlayerReady);

        // Receive all keyed events regardless of key
        getEventRegistry().registerGlobal(
                PlayerChatEvent.class,
                InputListener::onPlayerChat
        );

        this.getEntityStoreRegistry().registerSystem(new MobDeathTracker());


        // Initialize Event Systems
        this.getEntityStoreRegistry().registerSystem(new BlockBreakEventSystem());
        this.getEntityStoreRegistry().registerSystem(new BlockPlaceEventSystem());

        this.getCodecRegistry(Interaction.CODEC)
                .register("TriggerWave", TriggerWaveInteraction.class, TriggerWaveInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC)
                .register("OpenWaveUI", OpenWaveUIInteraction.class, OpenWaveUIInteraction.CODEC);
    }
}