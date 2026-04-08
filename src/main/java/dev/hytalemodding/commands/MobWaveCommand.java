package dev.hytalemodding.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.wave.WaveManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class MobWaveCommand extends AbstractTargetPlayerCommand {
    private final RequiredArg<Integer> waveArg;
    private final OptionalArg<String> messageArg;
    private final FlagArg debugArg;

    public MobWaveCommand() {
        super("mobwave", "Tower Defense mod command: spawn mobs at a given wave");

        this.waveArg = this.withRequiredArg("wave", "Wave number (1-20)", ArgTypes.INTEGER);
        this.messageArg = this.withOptionalArg("message", "Message to print while spawning mobs", ArgTypes.STRING);
        this.debugArg = this.withFlagArg("debug", "Add debug logs");
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext commandContext,
            @NullableDecl Ref<EntityStore> ref,
            @NonNullDecl Ref<EntityStore> ref1,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world,
            @NonNullDecl Store<EntityStore> store) {

        var wave = this.waveArg.get(commandContext);
        boolean debug = this.debugArg.get(commandContext);

        if (debug) {
            commandContext.sendMessage(Message.raw("Debugging mobwave command"));
            commandContext.sendMessage(Message.raw("Wave: " + wave));
        }

        var message = this.messageArg.get(commandContext);
        if (message != null) {
            commandContext.sendMessage(Message.raw(message));
        }

        commandContext.sendMessage(Message.raw("Get Ready to Fight! Starting wave " + wave));
        WaveManager.spawnWave(wave, store,
                msg -> commandContext.sendMessage(Message.raw(msg)), debug);
    }
}
