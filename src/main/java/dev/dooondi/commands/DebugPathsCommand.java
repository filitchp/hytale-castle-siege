package dev.dooondi.commands;

import com.hypixel.hytale.builtin.path.WorldPathData;
import com.hypixel.hytale.builtin.path.path.IPrefabPath;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.List;

// Dumps every saved prefab path in the player's current world to the server
// console, matching the fields /path list prints: worldGenId.uuid (name)
// [Length: N, Loaded nodes: M].
public class DebugPathsCommand extends AbstractTargetPlayerCommand {

    public DebugPathsCommand() {
        super("debugpaths", "Print all saved NPC paths in the current world to the server console");
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext commandContext,
            @NullableDecl Ref<EntityStore> ref,
            @NonNullDecl Ref<EntityStore> ref1,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world,
            @NonNullDecl Store<EntityStore> store) {

        WorldPathData pathData = store.getResource(WorldPathData.getResourceType());
        if (pathData == null) {
            String msg = "[CastleSiege] No WorldPathData resource on store for world " + world.getName();
            System.out.println(msg);
            commandContext.sendMessage(Message.raw(msg));
            return;
        }

        List<IPrefabPath> paths = pathData.getAllPrefabPaths();

        System.out.println("[CastleSiege] ===== Saved prefab paths in world '"
                + world.getName() + "' =====");
        System.out.println("[CastleSiege] Total paths: " + paths.size());

        for (IPrefabPath path : paths) {
            String line = String.format(
                    "[CastleSiege]   %d.%s (%s) [ Length: %d, Loaded nodes: %d ]",
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
}
