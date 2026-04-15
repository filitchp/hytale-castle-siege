package dev.dooondi.commands;

import com.hypixel.hytale.builtin.path.WorldPathData;
import com.hypixel.hytale.builtin.path.commands.PrefabPathHelper;
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
        } else if ("add".equalsIgnoreCase(action)) {
            executeAdd(commandContext, ref1, playerRef, pathData, store);
        } else if ("delnode".equalsIgnoreCase(action)) {
            executeDelNode(commandContext, playerRef, pathData, store);
        } else if ("delete".equalsIgnoreCase(action)) {
            executeDelete(commandContext, pathData);
        } else {
            commandContext.sendMessage(Message.raw("Unknown action: " + action + ". Supported: debug, edit, show, add, delnode, delete"));
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

    private void executeAdd(CommandContext commandContext, Ref<EntityStore> playerEntityRef,
                            PlayerRef playerRef, WorldPathData pathData, Store<EntityStore> store) {
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

        Vector3d playerPos = playerRef.getTransform().getPosition();
        // index -1 appends at end; pauseTime 0, observationAngle 0
        PrefabPathHelper.addMarker(store, playerEntityRef, pathUuid, target.getName(),
                0.0, 0f, (short) -1, target.getWorldGenId());

        commandContext.sendMessage(Message.raw(String.format(
                "Added waypoint at (%.1f, %.1f, %.1f) to path %s",
                playerPos.x, playerPos.y, playerPos.z, pathUuid)));
    }

    private void executeDelNode(CommandContext commandContext, PlayerRef playerRef,
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
        if (waypoints.isEmpty()) {
            commandContext.sendMessage(Message.raw("Path has no waypoints to delete."));
            return;
        }

        Vector3d playerPos = playerRef.getTransform().getPosition();
        IPrefabPathWaypoint closest = null;
        double closestDist = Double.MAX_VALUE;
        for (IPrefabPathWaypoint wp : waypoints) {
            Vector3d wpPos = wp.getWaypointPosition(store);
            double dist = Math.pow(wpPos.x - playerPos.x, 2)
                    + Math.pow(wpPos.y - playerPos.y, 2)
                    + Math.pow(wpPos.z - playerPos.z, 2);
            if (dist < closestDist) {
                closestDist = dist;
                closest = wp;
            }
        }

        Vector3d closestPos = closest.getWaypointPosition(store);
        int order = closest.getOrder();
        pathData.removePrefabPathWaypoint(target.getWorldGenId(), pathUuid, order);

        commandContext.sendMessage(Message.raw(String.format(
                "Deleted waypoint %d at (%.1f, %.1f, %.1f) — distance: %.1f blocks",
                order, closestPos.x, closestPos.y, closestPos.z, Math.sqrt(closestDist))));
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
