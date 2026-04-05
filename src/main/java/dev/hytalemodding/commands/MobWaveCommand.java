package dev.hytalemodding.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;




public class MobWaveCommand extends AbstractCommand {

    public MobWaveCommand(String name, String description) {
        super(name, description);
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        context.sendMessage(Message.raw("mob wave command!"));

        if (!context.isPlayer()) {
            return CompletableFuture.completedFuture(null);
        }
//
//        auto player = (Player)context;
//
//        World world = player.getWorld();
        return CompletableFuture.completedFuture(null);
    }

}
