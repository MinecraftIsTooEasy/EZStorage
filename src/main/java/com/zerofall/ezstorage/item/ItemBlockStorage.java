package com.zerofall.ezstorage.item;

import java.util.List;

import net.minecraft.*;

import com.zerofall.ezstorage.block.BlockStorage;

public class ItemBlockStorage extends ItemBlock {

    public ItemBlockStorage(Block block) {
        super(block);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack item_stack, EntityPlayer player, List info, boolean extended_info, Slot slot)
    {
        if (item_stack.getItem() instanceof ItemBlockStorage itemBlockStorage
            && itemBlockStorage.getBlock() instanceof BlockStorage blockStorage)
        {
            info.add(StatCollector.translateToLocalFormatted("hud.msg.ezstorage.storage.capacity", blockStorage.getCapacity()));
        }
    }
}