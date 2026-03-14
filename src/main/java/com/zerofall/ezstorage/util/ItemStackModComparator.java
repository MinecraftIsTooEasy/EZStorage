package com.zerofall.ezstorage.util;

import java.util.Comparator;

import net.minecraft.ItemStack;

public class ItemStackModComparator implements Comparator<ItemStack> {

    @Override
    public int compare(ItemStack o1, ItemStack o2) {
        String m1 = getModId(o1);
        String m2 = getModId(o2);
        return m1.compareToIgnoreCase(m2);
    }

    private String getModId(ItemStack stack) {
        // Unlocalized names often have format "tile.modid:name" or "item.modid.name"
        // We use the unlocalized item name as a best-effort mod ID proxy
        String name = stack.getItem().getUnlocalizedName(stack);
        if (name != null && name.contains(":")) {
            return name.split(":")[0];
        }
        return "minecraft";
    }
}