package com.zerofall.ezstorage.compat;

import com.zerofall.ezstorage.init.EZBlocks;
import net.minecraft.Block;
import net.minecraft.EntityPlayer;
import tschipp.carryon.api.CarryOnPlugin;

public class EZCarryOnCompat implements CarryOnPlugin {

    @Override
    public boolean denyCarryBlock(EntityPlayer player, Block block, int meta)
    {
        return block == EZBlocks.storage_core || block == EZBlocks.crafting_box || block == EZBlocks.input_port;
    }
}