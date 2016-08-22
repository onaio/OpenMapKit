package org.redcross.openmapkit;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redcross.openmapkit.odkcollect.Form;
import org.redcross.openmapkit.odkcollect.ODKCollectHandler;
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
public class SettingsTest {

    Context context;

    @Before
    public void setup() throws Exception {
        Intent launchIntent = ApplicationTest.getLaunchOMKIntent();
        ODKCollectHandler.registerIntent(launchIntent);
        context = InstrumentationRegistry.getContext();
        copySettingsDir();
    }

    private void copySettingsDir() {
        //first remove the existing dir if already exists
        File dir = new File(ExternalStorage.getSettingsDir());
        dir.mkdirs();
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
     * This method tests whether the location configuration variables are those set in the
     * sample instrumentation json file and not the default ones in Settings. Please make
     * sure the values in the omk_functional_test.json file under androidTest/assets does not have
     * similar default configurations in Settings
     *
     * @throws IOException
     * @throws XmlPullParserException
     */
    @Test
    public void testProximitySettings() {
        Settings.initialize();
        /*
        make sure the values in androidTest/assets/settings/omk_functional_test.json are not the
        default settings
         */
        assertTrue(Settings.singleton().getProximityRadius() != Settings.DEFAULT_PROXIMITY_RADIUS);
        assertTrue(Settings.singleton().getProximityCheck() != Settings.DEFAULT_PROXIMITY_CHECK);
        assertTrue(Settings.singleton().getGpsTimerDelay() != Settings.DEFAULT_GPS_TIMER_DELAY);
        assertTrue(Settings.singleton().getGpsProximityAccuracy() != Settings.DEFAULT_GPS_PROXIMITY_ACCURACY);

        //check if the parsed values concur with the ones in omk_functional_test.json
        assertEquals(Settings.singleton().getProximityRadius(), 100d);
        assertEquals(Settings.singleton().getProximityCheck(), true);
        assertEquals(Settings.singleton().getGpsTimerDelay(), 10);
        assertEquals(Settings.singleton().getGpsProximityAccuracy(), 10d);
    }

    /**
     * This method test if changing the default proximityEnabled value in Settings works
     *
     * @throws IOException
     * @throws XmlPullParserException
     */
    @Test
    public void testProximityEnabled() {
        Settings.initialize();
        assertFalse(Settings.isProximityEnabled());

        Settings.setProximityEnabled(true);
        assertTrue(Settings.isProximityEnabled());
    }

    /**
     * This method tests whether the OSM from ODK configuration variables are those set in the
     * sample instrumentation json file and not the default ones in Settings. Please make
     * sure the values in the omk_functional_test.json file under androidTest/assets does not have
     * similar default configurations in Settings
     */
    @Test
    public void testOsmFromOdkSettings() {
        Settings.initialize();
        /*
        make sure the values in androidTest/assets/settings/omk_functional_test.json are not the
        default settings
         */
        assertFalse(Settings.singleton().getOSMFromODKUsername().equals(Settings.DEFAULT_OSM_FROM_ODK_USERNAME));
        assertFalse(Settings.singleton().getOSMFromODKPassword().equals(Settings.DEFAULT_OSM_FROM_ODK_PASSWORD));
        assertFalse(Settings.singleton().getOSMFromODKServer().equals(Settings.DEFAULT_OSM_FROM_ODK_SERVER));
        assertFalse(Settings.singleton().getOSMFromODKQuery().equals(Settings.DEFAULT_OSM_FROM_ODK_QUERY));
        assertFalse(Settings.singleton().getOSMFromODKForms().size() == 0);

        assertEquals(Settings.singleton().getOSMFromODKUsername(), "testomkuser");
        assertEquals(Settings.singleton().getOSMFromODKPassword(), "testomkpassword");
        assertEquals(Settings.singleton().getOSMFromODKServer(), "https://stage-api.ona.io/api/v1/data/");
        assertEquals(Settings.singleton().getOSMFromODKQuery(), "test_query");
        assertTrue(Settings.singleton().getOSMFromODKForms().contains(new Form(null, 1234)));
        assertTrue(Settings.singleton().getOSMFromODKForms().contains(new Form(null, 5678)));
        Log.d("SettingsTest", "size = "+Settings.singleton().getOSMFromODKForms().size());
        assertTrue(Settings.singleton().getOSMFromODKForms().size() == 2);
    }

    /**
     * This method tests the node_name setting
     */
    @Test
    public void testNodeNameSetting() {
        Settings.initialize();

        assertFalse(Settings.singleton().getNodeName().equals(Settings.DEFAULT_NODE_NAME));
        assertEquals(Settings.singleton().getNodeName(), "Structure");
    }

    /**
     * This method tests the hidden_menu_items setting
     */
    @Test
    public void testHiddenMenuItemsSetting() {
        Settings.initialize();

        assertFalse(Settings.singleton().getHiddenMenuItems().size() == 0);

        assertTrue(Settings.singleton().getHiddenMenuItems().contains("deployments"));
        assertTrue(Settings.singleton().getHiddenMenuItems().contains("basemap"));
        assertTrue(Settings.singleton().getHiddenMenuItems().contains("osm user name"));
        assertTrue(Settings.singleton().getHiddenMenuItems().contains("osm xml downloader"));
        assertTrue(Settings.singleton().getHiddenMenuItems().contains("osm xml layers"));
        assertTrue(Settings.singleton().getHiddenMenuItems().contains("info"));
    }
}
