package com.zerofall.ezstorage.block;

import net.minecraft.Material;

import com.zerofall.ezstorage.configuration.EZConfiguration;

public class BlockHyperStorage extends BlockStorage {

    public BlockHyperStorage(int id) {
        super(id, "hyper_storage_box", Material.obsidian);
    }

    @Override
    public int getCapacity() {
        return EZConfiguration.hyperCapacity.getIntegerValue();
    }
}