package com.zerofall.ezstorage.item;

import java.util.List;

import net.minecraft.*;

import com.zerofall.ezstorage.block.BlockCraftingBox;

public class ItemBlockCraftingBox extends ItemBlock {

    public ItemBlockCraftingBox(Block block) {
        super(block);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName(stack);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, List info, boolean extended, Slot slot)
    {
        int tier = stack.getItemSubtype();
        String tierName = BlockCraftingBox.getTierName(tier);
        info.add(StatCollector.translateToLocalFormatted("ezstorage.crafting_box.tier_label", tierName));

        if (tier < BlockCraftingBox.getMaxTier())
        {
            info.add(StatCollector.translateToLocal("ezstorage.crafting_box.upgrade_hint"));
        }
    }
}