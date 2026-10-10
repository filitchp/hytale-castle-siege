package dev.dooondi.shop;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.InstructionType;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringNotEmptyValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;

import java.util.EnumSet;

/**
 * Registered with NPCPlugin as {@link #TYPE}; usable only inside a role's InteractionInstruction.
 * {@code "Shop"} names a store in {@link ShopCatalog#SHOPS}.
 */
public class OpenShopActionBuilder extends BuilderActionBase {

    public static final String TYPE = "OpenCastleSiegeShop";

    private String shopId;

    @Override
    public String getShortDescription() {
        return "Open the Castle Siege shop UI for the interacting player";
    }

    @Override
    public String getLongDescription() {
        return getShortDescription();
    }

    @Override
    public BuilderDescriptorState getBuilderDescriptorState() {
        return BuilderDescriptorState.Stable;
    }

    @Override
    public OpenShopActionBuilder readConfig(JsonElement data) {
        requireString(data, "Shop", id -> shopId = id, StringNotEmptyValidator.get(),
                BuilderDescriptorState.Stable, "The Castle Siege shop to open", null);
        requireInstructionType(EnumSet.of(InstructionType.Interaction));
        return this;
    }

    public String getShopId() {
        return shopId;
    }

    @Override
    public Action build(BuilderSupport support) {
        return new OpenShopAction(this);
    }
}
