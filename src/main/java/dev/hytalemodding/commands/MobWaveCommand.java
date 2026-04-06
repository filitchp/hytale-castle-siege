package dev.hytalemodding.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.path.IPath;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.builtin.path.path.TransientPath;
import com.hypixel.hytale.builtin.path.waypoint.RelativeWaypointDefinition;
import com.hypixel.hytale.builtin.adventure.npcobjectives.NPCObjectivesPlugin;
import it.unimi.dsi.fastutil.Pair;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MobWaveCommand extends AbstractTargetPlayerCommand {
    private final RequiredArg<Integer> waveArg;
    private final OptionalArg<String> messageArg;
    private final FlagArg debugArg;

    private record MobEntry(String name, int count) {}
    private record WavePath(IPath<?> path, double totalDistance) {}
    private record PendingMob(NPCEntity npcEntity, WavePath wavePath, double distanceFromOrigin) {}

    private static final ScheduledExecutorService WAVE_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "MobWave-Scheduler");
                t.setDaemon(true);
                return t;
            });

    private static final Map<Integer, List<MobEntry>> WAVE_TABLE = Map.ofEntries(
            Map.entry(1, List.of(
                    new MobEntry("Rat_Wave", 3)
            )),
            Map.entry(2, List.of(
                    new MobEntry("Rat_Wave", 5)
            )),
            Map.entry(3, List.of(
                    new MobEntry("Rat_Wave", 3),
                    new MobEntry("Skeleton_Wave", 2)
            )),
            Map.entry(4, List.of(
                    new MobEntry("Skeleton_Wave", 4),
                    new MobEntry("Rat_Wave", 2)
            )),
            Map.entry(5, List.of(
                    new MobEntry("Skeleton_Wave", 5),
                    new MobEntry("Snake_Rattle_Wave", 2)
            )),
            Map.entry(6, List.of(
                    new MobEntry("Snake_Rattle_Wave", 4),
                    new MobEntry("Skeleton_Wave", 3)
            )),
            Map.entry(7, List.of(
                    new MobEntry("Skeleton_Wave", 4),
                    new MobEntry("Snake_Rattle_Wave", 3),
                    new MobEntry("Rat_Wave", 4)
            )),
            Map.entry(8, List.of(
                    new MobEntry("Snake_Rattle_Wave", 5),
                    new MobEntry("Skeleton_Wave", 5)
            )),
            Map.entry(9, List.of(
                    new MobEntry("Skeleton_Wave", 6),
                    new MobEntry("Snake_Rattle_Wave", 4),
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 1)
            )),
            Map.entry(10, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 2),
                    new MobEntry("Skeleton_Wave", 6),
                    new MobEntry("Snake_Rattle_Wave", 4)
            )),
            Map.entry(11, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 3),
                    new MobEntry("Snake_Rattle_Wave", 5),
                    new MobEntry("Skeleton_Wave", 4)
            )),
            Map.entry(12, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 4),
                    new MobEntry("Snake_Rattle_Wave", 6),
                    new MobEntry("Skeleton_Wave", 5)
            )),
            Map.entry(13, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 5),
                    new MobEntry("Snake_Rattle_Wave", 5),
                    new MobEntry("Skeleton_Wave", 6)
            )),
            Map.entry(14, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 5),
                    new MobEntry("Snake_Rattle_Wave", 6),
                    new MobEntry("Skeleton_Wave", 6),
                    new MobEntry("Rat_Wave", 6)
            )),
            Map.entry(15, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 6),
                    new MobEntry("Snake_Rattle_Wave", 7),
                    new MobEntry("Skeleton_Wave", 7)
            )),
            Map.entry(16, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 7),
                    new MobEntry("Snake_Rattle_Wave", 8),
                    new MobEntry("Skeleton_Wave", 6)
            )),
            Map.entry(17, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 8),
                    new MobEntry("Snake_Rattle_Wave", 6),
                    new MobEntry("Skeleton_Wave", 8)
            )),
            Map.entry(18, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 8),
                    new MobEntry("Snake_Rattle_Wave", 8),
                    new MobEntry("Skeleton_Wave", 8)
            )),
            Map.entry(19, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 9),
                    new MobEntry("Snake_Rattle_Wave", 9),
                    new MobEntry("Skeleton_Wave", 8)
            )),
            Map.entry(20, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 10),
                    new MobEntry("Snake_Rattle_Wave", 10),
                    new MobEntry("Skeleton_Wave", 10)
            ))
    );

    public MobWaveCommand() {
        super("mobwave", "Tower Defense mod command: spawn mobs at a given wave");

        this.waveArg = this.withRequiredArg("wave", "Wave number (1-20)", ArgTypes.INTEGER);
        this.messageArg = this.withOptionalArg("message", "Message to print while spawning mobs", ArgTypes.STRING);
        this.debugArg = this.withFlagArg("debug", "Add debug logs");
    }

    /**
     * Computes the spawn position for a mob at the given grid index.
     * Mobs are arranged in a square grid centered on the origin with 1-block spacing.
     *
     * @param origin   the center point of the spawn area
     * @param index    the mob's sequential index in the grid
     * @param gridSize the side length of the square grid
     * @return the spawn position for this mob
     */
    private static Vector3d computeSpawnPosition(Vector3d origin, int index, int gridSize) {
        int row = index / gridSize;
        int col = index % gridSize;
        double halfGrid = (gridSize - 1) / 2.0;
        return new Vector3d(
                origin.x + (col - halfGrid),
                origin.y,
                origin.z + (row - halfGrid)
        );
    }

    private static final Random RANDOM = new Random();

    /**
     * Creates a randomized path for a wave mob. All mobs march forward (-Z) for
     * 23 blocks, then randomly branch:
     *   1/3 chance: turn right and move to a central attack position (+X)
     *   1/3 chance: turn left and move to a central attack position (-X)
     *   1/3 chance: continue forward to the stairs, then branch again:
     *       50% turn right and up the stairs (+X)
     *       50% turn left and up the stairs (-X)
     *
     * @param spawnPos       the mob's spawn position (path origin)
     * @param forwardRotation the initial facing direction
     * @return a path the mob will follow
     */
    private static WavePath createWavePath(Vector3d spawnPos, Vector3f forwardRotation) {
        Queue<RelativeWaypointDefinition> waypoints = new LinkedList<>();
        double totalDistance = 0;

        final double initialDistance = 23.0;

        // All mobs march forward 23 blocks (-Z direction / North)
        waypoints.add(new RelativeWaypointDefinition(0f, initialDistance));
        totalDistance += initialDistance;

        int branch = RANDOM.nextInt(3);
        final double firstBranchDistance = 8.0;
        if (branch == 0) {
            // Turn right toward +X (facing East)
            waypoints.add(new RelativeWaypointDefinition(-90f, firstBranchDistance));
            totalDistance += firstBranchDistance;
        } else if (branch == 1) {
            // Turn left toward -X (facing West)
            waypoints.add(new RelativeWaypointDefinition(90f, firstBranchDistance));
            totalDistance += firstBranchDistance;
        } else {

            final double secondBranchForwardDistance = 8.0;
            // Continue up the stairs
            waypoints.add(new RelativeWaypointDefinition(0f, secondBranchForwardDistance));
            totalDistance += secondBranchForwardDistance;
            // Second branch: left or right

            final double secondBranchTurnDistance = 16.0;
            if (RANDOM.nextBoolean()) {
                // Turn right toward +X (facing East)
                waypoints.add(new RelativeWaypointDefinition(-90f, secondBranchTurnDistance));
            } else {
                // Turn left toward -X (facing West)
                waypoints.add(new RelativeWaypointDefinition(90f, secondBranchTurnDistance));
            }
            totalDistance += secondBranchTurnDistance;
        }

        var path = TransientPath.buildPath(spawnPos, forwardRotation, waypoints, 1.0);
        return new WavePath(path, totalDistance);
    }

    /**
     * Spawns all mobs for the given wave number, then staggers their path
     * assignments one second apart, starting with the mob closest to the origin.
     *
     * @param waveNumber     the wave number (1-20)
     * @param origin         the center spawn position
     * @param store          the entity store for spawning
     * @param commandContext the command context for debug messaging
     * @param debug          whether to print debug messages
     */
    private void spawnWave(int waveNumber, Vector3d origin, Store<EntityStore> store,
                           CommandContext commandContext, boolean debug) {
        List<MobEntry> entries = WAVE_TABLE.get(waveNumber);
        if (entries == null) {
            commandContext.sendMessage(Message.raw("Unknown wave number: " + waveNumber + ". Valid range is 1-20."));
            return;
        }

        int totalMobs = entries.stream().mapToInt(MobEntry::count).sum();
        int gridSize = (int) Math.ceil(Math.sqrt(totalMobs));
        var rotation = new Vector3f(0f, 0f, 0f);

        // Phase 1: Spawn all mobs and collect them (no paths yet)
        List<PendingMob> pendingMobs = new ArrayList<>();
        int mobIndex = 0;
        for (MobEntry entry : entries) {
            if (debug) {
                commandContext.sendMessage(Message.raw(
                        "Spawning " + entry.count() + "x " + entry.name()));
            }
            for (int i = 0; i < entry.count(); i++) {
                Vector3d pos = computeSpawnPosition(origin, mobIndex, gridSize);

                Pair<Ref<EntityStore>, INonPlayerCharacter> result =
                        NPCPlugin.get().spawnNPC(store, entry.name(), null, pos, rotation);

                if (result != null) {
                    NPCEntity npcEntity = store.getComponent(
                            result.first(), NPCEntity.getComponentType());
                    if (npcEntity != null) {
                        var path = createWavePath(pos, rotation);
                        double dx = pos.x - origin.x;
                        double dz = pos.z - origin.z;
                        double distSq = dx * dx + dz * dz;
                        pendingMobs.add(new PendingMob(npcEntity, path, distSq));
                    }
                } else if (debug) {
                    commandContext.sendMessage(Message.raw(
                            "Failed to spawn " + entry.name() + " at ("
                                    + pos.x + ", " + pos.y + ", " + pos.z + ")"));
                }

                mobIndex++;
            }
        }

        // Phase 2: Sort by distance from origin (closest first) and stagger paths
        pendingMobs.sort(Comparator.comparingDouble(PendingMob::distanceFromOrigin));

        // Estimated mob travel speed (blocks/sec): MaxSpeed * BodyMotionPath RelSpeed
        // Most mobs have MaxSpeed ~6-8 and the role uses RelSpeed 0.18-0.25, so ~1.5 b/s
        double estimatedSpeed = 1.5;

        for (int i = 0; i < pendingMobs.size(); i++) {
            PendingMob mob = pendingMobs.get(i);
            long startDelaySec = i;
            long travelTimeSec = (long) Math.ceil(mob.wavePath().totalDistance() / estimatedSpeed);

            // Schedule path activation
            WAVE_SCHEDULER.schedule(
                    () -> mob.npcEntity().getPathManager().setTransientPath(mob.wavePath().path()),
                    startDelaySec,
                    TimeUnit.SECONDS
            );

            // Schedule path clear so the mob stops looping and switches to wander
            WAVE_SCHEDULER.schedule(
                    () -> mob.npcEntity().getPathManager().setTransientPath(null),
                    startDelaySec + travelTimeSec,
                    TimeUnit.SECONDS
            );
        }

        commandContext.sendMessage(Message.raw(
                "Wave " + waveNumber + " started! " + totalMobs
                        + " mobs spawned, deploying one per second."));
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

        var spawnOrigin = new Vector3d(-0.5, 80.0, 0.5);

        commandContext.sendMessage(Message.raw("Get Ready to Fight! Starting wave " + wave));
        spawnWave(wave, spawnOrigin, store, commandContext, debug);
    }
}
