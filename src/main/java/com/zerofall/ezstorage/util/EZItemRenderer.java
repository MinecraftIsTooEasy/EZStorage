package com.zerofall.ezstorage.util;

import net.minecraft.FontRenderer;
import net.minecraft.Minecraft;
import net.minecraft.Tessellator;
import net.minecraft.RenderItem;
import net.minecraft.ItemStack;

import org.lwjgl.opengl.GL11;


public class EZItemRenderer extends RenderItem {

    public void renderItemOverlayIntoGUI(FontRenderer fontRenderer, ItemStack stack, int xPosition, int yPosition, String text)
    {
        if (stack != null)
        {
            super.renderItemOverlayIntoGUI(fontRenderer, Minecraft.getMinecraft().getTextureManager(), stack, xPosition, yPosition, "");

            float ScaleFactor = 0.5f;
            float RScaleFactor = 1.0f / ScaleFactor;
            int offset = 0;

            boolean unicodeFlag = fontRenderer.getUnicodeFlag();
            fontRenderer.setUnicodeFlag(false);

            long amount = Long.parseLong(text);

            if (amount > 999999999999L) amount = 999999999999L;

            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glPushMatrix();
            GL11.glScaled(ScaleFactor, ScaleFactor, ScaleFactor);
            String var6;
            var6 = ReadableNumberConverter.INSTANCE.toWideReadableForm(amount);
            int X = (int) (((float) xPosition + offset + 15.0f - fontRenderer.getStringWidth(var6) * ScaleFactor) * RScaleFactor);
            int Y = (int) (((float) yPosition + offset + 15.0f - 7.0f * ScaleFactor) * RScaleFactor);
            fontRenderer.drawStringWithShadow(var6, X, Y, 16777215);
            GL11.glPopMatrix();
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_DEPTH_TEST);

            fontRenderer.setUnicodeFlag(unicodeFlag);
        }

    }

    private void renderQuad(Tessellator p_77017_1_, int p_77017_2_, int p_77017_3_, int p_77017_4_, int p_77017_5_,
        int p_77017_6_) {
        p_77017_1_.startDrawingQuads();
        p_77017_1_.setColorOpaque_I(p_77017_6_);
        p_77017_1_.addVertex((double) (p_77017_2_ + 0), (double) (p_77017_3_ + 0), 0.0D);
        p_77017_1_.addVertex((double) (p_77017_2_ + 0), (double) (p_77017_3_ + p_77017_5_), 0.0D);
        p_77017_1_.addVertex((double) (p_77017_2_ + p_77017_4_), (double) (p_77017_3_ + p_77017_5_), 0.0D);
        p_77017_1_.addVertex((double) (p_77017_2_ + p_77017_4_), (double) (p_77017_3_ + 0), 0.0D);
        p_77017_1_.draw();
    }
}