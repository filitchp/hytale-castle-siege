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
import com.hypixel.hytale.builtin.adventure.npcobjectives.NPCObjectivesPlugin;
import it.unimi.dsi.fastutil.Pair;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    private static final int MAX_COLS = 3;
    private static final double ROW_SPACING = 2.0;

    /**
     * Computes the spawn position for a mob at the given index.
     * Mobs are arranged in rows of up to 3 wide (centered on origin along X),
     * with each additional row placed 2 blocks behind the previous one (+Z).
     *
     * @param origin the center point of the front row
     * @param index  the mob's sequential index
     * @return the spawn position for this mob
     */
    private static Vector3d computeSpawnPosition(Vector3d origin, int index) {
        int row = index / MAX_COLS;
        int col = index % MAX_COLS;
        double halfCols = (MAX_COLS - 1) / 2.0;
        return new Vector3d(
                origin.x + (col - halfCols),
                origin.y,
                origin.z + (row * ROW_SPACING)
        );
    }

    private static final Random RANDOM = new Random();

    /**
     * Creates a randomized path using absolute coordinates. All mobs march
     * forward (-Z) for 23 blocks, then randomly branch:
     *   1/3 chance: turn right 8 blocks (+X)
     *   1/3 chance: turn left 8 blocks (-X)
     *   1/3 chance: continue forward 8 blocks, then branch again:
     *       50% turn right 16 blocks (+X)
     *       50% turn left 16 blocks (-X)
     *
     * @param spawnPos the mob's spawn position (first waypoint)
     * @param rotation the facing direction at each waypoint
     * @return a WavePath with the built path and total distance
     */
    // Fixed branch points — all mobs converge on these regardless of spawn position
    private static final Vector3d BRANCH_1 = new Vector3d(-0.5, 80.0, -23.5);
    private static final Vector3d BRANCH_2 = new Vector3d(-0.5, 80.0, -28.0);

    private static WavePath createWavePath(Vector3d spawnPos, Vector3f rotation) {
        TransientPath path = new TransientPath();

        // Waypoint 0: start at spawn
        path.addWaypoint(spawnPos, rotation);

        // Waypoint 1: all mobs converge on the first branch point
        double distToBranch1 = Math.sqrt(
                Math.pow(BRANCH_1.x - spawnPos.x, 2)
                + Math.pow(BRANCH_1.z - spawnPos.z, 2));
        path.addWaypoint(BRANCH_1, rotation);

        double totalDistance = distToBranch1;

        int branch = RANDOM.nextInt(3);
        if (branch == 0) {
            // Turn right 8 blocks (+X) from branch 1
            totalDistance += 8.0;
            path.addWaypoint(new Vector3d(BRANCH_1.x + 8.0, BRANCH_1.y, BRANCH_1.z), rotation);
        } else if (branch == 1) {
            // Turn left 8 blocks (-X) from branch 1
            totalDistance += 8.0;
            path.addWaypoint(new Vector3d(BRANCH_1.x - 8.0, BRANCH_1.y, BRANCH_1.z), rotation);
        } else {
            // Continue forward to the second branch point
            double distBranch1To2 = Math.abs(BRANCH_2.z - BRANCH_1.z);
            totalDistance += distBranch1To2;
            path.addWaypoint(BRANCH_2, rotation);

            // Turn left or right 16 blocks from branch 2
            if (RANDOM.nextBoolean()) {
                path.addWaypoint(new Vector3d(BRANCH_2.x + 16.0, BRANCH_2.y, BRANCH_2.z), rotation);
            } else {
                path.addWaypoint(new Vector3d(BRANCH_2.x - 16.0, BRANCH_2.y, BRANCH_2.z), rotation);
            }
            totalDistance += 16.0;
        }

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
                Vector3d pos = computeSpawnPosition(origin, mobIndex);

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
        // Wave mobs have MaxSpeed 14-20 and the role uses RelSpeed 0.18-0.25, so ~3.0 b/s
        double estimatedSpeed = 3.0;

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
                "Wave " + waveNumber + " started! " + pendingMobs.size()
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
