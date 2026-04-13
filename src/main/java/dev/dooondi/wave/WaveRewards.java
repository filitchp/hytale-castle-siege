package dev.dooondi.wave;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Library that awards items to each online player at the start and end of each wave.
 * To add or change a wave's rewards, edit {@link #WAVE_REWARDS}.
 */
public final class WaveRewards {

    public record RewardItem(String itemId, int quantity) {}

    public record WaveRewardSet(List<RewardItem> start, List<RewardItem> end) {
        public static WaveRewardSet startOnly(RewardItem... items) {
            return new WaveRewardSet(List.of(items), List.of());
        }
        public static WaveRewardSet endOnly(RewardItem... items) {
            return new WaveRewardSet(List.of(), List.of(items));
        }
        public static WaveRewardSet of(List<RewardItem> start, List<RewardItem> end) {
            return new WaveRewardSet(start, end);
        }
    }

    public static final Map<Integer, WaveRewardSet> WAVE_REWARDS = Map.ofEntries(
            // ------------------
            //       Wave 1
            // ------------------
            Map.entry(1, WaveRewardSet.startOnly(
                    new RewardItem("Wood_Oak_Trunk", 100),
                    new RewardItem("Ingredient_Fibre", 50)
            )),
            // ------------------
            //       Wave 2
            // ------------------
            Map.entry(2, WaveRewardSet.startOnly(
                    new RewardItem("Ore_Copper", 10)
            )),
            // ------------------
            //       Wave 3
            // ------------------
            Map.entry(3, WaveRewardSet.of(
                    Collections.singletonList(new RewardItem("Ore_Copper", 15)), // Start
                    Collections.singletonList(new RewardItem("Potion_Health", 2))  // End
            )),
            // ------------------
            //       Wave 4
            // ------------------
            Map.entry(4, WaveRewardSet.startOnly(
                    new RewardItem("Ore_Copper", 20) // Start
            )),
            // ------------------
            //       Wave 5
            // ------------------
            Map.entry(5, WaveRewardSet.of(
                    // Start of wave
                    List.of(new RewardItem("Ore_Iron", 10),
                            new RewardItem("Rubble_Marble", 50),
                            new RewardItem("Ingredient_Stick", 50)),
                    // End fo wave
                    List.of(new RewardItem("Ingredient_Fabric_Scrap_Linen", 25),
                            new RewardItem("Ingredient_Leather_Light", 10))
            )),
            // ------------------
            //       Wave 6
            // ------------------
            Map.entry(6, WaveRewardSet.of(
                    Collections.singletonList(new RewardItem("Ore_Iron", 15)), // Start
                    Collections.singletonList(new RewardItem("Potion_Health_Greater", 2))  // End
            )),
            // ------------------
            //       Wave 7
            // ------------------
            Map.entry(7, WaveRewardSet.startOnly(
                    new RewardItem("Ore_Iron", 20)
            )),
            // ------------------
            //       Wave 8
            // ------------------
            Map.entry(8, WaveRewardSet.startOnly(
                    new RewardItem("Ore_Iron", 10)
            )),
            // ------------------
            //       Wave 9
            // ------------------
            Map.entry(9, WaveRewardSet.startOnly(
                    new RewardItem("Ore_Iron", 10)
            )),
            // ------------------
            //       Wave 10
            // ------------------
            Map.entry(10, WaveRewardSet.of(
                    // Start of wave
                    List.of(new RewardItem("Ore_Thorium", 20),
                            new RewardItem("Rubble_Marble", 50),
                            new RewardItem("Ingredient_Stick", 50)),
                    // End fo wave
                    List.of(new RewardItem("Halloween_Broomstick", 1),
                            new RewardItem("Potion_Health_Greater", 4))
            ))
    );

    private WaveRewards() {}

    public static void awardWaveStart(int waveNumber, Store<EntityStore> store) {
        WaveRewardSet set = WAVE_REWARDS.get(waveNumber);
        if (set == null || set.start().isEmpty()) return;
        awardItemsToAllPlayers(set.start(), store, "Wave " + waveNumber + " start reward");
    }

    public static void awardWaveEnd(int waveNumber, Store<EntityStore> store) {
        WaveRewardSet set = WAVE_REWARDS.get(waveNumber);
        if (set == null || set.end().isEmpty()) return;
        awardItemsToAllPlayers(set.end(), store, "Wave " + waveNumber + " clear reward");
    }

    private static void awardItemsToAllPlayers(List<RewardItem> rewards,
                                               Store<EntityStore> store,
                                               String reasonLabel) {
        // Iterate all entities that have a Player component.
        store.forEachChunk(Player.getComponentType(), (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) continue;

                for (RewardItem reward : rewards) {
                    player.giveItem(new ItemStack(reward.itemId(), reward.quantity()), ref, store);
                }
                player.sendMessage(Message.raw("[" + reasonLabel + "] Received "
                        + summarize(rewards)));
            }
        });
    }

    private static String summarize(List<RewardItem> rewards) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rewards.size(); i++) {
            RewardItem r = rewards.get(i);
            if (i > 0) sb.append(", ");
            sb.append(r.quantity()).append("x ").append(r.itemId());
        }
        return sb.toString();
    }
}
