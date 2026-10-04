package dev.dooondi.shop;

import java.util.List;
import java.util.Map;

/**
 * Stores sold by the Castle Siege merchants, keyed by the shop ID a merchant role
 * passes to {@code "Type": "OpenCastleSiegeShop", "Shop": "<id>"}. Items show in
 * display order; the shop grid fits six per row. To add an item or change a price,
 * edit {@link #SHOPS}.
 */
public final class ShopCatalog {

    public record ShopItem(String itemId, int quantity, int price) {
        public ShopItem(String itemId, int price) {
            this(itemId, 1, price);
        }
    }

    public record Shop(String title, List<ShopItem> items) {}

    public static final String ARMORY = "Armory";
    public static final String POTIONS = "Potions";
    public static final String RANGED = "Ranged";

    public static final Map<String, Shop> SHOPS = Map.of(
            ARMORY, new Shop("Outlander Armory", List.of(
                    // Copper
                    new ShopItem("Weapon_Sword_Copper",      40),
                    new ShopItem("Weapon_Shield_Copper",     30),
                    new ShopItem("Armor_Copper_Head",        30),
                    new ShopItem("Armor_Copper_Chest",       50),
                    new ShopItem("Armor_Copper_Hands",       25),
                    new ShopItem("Armor_Copper_Legs",        40),
                    // Iron
                    new ShopItem("Weapon_Sword_Iron",        100),
                    new ShopItem("Weapon_Shield_Iron",       80),
                    new ShopItem("Armor_Iron_Head",          70),
                    new ShopItem("Armor_Iron_Chest",         120),
                    new ShopItem("Armor_Iron_Hands",         60),
                    new ShopItem("Armor_Iron_Legs",          100),
                    // Thorium
                    new ShopItem("Weapon_Sword_Thorium",     200),
                    new ShopItem("Weapon_Shield_Thorium",    170),
                    new ShopItem("Armor_Thorium_Head",       140),
                    new ShopItem("Armor_Thorium_Chest",      240),
                    new ShopItem("Armor_Thorium_Hands",      120),
                    new ShopItem("Armor_Thorium_Legs",       200),
                    // Adamantite
                    new ShopItem("Weapon_Sword_Adamantite",  400),
                    new ShopItem("Weapon_Shield_Adamantite", 350),
                    new ShopItem("Armor_Adamantite_Head",    280),
                    new ShopItem("Armor_Adamantite_Chest",   480),
                    new ShopItem("Armor_Adamantite_Hands",   240),
                    new ShopItem("Armor_Adamantite_Legs",    400)
            )),
            POTIONS, new Shop("Klops Apothecary", List.of(
                    new ShopItem("Potion_Health_Small", 15),
                    new ShopItem("Potion_Health_Large", 30)
            )),
            // Every bow and crossbow fires Weapon_Arrow_Crude; other arrow types aren't usable as ammo.
            RANGED, new Shop("Wraith Fletcher", List.of(
                    new ShopItem("Weapon_Arrow_Crude", 25, 5),
                    new ShopItem("Weapon_Arrow_Crude", 100, 15),
                    new ShopItem("Weapon_Shortbow_Copper",        40),
                    new ShopItem("Weapon_Shortbow_Iron",          100),
                    new ShopItem("Weapon_Crossbow_Iron",          100),
                    new ShopItem("Weapon_Shortbow_Thorium",       200),
                    new ShopItem("Weapon_Crossbow_Ancient_Steel", 200),
                    new ShopItem("Weapon_Shortbow_Adamantite",    400)
            ))
    );

    private ShopCatalog() {}
}
