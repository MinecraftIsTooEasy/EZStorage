package com.zerofall.ezstorage.network.C2S;

import com.zerofall.ezstorage.EZStorage;
import com.zerofall.ezstorage.container.ContainerStorageCoreCrafting;
import com.zerofall.ezstorage.util.EZInventoryManager;

import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;

import net.minecraft.*;

import java.io.ByteArrayInputStream;

/**
 * C2S: Client requests auto-populating the crafting grid.
 */
public class C2SReqCraftingPacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation(EZStorage.MOD_ID, "c2s_req_crafting");

    private final NBTTagCompound recipe;

    public C2SReqCraftingPacket(NBTTagCompound recipe) {
        this.recipe = recipe;
    }

    public C2SReqCraftingPacket(PacketByteBuf buf)
    {
        int len = buf.readInt();
        byte[] bytes = new byte[len];

        if (len > 0)
        {
            buf.readFully(bytes);
        }
        NBTTagCompound tag = null;

        if (bytes.length > 0)
        {
            try
            {
                tag = CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes));
            }
            catch (Exception exception)
            {
                EZStorage.instance.LOG.error("Failed to read recipe NBT", exception);
            }
        }

        this.recipe = tag != null ? tag : new NBTTagCompound();
    }

    @Override
    public void write(PacketByteBuf buf)
    {
        byte[] bytes = new byte[0];

        try
        {
            bytes = CompressedStreamTools.compress(recipe);
        }
        catch (Exception exception)
        {
            EZStorage.instance.LOG.error("Failed to write recipe NBT", exception);
        }
        buf.writeInt(bytes.length);

        if (bytes.length > 0)
        {
            buf.write(bytes, 0, bytes.length);
        }
    }

    @Override
    public void apply(EntityPlayer player)
    {
        if (!(player.openContainer instanceof ContainerStorageCoreCrafting con)) return;

        ItemStack[][] grid = new ItemStack[9][];

        for (int x = 0; x < 9; x++)
        {
            NBTTagList list = recipe.getTagList("#" + x);

            if (list.tagCount() > 0)
            {
                grid[x] = new ItemStack[list.tagCount()];

                for (int y = 0; y < list.tagCount(); y++)
                {
                    grid[x][y] = ItemStack.loadItemStackFromNBT((NBTTagCompound) list.tagAt(y));
                }
            }
        }

        if (con.tryToPopulateCraftingGrid(grid, player, true))
        {
            con.saveGrid(); // persist matrix to inventory before broadcast
            EZInventoryManager.sendToClients(con.inventory);
        }
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}
