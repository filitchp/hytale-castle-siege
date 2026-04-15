package dev.dooondi.wave;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.builtin.path.WorldPathData;
import com.hypixel.hytale.builtin.path.path.IPrefabPath;
import it.unimi.dsi.fastutil.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class WaveManager {

    public record MobEntry(String name, int count) {}

    private static final UUID COURTYARD_LOOP_UUID = UUID.fromString("afd6f331-9e8c-47a7-98c6-98ec9b99e312");
    private static final double Z_THRESHOLD = -20.0;

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
    // Per-player death counters keyed by player UUID (lifetime across all waves).
    private static final ConcurrentHashMap<UUID, AtomicInteger> playerDeaths = new ConcurrentHashMap<>();
    // Mobs that have already crossed the Z threshold (prevents duplicate messages).
    private static final Set<Ref<EntityStore>> crossedZThreshold = ConcurrentHashMap.newKeySet();
    private static volatile ScheduledFuture<?> positionTrackingTask;

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

    private static final int MAX_COLS = 2;
    private static final double ROW_SPACING = 8.0;
    private static final Vector3d SPAWN_ORIGIN = new Vector3d(-0.5, 80.0, 0.5);

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

    public static int getPlayerDeaths(UUID playerUuid) {
        AtomicInteger counter = playerDeaths.get(playerUuid);
        return counter == null ? 0 : counter.get();
    }

    public static void recordPlayerDeath(UUID playerUuid) {
        playerDeaths.computeIfAbsent(playerUuid, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Called from MobDeathTracker when a tracked wave mob dies.
     * Removes the mob from the alive set, bumps wave+lifetime counters,
     * attributes the kill to the player UUID if provided, and hands out
     * end-of-wave rewards when the last tracked mob is cleared.
     */
    public static void recordMobDeath(Ref<EntityStore> mobRef, UUID killerUuid,
                                      Store<EntityStore> store) {
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

        // Last mob in the wave just died — hand out end-of-wave rewards and show title.
        if (currentWaveMobs.isEmpty()) {
            int wave = currentWave.get();
            WaveRewards.awardWaveEnd(wave, store);

            EventTitleUtil.showEventTitleToWorld(
                    Message.raw("Wave " + wave + " / " + getMaxWave() + " Defeated!"),
                    Message.raw(""),
                    true,
                    EventTitleUtil.DEFAULT_ZONE,
                    EventTitleUtil.DEFAULT_DURATION,
                    EventTitleUtil.DEFAULT_FADE_DURATION,
                    EventTitleUtil.DEFAULT_FADE_DURATION,
                    store);
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
            playerDeaths.clear();
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
        crossedZThreshold.clear();

        // Hand out start-of-wave rewards before any mobs spawn.
        WaveRewards.awardWaveStart(waveNumber, store);

        // Show title to all players in the world.
        EventTitleUtil.showEventTitleToWorld(
                Message.raw("Starting Wave " + waveNumber + " / " + getMaxWave()),
                Message.raw(""),
                true,
                EventTitleUtil.DEFAULT_ZONE,
                EventTitleUtil.DEFAULT_DURATION,
                EventTitleUtil.DEFAULT_FADE_DURATION,
                EventTitleUtil.DEFAULT_FADE_DURATION,
                store);

        var rotation = new Vector3f(0f, 0f, 0f);

        // Look up the prefab path for wave mobs.
        WorldPathData pathData = store.getResource(WorldPathData.getResourceType());
        IPrefabPath prefabPath = null;
        if (pathData != null) {
            for (IPrefabPath p : pathData.getAllPrefabPaths()) {
                if (p.getId().equals(COURTYARD_LOOP_UUID)) {
                    prefabPath = p;
                    break;
                }
            }
        }
        if (prefabPath == null) {
            messageSender.accept("ERROR: Wave prefab path not found: " + COURTYARD_LOOP_UUID);
            return;
        }

        // Spawn all mobs and send them on the prefab path immediately.
        int spawnCount = 0;
        int mobIndex = 0;
        World world = null;
        for (MobEntry entry : entries) {
            for (int i = 0; i < entry.count(); i++) {
                Vector3d pos = computeSpawnPosition(SPAWN_ORIGIN, mobIndex);

                if (debug) {
                    messageSender.accept("Spawning '" + entry.name() + "' at ("
                            + pos.x + ", " + pos.y + ", " + pos.z + ")");
                }

                Pair<Ref<EntityStore>, INonPlayerCharacter> result =
                        NPCPlugin.get().spawnNPC(store, entry.name(), null, pos, rotation);

                if (result != null) {
                    NPCEntity npcEntity = store.getComponent(result.first(), NPCEntity.getComponentType());

                    if (npcEntity != null) {
                        currentWaveMobs.add(result.first());
                        currentWaveTotalMobs.incrementAndGet();
                        spawnCount++;
                        if (world == null) {
                            world = npcEntity.getWorld();
                        }

                        IPrefabPath pathRef = prefabPath;
                        WAVE_SCHEDULER.schedule(
                                () -> npcEntity.getPathManager().setPrefabPath(COURTYARD_LOOP_UUID, pathRef),
                                500, TimeUnit.MILLISECONDS
                        );
                    }
                } else if (debug) {
                    messageSender.accept("Failed to spawn " + entry.name() + " at ("
                            + pos.x + ", " + pos.y + ", " + pos.z + ")");
                }

                mobIndex++;
            }
        }

        messageSender.accept("Wave " + waveNumber + " started! " + spawnCount + " mobs spawned.");

        if (world != null) {
            startPositionTracking(store, world);
        }
    }

    private static void startPositionTracking(Store<EntityStore> store, World world) {
        ScheduledFuture<?> prev = positionTrackingTask;
        if (prev != null) {
            prev.cancel(false);
        }
        positionTrackingTask = WAVE_SCHEDULER.scheduleAtFixedRate(() -> {
            // Enqueue the store read onto the world thread.
            world.execute(() -> {
                try {
                    for (Ref<EntityStore> mobRef : currentWaveMobs) {
                        if (crossedZThreshold.contains(mobRef)) {
                            continue;
                        }
                        TransformComponent transform = store.getComponent(mobRef, TransformComponent.getComponentType());
                        if (transform == null) {
                            continue;
                        }
                        Vector3d pos = transform.getPosition();
                        if (pos.z <= Z_THRESHOLD) {
                            crossedZThreshold.add(mobRef);
                            System.out.printf("[CastleSiege] Mob %s crossed Z threshold: z=%.2f at (%.2f, %.2f, %.2f)%n",
                                    mobRef, Z_THRESHOLD, pos.x, pos.y, pos.z);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[CastleSiege] Position tracking error: " + e.getMessage());
                }
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    static Vector3d computeSpawnPosition(Vector3d origin, int index) {
        int row = index / MAX_COLS;
        int col = index % MAX_COLS;
        double halfCols = (MAX_COLS - 1) / 2.0;
        return new Vector3d(
                origin.x + 2*(col - halfCols),
                origin.y,
                origin.z + (row * ROW_SPACING)
        );
    }

}
