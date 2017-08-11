
package com.taskdesignsinc.android.thememanager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

public class ThemeManager {
    private static final String KEY_THEME = "theme";
    private static final String KEY_BG_THEME = "bg_theme";
    private static ThemeManager mInstance = null;

    private ThemeType currentTheme = ThemeType.Light;
    private boolean isLightBackground = true;

    public void setCurrentTheme(AppCompatActivity activity) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        int themeType = Integer.parseInt(mPrefs.getString(KEY_THEME, String.valueOf(ThemeType.Blue.getValue())));
        boolean isLight = mPrefs.getBoolean(KEY_BG_THEME, false);
        setCurrentTheme(activity, themeType, isLight);
    }

    public enum ThemeType {
        Light(0),
        Dark(1),
        Amber(2),
        Blue(3),
        Blue_Grey(4),
        Brown(5),
        Cyan(6),
        Deep_Orange(7),
        Deep_Purple(8),
        Green(9),
        Indigo(10),
        Light_Blue(11),
        Light_Green(12),
        Lime(13),
        Orange(14),
        Pink(15),
        Purple(16),
        Red(17),
        Teal(18),
        Yellow(19);

        private int value;

        ThemeType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private int mPrimaryColors[] = {
            R.color.holo_blue_light,
            R.color.holo_blue_dark,
            R.color.material_amber_primary,
            R.color.material_blue_primary,
            R.color.material_blue_grey_primary,
            R.color.material_brown_primary,
            R.color.material_cyan_primary,
            R.color.material_deep_orange_primary,
            R.color.material_deep_purple_primary,
            R.color.material_green_primary,
            R.color.material_indigo_primary,
            R.color.material_light_blue_primary,
            R.color.material_light_green_primary,
            R.color.material_lime_primary,
            R.color.material_orange_primary,
            R.color.material_pink_primary,
            R.color.material_purple_primary,
            R.color.material_red_primary,
            R.color.material_teal_primary,
            R.color.material_yellow_primary
    };

    public int mPrimaryDarkColors[] = {
            R.color.holo_blue_light,
            R.color.holo_blue_dark,
            R.color.material_amber_dark,
            R.color.material_blue_dark,
            R.color.material_blue_grey_dark,
            R.color.material_brown_dark,
            R.color.material_cyan_dark,
            R.color.material_deep_orange_dark,
            R.color.material_deep_purple_dark,
            R.color.material_green_dark,
            R.color.material_indigo_dark,
            R.color.material_light_blue_dark,
            R.color.material_light_green_dark,
            R.color.material_lime_dark,
            R.color.material_orange_dark,
            R.color.material_pink_dark,
            R.color.material_purple_dark,
            R.color.material_red_dark,
            R.color.material_teal_dark,
            R.color.material_yellow_dark
    };

    private int mAccentColors[] = {
            R.color.holo_blue_light,
            R.color.holo_blue_dark,
            R.color.material_amber_accent,
            R.color.material_blue_accent,
            R.color.material_blue_grey_accent,
            R.color.material_brown_accent,
            R.color.material_cyan_accent,
            R.color.material_deep_orange_accent,
            R.color.material_deep_purple_accent,
            R.color.material_green_accent,
            R.color.material_indigo_accent,
            R.color.material_light_blue_accent,
            R.color.material_light_green_accent,
            R.color.material_lime_accent,
            R.color.material_orange_accent,
            R.color.material_pink_accent,
            R.color.material_purple_accent,
            R.color.material_red_accent,
            R.color.material_teal_accent,
            R.color.material_yellow_accent
    };

    private ThemeManager() {
    }

    public static ThemeManager getInstance(Context ctx) {
        /**
         * use the application context as suggested by CommonsWare. this will
         * ensure that you dont accidentally leak an Activitys context (see this
         * article for more information:
         * http://developer.android.com/resources/articles
         * /avoiding-memory-leaks.html)
         */
        if (mInstance == null) {
            mInstance = new ThemeManager();
        }
        return mInstance;
    }

    public static ThemeManager getInstance() {
        /**
         * use the application context as suggested by CommonsWare. this will
         * ensure that you dont accidentally leak an Activitys context (see this
         * article for more information:
         * http://developer.android.com/resources/articles
         * /avoiding-memory-leaks.html)
         */
        return mInstance;
    }

    public void setCurrentTheme(int theme) {
        switch (theme) {
            default:
                currentTheme = ThemeType.values()[theme];
                break;
        }
    }

    public void setCurrentTheme(Activity act, int theme, boolean isLight) {
        isLightBackground = isLight;
        if (!isLight) {
            switch (ThemeType.values()[theme]) {
                case Dark:
                    isLightBackground = false;
                    currentTheme = ThemeType.Dark;
                    act.setTheme(R.style.DarkTheme);
                    break;
                case Light:
                    currentTheme = ThemeType.Light;
                    act.setTheme(R.style.LightTheme);
                    break;
                case Amber:
                    currentTheme = ThemeType.Amber;
                    act.setTheme(R.style.AmberTheme);
                    break;
                case Blue:
                    currentTheme = ThemeType.Blue;
                    act.setTheme(R.style.BlueTheme);
                    break;
                case Blue_Grey:
                    currentTheme = ThemeType.Blue_Grey;
                    act.setTheme(R.style.BlueGreyTheme);
                    break;
                case Brown:
                    currentTheme = ThemeType.Brown;
                    act.setTheme(R.style.BrownTheme);
                    break;
                case Cyan:
                    currentTheme = ThemeType.Cyan;
                    act.setTheme(R.style.CyanTheme);
                    break;
                case Deep_Orange:
                    currentTheme = ThemeType.Deep_Orange;
                    act.setTheme(R.style.DeepOrangeTheme);
                    break;
                case Deep_Purple:
                    currentTheme = ThemeType.Deep_Purple;
                    act.setTheme(R.style.DeepPurpleTheme);
                    break;
                case Green:
                    currentTheme = ThemeType.Green;
                    act.setTheme(R.style.GreenTheme);
                    break;
                case Indigo:
                    currentTheme = ThemeType.Indigo;
                    act.setTheme(R.style.IndigoTheme);
                    break;
                case Light_Blue:
                    currentTheme = ThemeType.Light_Blue;
                    act.setTheme(R.style.LightBlueTheme);
                    break;
                case Light_Green:
                    currentTheme = ThemeType.Light_Green;
                    act.setTheme(R.style.LightGreenTheme);
                    break;
                case Lime:
                    currentTheme = ThemeType.Lime;
                    act.setTheme(R.style.LimeTheme);
                    break;
                case Orange:
                    currentTheme = ThemeType.Orange;
                    act.setTheme(R.style.OrangeTheme);
                    break;
                case Pink:
                    currentTheme = ThemeType.Pink;
                    act.setTheme(R.style.PinkTheme);
                    break;
                case Purple:
                    currentTheme = ThemeType.Purple;
                    act.setTheme(R.style.PurpleTheme);
                    break;
                case Red:
                    currentTheme = ThemeType.Red;
                    act.setTheme(R.style.RedTheme);
                    break;
                case Teal:
                    currentTheme = ThemeType.Teal;
                    act.setTheme(R.style.TealTheme);
                    break;
                case Yellow:
                    currentTheme = ThemeType.Yellow;
                    act.setTheme(R.style.YellowTheme);
                    break;
            }
        } else {
            switch (ThemeType.values()[theme]) {
                case Dark:
                    isLightBackground = false;
                    currentTheme = ThemeType.Dark;
                    act.setTheme(R.style.DarkTheme);
                    break;
                case Light:
                    currentTheme = ThemeType.Light;
                    act.setTheme(R.style.LightTheme);
                    break;
                case Amber:
                    currentTheme = ThemeType.Amber;
                    act.setTheme(R.style.AmberThemeL);
                    break;
                case Blue:
                    currentTheme = ThemeType.Blue;
                    act.setTheme(R.style.BlueThemeL);
                    break;
                case Blue_Grey:
                    currentTheme = ThemeType.Blue_Grey;
                    act.setTheme(R.style.BlueGreyThemeL);
                    break;
                case Brown:
                    currentTheme = ThemeType.Brown;
                    act.setTheme(R.style.BrownThemeL);
                    break;
                case Cyan:
                    currentTheme = ThemeType.Cyan;
                    act.setTheme(R.style.CyanThemeL);
                    break;
                case Deep_Orange:
                    currentTheme = ThemeType.Deep_Orange;
                    act.setTheme(R.style.DeepOrangeThemeL);
                    break;
                case Deep_Purple:
                    currentTheme = ThemeType.Deep_Purple;
                    act.setTheme(R.style.DeepPurpleThemeL);
                    break;
                case Green:
                    currentTheme = ThemeType.Green;
                    act.setTheme(R.style.GreenThemeL);
                    break;
                case Indigo:
                    currentTheme = ThemeType.Indigo;
                    act.setTheme(R.style.IndigoThemeL);
                    break;
                case Light_Blue:
                    currentTheme = ThemeType.Light_Blue;
                    act.setTheme(R.style.LightBlueThemeL);
                    break;
                case Light_Green:
                    currentTheme = ThemeType.Light_Green;
                    act.setTheme(R.style.LightGreenThemeL);
                    break;
                case Lime:
                    currentTheme = ThemeType.Lime;
                    act.setTheme(R.style.LimeThemeL);
                    break;
                case Orange:
                    currentTheme = ThemeType.Orange;
                    act.setTheme(R.style.OrangeThemeL);
                    break;
                case Pink:
                    currentTheme = ThemeType.Pink;
                    act.setTheme(R.style.PinkThemeL);
                    break;
                case Purple:
                    currentTheme = ThemeType.Purple;
                    act.setTheme(R.style.PurpleThemeL);
                    break;
                case Red:
                    currentTheme = ThemeType.Red;
                    act.setTheme(R.style.RedThemeL);
                    break;
                case Teal:
                    currentTheme = ThemeType.Teal;
                    act.setTheme(R.style.TealThemeL);
                    break;
                case Yellow:
                    currentTheme = ThemeType.Yellow;
                    act.setTheme(R.style.YellowThemeL);
                    break;
            }
        }
    }

    public ThemeType getCurrentTheme() {
        return currentTheme;
    }

    public boolean isLightBackground() {
        return (currentTheme == ThemeType.Light) || isLightBackground && (currentTheme != ThemeType.Dark);
    }

    public int getTextColor() {
        switch (currentTheme) {
            case Light:
                return Color.BLACK;
            case Dark:
                return Color.WHITE;
            default:
                if (!isLightBackground)
                    return Color.WHITE;
                else
                    return Color.BLACK;
        }
    }

    public int getInvertedTextColor() {
        switch (currentTheme) {
            case Dark:
                return Color.BLACK;
            case Light:
                return Color.WHITE;
            default:
                if (isLightBackground)
                    return Color.WHITE;
                else
                    return Color.BLACK;
        }
    }

    public int getPrimaryTextColor(Context context) {
        switch (currentTheme) {
            case Light:
                return Color.BLACK;
            case Dark:
                return Color.WHITE;
            default:
                return context.getResources().getColor(mPrimaryColors[currentTheme.value]);
        }
    }

    public boolean isValidTextColor(int color) {
        switch (currentTheme) {
            case Light:
                return (color != Color.WHITE);
            case Dark:
                return (color != Color.BLACK);
            default:
                if (!isLightBackground)
                    return (color != Color.BLACK);
                else
                    return (color != Color.WHITE);
        }
    }

    public int getBackgroundColor(Context context) {
        switch (currentTheme) {
            case Light:
                return context.getResources().getColor(R.color.background_holo_light);
            case Dark:
                return context.getResources().getColor(R.color.background_holo_dark);
            default:
                if (!isLightBackground)
                    return context.getResources().getColor(R.color.background_floating_material_dark);
                else
                    return context.getResources().getColor(R.color.background_floating_material_light);
        }
    }

    public int getBackgroundColorResId() {
        switch (currentTheme) {
            case Light:
                return R.color.background_holo_light;
            case Dark:
                return R.color.background_holo_dark;
            default:
                return android.R.color.transparent;
        }
    }

    public int getIndicatorColor(Context context) {
        switch (currentTheme) {
            case Light:
                return context.getResources().getColor(R.color.holo_blue_light);
            case Dark:
                return context.getResources().getColor(R.color.holo_blue_dark);
            default:
                return context.getResources().getColor(mPrimaryDarkColors[currentTheme.value]);
        }
    }

    public int getAccentColorResId() {
        switch (currentTheme) {
            case Light:
                return R.color.background_holo_light;
            case Dark:
                return R.color.background_holo_dark;
            default:
                return mAccentColors[currentTheme.value];
        }
    }

    public int getAccentColor(Context context) {
        switch (currentTheme) {
            case Light:
                return context.getResources().getColor(R.color.holo_blue_light);
            case Dark:
                return context.getResources().getColor(R.color.holo_blue_dark);
            default:
                return context.getResources().getColor(mAccentColors[currentTheme.value]);
        }
    }

    public int getPrimaryDarkColorResId() {
        switch (currentTheme) {
            case Light:
                return R.color.background_holo_light;
            case Dark:
                return R.color.background_holo_dark;
            default:
                return mPrimaryDarkColors[currentTheme.value];
        }
    }

    public int getPrimaryDarkColor(Context context) {
        switch (currentTheme) {
            case Light:
                return context.getResources().getColor(R.color.holo_blue_light);
            case Dark:
                return context.getResources().getColor(R.color.holo_blue_dark);
            default:
                return context.getResources().getColor(mPrimaryDarkColors[currentTheme.value]);
        }
    }

    public int getPrimaryColorResId() {
        switch (currentTheme) {
            case Light:
                return R.color.primary_material_light;
            case Dark:
                return R.color.primary_dark_material_dark;
            default:
                return mPrimaryColors[currentTheme.value];
        }
    }

    public int getPrimaryColor(Context context) {
        switch (currentTheme) {
            case Light:
                return context.getResources().getColor(R.color.primary_material_light);
            case Dark:
                return context.getResources().getColor(R.color.primary_material_dark);
            default:
                return context.getResources().getColor(mPrimaryColors[currentTheme.value]);
        }
    }

}
