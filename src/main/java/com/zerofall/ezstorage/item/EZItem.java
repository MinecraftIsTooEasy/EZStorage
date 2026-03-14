package com.zerofall.ezstorage.item;

import net.minecraft.Item;

import com.zerofall.ezstorage.EZTab;
import com.zerofall.ezstorage.Reference;

public class EZItem extends Item {

    public EZItem(String name) {
        setCreativeTab(EZTab.TAB);
        setUnlocalizedName(name);
        setTextureName(Reference.MOD_ID + ":" + name);
    }
}