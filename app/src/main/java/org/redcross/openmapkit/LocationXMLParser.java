package org.redcross.openmapkit;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by coder on 7/10/15.
 */

public class LocationXMLParser {
    public static final String FILENAME = "proximity_settings.xml";
    public static final String PROXIMITY_CHECK = "proximity_check";
    public static final String PROXIMITY_RADIUS = "proximity_radius";
    public static final String MAX_GPS_FIX_TIME = "max_gps_timer_delay";
    public static final String GPS_THRESHOLD_ACCURACY = "gps_proximity_accuracy";
    public static final double DEFAULT_PROXIMITY_RADIUS = 50d;
    public static final boolean DEFAULT_PROXIMITY_CHECK = false;
    public static final int DEFAULT_GPS_TIMER_DELAY = 0;
    public static final float DEFAULT_GPS_PROXIMITY_ACCURACY = 25f;
    public static final boolean DEFAULT_PROXIMITY_ENABLED = false;

    private static double proximityRadius = DEFAULT_PROXIMITY_RADIUS;
    private static boolean proximityCheck = DEFAULT_PROXIMITY_CHECK;
    private static boolean proximityEnabled = DEFAULT_PROXIMITY_ENABLED;
    private static int gpsTimerDelay = DEFAULT_GPS_TIMER_DELAY;
    private static float gpsProximityAccuracy = DEFAULT_GPS_PROXIMITY_ACCURACY;

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
        return null;
    }

    public static void parseXML(Context ctx) throws XmlPullParserException, IOException {

        String input;
        //Add the default settings.
        XmlPullParser parser = createPullParser(ctx);
        if (parser == null) {
            return;
        }
        int eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String name = null;
            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    if (name.equals(PROXIMITY_CHECK)) {
                        input = parser.nextText();
                        if (input.toLowerCase().startsWith("f") || input.toLowerCase().startsWith("n")) {
                            proximityCheck = false;
                        } else if (input.toLowerCase().startsWith("t") || input.toLowerCase().startsWith("y")) {
                            proximityCheck = true;
                        }
                    } else if (name.equals(PROXIMITY_RADIUS)) {
                        input = parser.nextText().trim();
                        try {
                            proximityRadius = Double.parseDouble(input);
                        } catch (NumberFormatException e) {
                            //e.printStackTrace();
                        }
                    } else if (name.equals(MAX_GPS_FIX_TIME)) {
                        input = parser.nextText().trim();
                        try {
                            gpsTimerDelay = Integer.parseInt(input);
                        } catch (NumberFormatException e) {
                            //e.printStackTrace();
                        }
                    } else if (name.equals(GPS_THRESHOLD_ACCURACY)) {
                        input = parser.nextText().trim();
                        try {
                            gpsProximityAccuracy = Float.parseFloat(input);
                        } catch (NumberFormatException e) {
                            //e.printStackTrace();
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    break;
            }
            eventType = parser.next();
        }
    }

    /**
     *
     * @return proximity proximityRadius around user location.
     */
    public static double getProximityRadius() {
        return proximityRadius;
    }

    /**
     *
     * @return true if GPS must be enabled to show user location.
     */
    public static boolean getProximityCheck() {
        return proximityCheck;
    }

    /**
     *
     * @param value set whether to apply proximity settings.
     */
    public static void setProximityEnabled(boolean value) {
        proximityEnabled = value;
    }

    /**
     *
     * @return true if proximity settings should be applied.
     */
    public static boolean isProximityEnabled() {
        return proximityEnabled;
    }

    public static int getGpsTimerDelay() {
        return gpsTimerDelay;
    }

    public static float getGpsProximityAccuracy() {
        return gpsProximityAccuracy;
    }
}
