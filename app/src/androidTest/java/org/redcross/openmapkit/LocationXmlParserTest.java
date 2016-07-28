package org.redcross.openmapkit;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Created by Jason Rogena - jrogena@ona.io on 7/27/16.
 */
@RunWith(AndroidJUnit4.class)
public class LocationXmlParserTest {

    Context context;

    @Before
    public void setup() throws Exception {
        context = InstrumentationRegistry.getContext();
        copySettingsDir();
    }

    private void copySettingsDir() {
        //first remove the existing dir if already exists
        File dir = new File(ExternalStorage.getSettingsDir());
        if (dir.exists() && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                new File(dir, children[i]).delete();
            }
        }

        //add the fresh settings dir
        ExternalStorage.copyAssetsFileOrDirToExternalStorage(context, ExternalStorage.SETTINGS_DIR);
    }

    /**
     * Test whether the right tag names are used in the settings files.
     */
    @Test
    public void testLocationXmlTagNames() {
        assertEquals(LocationXMLParser.FILENAME, "proximity_settings.xml");
        assertEquals(LocationXMLParser.PROXIMITY_RADIUS, "proximity_radius");
        assertEquals(LocationXMLParser.PROXIMITY_CHECK, "proximity_check");
        assertEquals(LocationXMLParser.MAX_GPS_FIX_TIME, "max_gps_timer_delay");
        assertEquals(LocationXMLParser.GPS_THRESHOLD_ACCURACY, "gps_proximity_accuracy");
    }

    /**
     * This method tests whether the location configuration variables are those set in the
     * sample instrumentation xml file and not the default ones in LocationXMLParser. Please make
     * sure the values in the proximity_settings.xml file under androidTest/assets does not have
     * similar default configurations in LocationXMLParser
     *
     * @throws IOException
     * @throws XmlPullParserException
     */
    @Test
    public void testTagValues() throws IOException, XmlPullParserException {
        LocationXMLParser.parseXML(context);
        /*
        make sure the values in androidTest/assets/settings/proximity_settings.xml are not the
        default settings in LocationXMLParser
         */
        assertTrue(LocationXMLParser.getProximityRadius() != LocationXMLParser.DEFAULT_PROXIMITY_RADIUS);
        assertTrue(LocationXMLParser.getProximityCheck() != LocationXMLParser.DEFAULT_PROXIMITY_CHECK);
        assertTrue(LocationXMLParser.getGpsTimerDelay() != LocationXMLParser.DEFAULT_GPS_TIMER_DELAY);
        assertTrue(LocationXMLParser.getGpsProximityAccuracy() != LocationXMLParser.DEFAULT_GPS_PROXIMITY_ACCURACY);

        //check if the parsed values concur with the ones in proximity_settings.xml
        assertEquals(LocationXMLParser.getProximityRadius(), 100d);
        assertEquals(LocationXMLParser.getProximityCheck(), true);
        assertEquals(LocationXMLParser.getGpsTimerDelay(), 10);
        assertEquals(LocationXMLParser.getGpsProximityAccuracy(), 10f);
    }

    /**
     * This method test if changing the default proximityEnabled value in LocationXMLParser works
     *
     * @throws IOException
     * @throws XmlPullParserException
     */
    @Test
    public void testProximityEnabled() throws IOException, XmlPullParserException {
        LocationXMLParser.parseXML(context);
        assertFalse(LocationXMLParser.isProximityEnabled());

        LocationXMLParser.setProximityEnabled(true);
        assertTrue(LocationXMLParser.isProximityEnabled());
    }
}
