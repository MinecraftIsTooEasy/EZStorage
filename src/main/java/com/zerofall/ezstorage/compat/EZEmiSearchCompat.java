package com.zerofall.ezstorage.compat;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import net.minecraft.ItemStack;

public class EZEmiSearchCompat {

    private EZEmiSearchCompat() {
    }

    public static ItemStack getHoveredItemStack() {
        EmiStackInteraction interaction = EmiApi.getHoveredStack(false);
        if (interaction == null || interaction.isEmpty()) {
            return null;
        }

        EmiIngredient ingredient = interaction.getStack();
        if (ingredient == null || ingredient.isEmpty()) {
            return null;
        }

        for (EmiStack emiStack : ingredient.getEmiStacks()) {
            if (emiStack == null || emiStack.isEmpty()) {
                continue;
            }

            ItemStack stack = emiStack.getItemStack();
            if (stack != null && stack.getItem() != null) {
                return stack;
            }
        }

        return null;
    }
}
