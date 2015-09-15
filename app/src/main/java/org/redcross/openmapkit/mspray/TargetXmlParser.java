package org.redcross.openmapkit.mspray;

import android.content.Context;

import com.mapbox.mapboxsdk.util.ExternalStorage;

import org.redcross.openmapkit.mspray.TargetArea;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by coder on 8/6/15.
 */
public class TargetXmlParser {
    public static final String FILENAME = "target_areas.xml";
    public static final String TARGET = "target";
    public static final String NAME = "name";
    public static final String URL = "url";

    public static XmlPullParser createPullParser(Context ctx) {
        XmlPullParserFactory pullParserFactory;
        try {
            pullParserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = pullParserFactory.newPullParser();

            final File file = new File(ExternalStorage.getSettingsDir() + FILENAME);
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
        return null;
    }

    public static List<TargetArea> parseXML(Context ctx) throws XmlPullParserException, IOException {
        List<TargetArea> targetAreasList = new ArrayList<>();
        String input;
        //Add the default settings.
        XmlPullParser parser = createPullParser(ctx);
        if (parser == null) {
            return targetAreasList;
        }
        int eventType = parser.getEventType();
        TargetArea targetArea = new TargetArea();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String name = null;
            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    if (name.equals(NAME)) {
                        input = parser.nextText();
                        targetArea.setName(input);
                    } else if (name.equals(URL)) {
                        input = parser.nextText().trim();
                        targetArea.setUrl(input);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    name = parser.getName();
                    if (name.equals(TARGET)) {
                        targetAreasList.add(targetArea);
                        targetArea = new TargetArea();
                    }
                    break;
            }
            eventType = parser.next();
        }
        return targetAreasList;
    }
}
