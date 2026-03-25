package com.zerofall.ezstorage.client.gui;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.FontRenderer;
import net.minecraft.GuiScreen;
import net.minecraft.GuiTextField;
import net.minecraft.GuiContainer;
import net.minecraft.RenderHelper;
import net.minecraft.EntityPlayer;
import net.minecraft.ItemStack;
import net.minecraft.EnumChatFormatting;
import net.minecraft.MathHelper;
import net.minecraft.Tessellator;
import net.minecraft.ResourceLocation;
import net.minecraft.StatCollector;
import net.minecraft.World;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.zerofall.ezstorage.Reference;
import com.zerofall.ezstorage.configuration.EZConfiguration;
import com.zerofall.ezstorage.container.ContainerStorageCore;
import com.zerofall.ezstorage.container.ContainerStorageCoreCrafting;
import com.zerofall.ezstorage.enums.SearchMode;
import com.zerofall.ezstorage.enums.SortMode;
import com.zerofall.ezstorage.enums.SortOrder;
import com.zerofall.ezstorage.network.C2S.C2SInvSlotClickedPacket;
import com.zerofall.ezstorage.util.EZInventory;
import com.zerofall.ezstorage.util.EZItemRenderer;
import com.zerofall.ezstorage.util.ItemStackCountComparator;
import com.zerofall.ezstorage.util.ItemStackModComparator;
import com.zerofall.ezstorage.util.ItemStackNameComparator;
import com.zerofall.ezstorage.util.PinyinSearchUtils;
import moddedmite.rustedironcore.network.Network;

public class GuiStorageCore extends GuiContainer {

    protected static final ResourceLocation resCreativeInventoryTabs = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
    protected static final ResourceLocation resSearchBar = new ResourceLocation("textures/gui/container/creative_inventory/tab_item_search.png");
    protected static final ResourceLocation resSideButton = new ResourceLocation(Reference.MOD_ID, "textures/gui/storageSideButtonBackground.png");

    protected static String searchText = "";
    protected static SearchMode currentSearchMode = SearchMode.STANDARD;
    protected static SortMode currentSortMode = SortMode.AMOUNT;
    protected static SortOrder currentSortOrder = SortOrder.DESCENDING;
    protected static boolean saveSearch = false;
    protected static boolean autoRefill = true;

    private static final int BTN_W = 16;
    private static final int BTN_H = 16;
    private static final int BTN_STRIDE = 20;

    private static final int BTN_SORT_MODE = 0;
    private static final int BTN_SORT_ORDER = 1;
    private static final int BTN_SEARCH_MODE = 2;
    private static final int BTN_SAVE_SEARCH = 3;
    private static final int BTN_AUTO_REFILL = 4;

    private static final int CLEAR_SEARCH_BTN_REL_X = 100;
    private static final int CLEAR_SEARCH_BTN_REL_Y = 6;
    private static final int CLEAR_SEARCH_BTN_W = 8;
    private static final int CLEAR_SEARCH_BTN_H = 8;

    private static final class SideButton {

        final int id;
        int x;
        int y;

        SideButton(int id) {
            this.id = id;
        }

        boolean contains(int mx, int my) {
            return mx >= x && mx < x + BTN_W && my >= y && my < y + BTN_H;
        }
    }

    private final SideButton[] sideButtons = {
            new SideButton(BTN_SORT_MODE),
            new SideButton(BTN_SORT_ORDER),
            new SideButton(BTN_SEARCH_MODE),
            new SideButton(BTN_SAVE_SEARCH),
            new SideButton(BTN_AUTO_REFILL)
    };

    protected EZItemRenderer ezRenderer;
    protected int scrollRow = 0;
    protected float currentScroll;
    protected boolean isScrolling = false;
    protected boolean wasClicking = false;
    protected GuiTextField searchField;
    // Track search field bounds manually since GuiTextField fields are private in MITE
    protected int searchFieldX, searchFieldY, searchFieldW, searchFieldH;
    protected ItemStack mouseOverItem;
    protected List<ItemStack> filteredList = new ArrayList<>();
    protected long inventoryUpdateTimestamp;
    protected boolean needFullUpdate;

    // Key-repeat state for search field long-press (e.g. hold Backspace)
    private char heldChar = 0;
    private int heldKey = 0;
    private long keyRepeatTimer = 0;
    private long keyRepeatDelay = 500;
    private long keyRepeatRate = 50;
    private long lastRepeat = 0;

    protected World world;
    protected int blockX, blockY, blockZ;

    private boolean guiWasOpen = false;

    public GuiStorageCore(EntityPlayer player, World world, int x, int y, int z) {
        this(new ContainerStorageCore(player), world, x, y, z);
    }

    public GuiStorageCore(ContainerStorageCore containerStorageCore, World world, int x, int y, int z) {
        super(containerStorageCore);
        this.xSize = 195;
        this.ySize = 222;
        this.world = world;
        this.blockX = x;
        this.blockY = y;
        this.blockZ = z;
        loadSettingsFromConfig();
    }

    @Override
    public void initGui()
    {
        super.initGui();

        searchFieldX = this.guiLeft + 10;
        searchFieldY = this.guiTop + 6;
        searchFieldW = 100;
        searchFieldH = this.fontRenderer.FONT_HEIGHT;

        this.searchField = new GuiTextField(this.fontRenderer, searchFieldX, searchFieldY, searchFieldW, searchFieldH);
        this.searchField.setMaxStringLength(50);
        this.searchField.setEnableBackgroundDrawing(false);
        this.searchField.setTextColor(0xFFFFFF);
        this.searchField.setCanLoseFocus(true);

        // Restore search text only if saveSearch is enabled
        if (saveSearch && !searchText.isEmpty())
        {
            this.searchField.setText(searchText);
        }
        else
        {
            this.searchField.setText("");
            if (!saveSearch) searchText = "";
        }

        if (!guiWasOpen)
        {
            this.searchField.setFocused(EZConfiguration.focusGuiInput.getBooleanValue());
            guiWasOpen = true;
        }

        int bx = this.guiLeft - BTN_W - 2;
        int by = this.guiTop + 8;

        for (int i = 0; i < sideButtons.length; i++)
        {
            sideButtons[i].x = bx;
            sideButtons[i].y = by + i * BTN_STRIDE;
        }

        updateFilteredItems(true);
    }

    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();

        if (saveSearch)
        {
            searchText = this.searchField.getText()
                .trim();
        }

        saveSettingsToConfig();
    }

    public EZInventory getInventory() {
        return ((ContainerStorageCore) inventorySlots).inventory;
    }

    public boolean isOverTextField(int mousex, int mousey) {
        return mousex >= searchFieldX && mousex < searchFieldX + searchFieldW
            && mousey >= searchFieldY && mousey < searchFieldY + searchFieldH + 4;
    }

    public void setTextFieldValue(String displayName, int mousex, int mousey, ItemStack stack)
    {
        if (displayName != null && !displayName.isEmpty())
        {
            this.searchField.setText(displayName);
            searchText = displayName;
            this.searchField.setFocused(true);
            currentScroll = 0;
            scrollRow = 0;
            updateFilteredItems(true);
        }
    }

    // Button labels & tooltips
    private String getLabelFor(int btnId)
    {
        switch (btnId)
        {
            case BTN_SORT_MODE:
                switch (currentSortMode)
                {
                    case NAME:
                        return "N";
                    case MOD:
                        return "M";
                    default:
                        return "#";
                }

            case BTN_SORT_ORDER:
                return currentSortOrder == SortOrder.ASCENDING ? "/\\" : "\\/";
            case BTN_SEARCH_MODE:
                switch (currentSearchMode) {
                    default:
                        return "S";
                }
            case BTN_SAVE_SEARCH:
                return "S";
            case BTN_AUTO_REFILL:
                return "A";
            default:
                return "?";
        }
    }

    private String getTooltipFor(int btnId)
    {
        switch (btnId)
        {
            case BTN_SORT_MODE:
                return StatCollector.translateToLocal(currentSortMode.langKey);
            case BTN_SORT_ORDER:
                return StatCollector.translateToLocal(currentSortOrder.langKey);
            case BTN_SEARCH_MODE:
                return StatCollector.translateToLocal(currentSearchMode.langKey);
            case BTN_SAVE_SEARCH:
                return StatCollector.translateToLocal(
                    saveSearch ? "hud.msg.ezstorage.savesearch.on" : "hud.msg.ezstorage.savesearch.off");
            case BTN_AUTO_REFILL:
                return StatCollector.translateToLocal(
                    autoRefill ? "hud.msg.ezstorage.autorefill.on" : "hud.msg.ezstorage.autorefill.off");
            default:
                return "";
        }
    }

    private boolean isActive(int btnId) {
        if (btnId == BTN_SAVE_SEARCH) return saveSearch;
        if (btnId == BTN_AUTO_REFILL) return autoRefill;
        return true;
    }

    private void handleButtonClick(SideButton btn)
    {
        switch (btn.id)
        {
            case BTN_SORT_MODE:
                currentSortMode = currentSortMode.next();
                saveSettingsToConfig();
                updateFilteredItems(true);
                break;

            case BTN_SORT_ORDER:
                currentSortOrder = currentSortOrder.next();
                saveSettingsToConfig();
                updateFilteredItems(true);
                break;

            case BTN_SEARCH_MODE:
                currentSearchMode = currentSearchMode.next();
                saveSettingsToConfig();
                updateFilteredItems(true);
                break;

            case BTN_SAVE_SEARCH:
                saveSearch = !saveSearch;

                if (!saveSearch)
                {
                    searchText = "";
                } else
                {
                    // Capture current text immediately when enabling save search
                    searchText = this.searchField.getText().trim();
                }
                saveSettingsToConfig();
                break;

            case BTN_AUTO_REFILL:
                autoRefill = !autoRefill;
                saveSettingsToConfig();
                break;

            default:
                break;
        }

        this.mc.sndManager.playSound("random.click", (float) this.mc.thePlayer.posX, (float) this.mc.thePlayer.posY, (float) this.mc.thePlayer.posZ, 0.25F, 1.0F);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        super.drawScreen(mouseX, mouseY, partialTicks);

        cacheMouseOverItem(mouseX, mouseY);
        drawSideButtonTooltips(mouseX, mouseY);
        // Render our own item tooltip for the storage-grid item under the mouse
        renderMouseOverTooltip(mouseX, mouseY);
    }

    /** Renders the tooltip for the storage-grid item currently under the cursor. */
    protected void renderMouseOverTooltip(int mouseX, int mouseY)
    {
        if (mouseOverItem != null)
        {
            this.drawItemStackTooltip(mouseOverItem, mouseX, mouseY, null);
        }
    }

    private void drawSideButtons(int mouseX, int mouseY)
    {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        Tessellator tessellator = Tessellator.instance;

        for (SideButton btn : sideButtons)
        {
            boolean hovered = btn.contains(mouseX, mouseY);
            boolean active = isActive(btn.id);

            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            this.mc.getTextureManager().bindTexture(resSideButton);
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(btn.x, btn.y + BTN_H, this.zLevel, 0.0, 1.0);
            tessellator.addVertexWithUV(btn.x + BTN_W, btn.y + BTN_H, this.zLevel, 1.0, 1.0);
            tessellator.addVertexWithUV(btn.x + BTN_W, btn.y, this.zLevel, 1.0, 0.0);
            tessellator.addVertexWithUV(btn.x, btn.y, this.zLevel, 0.0, 0.0);
            tessellator.draw();

            if (hovered)
            {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.25f);
                tessellator.startDrawingQuads();
                tessellator.addVertex(btn.x, btn.y + BTN_H, this.zLevel);
                tessellator.addVertex(btn.x + BTN_W, btn.y + BTN_H, this.zLevel);
                tessellator.addVertex(btn.x + BTN_W, btn.y, this.zLevel);
                tessellator.addVertex(btn.x, btn.y, this.zLevel);
                tessellator.draw();
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }

            // Label
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            String label = getLabelFor(btn.id);
            int tw = this.fontRenderer.getStringWidth(label);
            int lx = btn.x + (BTN_W - tw) / 2;
            int ly = btn.y + (BTN_H - this.fontRenderer.FONT_HEIGHT) / 2;
            this.fontRenderer.drawStringWithShadow(label, lx, ly, active ? 0xFFFFFF : 0xAAAAAA);
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopAttrib();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawSideButtonTooltips(int mouseX, int mouseY)
    {
        for (SideButton btn : sideButtons)
        {
            if (btn.contains(mouseX, mouseY))
            {
                List<String> tip = new ArrayList<>();
                tip.add(getTooltipFor(btn.id));
                func_102021_a(tip, mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
        this.mc.getTextureManager().bindTexture(getBackground());
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
        this.searchField.setVisible(true);
        this.mc.getTextureManager().bindTexture(resSearchBar);
        drawTexturedModalRect(this.guiLeft + 8, this.guiTop + 4, 80, 4, 90, 12);
        this.searchField.drawTextBox();

        drawClearSearchButton(mouseX, mouseY);

        // Reset GL state fully before drawing side buttons, in case searchField left dirty state
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        drawSideButtons(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        handleScrolling(mouseX, mouseY);
        DecimalFormat formatter = new DecimalFormat("#,###");
        String totalCount = formatter.format(getInventory().getTotalCount());
        String max = formatter.format(getInventory().maxItems);
        String amount = StatCollector.translateToLocalFormatted("hud.msg.ezstorage.amount_count", totalCount, max);
        int stringWidth = fontRenderer.getStringWidth(amount);

        if (stringWidth > 88)
        {
            float scaleFactor = 0.7f;
            float rScaleFactor = 1.0f / scaleFactor;
            GL11.glPushMatrix();
            GL11.glScaled(scaleFactor, scaleFactor, scaleFactor);
            int bx = (int) (((float) 187 - stringWidth * scaleFactor) * rScaleFactor);
            fontRenderer.drawString(amount, bx, 10, 4210752);
            GL11.glPopMatrix();
        }
        else
        {
            fontRenderer.drawString(amount, 187 - stringWidth, 6, 4210752);
        }

        int x = 8;
        int y = 18;
        this.zLevel = 100.0F;
        itemRenderer.zLevel = 100.0F;

        if (this.ezRenderer == null)
        {
            this.ezRenderer = new EZItemRenderer();
        }
        this.ezRenderer.zLevel = 200.0F;

        boolean finished = false;

        for (int i = 0; i < this.rowsVisible(); i++)
        {
            x = 8;

            for (int j = 0; j < 9; j++)
            {
                int index = (i * 9) + j;
                index = scrollRow * 9 + index;

                if (index >= this.filteredList.size())
                {
                    finished = true;
                    break;
                }
                ItemStack stack = this.filteredList.get(index);
                if (stack != null)
                {
                    FontRenderer font = fontRenderer;
                    RenderHelper.enableGUIStandardItemLighting();
                    itemRenderer.renderItemAndEffectIntoGUI(font, this.mc.getTextureManager(), stack, x, y);
                    ezRenderer.renderItemOverlayIntoGUI(font, stack, x, y, "" + stack.stackSize);
                }
                x += 18;
            }

            if (finished) break;
            y += 18;
        }

        this.zLevel = 0.0F;
        itemRenderer.zLevel = 0.0F;
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        int i1 = 175;
        int k = 18;
        int l = k + 108;
        this.mc.getTextureManager().bindTexture(resCreativeInventoryTabs);
        this.drawTexturedModalRect(i1, k + (int) ((float) (l - k - 17) * this.currentScroll), 232, 0, 12, 15);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        // Side buttons (outside GuiContainer bounds — handle before super)
        if (mouseButton == 0 || mouseButton == 1)
        {
            for (SideButton btn : sideButtons)
            {
                if (btn.contains(mouseX, mouseY))
                {
                    handleButtonClick(btn);
                    return;
                }
            }

            if (mouseButton == 0 && isOverClearSearchBtn(mouseX, mouseY))
            {
                this.searchField.setText("");
                searchText = "";
                updateFilteredItems(true);
                this.mc.sndManager.playSound("random.click", (float) this.mc.thePlayer.posX, (float) this.mc.thePlayer.posY, (float) this.mc.thePlayer.posZ, 0.25F, 1.0F);
                return;
            }
        }

        boolean wantFocus;

        if (isOverSearchField(mouseX, mouseY))
        {
            ItemStack heldItem = this.mc.thePlayer.inventory.getItemStack();

            if (heldItem != null && (mouseButton == 0 || mouseButton == 1))
            {
                String displayName = EnumChatFormatting.func_110646_a(heldItem.getDisplayName());
                setTextFieldValue(displayName, mouseX, mouseY, heldItem);
            }
            else if (mouseButton == 1)
            { // Right click to clear
                searchText = "";
                this.searchField.setText("");
                updateFilteredItems(true);
            }
            else if (mouseButton == 0)
            {
                this.searchField.mouseClicked(mouseX, mouseY, mouseButton);
            }
            wantFocus = true;
        }
        else
        {
            wantFocus = false;
        }

        Integer slot = getSlotAt(mouseX, mouseY);

        if (slot != null && (mouseButton == 0 || mouseButton == 1))
        {
            int mode = GuiScreen.isShiftKeyDown() ? 1 : 0;
            int index = getInventory().slotCount();

            if (slot < this.filteredList.size())
            {
                ItemStack group = this.filteredList.get(slot);

                if (group == null || group.stackSize == 0)
                {
                    this.searchField.setFocused(wantFocus);
                    return;
                }
                index = getInventory().getIndexOf(group);

                if (index < 0)
                {
                    this.searchField.setFocused(wantFocus);
                    return;
                }
            }

            Network.sendToServer(new C2SInvSlotClickedPacket(index, mouseButton, mode));

            if (!(this.inventorySlots instanceof ContainerStorageCoreCrafting))
            {
                applyVisualSlotClick(slot, mouseButton, mode);
            }

            this.searchField.setFocused(false);
            return;
        }
        this.searchField.setFocused(wantFocus);

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }


    /**
     * Purely visual client-side update: adjusts filteredList and cursor to give immediate
     * feedback before the authoritative S2C response arrives.
     * Does NOT touch inventory.inventory — only the filteredList copies and cursor.
     */
    private void applyVisualSlotClick(int slotIndex, int mouseButton, int mode)
    {
        ItemStack heldStack = this.mc.thePlayer.inventory.getItemStack();

        if (heldStack == null)
        {
            // Picking up from storage: show item on cursor, hide from grid
            if (slotIndex >= filteredList.size()) return;

            ItemStack group = filteredList.get(slotIndex);

            if (group == null || group.stackSize <= 0) return;

            int maxTake = Math.min(group.stackSize, group.getMaxStackSize());
            int size = (mouseButton == 1) ? Math.max(1, maxTake / 2) : maxTake;

            // Build cursor item from a COPY — never mutate the inventory object itself
            ItemStack cursor = group.copy();
            cursor.stackSize = size;
            this.mc.thePlayer.inventory.setItemStack(cursor);

            // Update the filteredList entry: use a local visual-only copy
            int newCount = group.stackSize - size;

            if (newCount <= 0)
            {
                filteredList.remove(slotIndex);
            }
            else
            {
                // Replace the entry with a copy that has the reduced count, so the
                // real inventory object's stackSize is never touched here.
                ItemStack visual = group.copy();
                visual.stackSize = newCount;
                filteredList.set(slotIndex, visual);
            }
        }
        else
        {
            // Depositing: clear cursor immediately for visual feedback
            this.mc.thePlayer.inventory.setItemStack(null);
        }
    }

    private boolean isOverSearchField(int mouseX, int mouseY)
    {
        return mouseX >= searchFieldX && mouseX < searchFieldX + searchFieldW
            && mouseY >= searchFieldY
            && mouseY < searchFieldY + searchFieldH + 4;
    }

    private Integer getSlotAt(int x, int y)
    {
        int startX = this.guiLeft + 8 - 1;
        int startY = this.guiTop + 18 - 1;
        int cx = x - startX;
        int cy = y - startY;

        if (cx > 0 && cy > 0)
        {
            int col = cx / 18;

            if (col < 9)
            {
                int row = cy / 18;

                if (row < this.rowsVisible())
                {
                    return (row * 9) + col + (scrollRow * 9);
                }
            }
        }

        return null;
    }

    @Override
    public void handleMouseInput()
    {
        super.handleMouseInput();

        int i = Mouse.getEventDWheel();

        if (i != 0)
        {
            int j = filteredList.size() / 9 - this.rowsVisible() + 1;

            if (i > 0) i = 1;

            if (i < 0) i = -1;

            this.currentScroll = (float) ((double) this.currentScroll - (double) i / (double) j);
            this.currentScroll = MathHelper.clamp_float(this.currentScroll, 0.0F, 1.0F);
            scrollTo(this.currentScroll);
        }
    }

    private void handleScrolling(int mouseX, int mouseY)
    {
        boolean flag = Mouse.isButtonDown(0);
        int i1 = this.guiLeft + 175;
        int j1 = this.guiTop + 18;
        int k1 = i1 + 14;
        int l1 = j1 + 108;

        if (!this.wasClicking && flag && mouseX >= i1 && mouseY >= j1 && mouseX < k1 && mouseY < l1)
        {
            this.isScrolling = true;
        }

        if (!flag) this.isScrolling = false;
        this.wasClicking = flag;

        if (this.isScrolling)
        {
            this.currentScroll = ((float) (mouseY - j1) - 7.5F) / ((float) (l1 - j1) - 15.0F);
            this.currentScroll = MathHelper.clamp_float(this.currentScroll, 0.0F, 1.0F);
            scrollTo(this.currentScroll);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode)
    {
        if (!this.checkHotbarKeys(keyCode))
        {
            if (this.searchField.isFocused() && this.searchField.textboxKeyTyped(typedChar, keyCode))
            {
                currentScroll = 0;
                scrollRow = 0;
                updateFilteredItems(true);
                // Begin tracking this key for repeat
                heldChar = typedChar;
                heldKey = keyCode;
                keyRepeatTimer = System.currentTimeMillis();
                lastRepeat = keyRepeatTimer;
            }
            else
            {
                heldChar = 0;
                heldKey = 0;

                super.keyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    public void updateScreen()
    {
        if (inventorySlots instanceof ContainerStorageCore container)
        {
            if (inventoryUpdateTimestamp != container.inventoryUpdateTimestamp || (needFullUpdate && !GuiScreen.isShiftKeyDown()))
            {
                inventoryUpdateTimestamp = container.inventoryUpdateTimestamp;
                updateFilteredItems(false);
            }
        }

        // Key-repeat: if a key is held in the focused search field, repeat it
        if (heldKey != 0 && this.searchField != null && this.searchField.isFocused())
        {
            try {
                boolean stillHeld = org.lwjgl.input.Keyboard.isKeyDown(heldKey);
                if (!stillHeld)
                {
                    heldChar = 0;
                    heldKey = 0;
                }
                else
                {
                    long now = System.currentTimeMillis();
                    long sinceFirst = now - keyRepeatTimer;

                    if (sinceFirst >= keyRepeatDelay && now - lastRepeat >= keyRepeatRate)
                    {
                        lastRepeat = now;

                        if (this.searchField.textboxKeyTyped(heldChar, heldKey))
                        {
                            currentScroll = 0;
                            scrollRow = 0;
                            updateFilteredItems(true);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        super.updateScreen();
    }

    private void updateFilteredItems(boolean forceFullUpdate)
    {
        searchText = this.searchField.getText().trim();
        filteredList.clear();
        filterItems(searchText, getInventory().inventory);
        sortFilteredList();
        needFullUpdate = false;
    }

    private void sortFilteredList()
    {
        Comparator<ItemStack> comparator;

        switch (currentSortMode)
        {
            case NAME:
                comparator = new ItemStackNameComparator();
                break;

            case MOD:
                comparator = new ItemStackModComparator();
                break;

            default:
                comparator = new ItemStackCountComparator();
                break;
        }

        if (currentSortOrder == SortOrder.ASCENDING)
        {
            comparator = Collections.reverseOrder(comparator);
        }
        filteredList.sort(comparator);
    }

    private void filterItems(String text, List<ItemStack> input) {
        filterItemsViaVanilla(text, input);
    }

    @SuppressWarnings("unchecked")
    private void filterItemsViaVanilla(String raw, List<ItemStack> input)
    {
        if (raw.isEmpty())
        {
            filteredList.addAll(input);
            return;
        }

        String text = raw.toLowerCase();
        boolean modFilter = text.startsWith("@");
        String query = modFilter ? text.substring(1) : text;

        if (query.isEmpty())
        {
            filteredList.addAll(input);
            return;
        }

        for (ItemStack group : input)
        {
            if (modFilter)
            {
                String unloc = group.getItem().getUnlocalizedName(group).toLowerCase();

                if (PinyinSearchUtils.searchMatches(unloc, query))
                {
                    filteredList.add(group);
                }
            }
            else
            {
                List<String> lines = group.getTooltip(this.mc.thePlayer, this.mc.gameSettings.advancedItemTooltips, null);
                boolean matched = false;

                for (String line : lines)
                {
                    String plain = EnumChatFormatting.func_110646_a(line);

                    if (PinyinSearchUtils.searchMatches(plain, raw))
                    {
                        matched = true;
                        break;
                    }
                }
                if (matched) filteredList.add(group);
            }
        }
    }


    private void scrollTo(float scroll)
    {
        int i = (filteredList.size() + 8) / 9 - this.rowsVisible();
        int j = (int) ((double) (scroll * (float) i) + 0.5D);

        if (j < 0) j = 0;
        this.scrollRow = j;
    }

    protected void cacheMouseOverItem(int mouseX, int mouseY)
    {
        Integer slot = getSlotAt(mouseX, mouseY);

        if (slot != null && slot < this.filteredList.size())
        {
            ItemStack group = this.filteredList.get(slot);

            if (group != null)
            {
                mouseOverItem = group;
                return;
            }
        }

        mouseOverItem = null;
    }

    private static void loadSettingsFromConfig() {
        currentSortMode = EZConfiguration.guiSortMode.getEnumValue();
        currentSortOrder = EZConfiguration.guiSortOrder.getEnumValue();
        currentSearchMode = EZConfiguration.guiSearchMode.getEnumValue();
        saveSearch = EZConfiguration.guiSaveSearch.getBooleanValue();
        autoRefill = EZConfiguration.guiAutoRefill.getBooleanValue();
        if (saveSearch) {
            searchText = EZConfiguration.guiSearchText.getStringValue();
        }
    }

    private static void saveSettingsToConfig() {
        EZConfiguration.guiSortMode.setEnumValue(currentSortMode);
        EZConfiguration.guiSortOrder.setEnumValue(currentSortOrder);
        EZConfiguration.guiSearchMode.setEnumValue(currentSearchMode);
        EZConfiguration.guiSaveSearch.setBooleanValue(saveSearch);
        EZConfiguration.guiAutoRefill.setBooleanValue(autoRefill);
        EZConfiguration.guiSearchText.setValueFromString(saveSearch ? searchText : "");
        EZConfiguration.saveInstance();
    }

    protected ResourceLocation getBackground() {
        return new ResourceLocation(Reference.MOD_ID, "textures/gui/storageScrollGui.png");
    }

    public int rowsVisible() {
        return 6;
    }

    public ItemStack getMouseOverItem() {
        return mouseOverItem;
    }

    private boolean isOverClearSearchBtn(int mouseX, int mouseY) {
        int bx = this.guiLeft + CLEAR_SEARCH_BTN_REL_X;
        int by = this.guiTop + CLEAR_SEARCH_BTN_REL_Y;
        return mouseX >= bx && mouseX < bx + CLEAR_SEARCH_BTN_W
            && mouseY >= by && mouseY < by + CLEAR_SEARCH_BTN_H;
    }

    private void drawClearSearchButton(int mouseX, int mouseY) {
        int bx = this.guiLeft + CLEAR_SEARCH_BTN_REL_X;
        int by = this.guiTop + CLEAR_SEARCH_BTN_REL_Y;
        boolean hovered = isOverClearSearchBtn(mouseX, mouseY);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        this.mc.getTextureManager().bindTexture(resSideButton);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(bx, by + CLEAR_SEARCH_BTN_H, this.zLevel, 0.0, 1.0);
        tess.addVertexWithUV(bx + CLEAR_SEARCH_BTN_W, by + CLEAR_SEARCH_BTN_H, this.zLevel, 1.0, 1.0);
        tess.addVertexWithUV(bx + CLEAR_SEARCH_BTN_W, by, this.zLevel, 1.0, 0.0);
        tess.addVertexWithUV(bx, by, this.zLevel, 0.0, 0.0);
        tess.draw();

        if (hovered) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.3f);
            tess.startDrawingQuads();
            tess.addVertex(bx, by + CLEAR_SEARCH_BTN_H, this.zLevel);
            tess.addVertex(bx + CLEAR_SEARCH_BTN_W, by + CLEAR_SEARCH_BTN_H, this.zLevel);
            tess.addVertex(bx + CLEAR_SEARCH_BTN_W, by, this.zLevel);
            tess.addVertex(bx, by, this.zLevel);
            tess.draw();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }
}

