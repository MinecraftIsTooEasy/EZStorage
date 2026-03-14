package com.zerofall.ezstorage.block;

import net.minecraft.Material;
import net.minecraft.TileEntity;
import net.minecraft.World;

import com.zerofall.ezstorage.tileentity.TileEntityInventoryProxy;

public class BlockInputPort extends EZBlockContainer {

    public BlockInputPort(int id) {
        super(id, "input_port", Material.iron);
    }

    @Override
    public TileEntity createNewTileEntity(World world) {
        return new TileEntityInventoryProxy();
    }
}