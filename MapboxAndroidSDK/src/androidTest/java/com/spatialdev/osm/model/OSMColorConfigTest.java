package com.spatialdev.osm.model;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.test.R;

import junit.framework.Assert;

import org.hamcrest.core.IsNot;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

/**
 * Created by Jason Rogena - jrogena@ona.io on 7/29/16.
 */
@RunWith(AndroidJUnit4.class)
public class OSMColorConfigTest {

    /**
     * This test checks whether if the color config is set to default (no color changes based on tag
     * values), the default colors provided are what is returned when requesting for focusIn and
     * focusOut colors
     */
    @Test
    public void testDefaultConfigColor() {
        OSMColorConfig defaultConfig = OSMColorConfig.getDefaultConfig();
        OSMElement osmElement = new OSMNode(new LatLng(0.222222, 36.222222), defaultConfig);

        //check if color returned is provided default color
        OSMColorConfig.ARGB defaultArgb = new OSMColorConfig.ARGB(123, 222, 100, 43);
        OSMColorConfig.ARGB focusInArgb = OSMColorConfig.getFocusInARGB(osmElement, defaultArgb);
        Assert.assertEquals(defaultArgb.a, focusInArgb.a);
        Assert.assertEquals(defaultArgb.r, focusInArgb.r);
        Assert.assertEquals(defaultArgb.g, focusInArgb.g);
        Assert.assertEquals(defaultArgb.b, focusInArgb.b);

        OSMColorConfig.ARGB focusOutArgb = OSMColorConfig.getFocusOutARGB(osmElement, defaultArgb);
        Assert.assertEquals(defaultArgb.a, focusOutArgb.a);
        Assert.assertEquals(defaultArgb.r, focusOutArgb.r);
        Assert.assertEquals(defaultArgb.g, focusOutArgb.g);
        Assert.assertEquals(defaultArgb.b, focusOutArgb.b);
    }

    /**
     * This test checks whether if the color config is set to default (no color changes based on tag
     * values), the default drawables provided are what is returned when requesting for focusIn and
     * focusOut drawables
     */
    @Test
    @Ignore
    public void testDefaultConfigDrawable() {
        OSMColorConfig defaultConfig = OSMColorConfig.getDefaultConfig();
        OSMElement osmElement = new OSMNode(new LatLng(0.222222, 36.222222), defaultConfig);
        Context context = InstrumentationRegistry.getContext();
        Drawable originalDrawable = context.getResources().getDrawable(R.mipmap.maki_star_orange_1);
        Drawable focusInDrawable = OSMColorConfig.getFocusInDrawable(osmElement, context.getResources().getDrawable(R.mipmap.maki_star_orange_2));

        compareDrawableColorFilters(originalDrawable, focusInDrawable, true);

        Drawable focusOutDrawable = OSMColorConfig.getFocusOutDrawable(osmElement, context.getResources().getDrawable(R.mipmap.maki_star_orange_3));
        compareDrawableColorFilters(originalDrawable, focusOutDrawable, true);
    }

    /**
     * This method tests whether a non-default OSMColorConfig produces the right colors
     */
    @Test
    public void testNonDefaultConfigColor_focusIn() {
        HashMap<String, String> colorValues = new HashMap<>();
        String defaultColor = "#532343";
        String val1Color = "#324f21";
        String val2Color = "#4a4f21";
        String val3Color = "#ff4f25";
        colorValues.put("val_1", val1Color);
        colorValues.put("val_2", val2Color);
        colorValues.put("val_3", val3Color);
        colorValues.put("", defaultColor);//to be used for all other values apart from val_1, val_2, & val_3
        OSMColorConfig colorConfig = new OSMColorConfig(true, "test_tag", colorValues);
        
        OSMElement osmElement = new OSMNode(new LatLng(0.222222, 36.222222), colorConfig);

        //make sure the provided default ARGB is not the same with any of the value colors
        OSMColorConfig.ARGB nonEnabledArgb = new OSMColorConfig.ARGB(123, 222, 100, 43);
        org.junit.Assert.assertThat(nonEnabledArgb.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(val1Color).getIntValue()));
        org.junit.Assert.assertThat(nonEnabledArgb.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(val2Color).getIntValue()));
        org.junit.Assert.assertThat(nonEnabledArgb.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(val3Color).getIntValue()));
        org.junit.Assert.assertThat(nonEnabledArgb.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(defaultColor).getIntValue()));

        //test the focus in color
        osmElement.addOrEditTag("test_tag", "val_1");

        OSMColorConfig.ARGB focusInArgb = OSMColorConfig.getFocusInARGB(osmElement, nonEnabledArgb);
        Assert.assertEquals(focusInArgb.getIntValue(), nonEnabledArgb.getIntValue());
    }

    /**
     * This method tests whether a non-default OSMColorConfig produces the right colors
     */
    @Test
    public void testNonDefaultConfigColor_focusOut() {
        HashMap<String, String> colorValues = new HashMap<>();
        String defaultColor = "#532343";
        String val1Color = "#324f21";
        String val2Color = "#4a4f21";
        String val3Color = "#ff4f25";
        colorValues.put("val_1", val1Color);
        colorValues.put("val_2", val2Color);
        colorValues.put("val_3", val3Color);
        colorValues.put("", defaultColor);//to be used for all other values apart from val_1, val_2, & val_3
        OSMColorConfig colorConfig = new OSMColorConfig(true, "test_tag", colorValues);

        OSMElement osmElement = new OSMNode(new LatLng(0.222222, 36.222222), colorConfig);

        //make sure the provided default ARGB is not the same with any of the value colors
        OSMColorConfig.ARGB nonEnabledArgb = new OSMColorConfig.ARGB(123, 222, 100, 43);
        org.junit.Assert.assertThat(nonEnabledArgb.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(val1Color).getIntValue()));
        org.junit.Assert.assertThat(nonEnabledArgb.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(val2Color).getIntValue()));
        org.junit.Assert.assertThat(nonEnabledArgb.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(val3Color).getIntValue()));
        org.junit.Assert.assertThat(nonEnabledArgb.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(defaultColor).getIntValue()));

        //test the focus out color
        osmElement.addOrEditTag("test_tag", "val_3");

        OSMColorConfig.ARGB focusOutArgb = OSMColorConfig.getFocusOutARGB(osmElement, nonEnabledArgb);
        OSMColorConfig.ARGB focusOUtArgbT = new OSMColorConfig.ARGB(
                focusOutArgb.a+OSMColorConfig.FOCUS_OUT_ALPHA_DELTA,
                focusOutArgb.r,
                focusOutArgb.g,
                focusOutArgb.b);
        Assert.assertEquals(focusOUtArgbT.getIntValue(), new OSMColorConfig.ARGB(val3Color).getIntValue());
        org.junit.Assert.assertThat(focusOUtArgbT.getIntValue(), IsNot.not(nonEnabledArgb.getIntValue()));
        org.junit.Assert.assertThat(focusOUtArgbT.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(val1Color).getIntValue()));
        org.junit.Assert.assertThat(focusOUtArgbT.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(val2Color).getIntValue()));
        org.junit.Assert.assertThat(focusOUtArgbT.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(defaultColor).getIntValue()));
    }

    /**
     * This method tests whether a non-default OSMColorConfig produces the right colors
     */
    @Test
    public void testNonDefaultConfigColor_unknownValue() {
        HashMap<String, String> colorValues = new HashMap<>();
        String defaultColor = "#532343";
        String val1Color = "#324f21";
        String val2Color = "#4a4f21";
        String val3Color = "#ff4f25";
        colorValues.put("val_1", val1Color);
        colorValues.put("val_2", val2Color);
        colorValues.put("val_3", val3Color);
        colorValues.put("", defaultColor);//to be used for all other values apart from val_1, val_2, & val_3
        OSMColorConfig colorConfig = new OSMColorConfig(true, "test_tag", colorValues);

        OSMElement osmElement = new OSMNode(new LatLng(0.222222, 36.222222), colorConfig);

        //make sure the provided default ARGB is not the same with any of the value colors
        OSMColorConfig.ARGB nonEnabledArgb = new OSMColorConfig.ARGB(123, 222, 100, 43);
        org.junit.Assert.assertThat(nonEnabledArgb.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(val1Color).getIntValue()));
        org.junit.Assert.assertThat(nonEnabledArgb.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(val2Color).getIntValue()));
        org.junit.Assert.assertThat(nonEnabledArgb.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(val3Color).getIntValue()));
        org.junit.Assert.assertThat(nonEnabledArgb.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(defaultColor).getIntValue()));

        //test the default color on a tag value that is the default value
        osmElement.addOrEditTag("test_tag", OSMColorConfig.DEFAULT_VALUE);

        OSMColorConfig.ARGB default1Argb = OSMColorConfig.getFocusInARGB(osmElement, nonEnabledArgb);
        Assert.assertEquals(default1Argb.getIntValue(), nonEnabledArgb.getIntValue());
        org.junit.Assert.assertThat(default1Argb.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(defaultColor).getIntValue()));

        //test the default color on a tag value that has not been specified
        osmElement.addOrEditTag("test_tag", "fdafdfds");

        OSMColorConfig.ARGB default3Argb = OSMColorConfig.getFocusInARGB(osmElement, nonEnabledArgb);
        Assert.assertEquals(default3Argb.getIntValue(), nonEnabledArgb.getIntValue());
        org.junit.Assert.assertThat(default3Argb.getIntValue(), IsNot.not(new OSMColorConfig.ARGB(defaultColor).getIntValue()));
    }

    /**
     * This method tests whether a non-default OSMColorConfig produces the right drawables
     */
    @Test
    @Ignore
    public void testNonDefaultConfigDrawable_focusIn() {
        HashMap<String, String> colorValues = new HashMap<>();
        String defaultColor = "#532343";
        String val1Color = "#324f21";
        String val2Color = "#4a4f21";
        String val3Color = "#ff4f25";
        colorValues.put("val_1", val1Color);
        colorValues.put("val_2", val2Color);
        colorValues.put("val_3", val3Color);
        colorValues.put("", defaultColor);//to be used for all other values apart from val_1, val_2, & val_3
        OSMColorConfig colorConfig = new OSMColorConfig(true, "test_tag", colorValues);

        OSMElement osmElement = new OSMNode(new LatLng(0.222222, 36.222222), colorConfig);
        Context context = InstrumentationRegistry.getContext();
        Drawable originalDrawable = context.getResources().getDrawable(R.mipmap.maki_star_orange_1);

        //test the focus in drawable
        osmElement.addOrEditTag("test_tag", "val_1");

        Drawable focusInDrawable = OSMColorConfig.getFocusInDrawable(osmElement, context.getResources().getDrawable(R.mipmap.maki_star_orange_2));
        Drawable val1Drawable = OSMColorConfig.applyColorFilterToDrawable(context.getResources().getDrawable(R.mipmap.maki_star_orange_3), new OSMColorConfig.ARGB(val1Color));

        compareDrawableColorFilters(focusInDrawable, originalDrawable, false);
        compareDrawableColorFilters(focusInDrawable, val1Drawable, true);
    }

    /**
     * This method tests whether a non-default OSMColorConfig produces the right drawables
     */
    @Test
    @Ignore
     public void testNonDefaultConfigDrawable_focusOut() {
        HashMap<String, String> colorValues = new HashMap<>();
        String defaultColor = "#532343";
        String val1Color = "#324f21";
        String val2Color = "#4a4f21";
        String val3Color = "#ff4f25";
        colorValues.put("val_1", val1Color);
        colorValues.put("val_2", val2Color);
        colorValues.put("val_3", val3Color);
        colorValues.put("", defaultColor);//to be used for all other values apart from val_1, val_2, & val_3
        OSMColorConfig colorConfig = new OSMColorConfig(true, "test_tag", colorValues);

        OSMElement osmElement = new OSMNode(new LatLng(0.222222, 36.222222), colorConfig);
        Context context = InstrumentationRegistry.getContext();
        Drawable originalDrawable = context.getResources().getDrawable(R.mipmap.maki_star_orange_1);

        //test the focus out drawable
        osmElement.addOrEditTag("test_tag", "val_3");

        Drawable focusOutDrawable = OSMColorConfig.getFocusOutDrawable(osmElement, context.getResources().getDrawable(R.mipmap.maki_star_orange_2));
        OSMColorConfig.ARGB val3ArgbFocused = new OSMColorConfig.ARGB(val3Color);
        OSMColorConfig.ARGB val3Argb = new OSMColorConfig.ARGB(
                val3ArgbFocused.a - OSMColorConfig.FOCUS_OUT_ALPHA_DELTA,
                val3ArgbFocused.r,
                val3ArgbFocused.g,
                val3ArgbFocused.b
        );
        Drawable val3Drawable = OSMColorConfig.applyColorFilterToDrawable(context.getResources().getDrawable(R.mipmap.maki_star_orange_3), val3Argb);

        compareDrawableColorFilters(focusOutDrawable, originalDrawable, false);
        compareDrawableColorFilters(focusOutDrawable, val3Drawable, true);
    }

    /**
     * This method tests whether a non-default OSMColorConfig produces the right drawables
     */
    @Test
    @Ignore
    public void testNonDefaultConfigDrawable_unknownValue() {
        HashMap<String, String> colorValues = new HashMap<>();
        String defaultColor = "#532343";
        String val1Color = "#324f21";
        String val2Color = "#4a4f21";
        String val3Color = "#ff4f25";
        colorValues.put("val_1", val1Color);
        colorValues.put("val_2", val2Color);
        colorValues.put("val_3", val3Color);
        colorValues.put("", defaultColor);//to be used for all other values apart from val_1, val_2, & val_3
        OSMColorConfig colorConfig = new OSMColorConfig(true, "test_tag", colorValues);

        OSMElement osmElement = new OSMNode(new LatLng(0.222222, 36.222222), colorConfig);
        Context context = InstrumentationRegistry.getContext();
        Drawable originalDrawable = context.getResources().getDrawable(R.mipmap.maki_star_orange_1);

        //test the default drawable for unknown values
        osmElement.addOrEditTag("test_tag", "fddfsdfds");

        Drawable unknownDrawable = OSMColorConfig.getFocusInDrawable(osmElement, context.getResources().getDrawable(R.mipmap.maki_star_orange_2));
        Drawable defaultDrawable = OSMColorConfig.applyColorFilterToDrawable(context.getResources().getDrawable(R.mipmap.maki_star_orange_3), new OSMColorConfig.ARGB(defaultColor));

        compareDrawableColorFilters(unknownDrawable, originalDrawable, false);
        compareDrawableColorFilters(unknownDrawable, defaultDrawable, true);
    }

    /**
     * This method tests whether initializing an ARGB using the hex code results in the same result
     * as initializing an ARGB using the Alpah, Red, Green, and Blue int values
     */
    @Test
    public void testARGBInitialization() {
        OSMColorConfig.ARGB argb1 = new OSMColorConfig.ARGB("#ffffffff");
        OSMColorConfig.ARGB argb2 = new OSMColorConfig.ARGB(255, 255, 255, 255);
        
        Assert.assertEquals(argb1.a, argb2.a);
        Assert.assertEquals(argb1.r, argb2.r);
        Assert.assertEquals(argb1.g, argb2.g);
        Assert.assertEquals(argb1.b, argb2.b);
        Assert.assertEquals(argb1.getIntValue(), argb2.getIntValue());

        OSMColorConfig.ARGB argb3 = new OSMColorConfig.ARGB("#d343abac");
        OSMColorConfig.ARGB argb4 = new OSMColorConfig.ARGB(211, 67, 171, 172);

        Assert.assertEquals(argb3.a, argb4.a);
        Assert.assertEquals(argb3.r, argb4.r);
        Assert.assertEquals(argb3.g, argb4.g);
        Assert.assertEquals(argb3.b, argb4.b);
        Assert.assertEquals(argb3.getIntValue(), argb4.getIntValue());
    }

    private void compareDrawableColorFilters(Drawable a, Drawable b, boolean checkEqual) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(checkEqual) {
                Assert.assertTrue(a.getColorFilter().equals(b.getColorFilter()));
            } else {
                Assert.assertFalse(a.getColorFilter().equals(b.getColorFilter()));
            }
        } else {
            Log.w("OSMColorConfigTest", "Skipping drawable color filters test");
        }
    }
}
