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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

    // World ID=0 UUID=b9027423-f7fa-490f-91b2-3d12e6460e07 name="castle_straight" [ Length: 3, Loaded nodes: 3 ]
    //   Waypoint 0: (-0.64, 80.00, -0.82)
    //   Waypoint 1: (-0.55, 80.00, -27.90)
    //   Waypoint 2: (-0.64, 80.00, -0.82)
    // World ID=0 UUID=afd6f331-9e8c-47a7-98c6-98ec9b99e312 name=cs_courtyard_loop [ Length: 7, Loaded nodes: 7 ]
    //   Waypoint 0: (-0.48, 80.00, -21.99)
    //   Waypoint 1: (-11.25, 80.00, -22.17)
    //   Waypoint 2: (-11.27, 80.00, -33.99)
    //   Waypoint 3: (10.48, 80.00, -34.31)
    //   Waypoint 4: (10.44, 80.00, -22.71)
    //   Waypoint 5: (-0.22, 80.00, -22.42)
    //   Waypoint 6: (-0.48, 80.00, -0.51)



    // Gets them into the castle...
    private static final UUID CS_CASTLE_STRAIGHT_UUID = UUID.fromString("b9027423-f7fa-490f-91b2-3d12e6460e07");

    // Loop around the bottom
    private static final UUID CS_COURTYARD_LOOP_UUID = UUID.fromString("afd6f331-9e8c-47a7-98c6-98ec9b99e312");

    

    // Loop around the front lower staircase
    private static final UUID CS_FRONT_LOWER_LOOP_UUID = UUID.fromString("c8a0a8c0-8a10-413a-b35e-5222cda5505a");

    // World ID=0 UUID=c8a0a8c0-8a10-413a-b35e-5222cda5505a name=cs_front_lower_loop [ Length: 12, Loaded nodes: 12 ]
    //   Waypoint 0: (-0.67, 80.00, -27.41)
    //   Waypoint 1: (-6.97, 83.50, -27.54)
    //   Waypoint 2: (-13.67, 88.00, -27.42)
    //   Waypoint 3: (-13.52, 88.00, -20.94)
    //   Waypoint 4: (-10.99, 88.00, -20.81)
    //   Waypoint 5: (-10.81, 90.00, -16.57)
    //   Waypoint 6: (-0.34, 86.00, -16.88)
    //   Waypoint 7: (9.73, 90.00, -16.98)
    //   Waypoint 8: (10.13, 88.00, -21.07)
    //   Waypoint 9: (13.15, 88.00, -21.22)
    //   Waypoint 10: (12.71, 88.00, -27.82)
    //   Waypoint 11: (7.48, 85.00, -27.59)

    private static final UUID[] LOOP_PATH_UUIDS = {
            CS_COURTYARD_LOOP_UUID,
            CS_FRONT_LOWER_LOOP_UUID
    };

    private static final Random RANDOM = new Random();
    private static final double Z_THRESHOLD = -27.0;

    private static final AtomicInteger currentWave = new AtomicInteger(0);
    private static final AtomicInteger totalKills = new AtomicInteger(0);
    // Highest wave number ever cleared, persisted across server restarts.
    private static final AtomicInteger lastDefeatedWave = new AtomicInteger(0);
    private static volatile Path saveFile;

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
    // Mobs that have already crossed the Z threshold and switched to the courtyard loop.
    private static final Set<Ref<EntityStore>> crossedZThreshold = ConcurrentHashMap.newKeySet();
    // Map from mob ref to its NPCEntity, used to switch prefab paths at the Z threshold.
    private static final ConcurrentHashMap<Ref<EntityStore>, NPCEntity> waveMobEntities = new ConcurrentHashMap<>();
    private static volatile ScheduledFuture<?> positionTrackingTask;

    static final ScheduledExecutorService WAVE_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "MobWave-Scheduler");
                t.setDaemon(true);
                return t;
            });

    public static final Map<Integer, List<MobEntry>> WAVE_TABLE = Map.ofEntries(
            Map.entry(1, List.of(
                    new MobEntry("Rat_Wave", 4)
            )),
            Map.entry(2, List.of(
                    new MobEntry("Rat_Wave", 6)
            )),
            Map.entry(3, List.of(
                    new MobEntry("Rat_Wave", 4),
                    new MobEntry("Snake_Rattle_Wave", 4)
            )),
            Map.entry(4, List.of(
                    new MobEntry("Snake_Rattle_Wave", 4),
                    new MobEntry("Skeleton_Wave", 2)
            )),
            Map.entry(5, List.of(
                    new MobEntry("Skeleton_Wave", 4),
                    new MobEntry("Skeleton_Archer_Wave", 4)
            )),
            Map.entry(6, List.of(
                    new MobEntry("Skeleton_Wave", 6),
                    new MobEntry("Skeleton_Archer_Wave", 6)
            )),
            Map.entry(7, List.of(
                    new MobEntry("Skeleton_Wave", 8),
                    new MobEntry("Skeleton_Archer_Wave", 8)
            )),
            Map.entry(8, List.of(
                    new MobEntry("Skeleton_Wave", 10),
                    new MobEntry("Skeleton_Archer_Wave", 10)
            )),
            Map.entry(9, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 2),
                    new MobEntry("Skeleton_Wave", 10),
                    new MobEntry("Skeleton_Archer_Wave", 10)
            )),
            // TODO: boss
            Map.entry(10, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 2),
                    new MobEntry("Snake_Rattle_Wave", 4)
            )),
            Map.entry(11, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 3),
                    new MobEntry("Skeleton_Wave", 4)
            )),
            Map.entry(12, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 4),
                    new MobEntry("Skeleton_Wave", 5)
            )),
            Map.entry(13, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 5),
                    new MobEntry("Skeleton_Wave", 6),
                    new MobEntry("Snake_Rattle_Wave", 5)
            )),
            Map.entry(14, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 5),
                    new MobEntry("Skeleton_Wave", 6),
                    new MobEntry("Rat_Wave", 6)
            )),
            // TODO: boss
            Map.entry(15, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 6),
                    new MobEntry("Skeleton_Wave", 7),
                    new MobEntry("Snake_Rattle_Wave", 7)
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
            // TODO: boss
            Map.entry(20, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_Wave", 10),
                    new MobEntry("Snake_Rattle_Wave", 10),
                    new MobEntry("Skeleton_Wave", 10)
            ))
    );

    private static final int MAX_COLS = 2;
    private static final double ROW_SPACING = 4.0;
    private static final Vector3d SPAWN_ORIGIN = new Vector3d(-0.5, 80.0, 0.5);

    public static int getCurrentWave() {
        return currentWave.get();
    }

    public static int getLastDefeatedWave() {
        return lastDefeatedWave.get();
    }

    public static int getMaxWave() {
        return 20;
    }

    /**
     * Loads the persisted last-defeated wave from disk. Call once during plugin
     * setup with the plugin's data directory. The current wave is initialised to
     * the saved value so the next /cs next picks up where the world left off.
     */
    public static void initPersistence(Path dataDir) {
        try {
            Files.createDirectories(dataDir);
            saveFile = dataDir.resolve("wave_progress.txt");
            if (Files.exists(saveFile)) {
                int wave = Integer.parseInt(Files.readString(saveFile).trim());
                if (wave < 0) wave = 0;
                if (wave > getMaxWave()) wave = getMaxWave();
                lastDefeatedWave.set(wave);
                currentWave.set(wave);
                System.out.println("[CastleSiege] Loaded saved progress: last defeated wave = " + wave);
            } else {
                System.out.println("[CastleSiege] No saved wave progress at " + saveFile);
            }
        } catch (Exception e) {
            System.err.println("[CastleSiege] Failed to load wave progress: " + e.getMessage());
        }
    }

    private static void saveProgress() {
        Path file = saveFile;
        if (file == null) return;
        try {
            Files.writeString(file, Integer.toString(lastDefeatedWave.get()));
        } catch (Exception e) {
            System.err.println("[CastleSiege] Failed to save wave progress: " + e.getMessage());
        }
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
     * Wipes all wave state and despawns tracked wave mobs. Must be called from
     * the world thread because setToDespawn touches NPC entity state.
     */
    public static void resetGame() {
        ScheduledFuture<?> prev = positionTrackingTask;
        if (prev != null) {
            prev.cancel(false);
            positionTrackingTask = null;
        }
        for (NPCEntity npc : waveMobEntities.values()) {
            npc.setToDespawn();
        }
        currentWave.set(0);
        totalKills.set(0);
        currentWaveKills.set(0);
        currentWaveTotalMobs.set(0);
        currentWaveMobs.clear();
        waveMobEntities.clear();
        crossedZThreshold.clear();
        playerKills.clear();
        playerDeaths.clear();
        lastDefeatedWave.set(0);
        saveProgress();
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
            if (wave > lastDefeatedWave.get()) {
                lastDefeatedWave.set(wave);
                saveProgress();
            }
            WaveRewards.awardWaveEnd(wave, store);
            WaveRewards.healAllPlayersToFull(store);

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
        waveMobEntities.clear();

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

        // Look up prefab paths: straight approach and all loop variants.
        WorldPathData pathData = store.getResource(WorldPathData.getResourceType());
        IPrefabPath straightPath = null;
        Map<UUID, IPrefabPath> loopPaths = new java.util.HashMap<>();
        if (pathData != null) {
            for (IPrefabPath p : pathData.getAllPrefabPaths()) {
                if (p.getId().equals(CS_CASTLE_STRAIGHT_UUID)) {
                    straightPath = p;
                } else {
                    for (UUID loopUuid : LOOP_PATH_UUIDS) {
                        if (p.getId().equals(loopUuid)) {
                            loopPaths.put(loopUuid, p);
                        }
                    }
                }
            }
        }
        if (straightPath == null) {
            messageSender.accept("ERROR: Straight prefab path not found: " + CS_CASTLE_STRAIGHT_UUID);
            return;
        }
        for (UUID loopUuid : LOOP_PATH_UUIDS) {
            if (!loopPaths.containsKey(loopUuid)) {
                messageSender.accept("ERROR: Loop prefab path not found: " + loopUuid);
                return;
            }
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
                        waveMobEntities.put(result.first(), npcEntity);
                        currentWaveTotalMobs.incrementAndGet();
                        spawnCount++;
                        if (world == null) {
                            world = npcEntity.getWorld();
                        }

                        IPrefabPath startPath = straightPath;
                        WAVE_SCHEDULER.schedule(
                                () -> npcEntity.getPathManager().setPrefabPath(CS_CASTLE_STRAIGHT_UUID, startPath),
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
            startPositionTracking(store, world, loopPaths);
        }
    }

    private static void startPositionTracking(Store<EntityStore> store, World world,
                                              Map<UUID, IPrefabPath> loopPaths) {
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
                            NPCEntity npc = waveMobEntities.get(mobRef);
                            if (npc != null) {
                                UUID chosenUuid = LOOP_PATH_UUIDS[RANDOM.nextInt(LOOP_PATH_UUIDS.length)];
                                IPrefabPath chosenPath = loopPaths.get(chosenUuid);
                                npc.getPathManager().setPrefabPath(chosenUuid, chosenPath);
                                System.out.printf("[CastleSiege] Mob entered castle (z=%.2f), assigned to loop %s%n",
                                        pos.z, chosenPath.getName());
                            }
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
                origin.x + 4*(col - halfCols),
                origin.y,
                origin.z + (row * ROW_SPACING)
        );
    }

}
