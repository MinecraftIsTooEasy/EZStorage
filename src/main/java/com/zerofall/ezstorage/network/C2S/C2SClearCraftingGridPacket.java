package com.zerofall.ezstorage.network.C2S;

import com.zerofall.ezstorage.EZStorage;
import com.zerofall.ezstorage.container.ContainerStorageCoreCrafting;
import com.zerofall.ezstorage.util.EZInventoryManager;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;

/**
 * C2S: Client requests clearing the crafting grid.
 */
public class C2SClearCraftingGridPacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation(EZStorage.MOD_ID, "c2s_clear_crafting");

    public C2SClearCraftingGridPacket() {}

    public C2SClearCraftingGridPacket(PacketByteBuf buf) {}

    @Override
    public void write(PacketByteBuf buf) {}

    @Override
    public void apply(EntityPlayer player) {
        if (player.openContainer instanceof ContainerStorageCoreCrafting container) {
            container.clearGrid(player);
        }
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}