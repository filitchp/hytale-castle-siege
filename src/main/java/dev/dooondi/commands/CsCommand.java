package dev.dooondi.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.dooondi.events.WelcomeEvent;
import dev.dooondi.ui.WaveHUD;
import dev.dooondi.ui.WaveUI;
import dev.dooondi.wave.WaveManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

public class CsCommand extends AbstractTargetPlayerCommand {

    private final RequiredArg<String> actionArg;
    private final OptionalArg<Integer> waveArg;
    private final FlagArg confirmArg;

    public CsCommand() {
        super("cs", "Castle Siege command. Actions: reset [--confirm], ui, hud, wave <n>, next, debugmobs");

        this.setPermissionGroups("CastleSiege");
//        this.setPermissionGroups("Default");

        this.actionArg = this.withRequiredArg(
                "action", "Actions: reset, ui, hud, wave, next, debugmobs", ArgTypes.STRING);
        this.waveArg = this.withOptionalArg(
                "wave", "Wave number for the 'wave' action (1-20)", ArgTypes.INTEGER);
        this.confirmArg = this.withFlagArg(
                "confirm", "Required to actually run 'cs reset'");
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext commandContext,
            @NullableDecl Ref<EntityStore> ref,
            @NonNullDecl Ref<EntityStore> playerEntityRef,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world,
            @NonNullDecl Store<EntityStore> store) {

        String action = this.actionArg.get(commandContext);

        if ("reset".equalsIgnoreCase(action)) {
            executeReset(commandContext, world, store);
        } else if ("ui".equalsIgnoreCase(action)) {
            executeUi(commandContext, playerEntityRef, playerRef, world, store);
        } else if ("hud".equalsIgnoreCase(action)) {
            executeHud(commandContext, playerRef, world);
        } else if ("wave".equalsIgnoreCase(action)) {
            executeWave(commandContext, store);
        } else if ("next".equalsIgnoreCase(action)) {
            executeNext(commandContext, store);
        } else if ("debugmobs".equalsIgnoreCase(action)) {
            executeDebugMobs(commandContext);
        } else {
            commandContext.sendMessage(Message.raw(
                    "Unknown action: " + action + ". Supported: reset, ui, hud, wave, next, debugmobs"));
        }
    }

    private void executeReset(CommandContext commandContext, World world, Store<EntityStore> store) {
        if (!this.confirmArg.get(commandContext)) {
            commandContext.sendMessage(Message.raw(
                    "/cs reset will: reset the wave counter to 0, despawn all wave NPCs, " +
                    "teleport every player to spawn, and replace their inventory with " +
                    "just the Wave Hammer and a Crude Axe. Re-run with --confirm to proceed."));
            return;
        }

        // All store-mutating work runs on the world thread.
        CompletableFuture.runAsync(() -> {
            WaveManager.resetGame(store);

            ISpawnProvider spawnProvider = world.getWorldConfig().getSpawnProvider();

            store.forEachChunk(Player.getComponentType(), (chunk, buffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    Ref<EntityStore> pRef = chunk.getReferenceTo(i);
                    Player player = store.getComponent(pRef, Player.getComponentType());
                    PlayerRef playerRef = store.getComponent(pRef, PlayerRef.getComponentType());
                    if (player == null || playerRef == null) continue;

                    Transform spawn = spawnProvider.getSpawnPoint(world, playerRef.getUuid());
                    if (spawn != null) {
                        Vector3d pos = spawn.getPosition();
                        player.moveTo(pRef, pos.x, pos.y, pos.z, store);
                    }

                    CombinedItemContainer everything = InventoryComponent.getCombined(
                            store, pRef, InventoryComponent.EVERYTHING);
                    everything.clear();

                    player.giveItem(new ItemStack(WelcomeEvent.HAMMER_ID, 1), pRef, store);
                    player.giveItem(new ItemStack("Weapon_Axe_Crude", 1), pRef, store);

                    player.sendMessage(Message.raw(
                            "Castle Siege has been reset. You have the Wave Hammer and a Crude Axe."));
                }
            });

            commandContext.sendMessage(Message.raw("Castle Siege reset complete."));
        }, world);
    }

    private void executeUi(CommandContext commandContext, Ref<EntityStore> playerEntityRef,
                           PlayerRef playerRef, World world, Store<EntityStore> store) {
        Player player = commandContext.senderAs(Player.class);
        if (player == null) {
            commandContext.sendMessage(Message.raw("This command must be run by a player."));
            return;
        }
        CompletableFuture.runAsync(
                () -> player.getPageManager().openCustomPage(playerEntityRef, store, new WaveUI(playerRef)),
                world);
    }

    private void executeHud(CommandContext commandContext, PlayerRef playerRef, World world) {
        Player player = commandContext.senderAs(Player.class);
        if (player == null) {
            commandContext.sendMessage(Message.raw("This command must be run by a player."));
            return;
        }

        CompletableFuture.runAsync(() -> {
            if (player.getHudManager().getCustomHud() instanceof WaveHUD) {
                player.getHudManager().setCustomHud(playerRef, new CustomUIHud(playerRef) {
                    @Override
                    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
                    }
                });
                playerRef.sendMessage(Message.raw("Wave HUD hidden."));
            } else {
                WaveHUD hud = new WaveHUD(playerRef);
                player.getHudManager().setCustomHud(playerRef, hud);
                WaveManager.refreshWaveHud(hud);
                playerRef.sendMessage(Message.raw("Wave HUD shown."));
            }
        }, world);
    }

    private void executeWave(CommandContext commandContext, Store<EntityStore> store) {
        Integer wave = this.waveArg.get(commandContext);
        if (wave == null) {
            commandContext.sendMessage(Message.raw("Usage: /cs wave --wave <wave number>"));
            return;
        }
        commandContext.sendMessage(Message.raw("Get Ready to Fight! Starting wave " + wave));
        WaveManager.spawnWave(wave, store,
                msg -> commandContext.sendMessage(Message.raw(msg)), false);
    }

    private void executeNext(CommandContext commandContext, Store<EntityStore> store) {
        WaveManager.spawnNextWave(store,
                msg -> commandContext.sendMessage(Message.raw(msg)));
    }

    private void executeDebugMobs(CommandContext commandContext) {
        int max = WaveManager.getMaxWave();
        String header = "=== Wave Stats (mobs / melee DPS / ranged DPS) ===";
        commandContext.sendMessage(Message.raw(header));
        System.out.println("[CastleSiege] " + header);

        int totalMobsAll = 0;
        double totalMeleeAll = 0.0;
        double totalRangedAll = 0.0;
        java.util.Set<String> unknownAll = new java.util.LinkedHashSet<>();

        for (int w = 1; w <= max; w++) {
            WaveManager.WaveStatsSummary s = WaveManager.computeWaveStats(w);
            totalMobsAll += s.totalMobs();
            totalMeleeAll += s.meleeDps();
            totalRangedAll += s.rangedDps();
            unknownAll.addAll(s.unknownRoles());
            String line = String.format(
                    "Wave %2d: %3d mobs | %6.1f melee DPS | %6.1f ranged DPS",
                    w, s.totalMobs(), s.meleeDps(), s.rangedDps());
            commandContext.sendMessage(Message.raw(line));
            System.out.println("[CastleSiege] " + line);
        }

        String totalLine = String.format(
                "TOTAL  : %3d mobs | %6.1f melee DPS | %6.1f ranged DPS",
                totalMobsAll, totalMeleeAll, totalRangedAll);
        commandContext.sendMessage(Message.raw(totalLine));
        System.out.println("[CastleSiege] " + totalLine);

        if (!unknownAll.isEmpty()) {
            String warn = "WARN: missing MOB_STATS entries for: " + String.join(", ", unknownAll);
            commandContext.sendMessage(Message.raw(warn));
            System.out.println("[CastleSiege] " + warn);
        }
    }
}
