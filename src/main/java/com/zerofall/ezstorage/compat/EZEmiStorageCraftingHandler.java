package com.zerofall.ezstorage.compat;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.zerofall.ezstorage.container.ContainerStorageCoreCrafting;
import com.zerofall.ezstorage.network.C2S.C2SReqCraftingPacket;
import com.zerofall.ezstorage.util.EZInventory;

import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import moddedmite.rustedironcore.network.Network;
import net.minecraft.Minecraft;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;
import net.minecraft.NBTTagList;
import net.minecraft.Slot;
import shims.java.com.unascribed.retroemi.ItemStacks;

/**
 * EMI recipe fill handler for EZStorage crafting terminal.
 * Uses EZStorage's own C2S auto-fill packet so ingredients can come from network storage.
 */
public class EZEmiStorageCraftingHandler implements StandardRecipeHandler<ContainerStorageCoreCrafting> {

    @Override
    public List<Slot> getInputSources(ContainerStorageCoreCrafting handler)
    {
        List<Slot> list = Lists.newArrayList();
        list.addAll(getCraftingSlots(handler));

        int size = handler.inventorySlots.size();
        int playerStart = size - 46;
        int playerEnd = size - 10;

        for (int i = playerStart; i < playerEnd; i++)
        {
            list.add(handler.getSlot(i));
        }

        return list;
    }

    @Override
    public List<Slot> getCraftingSlots(ContainerStorageCoreCrafting handler)
    {
        List<Slot> list = Lists.newArrayList();
        int size = handler.inventorySlots.size();
        int start = size - 9;

        for (int i = start; i < size; i++)
        {
            list.add(handler.getSlot(i));
        }

        return list;
    }

    @Override
    public Slot getOutputSlot(ContainerStorageCoreCrafting handler)
    {
        int outputIndex = handler.inventorySlots.size() - 10;
        return handler.getSlot(outputIndex);
    }

    @Override
    public EmiPlayerInventory getInventory(net.minecraft.GuiContainer screen)
    {
        ContainerStorageCoreCrafting handler = (ContainerStorageCoreCrafting)screen.inventorySlots;
        List<EmiStack> stacks = new ArrayList<>();

        for (Slot slot : getInputSources(handler))
        {
            if (slot == null)
            {
                continue;
            }

            ItemStack stack = slot.getStack();

            if (!ItemStacks.isEmpty(stack))
            {
                stacks.add(EmiStack.of(stack));
            }
        }

        EZInventory storageInventory = handler.inventory;

        if (storageInventory != null)
        {
            for (ItemStack group : storageInventory.inventory)
            {
                if (group != null && group.stackSize > 0)
                {
                    stacks.add(EmiStack.of(group));
                }
            }
        }

        return new EmiPlayerInventory(stacks);
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<ContainerStorageCoreCrafting> context)
    {
        return supportsRecipe(recipe) && context.getInventory().canCraft(recipe);
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<ContainerStorageCoreCrafting> context)
    {
        if (!supportsRecipe(recipe))
        {
            return false;
        }

        NBTTagCompound tag = new NBTTagCompound();
        List<EmiIngredient> inputs = recipe.getInputs();

        for (int i = 0; i < 9; i++)
        {
            NBTTagList list = new NBTTagList();

            if (i < inputs.size())
            {
                EmiIngredient ingredient = inputs.get(i);

                if (ingredient != null)
                {
                    for (EmiStack emiStack : ingredient.getEmiStacks())
                    {
                        ItemStack itemStack = emiStack.getItemStack();

                        if (ItemStacks.isEmpty(itemStack))
                        {
                            continue;
                        }

                        ItemStack copy = itemStack.copy();
                        copy.stackSize = Math.max(1, (int)Math.min(Integer.MAX_VALUE, emiStack.getAmount()));

                        NBTTagCompound stackTag = new NBTTagCompound();
                        copy.writeToNBT(stackTag);
                        list.appendTag(stackTag);
                    }
                }
            }

            tag.setTag("#" + i, list);
        }

        Network.sendToServer(new C2SReqCraftingPacket(tag));
        Minecraft.getMinecraft().displayGuiScreen(context.getScreen());
        return true;
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe)
    {
        return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && recipe.supportsRecipeTree();
    }
}