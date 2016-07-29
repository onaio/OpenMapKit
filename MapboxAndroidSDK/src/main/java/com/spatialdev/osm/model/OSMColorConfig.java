package com.spatialdev.osm.model;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jason Rogena - jrogena@ona.io on 7/28/16.
 */
public class OSMColorConfig {
    private final boolean enabled;
    private final String osmTagKey;
    private final HashMap<String, String> valueColors;
    private static final int FOCUS_OUT_ALPHA_DELTA = 100;
    public static final ARGB DEFAULT_ARGB = new ARGB(255, 6, 21, 57);

    public OSMColorConfig(boolean enabled, String osmTagKey, HashMap<String, String> valueColors) {
        this.enabled = enabled;
        this.osmTagKey = osmTagKey.trim();
        this.valueColors = valueColors;
    }

    public static ARGB getFocusInARGB(OSMElement osmElement, ARGB nonEnabledColor) {
        if(osmElement != null && osmElement.getOsmColorConfig() != null) {
            if(osmElement.getOsmColorConfig().enabled == true) {
                return osmElement.getOsmColorConfig().getOSMElementColor(osmElement);
            }
        }
        return nonEnabledColor;
    }

    public static ARGB getFocusOutARGB(OSMElement osmElement, ARGB nonEnabledColor) {
        if(osmElement != null && osmElement.getOsmColorConfig() != null) {
            if(osmElement.getOsmColorConfig().enabled == true) {
                ARGB color = osmElement.getOsmColorConfig().getOSMElementColor(osmElement);
                return new ARGB(
                        color.a - FOCUS_OUT_ALPHA_DELTA,
                        color.r,
                        color.g,
                        color.b);
            }
        }
        return nonEnabledColor;
    }

    public static Drawable getFocusInDrawable(OSMElement osmElement, Drawable defaultDrawable) {
        if(osmElement != null && osmElement.getOsmColorConfig() != null) {
            if(osmElement.getOsmColorConfig().enabled) {
                ARGB argb = getFocusInARGB(osmElement, DEFAULT_ARGB);
                defaultDrawable.setColorFilter(argb.getIntValue(), PorterDuff.Mode.MULTIPLY);
            }
        }
        return defaultDrawable;
    }

    public static Drawable getFocusOutDrawable(OSMElement osmElement, Drawable defaultDrawable) {
        if(osmElement != null && osmElement.getOsmColorConfig() != null) {
            if(osmElement.getOsmColorConfig().enabled) {
                ARGB argb = getFocusOutARGB(osmElement, DEFAULT_ARGB);
                defaultDrawable.setColorFilter(argb.getIntValue(), PorterDuff.Mode.MULTIPLY);
            }
        }
        return defaultDrawable;
    }

    /**
     * This method returns a configuration where coloring of features based on tag values is disabled
     *
     * @return  Non NULL configuration where coloring of features based on tag values is disabled
     */
    public static OSMColorConfig getDefaultConfig() {
        return new OSMColorConfig(false, "", new HashMap<String, String>());
    }

    /**
     * This method determines the color corresponding to the provided OSMElement. If no custom color
     * is found, the default color is returned
     *
     * @param osmElement    The element the color is to be gotten
     * @return  Color corresponding to the provided OSMElement or the default color if nothing found
     */
    private ARGB getOSMElementColor(OSMElement osmElement) {
        if(osmElement != null) {
            Map<String, String> osmTags = osmElement.getTags();
            if(osmTags.containsKey(osmTagKey)) {
                String tagValue = osmTags.get(osmTagKey);
                if(valueColors.containsKey(tagValue)) {
                    String hexColor = valueColors.get(tagValue);
                    ARGB color = hexToARGB(hexColor);
                    if(color != null) {
                        return color;
                    }
                }
            }
        }
        return DEFAULT_ARGB;
    }

    /**
     * This method converts hex color codes to an ARGB color code
     *
     * @param hexCode   The hexadecimal representation of the color
     * @return  ARGB object representing the color or NULL if
     */
    public static ARGB hexToARGB(String hexCode) {
        try {
            int intColor = Color.parseColor(hexCode);
            return new ARGB(Color.alpha(intColor),
                    Color.red(intColor),
                    Color.green(intColor),
                    Color.blue(intColor));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class ARGB {
        public final int a;
        public final int r;
        public final int g;
        public final int b;

        public ARGB(int a, int r, int g, int b) {
            this.a = a;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public int getIntValue() {
            return Color.argb(a, r, g, b);
        }
    }
}
