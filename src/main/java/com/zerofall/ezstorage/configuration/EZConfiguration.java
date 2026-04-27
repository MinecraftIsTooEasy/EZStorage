package com.zerofall.ezstorage.configuration;

import com.zerofall.ezstorage.Reference;
import fi.dy.masa.malilib.config.ConfigTab;
import fi.dy.masa.malilib.config.SimpleConfigs;
import fi.dy.masa.malilib.config.options.ConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigString;
import com.zerofall.ezstorage.enums.SortMode;
import com.zerofall.ezstorage.enums.SearchMode;
import fi.dy.masa.malilib.config.options.ConfigEnum;
import com.zerofall.ezstorage.enums.SortOrder;

import java.util.ArrayList;
import java.util.List;

public class EZConfiguration extends SimpleConfigs {

    public static final ConfigInteger basicCapacity = new ConfigInteger(Reference.MOD_ID + ".basicCapacity", 400, 1, Integer.MAX_VALUE, Reference.MOD_ID + ".basicCapacity");

    public static final ConfigInteger condensedCapacity = new ConfigInteger(Reference.MOD_ID + ".condensedCapacity", 4000, 1, Integer.MAX_VALUE, Reference.MOD_ID + ".condensedCapacity");

    public static final ConfigInteger hyperCapacity = new ConfigInteger(Reference.MOD_ID + ".hyperCapacity", 400000, 1, Integer.MAX_VALUE, Reference.MOD_ID + ".hyperCapacity");

    public static final ConfigInteger maxItemTypes = new ConfigInteger(Reference.MOD_ID + ".maxItemTypes", 0, 0, Integer.MAX_VALUE, Reference.MOD_ID + ".maxItemTypes");

    public static final ConfigBoolean focusGuiInput = new ConfigBoolean(Reference.MOD_ID + ".focusGuiInput", false, Reference.MOD_ID + ".focusGuiInput");

    public static final ConfigEnum<SortMode> guiSortMode = new ConfigEnum<>(Reference.MOD_ID + ".guiSortMode", SortMode.NAME, Reference.MOD_ID + ".guiSortMode");

    public static final ConfigEnum<SortOrder> guiSortOrder = new ConfigEnum<>(Reference.MOD_ID + ".guiSortOrder", SortOrder.DESCENDING, Reference.MOD_ID + ".guiSortOrder");

    public static final ConfigEnum<SearchMode> guiSearchMode = new ConfigEnum<>(Reference.MOD_ID + ".guiSearchMode", SearchMode.STANDARD, Reference.MOD_ID + ".guiSearchMode");

    public static final ConfigBoolean guiAutoRefill = new ConfigBoolean(Reference.MOD_ID + ".guiAutoRefill", true, Reference.MOD_ID + ".guiAutoRefill");

    public static final ConfigBoolean renderCraftingPreview = new ConfigBoolean(Reference.MOD_ID + ".renderCraftingPreview", true, Reference.MOD_ID + ".renderCraftingPreview");

    public static final ConfigInteger itemCountFontScalePercent = new ConfigInteger(Reference.MOD_ID + ".itemCountFontScalePercent", 50, 25, 100, Reference.MOD_ID + ".itemCountFontScalePercent");

    public static final ConfigBoolean guiSaveSearch = new ConfigBoolean(Reference.MOD_ID + ".guiSaveSearch", false, Reference.MOD_ID + ".guiSaveSearch");

    public static final ConfigString guiSearchText = new ConfigString(Reference.MOD_ID + ".guiSearchText", "", Reference.MOD_ID + ".guiSearchText");

    public static final ConfigHotkey emiFillSearchHotkey = new ConfigHotkey(Reference.MOD_ID + ".emiFillSearchHotkey", "F", Reference.MOD_ID + ".emiFillSearchHotkey");

    private static final EZConfiguration INSTANCE;

    public static final List<ConfigBase<?>> ALL_OPTIONS;
    public static final List<ConfigHotkey> HOTKEYS;
    public static final List<ConfigTab> tabs;

    public EZConfiguration(String name, List<ConfigHotkey> hotkeys, List<ConfigBase<?>> values) {
        super(name, hotkeys, values);
    }

    @Override
    public List<ConfigTab> getConfigTabs() {
        return tabs;
    }

    public static EZConfiguration getInstance() {
        return INSTANCE;
    }

    public static void init() {
    }

    public static void saveInstance() {
        INSTANCE.save();
    }

    static {
        ALL_OPTIONS = new ArrayList<>();
        HOTKEYS = new ArrayList<>();
        tabs = new ArrayList<>();

        List<ConfigBase<?>> storage = List.of(
            basicCapacity, condensedCapacity, hyperCapacity, maxItemTypes
        );
        List<ConfigBase<?>> gui = List.of(
            focusGuiInput, guiSortMode, guiSortOrder, guiSearchMode, guiSaveSearch, guiAutoRefill, guiSearchText, renderCraftingPreview, itemCountFontScalePercent
        );
        List<ConfigBase<?>> hotkey = new ArrayList<>();

        ALL_OPTIONS.addAll(storage);
        ALL_OPTIONS.addAll(gui);
        HOTKEYS.add(emiFillSearchHotkey);
        hotkey.addAll(HOTKEYS);

        tabs.add(new ConfigTab("storage", storage));
        tabs.add(new ConfigTab("gui", gui));
        tabs.add(new ConfigTab("hotkey", hotkey));

        INSTANCE = new EZConfiguration(Reference.MOD_NAME, HOTKEYS, ALL_OPTIONS);
    }
}
