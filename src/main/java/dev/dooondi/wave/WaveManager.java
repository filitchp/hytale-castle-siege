package dev.dooondi.wave;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.builtin.path.WorldPathData;
import com.hypixel.hytale.builtin.path.path.IPrefabPath;
import dev.dooondi.ui.WaveHUD;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class WaveManager {

    public record MobEntry(String name, int count) {}

    // ---------------------------------
    //     Charge forward (all mobs)
    // ---------------------------------

    // World ID=0 UUID=b9027423-f7fa-490f-91b2-3d12e6460e07 name="castle_straight" [ Length: 3, Loaded nodes: 3 ]
    //   Waypoint 0: (-0.64, 80.00, -0.82)
    //   Waypoint 1: (-0.55, 80.00, -27.90)
    //   Waypoint 2: (-0.64, 80.00, -0.82)

    // These paths get mobs them into the castle...
    // They have different starting points so that mobs are close enough to the beginning, but also in front of the
    // mobs so they do not go backwards before going forwards...
    private static final UUID CS_CHARGE_CASTLE_CLOSE_UUID = UUID.fromString("b9027423-f7fa-490f-91b2-3d12e6460e07");
    private static final UUID CS_CHARGE_CASTLE_MED_UUID = UUID.fromString("0f035977-e728-4bfb-b10d-f4365b08ca7b");
    private static final UUID CS_CHARGE_CASTLE_FAR_UUID = UUID.fromString("4e439313-50fc-4ab4-b2fe-a0d0c8eac513");

    // ---------------------------------
    //      Melee Mob AI Node Hints
    // ---------------------------------
    // Loop around the bottom courtyard of the castle (no stairs)
    // World ID=0 UUID=afd6f331-9e8c-47a7-98c6-98ec9b99e312 name=cs_courtyard_loop [ Length: 7, Loaded nodes: 7 ]
    //   Waypoint 0: (-0.48, 80.00, -21.99)
    //   Waypoint 1: (-11.25, 80.00, -22.17)
    //   Waypoint 2: (-11.27, 80.00, -33.99)
    //   Waypoint 3: (10.48, 80.00, -34.31)
    //   Waypoint 4: (10.44, 80.00, -22.71)
    //   Waypoint 5: (-0.22, 80.00, -22.42)
    //   Waypoint 6: (-0.48, 80.00, -0.51)
    private static final UUID CS_COURTYARD_LOOP_UUID = UUID.fromString("afd6f331-9e8c-47a7-98c6-98ec9b99e312");

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

    // From the castle courtyard walk up the left staircase and do a loop around through the front/center crafting room
    // and back down the right staircase
    private static final UUID CS_FRONT_LOWER_LOOP_UUID = UUID.fromString("c8a0a8c0-8a10-413a-b35e-5222cda5505a");

    // From the castle courtyard walk up the right staircase and do a loop around through the elevated rear quarters
    // and back down the left staircase.
    // World ID=0 UUID=c7576cff-10ab-4fe7-b5b8-1d5708ff8f5a name=cs_front_upper_loop [ Length: 14, Loaded nodes: 14 ]
    // Waypoint 0: (-0.54, 80.00, -27.77)
    // Waypoint 1: (5.27, 83.00, -27.46)
    // Waypoint 2: (12.71, 88.00, -27.46)
    // Waypoint 3: (12.88, 88.00, -33.90)
    // Waypoint 4: (9.85, 88.00, -34.00)
    // Waypoint 5: (9.79, 88.00, -36.80)
    // Waypoint 6: (6.36, 90.00, -36.71)
    // Waypoint 7: (-0.01, 94.00, -37.06)
    // Waypoint 8: (-6.38, 91.00, -36.64)
    // Waypoint 9: (-10.97, 88.00, -36.59)
    // Waypoint 10: (-11.11, 88.00, -33.76)
    // Waypoint 11: (-14.19, 88.00, -33.80)
    // Waypoint 12: (-13.86, 88.00, -27.72)
    // Waypoint 13: (-7.83, 84.50, -27.55)
    private static final UUID CS_FRONT_UPPER_LOOP_UUID = UUID.fromString("c7576cff-10ab-4fe7-b5b8-1d5708ff8f5a");

    // ---------------------------
    //     Archer AI Node Hints
    // ---------------------------
    // World ID=0 UUID=c015894c-08d7-4272-a763-93c33354c08d name=cs_flank_right [ Length: 23, Loaded nodes: 23 ]
    // Waypoint 0: (-0.34, 80.00, 3.88)
    // Waypoint 1: (9.29, 80.00, 2.47)
    // Waypoint 2: (16.24, 80.00, -0.04)
    // Waypoint 3: (23.26, 80.00, -3.06)
    // Waypoint 4: (24.20, 80.00, -9.87)
    // Waypoint 5: (24.17, 80.00, -18.44)
    // Waypoint 6: (24.13, 80.00, -28.07)
    // Waypoint 7: (24.09, 80.00, -39.07)
    // Waypoint 8: (24.05, 80.00, -49.84)
    // Waypoint 9: (17.77, 80.00, -49.99)
    // Waypoint 10: (11.40, 80.00, -50.15)
    // Waypoint 11: (1.55, 80.00, -50.40)
    // Waypoint 12: (-8.67, 80.00, -50.48)
    // Waypoint 13: (-17.74, 80.00, -50.55)
    // Waypoint 14: (-23.83, 80.00, -50.60)
    // Waypoint 15: (-24.30, 80.00, -43.98)
    // Waypoint 16: (-24.36, 80.00, -35.68)
    // Waypoint 17: (-24.42, 80.00, -27.11)
    // Waypoint 18: (-24.49, 80.00, -17.26)
    // Waypoint 19: (-24.53, 80.00, -10.66)
    // Waypoint 20: (-24.57, 80.00, -4.93)
    // Waypoint 21: (-19.64, 80.00, -2.16)
    // Waypoint 22: (-11.18, 80.00, 0.91)

    // Flank castle left for archers. Around the castle
    private static final UUID CS_FLANK_LEFT_UUID = UUID.fromString("5aa77ec0-0fce-4222-a82b-095c12b5e3f2");

    // Flank castle right for archers. Around the castle
    private static final UUID CS_FLANK_RIGHT_UUID = UUID.fromString("c015894c-08d7-4272-a763-93c33354c08d");

    private static final UUID[] INNER_CASTLE_LOOP_PATHS = {
            CS_COURTYARD_LOOP_UUID,
            CS_FRONT_LOWER_LOOP_UUID,
            CS_FRONT_UPPER_LOOP_UUID
    };

    // Archers entering the outside path are randomly routed onto one of these flanks.
    private static final UUID[] FLANK_PATH_UUIDS = {
            CS_FLANK_LEFT_UUID,
            CS_FLANK_RIGHT_UUID
    };

    private static final Random RANDOM = new Random();
    // Castle courtyard rectangle (XZ). A mob is considered "in the castle" once
    // its position falls inside [COURTYARD_X_MIN, COURTYARD_X_MAX] x [COURTYARD_Z_MIN, COURTYARD_Z_MAX].
    private static final double COURTYARD_X_MIN = -14.0;
    private static final double COURTYARD_X_MAX = 14.0;
    private static final double COURTYARD_Z_MIN = -39.0;
    private static final double COURTYARD_Z_MAX = -27.0;
    // Charge-path tiering: a mob's distance behind the spawn origin (in +z) decides
    // which approach path it gets routed onto. (origin, MED] → med, (MED, FAR] → far.
    private static final double MED_SPAWN_Z_THRESHOLD = 11.0;
    private static final double FAR_SPAWN_Z_THRESHOLD = 26.0;

    // "Outside path" rectangle (XZ). Archers spawned inside this box get sent on
    // either the left-flank or right-flank path instead of charging straight down the center.
    private static final double OUTSIDE_X_MIN = -6.0;
    private static final double OUTSIDE_X_MAX = 6.0;
    private static final double OUTSIDE_Z_MIN = 0.0;
    private static final double OUTSIDE_Z_MAX = 6.0;

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
    // Archers that have already been redirected onto the left flank path.
    private static final Set<Ref<EntityStore>> flankedArchers = ConcurrentHashMap.newKeySet();
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
                    new MobEntry("Rat_CS", 4)
            )),
            Map.entry(2, List.of(
                    new MobEntry("Rat_CS", 6)
            )),
            Map.entry(3, List.of(
                    new MobEntry("Rat_CS", 4),
                    new MobEntry("Snake_Rattle_CS", 4)
            )),
            Map.entry(4, List.of(
                    new MobEntry("Rat_CS", 4),
                    new MobEntry("Snake_Rattle_CS", 4),
                    new MobEntry("Skeleton_Weak_CS", 2)
            )),
            Map.entry(5, List.of(
                    new MobEntry("Skeleton_Weak_CS", 4),
                    new MobEntry("Skeleton_Archer_Weak_CS", 2)
            )),
            Map.entry(6, List.of(
                    new MobEntry("Skeleton_Weak_CS", 4),
                    new MobEntry("Skeleton_Archer_Weak_CS", 4),
                    new MobEntry("Skeleton_Archer_Sturdy_CS", 4)
            )),
            Map.entry(7, List.of(
                    new MobEntry("Snake_Rattle_CS", 2),
                    new MobEntry("Skeleton_Weak_CS", 4),
                    new MobEntry("Skeleton_Archer_Weak_CS", 4)
            )),
            Map.entry(8, List.of(
                    new MobEntry("Skeleton_Weak_CS", 6),
                    new MobEntry("Skeleton_Archer_Weak_CS", 6)
            )),
            Map.entry(9, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_CS", 2),
                    new MobEntry("Skeleton_Weak_CS", 6),
                    new MobEntry("Skeleton_Archer_Weak_CS", 6)
            )),
            // TODO: boss
            Map.entry(10, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_CS", 1),
                    new MobEntry("Skeleton_Pirate_Gunner_CS", 1),
                    new MobEntry("Skeleton_Pirate_Striker_CS", 1),
                    new MobEntry("Snake_Rattle_CS", 4)
            )),
            Map.entry(11, List.of(
                    new MobEntry("Skeleton_Pirate_Captain_CS", 1),
                    new MobEntry("Skeleton_Pirate_Gunner_CS", 1),
                    new MobEntry("Skeleton_Pirate_Striker_CS", 2),
                    new MobEntry("Skeleton_Weak_CS", 4)
            )),
            Map.entry(12, List.of(
                    new MobEntry("Skeleton_Sturdy_CS", 2),
                    new MobEntry("Skeleton_Pirate_Gunner_CS", 2),
                    new MobEntry("Skeleton_Pirate_Striker_CS", 2),
                    new MobEntry("Skeleton_Pirate_Captain_CS", 1)
            )),
            Map.entry(13, List.of(
                    new MobEntry("Skeleton_Archer_Weak_CS", 3),
                    new MobEntry("Skeleton_Sturdy_CS", 3),
                    new MobEntry("Skeleton_Pirate_Gunner_CS", 2),
                    new MobEntry("Skeleton_Pirate_Striker_CS", 4),
                    new MobEntry("Skeleton_Pirate_Captain_CS", 1)
            )),
            Map.entry(14, List.of(
                    new MobEntry("Skeleton_Archer_Sturdy_CS", 3),
                    new MobEntry("Skeleton_Sturdy_CS", 3),
                    new MobEntry("Skeleton_Pirate_Gunner_CS", 3),
                    new MobEntry("Skeleton_Pirate_Striker_CS", 4),
                    new MobEntry("Skeleton_Pirate_Captain_CS", 1)
            )),
            Map.entry(15, List.of(
                    new MobEntry("Skeleton_Archer_Sturdy_CS", 3),
                    new MobEntry("Skeleton_Pirate_Gunner_CS", 3),
                    new MobEntry("Skeleton_Pirate_Captain_CS", 1),
                    new MobEntry("Skeleton_Sturdy_CS", 6)
            )),
            Map.entry(16, List.of(
                    new MobEntry("Skeleton_Archer_Weak_CS", 6),
                    new MobEntry("Skeleton_Archer_Sturdy_CS", 6),
                    new MobEntry("Spawn_Void_CS", 1),
                    new MobEntry("Skeleton_Sturdy_CS", 6)
            )),
            Map.entry(17, List.of(
                    new MobEntry("Skeleton_Archer_Weak_CS", 8),
                    new MobEntry("Skeleton_Archer_Sturdy_CS", 6),
                    new MobEntry("Spawn_Void_CS", 2),
                    new MobEntry("Skeleton_Sturdy_CS", 8)
            )),
            Map.entry(18, List.of(
                    new MobEntry("Skeleton_Archer_Weak_CS", 10),
                    new MobEntry("Skeleton_Archer_Sturdy_CS", 6),
                    new MobEntry("Spawn_Void_CS", 2),
                    new MobEntry("Skeleton_Sturdy_CS", 8)
            )),
            Map.entry(19, List.of(
                    new MobEntry("Skeleton_Archer_Weak_CS", 10),
                    new MobEntry("Skeleton_Archer_Sturdy_CS", 3),
                    new MobEntry("Skeleton_Pirate_Gunner_CS", 3),
                    new MobEntry("Skeleton_Pirate_Captain_CS", 1),
                    new MobEntry("Skeleton_Sturdy_CS", 6)
            )),
            Map.entry(20, List.of(
                    new MobEntry("Skeleton_Archer_Weak_CS", 10),
                    new MobEntry("Skeleton_Burnt_Praetorian_CS", 1)
            ))
    );

    // Boss summon happens at the courtyard center because the Burnt Praetorian
    // is too tall to fit under the castle gate.
    private static final String BOSS_ROLE = "Skeleton_Burnt_Praetorian_CS";
    private static final Vector3d BOSS_SPAWN_POS = new Vector3d(0.0, 80.0, -27.5); // Courtyard center
    private static final long BOSS_SPAWN_DELAY_MS = 4000;
    private static final String BOSS_PARTICLE = "Praetorian_Summon_Energy";
    private static final float BOSS_PARTICLE_SCALE = 4.0f;
    private static final Color BOSS_PARTICLE_COLOR = new Color((byte) 0xFF, (byte) 0xFF, (byte) 0xFF);
    private static final AtomicBoolean pendingBoss = new AtomicBoolean(false);

    private static final int MAX_COLS = 3;
    private static final double ROW_SPACING = 3.0;
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

    public record MobCombatStats(double attackDistance, double dps) {}
    public record WaveStatsSummary(int totalMobs, double meleeDps, double rangedDps,
                                   List<String> unknownRoles) {}

    private static final double RANGED_DISTANCE_THRESHOLD = 5.0;

    // Per-role attack distance pulled straight from each Wave/*.json. DPS values are
    // approximations: weapon-base damage / typical swing-or-shot interval. Update
    // when new wave roles land or when balance shifts.
    private static final Map<String, MobCombatStats> MOB_STATS = Map.ofEntries(
            Map.entry("Rat_CS",                       new MobCombatStats(2.0,  2.0)),  // bite
            Map.entry("Snake_Rattle_CS",              new MobCombatStats(2.0,  3.0)),  // bite
            Map.entry("Skeleton_Weak_CS",             new MobCombatStats(3.0,  5.0)),  // bone sword
            Map.entry("Skeleton_Sturdy_CS",           new MobCombatStats(3.5,  8.0)),  // iron battleaxe
            Map.entry("Skeleton_Pirate_Captain_CS",   new MobCombatStats(3.0,  7.0)),  // cutlass
            Map.entry("Skeleton_Pirate_Striker_CS",   new MobCombatStats(3.0,  7.0)),  // cutlass
            Map.entry("Skeleton_Pirate_Gunner_CS",    new MobCombatStats(15.0, 5.0)),  // blunderbuss
            Map.entry("Skeleton_Archer_Weak_CS",      new MobCombatStats(25.0, 3.5)),  // rusty shortbow
            Map.entry("Skeleton_Archer_Sturdy_CS",    new MobCombatStats(25.0, 5.0)),  // iron shortbow
            Map.entry("Skeleton_Burnt_Praetorian_CS", new MobCombatStats(4.0, 18.0))   // praetorian longsword + charge
    );

    public static WaveStatsSummary computeWaveStats(int waveNumber) {
        List<MobEntry> entries = WAVE_TABLE.get(waveNumber);
        if (entries == null) return new WaveStatsSummary(0, 0.0, 0.0, List.of());
        int total = 0;
        double melee = 0.0;
        double ranged = 0.0;
        java.util.List<String> unknown = new java.util.ArrayList<>();
        for (MobEntry e : entries) {
            total += e.count();
            MobCombatStats s = MOB_STATS.get(e.name());
            if (s == null) {
                unknown.add(e.name());
                continue;
            }
            double dps = s.dps() * e.count();
            if (s.attackDistance() > RANGED_DISTANCE_THRESHOLD) {
                ranged += dps;
            } else {
                melee += dps;
            }
        }
        return new WaveStatsSummary(total, melee, ranged, unknown);
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

    public static int getMobsRemaining() {
        return currentWaveMobs.size();
    }

    public static boolean isWaveInProgress() {
        return !currentWaveMobs.isEmpty() || pendingBoss.get();
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
    public static void resetGame(Store<EntityStore> store) {
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
        flankedArchers.clear();
        playerKills.clear();
        playerDeaths.clear();
        pendingBoss.set(false);
        lastDefeatedWave.set(0);
        saveProgress();
        refreshAllWaveHuds(store);
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

        refreshAllWaveHuds(store);

        // Last mob in the wave just died — hand out end-of-wave rewards and show title.
        // Don't fire end-of-wave logic if a boss summon is mid-flight; wait for him to land.
        if (currentWaveMobs.isEmpty() && !pendingBoss.get()) {
            int wave = currentWave.get();
            if (wave > lastDefeatedWave.get()) {
                lastDefeatedWave.set(wave);
                saveProgress();
            }
            WaveRewards.awardWaveEnd(wave, store);
            WaveRewards.healAllPlayersToFull(store);
            refreshAllWaveHuds(store);

            boolean finalWave = wave >= getMaxWave();
            String subtitle = finalWave ? "Congrats, you beat Castle Siege!" : "";
            EventTitleUtil.showEventTitleToWorld(
                    Message.raw("Wave " + wave + " / " + getMaxWave() + " Complete!"),
                    Message.raw(subtitle),
                    true,
                    EventTitleUtil.DEFAULT_ZONE,
                    EventTitleUtil.DEFAULT_DURATION,
                    EventTitleUtil.DEFAULT_FADE_DURATION,
                    EventTitleUtil.DEFAULT_FADE_DURATION,
                    store);

            if (finalWave) {
                playVictorySound(store);
            }
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

        currentWave.set(waveNumber);

        // Reset wave-scoped tracking.
        currentWaveMobs.clear();
        currentWaveKills.set(0);
        currentWaveTotalMobs.set(0);
        crossedZThreshold.clear();
        flankedArchers.clear();
        waveMobEntities.clear();
        pendingBoss.set(false);

        // Hand out start-of-wave rewards before any mobs spawn.
        WaveRewards.awardWaveStart(waveNumber, store);

        playWaveStartSound(store);
        refreshAllWaveHuds(store);

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

        // Look up prefab paths: close, med, far approach, and all loop variants.
        WorldPathData pathData = store.getResource(WorldPathData.getResourceType());
        IPrefabPath straightPath = null;
        IPrefabPath chargeMedPath = null;
        IPrefabPath chargeFarPath = null;
        Map<UUID, IPrefabPath> flankPaths = new java.util.HashMap<>();
        Map<UUID, IPrefabPath> loopPaths = new java.util.HashMap<>();
        if (pathData != null) {
            for (IPrefabPath p : pathData.getAllPrefabPaths()) {
                if (p.getId().equals(CS_CHARGE_CASTLE_CLOSE_UUID)) {
                    straightPath = p;
                } else if (p.getId().equals(CS_CHARGE_CASTLE_MED_UUID)) {
                    chargeMedPath = p;
                } else if (p.getId().equals(CS_CHARGE_CASTLE_FAR_UUID)) {
                    chargeFarPath = p;
                } else {
                    for (UUID flankUuid : FLANK_PATH_UUIDS) {
                        if (p.getId().equals(flankUuid)) {
                            flankPaths.put(flankUuid, p);
                        }
                    }
                    for (UUID loopUuid : INNER_CASTLE_LOOP_PATHS) {
                        if (p.getId().equals(loopUuid)) {
                            loopPaths.put(loopUuid, p);
                        }
                    }
                }
            }
        }
        if (straightPath == null) {
            messageSender.accept("ERROR: Straight prefab path not found: " + CS_CHARGE_CASTLE_CLOSE_UUID);
            return;
        }
        if (chargeMedPath == null) {
            messageSender.accept("ERROR: Charge-med prefab path not found: " + CS_CHARGE_CASTLE_MED_UUID);
            return;
        }
        if (chargeFarPath == null) {
            messageSender.accept("ERROR: Charge-far prefab path not found: " + CS_CHARGE_CASTLE_FAR_UUID);
            return;
        }
        for (UUID flankUuid : FLANK_PATH_UUIDS) {
            if (!flankPaths.containsKey(flankUuid)) {
                messageSender.accept("ERROR: Flank prefab path not found: " + flankUuid);
                return;
            }
        }
        for (UUID loopUuid : INNER_CASTLE_LOOP_PATHS) {
            if (!loopPaths.containsKey(loopUuid)) {
                messageSender.accept("ERROR: Loop prefab path not found: " + loopUuid);
                return;
            }
        }

        // Spawn all mobs and send them on the prefab path immediately.
        int spawnCount = 0;
        int mobIndex = 0;
        World world = null;
        boolean hasBoss = false;
        for (MobEntry entry : entries) {
            if (BOSS_ROLE.equals(entry.name())) {
                hasBoss = true;
                continue; // Boss is spawned separately with particles + delay.
            }
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

                        // Every mob first heads toward the castle on a charge path.
                        // Branching to flank/loop paths is handled at runtime by
                        // startPositionTracking once their position changes.
                        double zDist = pos.z - SPAWN_ORIGIN.z;
                        UUID startUuid;
                        IPrefabPath startPath;
                        if (zDist > FAR_SPAWN_Z_THRESHOLD) {
                            startUuid = CS_CHARGE_CASTLE_FAR_UUID;
                            startPath = chargeFarPath;
                        } else if (zDist > MED_SPAWN_Z_THRESHOLD) {
                            startUuid = CS_CHARGE_CASTLE_MED_UUID;
                            startPath = chargeMedPath;
                        } else {
                            startUuid = CS_CHARGE_CASTLE_CLOSE_UUID;
                            startPath = straightPath;
                        }

                        // TODO: is scheduler really needed here?
                        WAVE_SCHEDULER.schedule(
                                () -> npcEntity.getPathManager().setPrefabPath(startUuid, startPath),
                                100, TimeUnit.MILLISECONDS
                        );
                    }
                } else if (debug) {
                    messageSender.accept("Failed to spawn " + entry.name() + " at ("
                            + pos.x + ", " + pos.y + ", " + pos.z + ")");
                }

                mobIndex++;
            }
        }

//        messageSender.accept("Wave " + waveNumber + " started! " + spawnCount + " mobs spawned.");

        if (world != null) {
            startPositionTracking(store, world, loopPaths, flankPaths);
            if (hasBoss) {
                IPrefabPath bossLoopPath = loopPaths.get(CS_COURTYARD_LOOP_UUID);
                scheduleBossSpawn(store, world, waveNumber, bossLoopPath, messageSender);
            }
        }
    }

    private static void scheduleBossSpawn(Store<EntityStore> store, World world,
                                          int waveNumber, IPrefabPath bossLoopPath,
                                          Consumer<String> messageSender) {
        pendingBoss.set(true);
        spawnBossSummonParticles(store);
        messageSender.accept("A dreadful chill fills the courtyard...");

        WAVE_SCHEDULER.schedule(() -> world.execute(() -> {
            try {
                if (currentWave.get() != waveNumber) {
                    pendingBoss.set(false);
                    return;
                }
                Vector3f rotation = new Vector3f(0f, 0f, 0f);
                Pair<Ref<EntityStore>, INonPlayerCharacter> result =
                        NPCPlugin.get().spawnNPC(store, BOSS_ROLE, null, BOSS_SPAWN_POS, rotation);
                if (result == null) {
                    pendingBoss.set(false);
                    messageSender.accept("ERROR: Boss failed to spawn.");
                    return;
                }
                NPCEntity npcEntity = store.getComponent(result.first(), NPCEntity.getComponentType());
                if (npcEntity == null) {
                    pendingBoss.set(false);
                    messageSender.accept("ERROR: Boss spawned without NPCEntity component.");
                    return;
                }
                currentWaveMobs.add(result.first());
                waveMobEntities.put(result.first(), npcEntity);
                currentWaveTotalMobs.incrementAndGet();
                // Boss already in courtyard; mark as crossed so the position tracker leaves him alone.
                crossedZThreshold.add(result.first());
                pendingBoss.set(false);
                refreshAllWaveHuds(store);
                messageSender.accept("THE BURNT PRAETORIAN HAS RISEN!");

                if (bossLoopPath != null) {
                    WAVE_SCHEDULER.schedule(
                            () -> npcEntity.getPathManager().setPrefabPath(CS_COURTYARD_LOOP_UUID, bossLoopPath),
                            500, TimeUnit.MILLISECONDS
                    );
                }
            } catch (Exception e) {
                pendingBoss.set(false);
                System.err.println("[CastleSiege] Boss spawn failed: " + e.getMessage());
            }
        }), BOSS_SPAWN_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private static void spawnBossSummonParticles(Store<EntityStore> store) {
        // Convenience overloads don't expose particle scale, so collect player refs
        // ourselves and call the scale-aware variant directly.
        java.util.List<Ref<EntityStore>> playerRefs = new java.util.ArrayList<>();
        store.forEachChunk(Player.getComponentType(), (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                playerRefs.add(chunk.getReferenceTo(i));
            }
        });

        spawnScaledParticle(BOSS_SPAWN_POS, playerRefs, store);
        double r = 1.5;
        spawnScaledParticle(new Vector3d(BOSS_SPAWN_POS.x + r, BOSS_SPAWN_POS.y, BOSS_SPAWN_POS.z), playerRefs, store);
        spawnScaledParticle(new Vector3d(BOSS_SPAWN_POS.x - r, BOSS_SPAWN_POS.y, BOSS_SPAWN_POS.z), playerRefs, store);
        spawnScaledParticle(new Vector3d(BOSS_SPAWN_POS.x, BOSS_SPAWN_POS.y, BOSS_SPAWN_POS.z + r), playerRefs, store);
        spawnScaledParticle(new Vector3d(BOSS_SPAWN_POS.x, BOSS_SPAWN_POS.y, BOSS_SPAWN_POS.z - r), playerRefs, store);
    }

    private static void spawnScaledParticle(Vector3d pos, java.util.List<Ref<EntityStore>> playerRefs,
                                            Store<EntityStore> store) {
        ParticleUtil.spawnParticleEffect(BOSS_PARTICLE, pos,
                0f, 0f, 0f, BOSS_PARTICLE_SCALE, BOSS_PARTICLE_COLOR, playerRefs, store);
    }

    private static void startPositionTracking(Store<EntityStore> store, World world,
                                              Map<UUID, IPrefabPath> loopPaths,
                                              Map<UUID, IPrefabPath> flankPaths) {
        ScheduledFuture<?> prev = positionTrackingTask;
        if (prev != null) {
            prev.cancel(false);
        }
        positionTrackingTask = WAVE_SCHEDULER.scheduleAtFixedRate(() -> {
            // Enqueue the store read onto the world thread.
            world.execute(() -> {
                for (Ref<EntityStore> mobRef : currentWaveMobs) {
                    try {
                        if (crossedZThreshold.contains(mobRef)) {
                            continue;
                        }
                        TransformComponent transform = store.getComponent(mobRef, TransformComponent.getComponentType());
                        if (transform == null) {
                            continue;
                        }
                        Vector3d pos = transform.getPosition();
                        if (pos.x >= COURTYARD_X_MIN && pos.x <= COURTYARD_X_MAX
                                && pos.z >= COURTYARD_Z_MIN && pos.z <= COURTYARD_Z_MAX) {
                            crossedZThreshold.add(mobRef);
                            NPCEntity npc = waveMobEntities.get(mobRef);
                            if (npc != null) {
                                UUID chosenUuid = INNER_CASTLE_LOOP_PATHS[RANDOM.nextInt(INNER_CASTLE_LOOP_PATHS.length)];
                                IPrefabPath chosenPath = loopPaths.get(chosenUuid);
                                npc.getPathManager().setPrefabPath(chosenUuid, chosenPath);
                                // DEBUG
                                // System.out.printf("[CastleSiege] Mob entered castle (x=%.2f, z=%.2f), assigned to loop %s%n",
                                //        pos.x, pos.z, chosenPath.getName());
                            }
                            continue;
                        }
                        if (!flankedArchers.contains(mobRef)
                                && pos.x >= OUTSIDE_X_MIN && pos.x <= OUTSIDE_X_MAX
                                && pos.z >= OUTSIDE_Z_MIN && pos.z <= OUTSIDE_Z_MAX) {
                            NPCEntity npc = waveMobEntities.get(mobRef);
                            if (npc != null && npc.getRoleName() != null
                                    && npc.getRoleName().contains("Archer")) {
                                flankedArchers.add(mobRef);
                                UUID chosenUuid = FLANK_PATH_UUIDS[RANDOM.nextInt(FLANK_PATH_UUIDS.length)];
                                IPrefabPath chosenPath = flankPaths.get(chosenUuid);
                                npc.getPathManager().setPrefabPath(chosenUuid, chosenPath);
                                // System.out.printf("[CastleSiege] Archer reached outside path (x=%.2f, z=%.2f), routed to %s%n",
                                //         pos.x, pos.z, chosenPath.getName());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[CastleSiege] Position tracking dropped stale mob: " + e.getMessage());
                        untrackStaleMob(mobRef, store);
                    }
                }
            });
        }, 250, 250, TimeUnit.MILLISECONDS);
    }

    private static final String WAVE_START_SOUND_ID = "SFX_Eye_Void_Attack_Summon";
    private static final String VICTORY_SOUND_ID = "SFX_Discovery_Z1_Medium";
    private static final float VICTORY_SOUND_VOLUME = 4.0f;
    private static final float VICTORY_SOUND_PITCH = 1.0f;

    private static void untrackStaleMob(Ref<EntityStore> mobRef, Store<EntityStore> store) {
        waveMobEntities.remove(mobRef);
        crossedZThreshold.remove(mobRef);
        flankedArchers.remove(mobRef);
        // Forwards to the same end-of-wave bookkeeping recordMobDeath uses;
        // null killer = no player kill credit.
        recordMobDeath(mobRef, null, store);
    }

    public static void refreshAllWaveHuds(Store<EntityStore> store) {
        int current = currentWave.get();
        int max = getMaxWave();
        String status = computeHudStatus();

        store.forEachChunk(Player.getComponentType(), (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) continue;
                if (player.getHudManager().getCustomHud() instanceof WaveHUD hud) {
                    hud.setWaveLabel(current, max);
                    hud.setStatus(status);
                }
            }
        });
    }

    public static void refreshWaveHud(WaveHUD hud) {
        hud.setWaveLabel(currentWave.get(), getMaxWave());
        hud.setStatus(computeHudStatus());
    }

    private static String computeHudStatus() {
        // "Completed" only after the final wave has actually been beaten —
        // not during wave 20 startup, before mobs (or the boss) have spawned.
        if (lastDefeatedWave.get() >= getMaxWave()) {
            return "All waves completed!";
        }
        return "Mobs remaining: " + getMobsRemaining();
    }

    private static void playWaveStartSound(Store<EntityStore> store) {
        int idx = SoundEvent.getAssetMap().getIndexOrDefault(WAVE_START_SOUND_ID, -1);
        if (idx < 0) return;
        store.forEachChunk(Player.getComponentType(), (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
                if (pr == null) continue;
                SoundUtil.playSoundEvent2dToPlayer(pr, idx, SoundCategory.SFX);
            }
        });
    }

    private static void playVictorySound(Store<EntityStore> store) {
        int idx = SoundEvent.getAssetMap().getIndexOrDefault(VICTORY_SOUND_ID, -1);
        if (idx < 0) return;
        store.forEachChunk(Player.getComponentType(), (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
                if (pr == null) continue;
                SoundUtil.playSoundEvent2dToPlayer(
                        pr, idx, SoundCategory.SFX, VICTORY_SOUND_VOLUME, VICTORY_SOUND_PITCH);
            }
        });
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
