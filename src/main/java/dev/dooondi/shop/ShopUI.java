package dev.dooondi.shop;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.dooondi.shop.ShopCatalog.ShopItem;
import dev.dooondi.wave.TeamBank;
import dev.dooondi.wave.WaveManager;

import javax.annotation.Nonnull;
import java.util.List;

public class ShopUI extends InteractiveCustomUIPage<ShopUI.Data> {

    private static final String GRID = "#ItemGrid";
    private static final String AFFORDABLE_COLOR = "#e8c547";
    private static final String UNAFFORDABLE_COLOR = "#cc4444";

    public ShopUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder ui,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        ui.append("ShopUI.ui");
        ui.clear(GRID);

        List<ShopItem> items = ShopCatalog.ITEMS;
        for (int i = 0; i < items.size(); i++) {
            ShopItem item = items.get(i);
            String card = GRID + "[" + i + "]";
            ui.append(GRID, "ShopItemCard.ui");
            ui.set(card + " #ItemSlot.ItemId", item.itemId());
            ui.set(card + " #ItemName.TextSpans", itemName(item.itemId()));
            ui.set(card + " #Price.TextSpans", Message.raw(TeamBank.format(item.price())));
            events.addEventBinding(CustomUIEventBindingType.Activating, card + " #BuyButton",
                    EventData.of(Data.ITEM_INDEX, String.valueOf(i)));
        }
        applyBalance(ui);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, Data data) {
        super.handleDataEvent(ref, store, data);

        UICommandBuilder ui = new UICommandBuilder();
        ui.set("#StatusLabel.TextSpans", purchase(ref, store, data.itemIndex));
        applyBalance(ui);
        sendUpdate(ui);
    }

    private Message purchase(Ref<EntityStore> ref, Store<EntityStore> store, int index) {
        if (index < 0 || index >= ShopCatalog.ITEMS.size()) {
            return Message.raw("That item is no longer for sale.");
        }
        ShopItem item = ShopCatalog.ITEMS.get(index);
        Message name = itemName(item.itemId());

        if (!TeamBank.trySpend(item.price())) {
            return Message.join(Message.raw("Not enough money for "), name,
                    Message.raw(" (" + TeamBank.format(item.price()) + ")."));
        }

        // giveItem doesn't drop overflow, so refund anything that didn't fit.
        ItemStackTransaction tx = Player.giveItem(new ItemStack(item.itemId(), 1), ref, store);
        ItemStack remainder = tx.getRemainder();
        if (!tx.succeeded() || (remainder != null && !remainder.isEmpty())) {
            TeamBank.earn(item.price());
            return Message.raw("Your inventory is full. You were not charged.");
        }

        WaveManager.refreshAllWaveHuds(store);
        broadcastPurchase(store, name, item.price());
        return Message.join(Message.raw("Bought "), name,
                Message.raw(" for " + TeamBank.format(item.price()) + "."));
    }

    private void broadcastPurchase(Store<EntityStore> store, Message itemName, int price) {
        Message msg = Message.join(Message.raw(playerRef.getUsername() + " bought "), itemName,
                Message.raw(" for " + TeamBank.format(price) + ". Team money: "
                        + TeamBank.format(TeamBank.getBalance())));
        store.forEachChunk(PlayerRef.getComponentType(), (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                PlayerRef other = store.getComponent(chunk.getReferenceTo(i), PlayerRef.getComponentType());
                if (other != null && !other.getUuid().equals(playerRef.getUuid())) {
                    other.sendMessage(msg);
                }
            }
        });
    }

    private static void applyBalance(UICommandBuilder ui) {
        int balance = TeamBank.getBalance();
        ui.set("#MoneyLabel.TextSpans", Message.raw("Team Money: " + TeamBank.format(balance)));
        List<ShopItem> items = ShopCatalog.ITEMS;
        for (int i = 0; i < items.size(); i++) {
            ui.set(GRID + "[" + i + "] #Price.Style.TextColor",
                    items.get(i).price() <= balance ? AFFORDABLE_COLOR : UNAFFORDABLE_COLOR);
        }
    }

    private static Message itemName(String itemId) {
        Item item = Item.getAssetMap().getAsset(itemId);
        return item != null ? item.getTranslationMessage() : Message.raw(itemId);
    }

    public static class Data {
        static final String ITEM_INDEX = "ItemIndex";

        public static final BuilderCodec<Data> CODEC = BuilderCodec.builder(Data.class, Data::new)
                .append(new KeyedCodec<>(ITEM_INDEX, Codec.STRING),
                        (d, v) -> d.itemIndex = Integer.parseInt(v),
                        d -> String.valueOf(d.itemIndex))
                .add()
                .build();

        private int itemIndex = -1;
    }
}
