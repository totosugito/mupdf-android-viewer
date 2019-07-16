package com.artifex.mupdf.viewer.gp.util;

import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;

public class ThemeColor {
    private static ThemeColor instance;

    private int themeType = 1;
    private String foregroundColor = "#2980B9";

    private final String DARK_THEME_COLOR = "#222222";
    private final String LIGHT_THEME_COLOR = "#ffffff";

    public static final int DARK_THEME_TYPE = 1;
    public static final int LIGHT_THEME_TYPE = 2;

    public ThemeColor() {

    }

    public static ThemeColor getInstance() {
        if (instance == null) {
            instance = new ThemeColor();
        }
        return instance;
    }

    public int getThemeColor() {
        if (themeType == DARK_THEME_TYPE)
            return Color.parseColor(DARK_THEME_COLOR);
        return Color.parseColor(LIGHT_THEME_COLOR);
    }

    public void setThemeType(int themeType) {
        this.themeType = themeType;
    }

    public void setForegroundColor(String foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    public int getThemeType() {
        return themeType;
    }

    public int getForegroundColor() {
        return Color.parseColor(foregroundColor);
    }

    public ColorFilter getForegroundColorFilter() {
        int color = getForegroundColor();
        int red = (color & 0xFF0000) / 0xFFFF;
        int green = (color & 0xFF00) / 0xFF;
        int blue = color & 0xFF;
        float[] matrix = {0, 0, 0, 0, red
                , 0, 0, 0, 0, green
                , 0, 0, 0, 0, blue
                , 0, 0, 0, 1, 0};
        return new ColorMatrixColorFilter(matrix);
    }
}
