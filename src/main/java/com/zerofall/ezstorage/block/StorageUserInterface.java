package com.zerofall.ezstorage.block;

import net.minecraft.ChatMessageComponent;
import net.minecraft.EntityPlayer;
import net.minecraft.EnumFace;
import net.minecraft.Material;
import net.minecraft.ServerPlayer;
import net.minecraft.TileEntity;
import net.minecraft.World;

import com.zerofall.ezstorage.tileentity.TileEntityStorageCore;
import com.zerofall.ezstorage.util.BlockRef;
import com.zerofall.ezstorage.util.EZInventory;

public abstract class StorageUserInterface extends EZBlockContainer {

    protected StorageUserInterface(int id, String name, Material materialIn) {
        super(id, name, materialIn);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, EnumFace face, float offset_x, float offset_y, float offset_z)
    {
        if (!world.isRemote && player instanceof ServerPlayer serverPlayer)
        {
            TileEntityStorageCore core;
            TileEntity tileEntity = world.getBlockTileEntity(x, y, z);

            if (tileEntity instanceof TileEntityStorageCore)
            {
                core = (TileEntityStorageCore) tileEntity;
            }
            else
            {
                BlockRef blockRef = new BlockRef(this, x, y, z);
                core = findCore(blockRef, world, null);
            }

            if (core == null)
            {
                return sendNoCoreMessage(serverPlayer);
            }

            EZInventory inventory = core.getInventory();
            if (inventory != null)
            {
                openPlayerInventoryGui(serverPlayer, inventory, world, x, y, z, core);
            }
        }

        return true;
    }

    private boolean sendNoCoreMessage(ServerPlayer serverPlayer) {
        serverPlayer.sendChatToPlayer(ChatMessageComponent.createFromTranslationKey("chat.msg.storagecore_not_found"));
        return true;
    }
}