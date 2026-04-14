package dev.dooondi.commands;

import com.hypixel.hytale.builtin.path.WorldPathData;
import com.hypixel.hytale.builtin.path.path.IPrefabPath;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.List;
import java.util.UUID;

public class PrefabPathCommand extends AbstractTargetPlayerCommand {

    private final RequiredArg<String> actionArg;
    private final OptionalArg<UUID> uuidArg;

    public PrefabPathCommand() {
        super("prefabpath", "Manage prefab paths. Usage: /prefabpath debug | /prefabpath delete <UUID>");

        this.actionArg = this.withRequiredArg("action", "Action to perform (debug, delete)", ArgTypes.STRING);
        this.uuidArg = this.withOptionalArg("uuid", "UUID of the prefab path (required for delete)", ArgTypes.UUID);
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext commandContext,
            @NullableDecl Ref<EntityStore> ref,
            @NonNullDecl Ref<EntityStore> ref1,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world,
            @NonNullDecl Store<EntityStore> store) {

        String action = this.actionArg.get(commandContext);

        WorldPathData pathData = store.getResource(WorldPathData.getResourceType());
        if (pathData == null) {
            commandContext.sendMessage(Message.raw("No WorldPathData found in this world."));
            return;
        }

        if ("debug".equalsIgnoreCase(action)) {
            executeDebug(commandContext, world, pathData);
        } else if ("delete".equalsIgnoreCase(action)) {
            executeDelete(commandContext, pathData);
        } else {
            commandContext.sendMessage(Message.raw("Unknown action: " + action + ". Supported: debug, delete"));
        }
    }

    private void executeDebug(CommandContext commandContext, World world, WorldPathData pathData) {
        List<IPrefabPath> paths = pathData.getAllPrefabPaths();

        System.out.println("[CastleSiege] ===== Saved prefab paths in world '"
                + world.getName() + "' =====");
        System.out.println("[CastleSiege] Total paths: " + paths.size());

        for (IPrefabPath path : paths) {
            String line = String.format(
                    "[CastleSiege]   World ID=%d UUID=%s name=%s [ Length: %d, Loaded nodes: %d ]",
                    path.getWorldGenId(),
                    path.getId(),
                    path.getName(),
                    path.length(),
                    path.loadedWaypointCount()
            );
            System.out.println(line);
        }
        System.out.println("[CastleSiege] ===== end of path list =====");

        commandContext.sendMessage(Message.raw("Printed " + paths.size()
                + " path(s) to the server console."));
    }

    private void executeDelete(CommandContext commandContext, WorldPathData pathData) {
        UUID pathUuid = this.uuidArg.get(commandContext);
        if (pathUuid == null) {
            commandContext.sendMessage(Message.raw("Usage: /prefabpath delete <PATH_UUID>"));
            return;
        }

        IPrefabPath target = null;
        for (IPrefabPath path : pathData.getAllPrefabPaths()) {
            if (path.getId().equals(pathUuid)) {
                target = path;
                break;
            }
        }

        if (target == null) {
            commandContext.sendMessage(Message.raw("No prefab path found with UUID: " + pathUuid));
            return;
        }

        int worldGenId = target.getWorldGenId();
        String name = target.getName();
        pathData.removePrefabPath(worldGenId, pathUuid);

        String msg = "Deleted prefab path " + worldGenId + "." + pathUuid + " (" + name + ")";
        System.out.println("[CastleSiege] " + msg);
        commandContext.sendMessage(Message.raw(msg));
    }
}
