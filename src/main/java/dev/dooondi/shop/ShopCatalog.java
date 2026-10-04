package dev.dooondi.shop;

import java.util.List;

/**
 * Items sold by the Castle Siege merchant, in display order. Each tier fills
 * one row of the shop grid. To add an item or change a price, edit {@link #ITEMS}.
 */
public final class ShopCatalog {

    public record ShopItem(String itemId, int price) {}

    public static final List<ShopItem> ITEMS = List.of(
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
    );

    private ShopCatalog() {}
}
