package com.zerofall.ezstorage.network.C2S;

import com.zerofall.ezstorage.container.ContainerStorageCore;
import com.zerofall.ezstorage.container.ContainerStorageCoreCrafting;
import com.zerofall.ezstorage.network.S2C.S2CCursorItemPacket;
import com.zerofall.ezstorage.EZStorage;
import com.zerofall.ezstorage.util.EZInventoryManager;

import moddedmite.rustedironcore.network.Network;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;

import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import net.minecraft.ServerPlayer;

/**
 * C2S: Client clicks a storage slot.
 */
public class C2SInvSlotClickedPacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation(EZStorage.MOD_ID, "c2s_slot_click");

    private final int index;
    private final int button;
    private final int mode;

    public C2SInvSlotClickedPacket(int index, int button, int mode) {
        this.index = index;
        this.button = button;
        this.mode = mode;
    }

    public C2SInvSlotClickedPacket(PacketByteBuf buf) {
        this.index = buf.readVarInt();
        this.button = buf.readVarInt();
        this.mode = buf.readVarInt();
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(index);
        buf.writeVarInt(button);
        buf.writeVarInt(mode);
    }

    @Override
    public void apply(EntityPlayer player)
    {
        if (player.openContainer instanceof ContainerStorageCore storageContainer)
        {
            storageContainer.customSlotClick(index, button, mode, player);
            // Persist crafting grid to inventory before broadcasting so S2C includes it
            if (storageContainer instanceof ContainerStorageCoreCrafting craftingContainer)
            {
                craftingContainer.saveGrid();
                EZInventoryManager.sendToClients(storageContainer.inventory);
            }
            // Sync cursor item back to the client so it appears immediately
            if (player instanceof ServerPlayer serverPlayer)
            {
                serverPlayer.sendContainerAndContentsToPlayer(storageContainer, storageContainer.getInventory());
                Network.sendToClient(serverPlayer, new S2CCursorItemPacket(player.inventory.getItemStack()));
            }

            // Force sync container slots (including player inventory) to client
            storageContainer.detectAndSendChanges();
        }
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}