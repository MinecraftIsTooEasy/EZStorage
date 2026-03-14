package com.zerofall.ezstorage.network.S2C;

import com.zerofall.ezstorage.EZStorage;
import com.zerofall.ezstorage.container.ContainerStorageCore;
import com.zerofall.ezstorage.container.ContainerStorageCoreCrafting;
import com.zerofall.ezstorage.util.EZInventory;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.CompressedStreamTools;
import net.minecraft.EntityPlayer;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;
import net.minecraft.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * S2C: Server sends storage inventory data to client.
 */
public class S2CStoragePacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation(EZStorage.MOD_ID, "s2c_storage");

    private final byte[] nbtBytes;

    /** Construct from an EZInventory (server-side). */
    public S2CStoragePacket(EZInventory inventory)
    {
        byte[] bytes = new byte[0];

        try
        {
            NBTTagCompound tag = new NBTTagCompound();

            if (inventory != null)
            {
                inventory.writeToNBT(tag);
            }
            bytes = CompressedStreamTools.compress(tag);
        }
        catch (Exception exception)
        {
            EZStorage.instance.LOG.error("Failed to serialize EZInventory", exception);
        }

        this.nbtBytes = bytes;
    }

    /** Construct from buf (client-side deserialization). */
    public S2CStoragePacket(PacketByteBuf buf)
    {
        int len = buf.readInt();
        byte[] bytes = new byte[len];

        if (len > 0)
        {
            buf.readFully(bytes);
        }

        this.nbtBytes = bytes;
    }

    @Override
    public void write(PacketByteBuf buf)
    {
        buf.writeInt(nbtBytes.length);

        if (nbtBytes.length > 0)
        {
            buf.write(nbtBytes, 0, nbtBytes.length);
        }
    }

    @Override
    public void apply(EntityPlayer player)
    {
        // Client-side: update the open container
        if (player.openContainer instanceof ContainerStorageCore container)
        {
            try
            {
                if (nbtBytes.length > 0)
                {
                    NBTTagCompound tag = CompressedStreamTools.readCompressed(new ByteArrayInputStream(nbtBytes));
                    container.inventory.readFromNBT(tag);
                    container.markInventoryUpdated();

                    // If this is a crafting container, also sync the craftMatrix InventoryCrafting
                    // so the crafting grid slots show up without needing a click.
                    if (container instanceof ContainerStorageCoreCrafting craftingContainer)
                    {
                        ItemStack[] matrix = craftingContainer.inventory.craftMatrix;

                        if (matrix != null)
                        {
                            for (int i = 0; i < 9; i++)
                            {
                                ItemStack stack = i < matrix.length ? matrix[i] : null;
                                craftingContainer.craftMatrix.setInventorySlotContents(i, stack == null ? null : stack.copy());
                            }
                            craftingContainer.onCraftMatrixChanged(craftingContainer.craftMatrix);
                        }
                    }
                }
            }
            catch (Exception exception)
            {
                EZStorage.instance.LOG.error("Failed to deserialize EZInventory", exception);
            }
        }
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}