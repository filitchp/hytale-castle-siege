package dev.hytalemodding.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;


// Example usage: /heal --health 50 --message "Feels Good" --debug
public class HealCommand extends AbstractTargetPlayerCommand {
    private final DefaultArg<Float> healthArg;
    private final OptionalArg<String> messageArg;
    private final FlagArg debugArg;

    public HealCommand() {
        super("heal", "Healing a player for an <input> amount of HP (default: 100)");

        // Abstract TargetPlayerCommand passes the player that ran the command by default and implements
        // the use of `--player <value>` to specify someone else

        // args <Command String> <Description> <Default Value> <Desc of default value>
        this.healthArg = this.withDefaultArg("health", "Amount to heal player", ArgTypes.FLOAT, (float) 100, "Desc of Default: 100");

        // Or you could do the following making the health value required instead of with a default.
        //    You would need to change the declaration to RequiredArg<Float>
        // this.healthArg = this.withRequiredArg("health", "Amount to heal player", ArgTypes.FLOAT);

        // Optional args require the user the input `--<key> <value>` so the following would require `--message "Good Luck"`
        this.messageArg = this.withOptionalArg("message", "Message to print while healing", ArgTypes.STRING);

        // No type needed due to being a bool
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

        if (this.debugArg.get(commandContext) == true) { // <-- See commandContext passed to argument here
            commandContext.sendMessage(Message.raw("We are debugging"));
        }

        // Health is stored in a generic stat map to allowing mods/future content to easily add more stats if desired.
        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        int healthIdx = DefaultEntityStatTypes.getHealth();
        EntityStatValue health = stats.get(healthIdx);

        float missing = health.getMax() - health.get();

        if (this.debugArg.get(commandContext) == true) { // <-- See commandContext passed to argument here
            commandContext.sendMessage(Message.raw("Missing:  " + missing + " health"));
            commandContext.sendMessage(Message.raw("Adding:  " + healthArg.get(commandContext) + " health to "));
            commandContext.sendMessage(Message.raw(messageArg.get(commandContext)));
            commandContext.sendMessage(Message.raw("Input Value: " + healthArg.get(commandContext) + " Default"));
            commandContext.sendMessage(Message.raw("Default Health Value: " + healthArg.getDefaultValue()));
        }

        stats.addStatValue(healthIdx, healthArg.get(commandContext)); // <-- See commandContext passed to argument here
    }

//    @Override
//    protected void execute(
//            @NonNullDecl CommandContext commandContext,
//            @NullableDecl Ref<EntityStore> ref,
//            @NonNullDecl Ref<EntityStore> ref1,
//            @NonNullDecl PlayerRef playerRef,
//            @NonNullDecl World world,
//            @NonNullDecl Store<EntityStore> store) {
//
//    }
}

