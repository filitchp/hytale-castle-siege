package dev.dooondi.wave;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Team-wide money balance shared by all players. Earned from wave mob kills,
 * spent at the shop. Persists across restarts and resets when a new game starts.
 * To change kill payouts, edit {@link #KILL_REWARDS}.
 */
public final class TeamBank {

    public static final int STARTING_MONEY = 100;
    private static final int DEFAULT_KILL_REWARD = 5;

    private static final Map<String, Integer> KILL_REWARDS = Map.ofEntries(
            Map.entry("Rat_CS",                       5),
            Map.entry("Snake_Rattle_CS",              5),
            Map.entry("Skeleton_Weak_CS",             10),
            Map.entry("Skeleton_Archer_Weak_CS",      10),
            Map.entry("Spawn_Void_CS",                15),
            Map.entry("Skeleton_Sturdy_CS",           20),
            Map.entry("Skeleton_Archer_Sturdy_CS",    20),
            Map.entry("Skeleton_Pirate_Striker_CS",   20),
            Map.entry("Skeleton_Pirate_Gunner_CS",    20),
            Map.entry("Skeleton_Pirate_Captain_CS",   30),
            Map.entry("Skeleton_Burnt_Praetorian_CS", 250)
    );

    private static final AtomicInteger balance = new AtomicInteger(STARTING_MONEY);
    private static volatile Path saveFile;

    private TeamBank() {}

    public static void initPersistence(Path dataDir) {
        try {
            Files.createDirectories(dataDir);
            saveFile = dataDir.resolve("team_money.txt");
            if (Files.exists(saveFile)) {
                balance.set(Math.max(0, Integer.parseInt(Files.readString(saveFile).trim())));
                System.out.println("[CastleSiege] Loaded team money: $" + balance.get());
            }
        } catch (Exception e) {
            System.err.println("[CastleSiege] Failed to load team money: " + e.getMessage());
        }
    }

    public static int getBalance() {
        return balance.get();
    }

    public static int getKillReward(String roleName) {
        // Map.ofEntries rejects null keys, even in lookups.
        if (roleName == null) return DEFAULT_KILL_REWARD;
        return KILL_REWARDS.getOrDefault(roleName, DEFAULT_KILL_REWARD);
    }

    public static void earn(int amount) {
        if (amount <= 0) return;
        balance.addAndGet(amount);
        save();
    }

    /** Atomically deducts {@code cost}; returns false (and changes nothing) if the team can't afford it. */
    public static boolean trySpend(int cost) {
        while (true) {
            int current = balance.get();
            if (current < cost) return false;
            if (balance.compareAndSet(current, current - cost)) {
                save();
                return true;
            }
        }
    }

    public static void reset() {
        balance.set(STARTING_MONEY);
        save();
    }

    public static String format(int amount) {
        return String.format("$%,d", amount);
    }

    private static void save() {
        Path file = saveFile;
        if (file == null) return;
        try {
            Files.writeString(file, Integer.toString(balance.get()));
        } catch (Exception e) {
            System.err.println("[CastleSiege] Failed to save team money: " + e.getMessage());
        }
    }
}
