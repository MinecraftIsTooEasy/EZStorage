package com.zerofall.ezstorage.util;

import me.towdium.pinin.PinyinMatch;

import net.minecraft.Minecraft;

import net.xiaoyu233.fml.FishModLoader;

public class PinyinSearchUtils {

    /**
     * Convenience entry point that infers locale and pinin-mod availability.
     */
    public static boolean searchMatches(String text, String query) {
        return searchMatches(text, query, isChineseLocale(), FishModLoader.hasMod("pinin"));
    }

    /**
     * Returns true if the query is found in text (with optional pinyin support).
     */
    public static boolean searchMatches(String text, String query, boolean isChineseLocale, boolean hasPininMod)
    {
        if (query == null || query.isEmpty()) return true;

        if (text == null || text.isEmpty()) return false;

        if (isChineseLocale && hasPininMod)
        {
            try
            {
                return PinyinMatch.contains(text, query);
            }
            catch (Throwable ignored) {}
        }

        return text.toLowerCase().contains(query.toLowerCase());
    }

    public static boolean isChineseLocale()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc != null && mc.gameSettings != null && mc.gameSettings.language != null)
        {
            return mc.gameSettings.language.startsWith("zh_CN");
        }

        return false;
    }
}