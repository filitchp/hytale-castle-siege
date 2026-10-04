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
import dev.dooondi.shop.ShopCatalog.Shop;
import dev.dooondi.shop.ShopCatalog.ShopItem;
import dev.dooondi.wave.TeamBank;
import dev.dooondi.wave.WaveManager;

import javax.annotation.Nonnull;
import java.util.List;

public class ShopUI extends InteractiveCustomUIPage<ShopUI.Data> {

    private static final String GRID = "#ItemGrid";
    private static final String AFFORDABLE_COLOR = "#e8c547";
    private static final String UNAFFORDABLE_COLOR = "#cc4444";

    private final String shopId;
    private final List<ShopItem> items;

    public ShopUI(@Nonnull PlayerRef playerRef, @Nonnull String shopId) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
        this.shopId = shopId;
        Shop shop = ShopCatalog.SHOPS.get(shopId);
        this.items = shop != null ? shop.items() : List.of();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder ui,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        ui.append("ShopUI.ui");
        Shop shop = ShopCatalog.SHOPS.get(shopId);
        if (shop == null) {
            ui.set("#ShopTitle.TextSpans", Message.raw("Closed"));
            ui.set("#StatusLabel.TextSpans", Message.raw("Unknown shop '" + shopId + "'."));
        } else {
            ui.set("#ShopTitle.TextSpans", Message.raw(shop.title()));
        }
        ui.clear(GRID);

        for (int i = 0; i < items.size(); i++) {
            ShopItem item = items.get(i);
            String card = GRID + "[" + i + "]";
            ui.append(GRID, "ShopItemCard.ui");
            ui.set(card + " #ItemSlot.ItemId", item.itemId());
            ui.set(card + " #ItemName.TextSpans", itemName(item.itemId()));
            ui.set(card + " #Quantity.TextSpans",
                    Message.raw(item.quantity() > 1 ? "x" + item.quantity() : ""));
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
        if (index < 0 || index >= items.size()) {
            return Message.raw("That item is no longer for sale.");
        }
        ShopItem item = items.get(index);
        Message label = itemLabel(item.itemId(), item.quantity());

        if (!TeamBank.trySpend(item.price())) {
            return Message.join(Message.raw("Not enough money for "), label,
                    Message.raw(" (" + TeamBank.format(item.price()) + ")."));
        }

        // giveItem doesn't drop overflow, so refund whatever didn't fit, rounding in the team's favour.
        ItemStackTransaction tx = Player.giveItem(new ItemStack(item.itemId(), item.quantity()), ref, store);
        ItemStack remainder = tx.getRemainder();
        int leftOver = !tx.succeeded() ? item.quantity()
                : (remainder == null || remainder.isEmpty()) ? 0 : remainder.getQuantity();
        if (leftOver >= item.quantity()) {
            TeamBank.earn(item.price());
            return Message.raw("Your inventory is full. You were not charged.");
        }
        int given = item.quantity() - leftOver;
        int refund = (item.price() * leftOver + item.quantity() - 1) / item.quantity();
        int cost = item.price() - refund;
        TeamBank.earn(refund);

        Message boughtLabel = itemLabel(item.itemId(), given);
        WaveManager.refreshAllWaveHuds(store);
        broadcastPurchase(store, boughtLabel, cost);
        if (leftOver > 0) {
            return Message.join(Message.raw("Inventory full: bought "), boughtLabel,
                    Message.raw(" for " + TeamBank.format(cost) + " (refunded " + TeamBank.format(refund) + ")."));
        }
        return Message.join(Message.raw("Bought "), boughtLabel,
                Message.raw(" for " + TeamBank.format(cost) + "."));
    }

    private void broadcastPurchase(Store<EntityStore> store, Message itemLabel, int price) {
        Message msg = Message.join(Message.raw(playerRef.getUsername() + " bought "), itemLabel,
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

    private void applyBalance(UICommandBuilder ui) {
        int balance = TeamBank.getBalance();
        ui.set("#MoneyLabel.TextSpans", Message.raw("Team Money: " + TeamBank.format(balance)));
        for (int i = 0; i < items.size(); i++) {
            ui.set(GRID + "[" + i + "] #Price.Style.TextColor",
                    items.get(i).price() <= balance ? AFFORDABLE_COLOR : UNAFFORDABLE_COLOR);
        }
    }

    private static Message itemLabel(String itemId, int quantity) {
        Message name = itemName(itemId);
        return quantity > 1 ? Message.join(Message.raw(quantity + "x "), name) : name;
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
