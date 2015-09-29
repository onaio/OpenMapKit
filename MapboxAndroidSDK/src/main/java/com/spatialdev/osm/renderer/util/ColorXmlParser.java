package com.spatialdev.osm.renderer.util;

import android.content.Context;
import android.widget.Toast;

import com.mapbox.mapboxsdk.util.ExternalStorage;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by coder on 7/30/15.
 */
public class ColorXmlParser {
    public static final String FILENAME = "coloring.xml";
    public static final String COLOR = "color";
    public static final String KEY = "tag";
    public static final String VALUE = "value";
    public static final String COLOR_CODE = "color_code";
    public static final String PRIORITY = "priority";
    public static final String OPACITY = "opacity";
    public static final int DEFAULT_ALPHA = 50;

    public static XmlPullParser createPullParser(Context ctx) {
        XmlPullParserFactory pullParserFactory;
        try
        {
            pullParserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = pullParserFactory.newPullParser();

            final File file = new File(ExternalStorage.getSettingsDir()+FILENAME);
            InputStream in_s = new FileInputStream(file);
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in_s, null);
            return parser;

        } catch (XmlPullParserException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
        Toast.makeText(ctx, "Add the file " + FILENAME + " in the dir " + ExternalStorage.getSettingsDir(), Toast.LENGTH_LONG).show();
        return null;
    }

    public static List<ColorElement> parseXML(Context ctx) throws XmlPullParserException, IOException {
        List<ColorElement> colorElementList = new ArrayList<>();
        String input;
        //Add the default settings.
        XmlPullParser parser = createPullParser(ctx);
        if (parser == null) {
            return colorElementList;
        }
        int eventType = parser.getEventType();
        ColorElement colorElement = new ColorElement();
        // Set the opacity to default.
        colorElement.setOpacity(DEFAULT_ALPHA);

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String name = null;
            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    if (name.equals(KEY)) {
                        input = parser.nextText();
                        colorElement.setKey(input);

                    } else if (name.equals(VALUE)) {
                        input = parser.nextText().trim();
                        colorElement.setValue(input);

                    } else if (name.equals(COLOR_CODE)) {
                        input = parser.nextText().trim();
                        colorElement.setColorCode(input);

                    } else if (name.equals(PRIORITY)) {
                        input = parser.nextText().trim();
                        colorElement.setPriority(Integer.parseInt(input));

                    } else if (name.equals(OPACITY)) {
                        input = parser.nextText().trim();
                        colorElement.setOpacity(Integer.parseInt(input));

                    }
                    break;
                case XmlPullParser.END_TAG:
                    name = parser.getName();
                    if (name.equals(COLOR)) {
                        colorElementList.add(colorElement);
                        colorElement = new ColorElement();
                    }
                    break;
            }
            eventType = parser.next();
        }
        //Sort the elements according to priority
        Collections.sort(colorElementList);
        return colorElementList;
    }
}
