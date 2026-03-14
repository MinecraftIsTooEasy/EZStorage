package com.zerofall.ezstorage.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.Block;
import net.minecraft.CraftingResult;
import net.minecraft.EnumChatFormatting;
import net.minecraft.GuiButton;
import net.minecraft.EntityPlayer;
import net.minecraft.IRecipe;
import net.minecraft.Item;
import net.minecraft.ItemCoin;
import net.minecraft.ItemStack;
import net.minecraft.Material;
import net.minecraft.RecipesArmorDyes;
import net.minecraft.ResourceLocation;
import net.minecraft.Skill;
import net.minecraft.StatCollector;
import net.minecraft.Tessellator;
import net.minecraft.Translator;
import net.minecraft.World;

import com.zerofall.ezstorage.Reference;
import com.zerofall.ezstorage.block.BlockCraftingBox;
import com.zerofall.ezstorage.compat.EZCraftingBoxCompat;
import com.zerofall.ezstorage.container.ContainerStorageCoreCrafting;
import com.zerofall.ezstorage.init.EZBlocks;
import com.zerofall.ezstorage.network.C2S.C2SClearCraftingGridPacket;
import moddedmite.rustedironcore.network.Network;

import org.lwjgl.opengl.GL11;

public class GuiCraftingCore extends GuiStorageCore {

    private static final int PROGRESS_U_FILL  = 195;
    private static final int PROGRESS_V_FILL  = 0;
    private static final int PROGRESS_W = 16;
    private static final int PROGRESS_H = 15;
    private static final int PROGRESS_GUI_X = 99;
    private static final int PROGRESS_GUI_Y = 132;
    private static final int OUTPUT_SLOT_VIS_X = 118;
    private static final int OUTPUT_SLOT_VIS_Y = 132;
    private static final int CLEAR_BTN_REL_X = 99;
    private static final int CLEAR_BTN_REL_Y = 114;
    private static final int CLEAR_BTN_W = 8;
    private static final int CLEAR_BTN_H = 8;

    protected GuiButton btnClearCraftingPanel;

    public GuiCraftingCore(EntityPlayer player, World world, int x, int y, int z) {
        super(new ContainerStorageCoreCrafting(player, world, x, y, z), world, x, y, z);
        this.xSize = 195;
        this.ySize = 256;
    }

    public GuiCraftingCore(ContainerStorageCoreCrafting container, World world, int x, int y, int z) {
        super(container, world, x, y, z);
        this.xSize = 195;
        this.ySize = 256;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        btnClearCraftingPanel = new GuiButton(10, -100, -100, 1, 1, "");
        buttonList.add(btnClearCraftingPanel);
    }

    private int getBlockTier()
    {
        if (world == null) return 0;

        Block block = world.getBlock(blockX, blockY, blockZ);

        if (block != EZBlocks.crafting_box) return 0;

        return world.getBlockMetadata(blockX, blockY, blockZ);
    }

    private String getTierName(int tier) {
        return BlockCraftingBox.getTierName(tier);
    }

    private float getTierSpeedModifier(int tier)
    {
        Material terminalMaterial = BlockCraftingBox.getToolMaterialForTier(tier);
        return EZCraftingBoxCompat.getBenchAndToolsModifierForMaterial(terminalMaterial);
    }

    private String getBenchModifierText(int tier)
    {
        float modifier = getTierSpeedModifier(tier);

        if (!Float.isFinite(modifier) || modifier >= 1000000.0F)
        {
            return "+∞";
        }

        return String.format(Locale.ROOT, "%+.2f", modifier);
    }

    private int getProgressScaled() {
        if (this.mc == null || this.mc.thePlayer == null) return 0;
        if (this.mc.thePlayer.crafting_ticks <= 0 || this.mc.thePlayer.crafting_period <= 0) return 0;
        return this.mc.thePlayer.crafting_ticks * PROGRESS_W / this.mc.thePlayer.crafting_period;
    }

    /** Returns true if the mouse is over the clear button. */
    private boolean isOverClearBtn(int mouseX, int mouseY) {
        int bx = this.guiLeft + CLEAR_BTN_REL_X;
        int by = this.guiTop + CLEAR_BTN_REL_Y;
        return mouseX >= bx && mouseX < bx + CLEAR_BTN_W
            && mouseY >= by && mouseY < by + CLEAR_BTN_H;
    }

    /** Draw the clear button using the side-button background texture (same as the 4 side buttons). */
    private void drawClearButton(int mouseX, int mouseY)
    {
        int bx = this.guiLeft + CLEAR_BTN_REL_X;
        int by = this.guiTop + CLEAR_BTN_REL_Y;
        boolean hovered = isOverClearBtn(mouseX, mouseY);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        this.mc.getTextureManager().bindTexture(resSideButton);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(bx, by + CLEAR_BTN_H, this.zLevel, 0.0, 1.0);
        tessellator.addVertexWithUV(bx + CLEAR_BTN_W, by + CLEAR_BTN_H, this.zLevel, 1.0, 1.0);
        tessellator.addVertexWithUV(bx + CLEAR_BTN_W, by, this.zLevel, 1.0, 0.0);
        tessellator.addVertexWithUV(bx, by, this.zLevel, 0.0, 0.0);
        tessellator.draw();

        if (hovered)
        {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.3f);
            tessellator.startDrawingQuads();
            tessellator.addVertex(bx, by + CLEAR_BTN_H, this.zLevel);
            tessellator.addVertex(bx + CLEAR_BTN_W, by + CLEAR_BTN_H, this.zLevel);
            tessellator.addVertex(bx + CLEAR_BTN_W, by, this.zLevel);
            tessellator.addVertex(bx, by, this.zLevel);
            tessellator.draw();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);

        // Progress highlight (the empty arrow is already baked into the GUI background).
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        this.mc.getTextureManager().bindTexture(getBackground());
        int px = this.guiLeft + PROGRESS_GUI_X;
        int py = this.guiTop + PROGRESS_GUI_Y;
        int filled = getProgressScaled();

        if (filled > 0)
        {
            drawTexturedModalRect(px, py, PROGRESS_U_FILL, PROGRESS_V_FILL, filled, PROGRESS_H);
        }

        // Draw clear button
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        drawClearButton(mouseX, mouseY);
        GL11.glPopAttrib();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (mouseButton == 0 && isOverClearBtn(mouseX, mouseY))
        {
            Network.sendToServer(new C2SClearCraftingGridPacket());
            this.mc.sndManager.playSound("random.click", (float) this.mc.thePlayer.posX, (float) this.mc.thePlayer.posY, (float) this.mc.thePlayer.posZ, 0.25F, 1.0F);
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        int tier = getBlockTier();
        String tierLabel = StatCollector.translateToLocalFormatted("ezstorage.crafting_box.tier_label", getTierName(tier));
        int tw = fontRenderer.getStringWidth(tierLabel);
        fontRenderer.drawString(tierLabel, this.xSize - tw - 24, 110, 0x000000);

        String speedValue = getBenchModifierText(tier);
        String speedLabel = StatCollector.translateToLocalFormatted("ezstorage.crafting_box.speed_label", speedValue);
        int sw = fontRenderer.getStringWidth(speedLabel);
        fontRenderer.drawString(speedLabel, this.xSize - sw - 24, 120, 0x000000);
    }

    @Override
    @SuppressWarnings("unchecked, rawtypes")
    protected void renderMouseOverTooltip(int mouseX, int mouseY)
    {
        super.renderMouseOverTooltip(mouseX, mouseY);

        if (!(inventorySlots instanceof ContainerStorageCoreCrafting container)) return;

        int x = this.guiLeft + OUTPUT_SLOT_VIS_X;
        int y = this.guiTop + OUTPUT_SLOT_VIS_Y;

        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16)
        {
            ItemStack result = container.craftResult.getStackInSlot(0);

            if (result != null)
            {
                List<String> tooltip = new ArrayList<>(result.getTooltip(this.mc.thePlayer,
                        this.mc.gameSettings.advancedItemTooltips, null));
                appendCraftingRestrictionTooltip(tooltip, container, result);
                func_102021_a(tooltip, mouseX, mouseY);
            }
            return;
        }

        for (int i = 0; i < 9; i++)
        {
            int col = i % 3;
            int row = i / 3;
            int sx = this.guiLeft + 44 + col * 18;
            int sy = this.guiTop + 114 + row * 18;

            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16)
            {
                ItemStack stack = container.craftMatrix.getStackInSlot(i);

                if (stack != null)
                {
                    func_102021_a(stack.getTooltip(this.mc.thePlayer,
                            this.mc.gameSettings.advancedItemTooltips, null),
                            mouseX, mouseY);
                }
                return;
            }
        }

        int progressX = this.guiLeft + PROGRESS_GUI_X;
        int progressY = this.guiTop + PROGRESS_GUI_Y;

        if (mouseX >= progressX && mouseX < progressX + PROGRESS_W && mouseY >= progressY && mouseY < progressY + PROGRESS_H)
        {
            if (this.mc != null && this.mc.thePlayer != null && this.mc.thePlayer.crafting_period > 0)
            {
                int remainingTicks = Math.max(0, this.mc.thePlayer.crafting_period - this.mc.thePlayer.crafting_ticks);
                double remainingSeconds = remainingTicks / 20.0D;
                String secondsText = String.format(Locale.ROOT, "%.1f", remainingSeconds);
                List<String> tooltip = new ArrayList<>();
                tooltip.add(StatCollector.translateToLocalFormatted("ezstorage.crafting.remaining_time", secondsText));
                func_102021_a(tooltip, mouseX, mouseY);
            }
        }
    }

    private void appendCraftingRestrictionTooltip(List<String> tooltip, ContainerStorageCoreCrafting container, ItemStack result)
    {
        CraftingResult craftingResult = container.current_crafting_result;

        if (craftingResult == null)
        {
            return;
        }

        Item item = result.getItem();
        IRecipe recipe = container.getRecipe();
        Material requiredMaterial = recipe == null
            ? item.getHardestMetalMaterial()
            : recipe.getMaterialToCheckToolBenchHardnessAgainst();

        boolean upperBodyInWeb = this.mc.thePlayer.isUpperBodyInWeb();

        if (requiredMaterial != null || upperBodyInWeb)
        {
            if (upperBodyInWeb)
            {
                tooltip.add(EnumChatFormatting.GOLD + Translator.get("container.crafting.prevented"));
                return;
            }

            if (container.isCurrentRecipeBlockedByTier())
            {
                tooltip.add(EnumChatFormatting.GOLD + Translator.get("container.crafting.needsBetterTools"));
                return;
            }
        }

        if (this.mc.theWorld.areSkillsEnabled() && !this.mc.thePlayer.hasSkillsForCraftingResult(craftingResult))
        {
            if (item.hasQuality() && !(craftingResult.recipe instanceof RecipesArmorDyes))
            {
                tooltip.add(EnumChatFormatting.DARK_GRAY + Skill.getSkillsetsString(craftingResult.applicable_skillsets, false));
            }
            else
            {
                tooltip.add(EnumChatFormatting.DARK_GRAY + "Requires " + Skill.getSkillsetsString(craftingResult.applicable_skillsets, true));
            }
            return;
        }

        if (item instanceof ItemCoin && this.mc.thePlayer.experience < ((ItemCoin)item).getExperienceValue() * result.stackSize)
        {
            tooltip.add(EnumChatFormatting.GOLD + Translator.get("container.crafting.requiresExperience"));
            return;
        }

        if (container.craftMatrix.hasDamagedItem() && !craftingResult.isRepair())
        {
            tooltip.add(EnumChatFormatting.GOLD + Translator.get("container.crafting.damagedComponent"));
        }
    }


    @Override
    public int rowsVisible() {
        return 5;
    }

    @Override
    protected ResourceLocation getBackground() {
        return new ResourceLocation(Reference.MOD_ID, "textures/gui/storageCraftingGui.png");
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        // Handled via mouseClicked + direct packet send
    }
}