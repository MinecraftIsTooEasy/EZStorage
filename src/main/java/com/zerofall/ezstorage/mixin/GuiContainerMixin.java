package com.zerofall.ezstorage.mixin;

import com.zerofall.ezstorage.container.ContainerStorageCoreCrafting;
import net.minecraft.GuiContainer;
import net.minecraft.ItemStack;
import net.minecraft.Slot;
import net.minecraft.SlotCrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public abstract class GuiContainerMixin {
    @Inject(method = "drawItemStackTooltip(Lnet/minecraft/ItemStack;IILnet/minecraft/Slot;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void ezstorage$preventMiteContainerCraftingCastCrash(ItemStack stack, int x, int y, Slot slot, CallbackInfo ci)
    {
        GuiContainer self = (GuiContainer)(Object)this;
        // MITE's GuiContainer assumes SlotCrafting belongs to MITEContainerCrafting and casts.
        // EZStorage uses a custom container, so cancel this one dangerous path only.
        if (self.inventorySlots instanceof ContainerStorageCoreCrafting && slot instanceof SlotCrafting)
        {
            ci.cancel();
        }
    }
}