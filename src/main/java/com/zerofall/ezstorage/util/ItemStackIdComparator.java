package com.zerofall.ezstorage.util;

import java.util.Comparator;

import net.minecraft.ItemStack;

public class ItemStackIdComparator implements Comparator<ItemStack> {

    @Override
    public int compare(ItemStack o1, ItemStack o2) {
        int idCompare = Integer.compare(o2.itemID, o1.itemID);
        if (idCompare != 0) {
            return idCompare;
        }

        int subtypeCompare = Integer.compare(getComparableSubtype(o2), getComparableSubtype(o1));
        if (subtypeCompare != 0) {
            return subtypeCompare;
        }

        String n1 = o1.getDisplayName();
        String n2 = o2.getDisplayName();
        if (n1 == null) n1 = "";
        if (n2 == null) n2 = "";
        return n2.compareToIgnoreCase(n1);
    }

    private static int getComparableSubtype(ItemStack stack) {
        try {
            return stack.getItemSubtype();
        } catch (Throwable ignored) {
            return stack.getItemDamage();
        }
    }
}
