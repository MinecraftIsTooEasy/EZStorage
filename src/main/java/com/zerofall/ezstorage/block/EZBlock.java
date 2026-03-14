package com.zerofall.ezstorage.block;

import net.minecraft.Block;
import net.minecraft.BlockConstants;
import net.minecraft.Material;

import com.zerofall.ezstorage.EZTab;
import com.zerofall.ezstorage.Reference;

public class EZBlock extends Block {

    protected EZBlock(int id, String name, Material materialIn) {
        super(id, materialIn, new BlockConstants());
        this.setUnlocalizedName(name);
        this.setTextureName(Reference.MOD_ID + ":" + name);
        this.setCreativeTab(EZTab.TAB);
        this.setHardness(1.6F);
    }

}