package com.zerofall.ezstorage.block;

import net.minecraft.Material;

import com.zerofall.ezstorage.configuration.EZConfiguration;

public class BlockStorage extends StorageMultiblock {

    public BlockStorage(int id) {
        super(id, "storage_box", Material.wood);
    }

    public BlockStorage(int id, String name, Material material) {
        super(id, name, material);
    }

    public int getCapacity() {
        return EZConfiguration.basicCapacity.getIntegerValue();
    }
}