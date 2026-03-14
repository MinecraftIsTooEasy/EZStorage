package com.zerofall.ezstorage.mixin;

import com.zerofall.ezstorage.container.ContainerStorageCoreCrafting;
import com.zerofall.ezstorage.configuration.EZConfiguration;
import com.zerofall.ezstorage.util.EZInventoryManager;
import net.minecraft.Block;
import net.minecraft.BlockWorkbench;
import net.minecraft.CraftingResult;
import net.minecraft.EntityPlayer;
import net.minecraft.IInventory;
import net.minecraft.Item;
import net.minecraft.InventoryPlayer;
import net.minecraft.ItemStack;
import net.minecraft.SlotCrafting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlotCrafting.class)
public abstract class SlotCraftingMixin {

    @Final @Shadow private IInventory craftMatrix;
    @Shadow private EntityPlayer thePlayer;
    @Shadow private int amountCrafted;
    @Shadow public CraftingResult crafting_result;

    @Shadow protected abstract void onCrafting(ItemStack par1ItemStack);

    @Inject(method = "onPickupFromSlot", at = @At("HEAD"), cancellable = true)
    private void ezstorage$routeCraftingOutput(EntityPlayer player, ItemStack craftedStack, CallbackInfo ci)
    {
        if (!(this.thePlayer.openContainer instanceof ContainerStorageCoreCrafting container))
        {
            return;
        }

        if (craftedStack == null || craftedStack.stackSize <= 0)
        {
            ci.cancel();
            return;
        }

        container.beginSuppressCraftingReset();

        try
        {
            ItemStack[][] recipe = new ItemStack[9][];

            for (int i = 0; i < recipe.length; i++)
            {
                ItemStack matrixStack = this.craftMatrix.getStackInSlot(i);

                if (matrixStack != null)
                {
                    recipe[i] = new ItemStack[] { matrixStack.copy() };
                }
            }

            int consumption = this.crafting_result == null ? 1 : this.crafting_result.consumption;
            this.amountCrafted = craftedStack.stackSize;
            this.onCrafting(craftedStack);

            ItemStack remaining = craftedStack.copy();
            InventoryPlayer playerInventory = this.thePlayer.inventory;

            // First: prioritize player inventory (vanilla behavior).
            playerInventory.addItemStackToInventory(remaining);

            if (remaining.stackSize > 0)
            {
                // Second: route overflow into EZ storage.
                remaining = container.inventory.input(remaining);

                // Third: if storage is full, drop the remaining stack.
                if (remaining != null && remaining.stackSize > 0)
                {
                    this.thePlayer.dropPlayerItem(remaining);
                }
            }

            int xpReclaimed = 0;

            for (int i = 0; i < this.craftMatrix.getSizeInventory(); ++i)
            {
                ItemStack matrixStack = this.craftMatrix.getStackInSlot(i);

                if (matrixStack == null)
                {
                    continue;
                }

                Item item = matrixStack.getItem();

                if (item instanceof net.minecraft.ItemCoin coin)
                {
                    xpReclaimed += coin.getExperienceValue();
                }

                this.craftMatrix.decrStackSize(i, consumption);

                if (matrixStack.getItem().hasContainerItem())
                {
                    ItemStack containerItem = new ItemStack(matrixStack.getItem().getContainerItem());
                    Item containerItemType = containerItem.getItem();

                    if (containerItemType.getClass() == craftedStack.getItem().getClass()
                        || matrixStack.getItem().doesContainerItemLeaveCraftingGrid(matrixStack)
                        && this.thePlayer.inventory.addItemStackToInventory(containerItem))
                    {
                        continue;
                    }

                    if (this.craftMatrix.getStackInSlot(i) == null)
                    {
                        this.craftMatrix.setInventorySlotContents(i, containerItem);
                        continue;
                    }

                    this.thePlayer.dropPlayerItem(containerItem);
                    continue;
                }

                if (matrixStack.itemID == Block.workbench.blockID)
                {
                    this.thePlayer.inventory.addItemStackToInventoryOrDropIt(BlockWorkbench.getBlockComponent(matrixStack.getItemSubtype()));
                }
            }

            if (xpReclaimed > 0)
            {
                player.addExperience(xpReclaimed, true, false);
            }

            if (EZConfiguration.guiAutoRefill.getBooleanValue())
            {
                container.tryToPopulateCraftingGrid(recipe, player, false);
            }

            container.saveGrid();
            EZInventoryManager.sendToClients(container.inventory);
            container.detectAndSendChanges();
        }
        finally
        {
            container.endSuppressCraftingReset();
        }

        ci.cancel();
    }
}