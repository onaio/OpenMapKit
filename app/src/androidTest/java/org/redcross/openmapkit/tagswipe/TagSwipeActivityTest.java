package org.redcross.openmapkit.tagswipe;

import android.app.Activity;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.util.Log;
import android.util.Xml;

import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redcross.openmapkit.ApplicationTest;
import org.redcross.openmapkit.MapActivity;
import org.redcross.openmapkit.MapActivityTest;
import org.redcross.openmapkit.R;
import org.redcross.openmapkit.Settings;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Collection;

/**
 * Created by Jason Rogena - jrogena@ona.io on 7/26/16.
 */
@RunWith(AndroidJUnit4.class)
public class TagSwipeActivityTest {
    private static final String TAG = "TagSwipeActivityTest";
    private static final long UI_STANDARD_WAIT_TIME = 100l;//standard time to wait for a UI update
    private static final long UI_LONG_WAIT_TIME = 2000l;
    private Activity currentActivity;
    /*
    Launch the MapActivity with touch mode set to true (nothing is in focus and nothing is initially
    selected) and launchActivity set to false so that the activity is launched for each test
     */
    @Rule
    public ActivityTestRule<MapActivity> mapActivityTR = new ActivityTestRule<MapActivity>(MapActivity.class, true, false);

    /**
     * This test checks whether the enable GPS provider dialog pops up if the recommended GPS
     * provider is off
     */
    @Test
    @Ignore
    public void testCheckLocationProviderDisabled() {
        Log.i(TAG, "Running test testCheckLocationProviderDisabled");
        startTagSwipeActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                final TagSwipeActivity tagSwipeActivity = (TagSwipeActivity) activity;
                String testProvider = createTestLocationProvider(tagSwipeActivity);
                LocationManager locationManager = tagSwipeActivity.getLocationManager();

                //test to see if dialog is shown when GPS is disabled
                locationManager.setTestProviderEnabled(testProvider, false);

                try {
                    Thread.sleep(UI_LONG_WAIT_TIME);
                    Assert.assertTrue(tagSwipeActivity.isGpsProviderAlertDialogShowing());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Assert.assertTrue(e.getMessage(), false);
                }

                locationManager.removeTestProvider(testProvider);
            }
        });
    }

    /**
     * This test checks whether the enable GPS provider dialog pops up if the recommended GPS
     * provider is on
     */
    @Test
    public void testCheckLocationProviderEnabled() {
        Log.i(TAG, "Running test testCheckLocationProviderEnabled");
        startTagSwipeActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                TagSwipeActivity tagSwipeActivity = (TagSwipeActivity) activity;
                String testProvider = createTestLocationProvider(tagSwipeActivity);
                LocationManager locationManager = tagSwipeActivity.getLocationManager();

                //test to see if dialog is shown when GPS is disabled
                try {
                    Thread.sleep(UI_LONG_WAIT_TIME);
                    Assert.assertFalse(tagSwipeActivity.isGpsProviderAlertDialogShowing());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Assert.assertTrue(e.getMessage(), false);
                }

                locationManager.removeTestProvider(testProvider);
            }
        });
    }

    /**
     * This test checks whether an updated user location updates the user location tags stored in
     * the TagEdit hash map
     */
    @Test
    public void testUpdateUserLocation() {
        Log.i(TAG, "Running test testUpdateUserLocation");
        startTagSwipeActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                final TagSwipeActivity tagSwipeActivity = (TagSwipeActivity) activity;
                String testProvider = createTestLocationProvider(tagSwipeActivity);
                LocationManager locationManager = tagSwipeActivity.getLocationManager();
                try {
                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    Location location = new Location(testProvider);
                    location.setLatitude(-0.3212321d);
                    location.setLongitude(36.324324d);
                    location.setAccuracy(30.0f);
                    location.setTime(System.currentTimeMillis());
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

                    }
                    locationManager.setTestProviderLocation(testProvider, location);

                    Thread.sleep(UI_STANDARD_WAIT_TIME);

                    TagEdit userLocationTag = TagEdit.getTag(Settings.singleton().getUserLatLngName());
                    Assert.assertTrue(TagEdit.locationToString(location).equals(userLocationTag.getTagVal()));

                    TagEdit userLocationAccuracy = TagEdit.getTag(Settings.singleton().getUserAccuracyName());
                    Assert.assertTrue(TagEdit.locationAccuracyToString(location.getAccuracy()).equals(userLocationAccuracy.getTagVal()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Assert.assertTrue(e.getMessage(), false);
                }

                locationManager.removeTestProvider(testProvider);
            }
        });
    }

    /**
     * This test checks whether the 'GPS is still searching' dialog is shown when a user presses the
     * save to ODK button before a location fix is recorded
     */
    @Test
    @Ignore
    public void testShowGpsSearchingDialog() {
        Log.i(TAG, "Running test testShowGpsSearchingDialog");
        startTagSwipeActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                final TagSwipeActivity tagSwipeActivity = (TagSwipeActivity) activity;
                String testProvider = createTestLocationProvider(tagSwipeActivity);
                LocationManager locationManager = tagSwipeActivity.getLocationManager();

                TagEdit.cleanUserLocationTags();
                Espresso.onView(ViewMatchers.withText(R.string.save_to_odk_collect))
                        .perform(ViewActions.click());

                try {
                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    tagSwipeActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tagSwipeActivity.setOsmUsername("test");
                        }
                    });
                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    Assert.assertTrue(tagSwipeActivity.getGpsSearchingProgressDialog() != null);
                    Assert.assertTrue(tagSwipeActivity.getGpsSearchingProgressDialog().isShowing());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Assert.assertTrue(e.getMessage(), false);
                }

                locationManager.removeTestProvider(testProvider);
            }
        });
    }

    /**
     * This test checks whether the user's location is saved in the .osm file after submission
     */
    @Test
    public void testUserLocationTagsInOsmFile() {
        Log.i(TAG, "Running test testUserLocationTagsInOsmFile");
        startTagSwipeActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                final TagSwipeActivity tagSwipeActivity = (TagSwipeActivity) activity;
                String testProvider = createTestLocationProvider(tagSwipeActivity);
                LocationManager locationManager = tagSwipeActivity.getLocationManager();

                final Location location = new Location(testProvider);
                location.setLatitude(-0.3212321d);
                location.setLongitude(36.324324d);
                location.setAccuracy(9f);
                location.setTime(System.currentTimeMillis());
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

                }

                tagSwipeActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TagEdit.updateUserLocationTags(location);
                    }
                });

                try {
                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    Espresso.onView(ViewMatchers.withText(R.string.save_to_odk_collect))
                            .perform(ViewActions.click());

                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    tagSwipeActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tagSwipeActivity.setOsmUsername("test");
                        }
                    });

                    Thread.sleep(UI_LONG_WAIT_TIME);
                    String osmFilePath = tagSwipeActivity.getOsmFilePath();
                    if(osmFilePath != null) {
                        try {
                            String contents = getStringFromFile(osmFilePath);
                            checkLocationTagsInXml(contents, location);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Assert.assertTrue(e.getMessage(), false);
                        }
                    } else {
                        Assert.assertTrue("OSM file path is null", false);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Assert.assertTrue(e.getMessage(), false);
                }

                locationManager.removeTestProvider(testProvider);
            }
        });
    }

    /**
     * This method parses the provided XML string to determine if the provided location is encoded
     * as OSM tags
     *
     * @param xml       The xml string to be checked
     * @param location  Location to be searched in the xml string
     */
    private void checkLocationTagsInXml(String xml, Location location){
        String ns = null;
        try {
            boolean userLocationFound = false;
            boolean userLocationAccuracyFound = false;

            //parse the xml to check if user location tags exist
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new StringReader(xml));
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, ns, "osm");
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, ns, "node");
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if(parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                if(parser.getName().equals("tag")) {
                    if(parser.getAttributeValue(0).trim().equals(Settings.singleton().getUserLatLngName())) {
                        if(parser.getAttributeValue(1).trim().equals(TagEdit.locationToString(location))) {
                            userLocationFound = true;
                        }
                    } else if(parser.getAttributeValue(0).trim().equals(Settings.singleton().getUserAccuracyName())) {
                        if(parser.getAttributeValue(1).trim().equals(TagEdit.locationAccuracyToString(location.getAccuracy()))) {
                            userLocationAccuracyFound = true;
                        }
                    }

                    if(userLocationFound && userLocationAccuracyFound) {
                        break;
                    }
                }
            }

            Assert.assertTrue(userLocationAccuracyFound && userLocationFound);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.assertTrue(e.getMessage(), false);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            Assert.assertTrue(e.getMessage(), false);
        }
    }

    /**
     * This method converts an InputStream into a String
     *
     * @param is    input stream to be converted into a string
     * @return  The acquired string
     * @throws IOException Throws exception if an IO Error occurs
     * @see InputStream
     */
    private static String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    /**
     * This method gets the contents of a file as a string
     *
     * @param filePath  The path to the file
     * @return  The string contents of the file
     * @throws IOException Throws exception if an IO Error occurs
     */
    private static String getStringFromFile (String filePath) throws IOException {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        fin.close();
        return ret;
    }

    private void startTagSwipeActivity(OnPostLaunchActivity onPostLaunchActivity) {
        Intent intent = ApplicationTest.simulateODKLaunch();
        mapActivityTR.launchActivity(intent);
        try {
            Thread.sleep(UI_LONG_WAIT_TIME);
            Activity activity1 = getActivityInstance();
            if(activity1 instanceof MapActivity) {
                final MapActivity mapActivity = (MapActivity) activity1;
                mapActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mapActivity.clickMbtilesPositiveButton();
                        mapActivity.zoomToRecommendedLevel();
                    }
                });

                String testProvider = MapActivityTest.createTestLocationProvider(mapActivity);
                GpsLocationProvider gpsLocationProvider = mapActivity.getGpsLocationProvider();

                //set a location with a good accuracy so that if add node button is clicked, the add
                //node views show
                Location location = new Location(testProvider);
                location.setLatitude(-0.3212321d);
                location.setLongitude(36.324324d);
                location.setAccuracy(9f);//accuracy in settings set to 10
                location.setTime(System.currentTimeMillis());
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                }

                gpsLocationProvider.getLocationManager().setTestProviderLocation(testProvider, location);

                //launch the TagSwipeActivity by trying to add a new node
                Thread.sleep(MapActivityTest.GPS_DIALOG_TIMEOUT);
                Espresso.onView(ViewMatchers.withId(R.id.nodeModeButton)).perform(ViewActions.click());
                Espresso.onView(ViewMatchers.withId(R.id.addNodeBtn)).perform(ViewActions.click());
                Espresso.onView(ViewMatchers.withText("spray_status")).perform(ViewActions.click());

                gpsLocationProvider.getLocationManager().removeTestProvider(testProvider);

                //check if the current activity is TagSwipeActivity
                Activity activity2 = getActivityInstance();
                if(activity2 instanceof TagSwipeActivity) {
                    onPostLaunchActivity.run(activity2);
                } else {
                    Assert.assertTrue("Current context is not TagSwipeActivity", false);
                }

                activity2.finish();
            } else {
                Assert.assertTrue("Current context is not MapActivity", false);
            }

            activity1.finish();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Assert.assertTrue(e.getMessage(), false);
        }
    }

    private Activity getActivityInstance() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                Collection resumedActivities = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED);
                if (resumedActivities.iterator().hasNext()) {
                    currentActivity = (Activity) resumedActivities.iterator().next();
                }
            }
        });
        return currentActivity;
    }

    private interface OnPostLaunchActivity {
        void run(Activity activity);
    }

    /**
     * This method creates a test provider and attaches it to the LocationManager initialized in the
     * provided gpsLocationProvider
     *
     * @param tagSwipeActivity   Activity containing the LocationManager to attach the test provider
     * @return  The name of the test provider created
     *
     * @see android.location.LocationManager
     * @see com.mapbox.mapboxsdk.overlay.GpsLocationProvider
     */
    public static String createTestLocationProvider(final TagSwipeActivity tagSwipeActivity) {
        final String providerName = "test_provider" + String.valueOf(Calendar.getInstance().getTimeInMillis());

        Runnable uiRunnable = new Runnable() {
            @Override
            public void run() {
                LocationManager locationManager = tagSwipeActivity.getLocationManager();
                locationManager.removeUpdates(tagSwipeActivity.getLocationListener());

                if(locationManager.getProvider(providerName) == null) {
                    locationManager.addTestProvider(providerName, true, false, false, false, true, true,
                            true, Criteria.POWER_MEDIUM, Criteria.ACCURACY_FINE);
                    locationManager.setTestProviderEnabled(providerName, true);
                    tagSwipeActivity.setPreferredLocationProvider(providerName);
                    locationManager.requestLocationUpdates(providerName,
                            0,
                            0,
                            tagSwipeActivity.getLocationListener());
                }

                synchronized (this) {
                    this.notify();
                }
            }
        };

        synchronized (uiRunnable) {
            tagSwipeActivity.runOnUiThread(uiRunnable);

            try {
                uiRunnable.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return providerName;
    }
}