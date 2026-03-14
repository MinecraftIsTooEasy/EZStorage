package com.zerofall.ezstorage.block;

import net.minecraft.EntityLivingBase;
import net.minecraft.Material;
import net.minecraft.TileEntity;
import net.minecraft.World;

import com.zerofall.ezstorage.tileentity.TileEntityStorageCore;
import com.zerofall.ezstorage.util.EZInventoryManager;

public class BlockStorageCore extends StorageUserInterface {

    public BlockStorageCore(int id) {
        super(id, "storage_core", Material.wood);
        this.setResistance(6000.0f);
    }

    @Override
    public TileEntity createNewTileEntity(World world) {
        return new TileEntityStorageCore();
    }

    @Override
    public void breakBlock(World worldIn, int x, int y, int z, int blockId, int meta)
    {
        TileEntity te = worldIn.getBlockTileEntity(x, y, z);
        if (te instanceof TileEntityStorageCore core) {
            if (!core.hasStoredItems()) {
                var inventory = core.getInventory();
                if (inventory != null) {
                    EZInventoryManager.deleteInventory(inventory);
                }
            }
        }

        super.breakBlock(worldIn, x, y, z, blockId, meta);
    }

    @Override
    public boolean isPortable(World world, EntityLivingBase entity_living_base, int x, int y, int z) {
        return true;
    }

}