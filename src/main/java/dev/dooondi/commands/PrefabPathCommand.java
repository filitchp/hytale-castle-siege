package dev.dooondi.commands;

import com.hypixel.hytale.builtin.path.WorldPathData;
import com.hypixel.hytale.builtin.path.path.IPrefabPath;
import com.hypixel.hytale.builtin.path.waypoint.IPrefabPathWaypoint;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PrefabPathCommand extends AbstractTargetPlayerCommand {

    // Per-player selected path for editing, keyed by player UUID.
    private static final ConcurrentHashMap<UUID, UUID> selectedPaths = new ConcurrentHashMap<>();

    private final RequiredArg<String> actionArg;
    private final OptionalArg<UUID> uuidArg;

    public PrefabPathCommand() {
        super("prefabpath", "Manage prefab paths. Usage: /prefabpath debug | /prefabpath edit <UUID> | /prefabpath delete <UUID>");

        this.actionArg = this.withRequiredArg("action", "Action to perform (debug, edit, show, delete)", ArgTypes.STRING);
        this.uuidArg = this.withOptionalArg("uuid", "UUID of the prefab path", ArgTypes.UUID);
    }

    public static UUID getSelectedPath(UUID playerUuid) {
        return selectedPaths.get(playerUuid);
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
            executeDebug(commandContext, world, pathData, store);
        } else if ("edit".equalsIgnoreCase(action)) {
            executeEdit(commandContext, playerRef, pathData);
        } else if ("show".equalsIgnoreCase(action)) {
            executeShow(commandContext, playerRef, pathData, store);
        } else if ("delete".equalsIgnoreCase(action)) {
            executeDelete(commandContext, pathData);
        } else {
            commandContext.sendMessage(Message.raw("Unknown action: " + action + ". Supported: debug, edit, show, delete"));
        }
    }

    private void executeDebug(CommandContext commandContext, World world,
                              WorldPathData pathData, Store<EntityStore> store) {
        List<IPrefabPath> paths = pathData.getAllPrefabPaths();

        System.out.println("[CastleSiege] ===== Saved prefab paths in world '"
                + world.getName() + "' =====");
        System.out.println("[CastleSiege] Total paths: " + paths.size());

        for (IPrefabPath path : paths) {
            String header = String.format(
                    "[CastleSiege]   World ID=%d UUID=%s name=%s [ Length: %d, Loaded nodes: %d ]",
                    path.getWorldGenId(),
                    path.getId(),
                    path.getName(),
                    path.length(),
                    path.loadedWaypointCount()
            );
            System.out.println(header);

            List<IPrefabPathWaypoint> waypoints = path.getPathWaypoints();
            for (int i = 0; i < waypoints.size(); i++) {
                IPrefabPathWaypoint wp = waypoints.get(i);
                Vector3d pos = wp.getWaypointPosition(store);
                System.out.printf("[CastleSiege]     Waypoint %d: (%.2f, %.2f, %.2f)%n",
                        i, pos.x, pos.y, pos.z);
            }
        }
        System.out.println("[CastleSiege] ===== end of path list =====");

        commandContext.sendMessage(Message.raw("Printed " + paths.size()
                + " path(s) to the server console."));
    }

    private static final String WAYPOINT_PARTICLE = "Totem_Heal_Extra";

    private IPrefabPath findPath(UUID pathUuid, WorldPathData pathData) {
        for (IPrefabPath path : pathData.getAllPrefabPaths()) {
            if (path.getId().equals(pathUuid)) {
                return path;
            }
        }
        return null;
    }

    private void executeShow(CommandContext commandContext, PlayerRef playerRef,
                             WorldPathData pathData, Store<EntityStore> store) {
        UUID pathUuid = selectedPaths.get(playerRef.getUuid());
        if (pathUuid == null) {
            commandContext.sendMessage(Message.raw("No path selected. Use /prefabpath edit <UUID> first."));
            return;
        }

        IPrefabPath target = findPath(pathUuid, pathData);
        if (target == null) {
            commandContext.sendMessage(Message.raw("Selected path no longer exists: " + pathUuid));
            selectedPaths.remove(playerRef.getUuid());
            return;
        }

        List<IPrefabPathWaypoint> waypoints = target.getPathWaypoints();
        for (IPrefabPathWaypoint wp : waypoints) {
            Vector3d pos = wp.getWaypointPosition(store);
            ParticleUtil.spawnParticleEffect(WAYPOINT_PARTICLE, pos, store);
        }

        commandContext.sendMessage(Message.raw("Spawned particles at " + waypoints.size()
                + " waypoint(s) on path " + pathUuid));
    }

    private void executeEdit(CommandContext commandContext, PlayerRef playerRef, WorldPathData pathData) {
        UUID pathUuid = this.uuidArg.get(commandContext);
        if (pathUuid == null) {
            commandContext.sendMessage(Message.raw("Usage: /prefabpath edit <PATH_UUID>"));
            return;
        }

        IPrefabPath target = findPath(pathUuid, pathData);
        if (target == null) {
            commandContext.sendMessage(Message.raw("No prefab path found with UUID: " + pathUuid));
            return;
        }

        selectedPaths.put(playerRef.getUuid(), pathUuid);
        commandContext.sendMessage(Message.raw("Selected path " + target.getWorldGenId()
                + "." + pathUuid + " (" + target.getName() + ") for editing."));
    }

    private void executeDelete(CommandContext commandContext, WorldPathData pathData) {
        UUID pathUuid = this.uuidArg.get(commandContext);
        if (pathUuid == null) {
            commandContext.sendMessage(Message.raw("Usage: /prefabpath delete <PATH_UUID>"));
            return;
        }

        IPrefabPath target = findPath(pathUuid, pathData);
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
