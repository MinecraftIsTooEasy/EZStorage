package com.zerofall.ezstorage;

import huix.glacier.api.extension.creativetab.GlacierCreativeTabs;

import com.zerofall.ezstorage.init.EZBlocks;

public class EZTab extends GlacierCreativeTabs {

    public static final EZTab TAB = new EZTab();

    public EZTab() {
        super(Reference.MOD_NAME);
    }

    public int getTabIconItemIndex() {
        return EZBlocks.condensed_storage_box.blockID;
    }
}