package com.zerofall.ezstorage.tileentity;

import net.minecraft.EntityPlayer;
import net.minecraft.ISidedInventory;
import net.minecraft.ItemStack;
import net.minecraft.TileEntity;

import com.zerofall.ezstorage.configuration.EZConfiguration;
import com.zerofall.ezstorage.util.EZInventory;

public class TileEntityInventoryProxy extends TileEntity implements ISidedInventory {

    public TileEntityStorageCore core;

    public EZInventory getInventory() {
        if (core != null) return core.getInventory();
        return null;
    }

    @Override public int getSizeInventory()
    {
        EZInventory inventory = getInventory();

        if (inventory == null) return 1;

        int size = inventory.inventory.size();

        if (inventory.getTotalCount() < inventory.maxItems
            && (EZConfiguration.maxItemTypes.getIntegerValue() == 0
                || inventory.slotCount() < EZConfiguration.maxItemTypes.getIntegerValue()))
        {
            size += 1;
        }

        return size;
    }

    @Override public ItemStack getStackInSlot(int index)
    {
        EZInventory inventory = getInventory();

        if (inventory != null && index < inventory.inventory.size()) return inventory.inventory.get(index);
        return null;
    }

    @Override public ItemStack decrStackSize(int index, int count)
    {
        EZInventory inventory = getInventory();

        if (inventory == null) return null;

        ItemStack result = inventory.getItemStackAt(index, count);
        core.updateTileEntity();
        return result;
    }

    @Override public ItemStack getStackInSlotOnClosing(int index)
    {
        ItemStack stack = getStackInSlot(index);

        if (stack != null) { setInventorySlotContents(index, null); core.updateTileEntity(); }
        return stack;
    }

    @Override public void setInventorySlotContents(int index, ItemStack stack)
    {
        EZInventory inventory = getInventory();

        if (inventory == null) return;

        else if (stack == null || stack.stackSize == 0)
        {
            if (index >= 0 && index < inventory.inventory.size())
            {
                inventory.inventory.remove(index);
            }
            else
            {
                return;
            }
        }

        else if (index >= inventory.inventory.size()) inventory.input(stack);

        else if (isItemValidForSlot(index, stack)) inventory.inventory.set(index, stack);

        else return;

        core.updateTileEntity();
    }

    @Override public int getInventoryStackLimit()
    {
        EZInventory inventory = getInventory();

        if (inventory == null) return 0;

        return (int) Math.min(inventory.maxItems, Integer.MAX_VALUE);
    }

    @Override public void onInventoryChanged() {}

    @Override public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override public void openChest() {}

    @Override public void closeChest() {}

    @Override public void destroyInventory() {}

    @Override public boolean isItemValidForSlot(int index, ItemStack stack)
    {
        EZInventory inventory = getInventory();

        if (inventory == null) return false;

        int foundIndex = -1;

        for (int i = 0; i < inventory.inventory.size(); i++)
        {
            if (EZInventory.stacksEqual(inventory.inventory.get(i), stack)) foundIndex = i;
        }
        if (index >= inventory.inventory.size()) return true;

        if (foundIndex == index) return true;

        return false;
    }

    @Override public int[] getAccessibleSlotsFromSide(int side)
    {
        int size = getSizeInventory();
        int[] slots = new int[size];

        for (int i = 0; i < size; i++) slots[i] = i;
        return slots;
    }

    @Override public boolean canInsertItem(int index, ItemStack stack, int direction) {
        return isItemValidForSlot(index, stack);
    }

    @Override public boolean canExtractItem(int index, ItemStack stack, int direction)
    {
        EZInventory inventory = getInventory();

        if (inventory == null || index >= inventory.inventory.size()) return false;

        ItemStack theGroup = inventory.inventory.get(index);

        return theGroup != null && EZInventory.stacksEqual(theGroup, stack);
    }
}