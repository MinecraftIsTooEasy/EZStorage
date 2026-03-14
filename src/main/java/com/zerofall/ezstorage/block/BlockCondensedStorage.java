package com.zerofall.ezstorage.block;

import net.minecraft.Material;

import com.zerofall.ezstorage.configuration.EZConfiguration;

public class BlockCondensedStorage extends BlockStorage {

    public BlockCondensedStorage(int id) {
        super(id, "condensed_storage_box", Material.iron);
    }

    @Override
    public int getCapacity() {
        return EZConfiguration.condensedCapacity.getIntegerValue();
    }
}