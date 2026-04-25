package dev.dooondi;

import com.hypixel.hytale.server.core.event.events.player.*;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.dooondi.commands.CsCommand;
import dev.dooondi.commands.PrefabPathCommand;
import dev.dooondi.events.InputListener;
import dev.dooondi.events.WelcomeEvent;
import dev.dooondi.systems.BlockBreakEventSystem;
import dev.dooondi.systems.BlockPlaceEventSystem;
import dev.dooondi.wave.MobDeathTracker;
import dev.dooondi.wave.OpenWaveUIInteraction;
import dev.dooondi.wave.TriggerWaveInteraction;
import dev.dooondi.wave.WaveManager;

import javax.annotation.Nonnull;

public class CastleSiege extends JavaPlugin {

    public CastleSiege(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        WaveManager.initPersistence(this.getDataDirectory());

        CsCommand csCommand = new CsCommand();
        this.getCommandRegistry().registerCommand(csCommand);
        this.getCommandRegistry().registerCommand(new PrefabPathCommand());

        if (csCommand.getPermission() != null) {
            PermissionsModule.get().addGroupPermission(
                    WelcomeEvent.CS_PERMISSION_GROUP,
                    java.util.Set.of(csCommand.getPermission()));
        }

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