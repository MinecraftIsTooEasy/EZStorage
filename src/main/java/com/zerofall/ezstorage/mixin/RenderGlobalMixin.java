package com.zerofall.ezstorage.mixin;

import java.util.List;

import com.zerofall.ezstorage.client.renderer.StorageCoreCraftingPreviewRenderer;
import com.zerofall.ezstorage.configuration.EZConfiguration;
import com.zerofall.ezstorage.tileentity.TileEntityStorageCore;

import net.minecraft.ICamera;
import net.minecraft.RenderGlobal;
import net.minecraft.TileEntity;
import net.minecraft.Vec3;
import net.minecraft.WorldClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
@SuppressWarnings("rawtypes")
public abstract class RenderGlobalMixin {

    @Shadow
    private WorldClient theWorld;

    @Inject(method = "renderEntities", at = @At("TAIL"), require = 0)
    private void ezstorage$renderCraftingPreview(Vec3 cameraPos, ICamera camera, float partialTicks, CallbackInfo ci)
    {
        if (!EZConfiguration.renderCraftingPreview.getBooleanValue())
        {
            return;
        }

        if (this.theWorld == null || this.theWorld.loadedTileEntityList == null)
        {
            return;
        }

        List loaded = this.theWorld.loadedTileEntityList;

        for (int i = 0; i < loaded.size(); i++)
        {
            Object entry = loaded.get(i);

            if (entry instanceof TileEntity tileEntity && tileEntity instanceof TileEntityStorageCore core)
            {
                StorageCoreCraftingPreviewRenderer.renderCorePreviewWorld(core, partialTicks);
            }
        }
    }
}