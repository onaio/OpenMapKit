package com.spatialdev.osm.renderer.util;

/**
 * Created by coder on 7/30/15.
 */
public class ColorElement implements Comparable<ColorElement> {
    private String key;
    private String value;
    private String colorCode;
    private int priority;

    public ColorElement() {}

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int compareTo(ColorElement colorElement) {
        if (this.getPriority() > colorElement.getPriority()) {
            return -1;
        } else if (this.getPriority() < colorElement.getPriority()) {
            return 1;
        }
        return 0;
    }
}
