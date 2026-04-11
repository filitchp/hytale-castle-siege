package dev.hytalemodding.wave;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.path.IPath;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.builtin.path.path.TransientPath;
import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class WaveManager {

    public record MobEntry(String name, int count) {}
    record WavePath(IPath<?> path, double totalDistance) {}
    record PendingMob(NPCEntity npcEntity, WavePath wavePath, double distanceFromOrigin) {}

    private static final AtomicInteger currentWave = new AtomicInteger(0);
    private static final AtomicInteger totalKills = new AtomicInteger(0);

    // Set of entity refs for mobs spawned in the current wave that are still alive.
    private static final Set<Ref<EntityStore>> currentWaveMobs = ConcurrentHashMap.newKeySet();
    // Total mobs spawned in the current wave (denominator for "X / Y killed" in UI).
    private static final AtomicInteger currentWaveTotalMobs = new AtomicInteger(0);
    // Mobs killed in the current wave (across all players).
    private static final AtomicInteger currentWaveKills = new AtomicInteger(0);
    // Per-player kill counters keyed by player UUID (lifetime across all waves).
    private static final ConcurrentHashMap<UUID, AtomicInteger> playerKills = new ConcurrentHashMap<>();

    static final ScheduledExecutorService WAVE_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "MobWave-Scheduler");
                t.setDaemon(true);
                return t;
            });

    public static final Map<Integer, List<MobEntry>> WAVE_TABLE = Map.ofEntries(
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

    private static final int MAX_COLS = 3;
    private static final double ROW_SPACING = 2.0;
    private static final Random RANDOM = new Random();
    private static final Vector3d SPAWN_ORIGIN = new Vector3d(-0.5, 80.0, 0.5);
    private static final Vector3d BRANCH_1 = new Vector3d(-0.5, 80.0, -23.5);
    private static final Vector3d BRANCH_2 = new Vector3d(-0.5, 80.0, -28.0);

    public static int getCurrentWave() {
        return currentWave.get();
    }

    public static int getMaxWave() {
        return 20;
    }

    public static int getTotalKills() {
        return totalKills.get();
    }

    public static int getCurrentWaveKills() {
        return currentWaveKills.get();
    }

    public static int getCurrentWaveTotalMobs() {
        return currentWaveTotalMobs.get();
    }

    public static boolean isWaveInProgress() {
        return !currentWaveMobs.isEmpty();
    }

    public static int getPlayerKills(UUID playerUuid) {
        AtomicInteger counter = playerKills.get(playerUuid);
        return counter == null ? 0 : counter.get();
    }

    /**
     * Called from MobDeathTracker when a tracked wave mob dies.
     * Removes the mob from the alive set, bumps wave+lifetime counters,
     * and attributes the kill to the player UUID if provided.
     */
    public static void recordMobDeath(Ref<EntityStore> mobRef, UUID killerUuid) {
        if (!currentWaveMobs.remove(mobRef)) {
            // Not a tracked wave mob — ignore (still count toward lifetime totals).
            totalKills.incrementAndGet();
            if (killerUuid != null) {
                playerKills.computeIfAbsent(killerUuid, k -> new AtomicInteger(0)).incrementAndGet();
            }
            return;
        }
        totalKills.incrementAndGet();
        currentWaveKills.incrementAndGet();
        if (killerUuid != null) {
            playerKills.computeIfAbsent(killerUuid, k -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    public static int spawnNextWave(Store<EntityStore> store, Consumer<String> messageSender) {
        if (isWaveInProgress()) {
            int remaining = currentWaveMobs.size();
            messageSender.accept("Cannot advance: " + remaining + " mob(s) from the current wave are still alive.");
            return -2;
        }

        int wave = currentWave.incrementAndGet();
        if (wave > getMaxWave()) {
            currentWave.set(getMaxWave());
            messageSender.accept("All 20 waves completed!");
            return -1;
        }
        if (wave == 1) {
            playerKills.clear();
            totalKills.set(0);
        }
        messageSender.accept("Get Ready to Fight! Starting wave " + wave);
        spawnWave(wave, store, messageSender, false);
        return wave;
    }

    public static void spawnWave(int waveNumber, Store<EntityStore> store,
                                  Consumer<String> messageSender, boolean debug) {
        List<MobEntry> entries = WAVE_TABLE.get(waveNumber);
        if (entries == null) {
            messageSender.accept("Unknown wave number: " + waveNumber + ". Valid range is 1-20.");
            return;
        }

        // Reset wave-scoped tracking.
        currentWaveMobs.clear();
        currentWaveKills.set(0);
        currentWaveTotalMobs.set(0);

        var rotation = new Vector3f(0f, 0f, 0f);

        List<PendingMob> pendingMobs = new ArrayList<>();
        int mobIndex = 0;
        for (MobEntry entry : entries) {
            for (int i = 0; i < entry.count(); i++) {
                Vector3d pos = computeSpawnPosition(SPAWN_ORIGIN, mobIndex);

                if (debug) {
                    var entity_count = i+1;
                    messageSender.accept("Spawning '" + entry.name() + "' " + entity_count + "/" + entry.count());
                }

                Pair<Ref<EntityStore>, INonPlayerCharacter> result =
                        NPCPlugin.get().spawnNPC(store, entry.name(), null, pos, rotation);

                if (result != null) {

                    if (debug) {
                        // DEBUG
                        messageSender.accept("Got entity from entity store: " + entry.name());
                    }

                    NPCEntity npcEntity = store.getComponent(result.first(), NPCEntity.getComponentType());

                    if (npcEntity != null) {
                        // Track this mob as part of the current wave.
                        currentWaveMobs.add(result.first());
                        currentWaveTotalMobs.incrementAndGet();

                        var path = createMobPath(pos, rotation);
                        double dx = pos.x - SPAWN_ORIGIN.x;
                        double dz = pos.z - SPAWN_ORIGIN.z;
                        double distSq = dx * dx + dz * dz;
                        pendingMobs.add(new PendingMob(npcEntity, path, distSq));
                    }
                } else {

                    if (debug) {
                        // DEBUG
                        messageSender.accept("Failed to spawn " + entry.name() + " at ("
                                + pos.x + ", " + pos.y + ", " + pos.z + ")");
                    }
                }

                mobIndex++;
            }
        }

        pendingMobs.sort(Comparator.comparingDouble(PendingMob::distanceFromOrigin));

        // Estimated mob travel speed (blocks/sec): MaxSpeed * BodyMotionPath RelSpeed
        // Wave mobs have MaxSpeed 14-20 and the role uses RelSpeed 0.18-0.25, so ~3.0 b/s
        double estimatedSpeed = 3.0;

        for (int i = 0; i < pendingMobs.size(); i++) {
            PendingMob mob = pendingMobs.get(i);
            long startDelaySec = i;
            long travelTimeSec = (long) Math.ceil(mob.wavePath().totalDistance() / estimatedSpeed);

            WAVE_SCHEDULER.schedule(
                    () -> mob.npcEntity().getPathManager().setTransientPath(mob.wavePath().path()),
                    startDelaySec,
                    TimeUnit.SECONDS
            );

            WAVE_SCHEDULER.schedule(
                    () -> mob.npcEntity().getPathManager().setTransientPath(null),
                    startDelaySec + travelTimeSec,
                    TimeUnit.SECONDS
            );
        }

        messageSender.accept("Wave " + waveNumber + " started! " + pendingMobs.size()
                + " mobs spawned, deploying one per second.");
    }

    static Vector3d computeSpawnPosition(Vector3d origin, int index) {
        int row = index / MAX_COLS;
        int col = index % MAX_COLS;
        double halfCols = (MAX_COLS - 1) / 2.0;
        return new Vector3d(
                origin.x + (col - halfCols),
                origin.y,
                origin.z + (row * ROW_SPACING)
        );
    }

    static WavePath createMobPath(Vector3d spawnPos, Vector3f rotation) {
        TransientPath path = new TransientPath();

        path.addWaypoint(spawnPos, rotation);

        double distToBranch1 = Math.sqrt(
                Math.pow(BRANCH_1.x - spawnPos.x, 2)
                + Math.pow(BRANCH_1.z - spawnPos.z, 2));
        path.addWaypoint(BRANCH_1, rotation);

        double totalDistance = distToBranch1;

        int branch = RANDOM.nextInt(3);
        if (branch == 0) {
            totalDistance += 8.0;
            path.addWaypoint(new Vector3d(BRANCH_1.x + 8.0, BRANCH_1.y, BRANCH_1.z), rotation);
        } else if (branch == 1) {
            totalDistance += 8.0;
            path.addWaypoint(new Vector3d(BRANCH_1.x - 8.0, BRANCH_1.y, BRANCH_1.z), rotation);
        } else {
            double distBranch1To2 = Math.abs(BRANCH_2.z - BRANCH_1.z);
            totalDistance += distBranch1To2;
            path.addWaypoint(BRANCH_2, rotation);
            var nextBlocks = 20.0;

            if (RANDOM.nextBoolean()) {
                path.addWaypoint(new Vector3d(BRANCH_2.x + nextBlocks, BRANCH_2.y, BRANCH_2.z), rotation);
            } else {
                path.addWaypoint(new Vector3d(BRANCH_2.x - nextBlocks, BRANCH_2.y, BRANCH_2.z), rotation);
            }
            totalDistance += nextBlocks;
        }

        return new WavePath(path, totalDistance);
    }
}
