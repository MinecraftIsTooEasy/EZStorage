package com.zerofall.ezstorage.container;

import net.minecraft.EntityPlayer;
import net.minecraft.InventoryPlayer;
import net.minecraft.Container;
import net.minecraft.IInventory;
import net.minecraft.InventoryBasic;
import net.minecraft.Slot;
import net.minecraft.ItemStack;
import net.minecraft.ServerPlayer;

import com.zerofall.ezstorage.util.EZInventory;
import com.zerofall.ezstorage.util.EZInventoryManager;

public class ContainerStorageCore extends Container {

    /**
     * A read-only slot used for the storage display area.
     * MITE's Container.slotClick is final and cannot be overridden.  When the
     * player clicks a storage slot and our mouseClicked gives them an item on
     * the cursor, MITE's GuiContainer.mouseMovedOrUp fires on mouse-release and
     * calls handleMouseClick → playerController.windowClick → Packet102WindowClick
     * → server Container.slotClick.  If the slot says isItemValid(cursor)==true the
     * item gets deposited into the dummy slot and vanishes.
     * By returning false from both isItemValid and canTakeStack we make slotClick
     * a no-op for these slots, which is exactly what the original Forge 1.7.10
     * mod assumed (Forge's GuiContainer never sent a windowClick for these slots
     * because our mouseClicked overrode the full click path).
     */
    private static final class SlotReadOnly extends Slot {

        SlotReadOnly(IInventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override public boolean canTakeStack(EntityPlayer player) {
            return false;
        }
    }

    public EZInventory inventory = new EZInventory();
    /** Timestamp updated when an S2C inventory packet is received, used by GUI to detect updates. */
    public long inventoryUpdateTimestamp = System.currentTimeMillis();

    public ContainerStorageCore(EntityPlayer player, EZInventory inventory) {
        this(player);
        this.inventory = inventory;
    }

    public ContainerStorageCore(EntityPlayer player)
    {
        super(player);
        int startingY = 18;
        int startingX = 8;
        IInventory dummy = new InventoryBasic("Storage Core", false, this.rowCount() * 9);

        for (int i = 0; i < this.rowCount(); i++)
        {
            for (int j = 0; j < 9; j++)
            {
                addSlotToContainer(new SlotReadOnly(dummy, j + i * 9, startingX + j * 18, startingY + i * 18));
            }
        }

        bindPlayerInventory(player.inventory);
    }

    /** Called from S2CStoragePacket.apply() to signal the GUI that inventory was updated. */
    public void markInventoryUpdated() {
        this.inventoryUpdateTimestamp = System.currentTimeMillis();
    }

    protected void bindPlayerInventory(InventoryPlayer inventoryPlayer)
    {
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 9; j++)
            {
                addSlotToContainer(new Slot(
                        inventoryPlayer,
                        (j + i * 9) + 9,
                        playerInventoryX() + j * 18,
                        playerInventoryY() + i * 18));
            }
        }
        for (int i = 0; i < 9; i++)
        {
            addSlotToContainer(new Slot(inventoryPlayer, i, playerInventoryX() + i * 18, playerInventoryY() + 58));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    // Shift clicking
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index)
    {
        if (playerIn.worldObj.isRemote)
        {
            return null;
        }

        Slot slotObject = (Slot) inventorySlots.get(index);

        if (slotObject != null && slotObject.getHasStack())
        {
            ItemStack stackInSlot = slotObject.getStack();
            ItemStack movedStack = stackInSlot.copy();
            ItemStack remainingStack = this.inventory.input(stackInSlot);

            slotObject.putStack(remainingStack);
            slotObject.onSlotChanged();

            EZInventoryManager.sendToClients(inventory);
            this.detectAndSendChanges();

            if (playerIn instanceof ServerPlayer serverPlayer)
            {
                serverPlayer.sendContainerAndContentsToPlayer(this, this.getInventory());
            }

            if (remainingStack == null || remainingStack.stackSize != movedStack.stackSize)
            {
                return movedStack;
            }
        }

        return null;
    }

    // Note: slotClick is final in MITE's Container, so we intercept at the GUI level instead.
    // Storage slots (0..rowCount*9-1) are display-only; player inventory slots pass through normally.

    public ItemStack customSlotClick(int itemIndex, int clickedButton, int mode, EntityPlayer playerIn) {
        // In MITE singleplayer the client and server share the same JVM and the same
        // EZInventory object.  We must only mutate inventory on the server side; the
        // client side gets the authoritative state via S2CStoragePacket.
        if (playerIn.worldObj.isRemote) {
            return null;
        }

        ItemStack heldStack = playerIn.inventory.getItemStack();
        ItemStack result = null;
        boolean sendToClients = false;

        if (heldStack == null)
        {
            int type = (clickedButton == 1) ? 1 : 0;
            ItemStack stack = this.inventory.getItemsAt(itemIndex, type);

            if (stack != null)
            {
                if (clickedButton == 0 && mode == 1)
                {
                    if (!this.mergeItemStack(stack, this.rowCount() * 9, this.rowCount() * 9 + 36, true))
                    {
                        this.inventory.input(stack);
                    }
                }
                else
                {
                    playerIn.inventory.setItemStack(stack);
                }
                sendToClients = true;
                result = stack;
            }
        }
        else
        {
            playerIn.inventory.setItemStack(this.inventory.input(heldStack));
            sendToClients = true;
        }

        if (sendToClients)
        {
            EZInventoryManager.sendToClients(inventory);
            this.detectAndSendChanges();

            if (playerIn instanceof ServerPlayer serverPlayer)
            {
                serverPlayer.sendContainerAndContentsToPlayer(this, this.getInventory());
            }
        }

        return result;
    }

    protected int playerInventoryX() {
        return 8;
    }

    protected int playerInventoryY() {
        return 140;
    }

    protected int rowCount() {
        return 6;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn)
    {
        super.onContainerClosed(playerIn);

        if (!playerIn.worldObj.isRemote)
        {
            this.inventory.sort();
            EZInventoryManager.sendToClients(inventory);
        }
    }
}