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
    public static final String DEFAULT_VALUE = "";
    private final boolean enabled;
    private final String osmTagKey;
    /**
     * Color that should be used if the tag value does not match any of the ones provided
     * The difference between the defaultArgb and the nonEnabledArgb is the defaultArgb is what is
     * used in an enabled configuration when no color is found for a tag value. The nonEnabledArgb
     * is the color used in cases where the configuration is not enabled
     */
    private final ARGB defaultArgb;
    private final HashMap<String, String> valueColors;
    public static final int FOCUS_OUT_ALPHA_DELTA = 100;

    public OSMColorConfig(boolean enabled, String osmTagKey, HashMap<String, String> valueColors) {
        if(enabled == true) {
            this.enabled = check(osmTagKey, valueColors);
        } else {
            this.enabled = enabled;
        }
        this.osmTagKey = osmTagKey.trim();
        this.valueColors = valueColors;
        if(this.enabled == true) {
            defaultArgb = new ARGB(valueColors.get(DEFAULT_VALUE));
        } else {
            defaultArgb = null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getOsmTagKey() {
        return osmTagKey;
    }

    public ARGB getDefaultArgb() {
        return defaultArgb;
    }

    public HashMap<String, String> getValueColors() {
        return valueColors;
    }

    /**
     * This method checks whether the OSMColorConfig data is correct to warrant for the config to
     * qualify to be enabled
     *
     * @param osmTagKey     The OSM tag key
     * @param valueColors   Value colors corresponding to the tag
     * @return  TRUE if the data is correct
     */
    private boolean check(String osmTagKey, HashMap<String, String> valueColors) {
        if(osmTagKey == null || osmTagKey.length() == 0) {
            return false;
        }
        if(valueColors == null || !valueColors.containsKey(DEFAULT_VALUE)) {
            return false;
        }
        return true;
    }

    public static ARGB getFocusInARGB(OSMElement osmElement, ARGB nonEnabledColor) {
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

    public static Drawable getFocusInDrawable(OSMElement osmElement, Drawable originalDrawable) {
        if(osmElement != null && osmElement.getOsmColorConfig() != null) {
            if(osmElement.getOsmColorConfig().enabled) {
                originalDrawable = originalDrawable.mutate();
                ARGB argb = getFocusInARGB(osmElement, osmElement.getOsmColorConfig().defaultArgb);
                originalDrawable = applyColorFilterToDrawable(originalDrawable, argb);
            }
        }
        return originalDrawable;
    }

    public static Drawable getFocusOutDrawable(OSMElement osmElement, Drawable originalDrawable) {
        if(osmElement != null && osmElement.getOsmColorConfig() != null) {
            if(osmElement.getOsmColorConfig().enabled) {
                originalDrawable = originalDrawable.mutate();
                ARGB argb = getFocusOutARGB(osmElement, osmElement.getOsmColorConfig().defaultArgb);
                originalDrawable = applyColorFilterToDrawable(originalDrawable, argb);
            }
        }
        return originalDrawable;
    }

    public static Drawable applyColorFilterToDrawable(Drawable drawable, ARGB argb) {
        drawable.setColorFilter(argb.getIntValue(), PorterDuff.Mode.MULTIPLY);
        return drawable;
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
                if(tagValue != null && valueColors.containsKey(tagValue)) {
                    String hexColor = valueColors.get(tagValue);
                    ARGB color = new ARGB(hexColor);
                    if(color != null) {
                        return color;
                    }
                }
            }

            //no color for value gotten
            return osmElement.getOsmColorConfig().defaultArgb;
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

        public ARGB(String hexCode) {
            int intColor = Color.parseColor(hexCode);
            this.a = Color.alpha(intColor);
            this.r = Color.red(intColor);
            this.g = Color.green(intColor);
            this.b = Color.blue(intColor);
        }

        public int getIntValue() {
            return Color.argb(a, r, g, b);
        }
    }
}
