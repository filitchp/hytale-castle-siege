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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import it.unimi.dsi.fastutil.Pair;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.List;
import java.util.Map;

public class MobWaveCommand extends AbstractTargetPlayerCommand {
    private final RequiredArg<Integer> waveArg;
    private final OptionalArg<String> messageArg;
    private final FlagArg debugArg;

    private record MobEntry(String name, int count) {}

    private static final Map<Integer, List<MobEntry>> WAVE_TABLE = Map.ofEntries(
            Map.entry(1, List.of(
                    new MobEntry("Rat", 3)
            )),
            Map.entry(2, List.of(
                    new MobEntry("Rat", 5)
            )),
            Map.entry(3, List.of(
                    new MobEntry("Rat", 3),
                    new MobEntry("Skeleton", 2)
            )),
            Map.entry(4, List.of(
                    new MobEntry("Skeleton", 4),
                    new MobEntry("Rat", 2)
            )),
            Map.entry(5, List.of(
                    new MobEntry("Skeleton", 5),
                    new MobEntry("Snake_Rattle", 2)
            )),
            Map.entry(6, List.of(
                    new MobEntry("Snake_Rattle", 4),
                    new MobEntry("Skeleton", 3)
            )),
            Map.entry(7, List.of(
                    new MobEntry("Skeleton", 4),
                    new MobEntry("Snake_Rattle", 3),
                    new MobEntry("Rat", 4)
            )),
            Map.entry(8, List.of(
                    new MobEntry("Snake_Rattle", 5),
                    new MobEntry("Skeleton", 5)
            )),
            Map.entry(9, List.of(
                    new MobEntry("Skeleton", 6),
                    new MobEntry("Snake_Rattle", 4),
                    new MobEntry("Skeleton_Pirate_Captain", 1)
            )),
            Map.entry(10, List.of(
                    new MobEntry("Skeleton_Pirate_Captain", 2),
                    new MobEntry("Skeleton", 6),
                    new MobEntry("Snake_Rattle", 4)
            )),
            Map.entry(11, List.of(
                    new MobEntry("Skeleton_Pirate_Captain", 3),
                    new MobEntry("Snake_Rattle", 5),
                    new MobEntry("Skeleton", 4)
            )),
            Map.entry(12, List.of(
                    new MobEntry("Skeleton_Pirate_Captain", 4),
                    new MobEntry("Snake_Rattle", 6),
                    new MobEntry("Skeleton", 5)
            )),
            Map.entry(13, List.of(
                    new MobEntry("Skeleton_Pirate_Captain", 5),
                    new MobEntry("Snake_Rattle", 5),
                    new MobEntry("Skeleton", 6)
            )),
            Map.entry(14, List.of(
                    new MobEntry("Skeleton_Pirate_Captain", 5),
                    new MobEntry("Snake_Rattle", 6),
                    new MobEntry("Skeleton", 6),
                    new MobEntry("Rat", 6)
            )),
            Map.entry(15, List.of(
                    new MobEntry("Skeleton_Pirate_Captain", 6),
                    new MobEntry("Snake_Rattle", 7),
                    new MobEntry("Skeleton", 7)
            )),
            Map.entry(16, List.of(
                    new MobEntry("Skeleton_Pirate_Captain", 7),
                    new MobEntry("Snake_Rattle", 8),
                    new MobEntry("Skeleton", 6)
            )),
            Map.entry(17, List.of(
                    new MobEntry("Skeleton_Pirate_Captain", 8),
                    new MobEntry("Snake_Rattle", 6),
                    new MobEntry("Skeleton", 8)
            )),
            Map.entry(18, List.of(
                    new MobEntry("Skeleton_Pirate_Captain", 8),
                    new MobEntry("Snake_Rattle", 8),
                    new MobEntry("Skeleton", 8)
            )),
            Map.entry(19, List.of(
                    new MobEntry("Skeleton_Pirate_Captain", 9),
                    new MobEntry("Snake_Rattle", 9),
                    new MobEntry("Skeleton", 8)
            )),
            Map.entry(20, List.of(
                    new MobEntry("Skeleton_Pirate_Captain", 10),
                    new MobEntry("Snake_Rattle", 10),
                    new MobEntry("Skeleton", 10)
            ))
    );

    public MobWaveCommand() {
        super("mobwave", "Tower Defense mod command: spawn mobs at a given wave");

        this.waveArg = this.withRequiredArg("wave", "Wave number (1-20)", ArgTypes.INTEGER);
        this.messageArg = this.withOptionalArg("message", "Message to print while spawning mobs", ArgTypes.STRING);
        this.debugArg = this.withFlagArg("debug", "Add debug logs");
    }

    /**
     * Computes spawn positions for mobs arranged in a square grid pattern.
     * Each mob is spaced 1 block apart from its neighbors. The grid is centered
     * on the given origin position on the X/Z plane (Y stays constant).
     *
     * Mobs are placed sequentially: the first mob group fills positions first,
     * then the next group continues from where the previous left off.
     *
     * @param origin     the center point of the spawn area
     * @param totalCount the total number of mobs to place
     * @return a list of positions for each mob
     */
    private static List<Vector3d> computeSpawnPositions(Vector3d origin, int totalCount) {
        List<Vector3d> positions = new java.util.ArrayList<>(totalCount);
        // Arrange mobs in a square grid with 1-block spacing
        int gridSize = (int) Math.ceil(Math.sqrt(totalCount));
        // Center the grid on the origin
        double offsetX = -(gridSize - 1) / 2.0;
        double offsetZ = -(gridSize - 1) / 2.0;

        for (int i = 0; i < totalCount; i++) {
            int row = i / gridSize;
            int col = i % gridSize;
            positions.add(new Vector3d(
                    origin.x + offsetX + col,
                    origin.y,
                    origin.z + offsetZ + row
            ));
        }
        return positions;
    }

    /**
     * Spawns all mobs for the given wave number using the wave lookup table.
     * Mobs appear in the order they are listed in the table, spaced in a square
     * grid pattern with 1-block spacing between each mob.
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
        List<Vector3d> positions = computeSpawnPositions(origin, totalMobs);
        var rotation = new Vector3f(0.f, 0.f, 0.f);

        int posIndex = 0;
        for (MobEntry entry : entries) {
            if (debug) {
                commandContext.sendMessage(Message.raw(
                        "Spawning " + entry.count() + "x " + entry.name()));
            }
            for (int i = 0; i < entry.count(); i++) {
                Vector3d pos = positions.get(posIndex++);
                Pair<Ref<EntityStore>, INonPlayerCharacter> result =
                        NPCPlugin.get().spawnNPC(store, entry.name(), null, pos, rotation);
                if (result == null && debug) {
                    commandContext.sendMessage(Message.raw(
                            "Failed to spawn " + entry.name() + " at (" + pos.x + ", " + pos.y + ", " + pos.z + ")"));
                }
            }
        }

        commandContext.sendMessage(Message.raw(
                "Wave " + waveNumber + " started! " + totalMobs + " mobs spawned."));
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

        var spawnOrigin = new Vector3d(0.0, 80.0, 0.0);

        commandContext.sendMessage(Message.raw("Get Ready to Fight! Starting wave " + wave));
        spawnWave(wave, spawnOrigin, store, commandContext, debug);
    }
}
