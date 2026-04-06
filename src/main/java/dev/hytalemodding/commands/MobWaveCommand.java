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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class MobWaveCommand extends AbstractTargetPlayerCommand {
    private final RequiredArg<Integer> waveArg;
    private final OptionalArg<String> messageArg;
    private final FlagArg debugArg;

    private record MobEntry(String name, int count) {}

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

    /**
     * Creates a flanking path using relative waypoints based on the mob's lateral
     * offset from the grid center. Mobs near the center march straight toward the
     * castle (positive X). Mobs on the sides angle outward, curve around the castle
     * walls, and converge on the far side.
     *
     * @param spawnPos  the mob's spawn position (path origin)
     * @param zOffset   the mob's Z offset from the grid center (negative = left, positive = right)
     * @param maxOffset the maximum absolute Z offset in the grid (used to normalize)
     * @return a path the mob will follow
     */
    private static IPath<?> createFlankingPath(Vector3d spawnPos, double zOffset, double maxOffset) {
        // Normalize lateral position to [-1, 1]; 0 = center, ±1 = outermost edge
        double flankFactor = (maxOffset > 0) ? (zOffset / maxOffset) : 0.0;

        // Max angle that the outermost mobs will turn to flank the castle
        float maxFlankAngle = 45f;
        float flankAngle = (float) (flankFactor * maxFlankAngle);

        Queue<RelativeWaypointDefinition> waypoints = new LinkedList<>();

        if (Math.abs(flankFactor) < 0.25) {
            // Center mobs: march straight toward the castle front
            waypoints.add(new RelativeWaypointDefinition(0f, 100.0));
        } else {
            // Flanking mobs: angle out to the side of the castle, curve around it,
            // then converge toward the back/entrance
            waypoints.add(new RelativeWaypointDefinition(flankAngle, 15.0));         // angle outward
            waypoints.add(new RelativeWaypointDefinition(-flankAngle * 2f, 20.0));   // curve around the wall
            waypoints.add(new RelativeWaypointDefinition(flankAngle * 0.5f, 20.0));   // converge on the castle
        }

        // All mobs initially face +X (toward the castle)
        var forwardRotation = new Vector3f(0f, 0f, 0f);
        return TransientPath.buildPath(spawnPos, forwardRotation, waypoints, 1.0);
    }

    /**
     * Spawns all mobs for the given wave number and assigns each mob a flanking
     * path toward the castle. Center mobs charge straight ahead while side mobs
     * flank around the structure.
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
        double halfGrid = (gridSize - 1) / 2.0;
        var rotation = new Vector3f(0f, 0f, 0f);

        int mobIndex = 0;
        for (MobEntry entry : entries) {
            if (debug) {
                commandContext.sendMessage(Message.raw(
                        "Spawning " + entry.count() + "x " + entry.name()));
            }
            for (int i = 0; i < entry.count(); i++) {
                Vector3d pos = computeSpawnPosition(origin, mobIndex, gridSize);
                int row = mobIndex / gridSize;
                double zOffset = row - halfGrid;

                Pair<Ref<EntityStore>, INonPlayerCharacter> result =
                        NPCPlugin.get().spawnNPC(store, entry.name(), null, pos, rotation);

                if (result != null) {
                    // Retrieve the NPCEntity component to access the path manager
                    NPCEntity npcEntity = store.getComponent(
                            result.first(), NPCEntity.getComponentType());
                    if (npcEntity != null) {
                        var path = createFlankingPath(pos, zOffset, halfGrid);
                        npcEntity.getPathManager().setTransientPath(path);
                        if (debug) {
                            commandContext.sendMessage(Message.raw(
                                    "  " + entry.name() + " at Z-offset " + zOffset
                                            + " → flank factor " + (halfGrid > 0 ? zOffset / halfGrid : 0)));
                        }
                    }
                } else if (debug) {
                    commandContext.sendMessage(Message.raw(
                            "Failed to spawn " + entry.name() + " at ("
                                    + pos.x + ", " + pos.y + ", " + pos.z + ")"));
                }

                mobIndex++;
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
