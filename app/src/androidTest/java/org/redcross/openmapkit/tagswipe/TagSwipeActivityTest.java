package org.redcross.openmapkit.tagswipe;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.util.Log;
import android.util.Xml;

import junit.framework.Assert;

import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redcross.openmapkit.MapActivity;
import org.redcross.openmapkit.R;
import org.redcross.openmapkit.odkcollect.ODKCollectData;
import org.redcross.openmapkit.odkcollect.ODKCollectHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.regex.Matcher;

/**
 * Created by Jason Rogena - jrogena@ona.io on 7/26/16.
 */
@RunWith(AndroidJUnit4.class)
public class TagSwipeActivityTest {
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
        startTagSwipeActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                TagSwipeActivity tagSwipeActivity = (TagSwipeActivity) activity;

                //test to see if dialog is shown when GPS is disabled
                tagSwipeActivity.changeTestProviderEnabled(false);
                try {
                    Thread.sleep(UI_LONG_WAIT_TIME);
                    Assert.assertTrue(tagSwipeActivity.isGpsProviderAlertDialogShowing());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Assert.assertTrue(e.getMessage(), false);
                }
            }
        });
    }

    /**
     * This test checks whether the enable GPS provider dialog pops up if the recommended GPS
     * provider is on
     */
    @Test
    @Ignore
    public void testCheckLocationProviderEnabled() {
        startTagSwipeActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                TagSwipeActivity tagSwipeActivity = (TagSwipeActivity) activity;

                //test to see if dialog is shown when GPS is disabled
                tagSwipeActivity.changeTestProviderEnabled(true);
                try {
                    Thread.sleep(UI_LONG_WAIT_TIME);
                    Assert.assertFalse(tagSwipeActivity.isGpsProviderAlertDialogShowing());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Assert.assertTrue(e.getMessage(), false);
                }
            }
        });
    }

    /**
     * This test checks whether an updated user location updates the user location tags stored in
     * the TagEdit hash map
     */
    @Test
    @Ignore
    public void testUpdateUserLocation() {
        startTagSwipeActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                final TagSwipeActivity tagSwipeActivity = (TagSwipeActivity) activity;
                try {
                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    Location location = new Location("test_location");
                    location.setLatitude(-0.3212321d);
                    location.setLongitude(36.324324d);
                    location.setAccuracy(30.0f);
                    location.setTime(System.currentTimeMillis());
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

                    }
                    tagSwipeActivity.changeTestProviderLocation(location);

                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    tagSwipeActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tagSwipeActivity.updateUsersLocation();//locationManager still has a null last location
                        }
                    });

                    TagEdit userLocationTag = TagEdit.getTag(TagEdit.TAG_KEY_USER_LOCATION);
                    Assert.assertTrue(TagEdit.locationToString(location).equals(userLocationTag.getTagVal()));

                    TagEdit userLocationAccuracy = TagEdit.getTag(TagEdit.TAG_KEY_USER_LOCATION_ACCURACY);
                    Assert.assertTrue(TagEdit.locationAccuracyToString(location.getAccuracy()).equals(userLocationAccuracy.getTagVal()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Assert.assertTrue(e.getMessage(), false);
                }
            }
        });
    }

    /**
     * This test checks whether the 'GPS is still searching' dialog is shown when a user presses the
     * save to ODK button before a location fix is recorded
     */
    @Test
    public void testShowGpsSearchingDialog() {
        startTagSwipeActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                final TagSwipeActivity tagSwipeActivity = (TagSwipeActivity) activity;
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
                    Espresso.onView(ViewMatchers.withText(R.string.waiting_for_user_location))
                            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Assert.assertTrue(e.getMessage(), false);
                }
            }
        });
    }

    /**
     * This test checks whether the user's location is saved in the .osm file after submission
     */
    @Test
    public void testUserLocationTagsInOsmFile() {
        startTagSwipeActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                final Location location = new Location("test_location");
                location.setLatitude(-0.3212321d);
                location.setLongitude(36.324324d);
                location.setAccuracy(30.0f);
                location.setTime(System.currentTimeMillis());
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

                }

                final TagSwipeActivity tagSwipeActivity = (TagSwipeActivity) activity;
                tagSwipeActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TagEdit.updateUserLocationTags(location);
                    }
                });

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
                    if(parser.getAttributeValue(0).trim().equals(TagEdit.TAG_KEY_USER_LOCATION)) {
                        if(parser.getAttributeValue(1).trim().equals(TagEdit.locationToString(location))) {
                            userLocationFound = true;
                        }
                    } else if(parser.getAttributeValue(0).trim().equals(TagEdit.TAG_KEY_USER_LOCATION_ACCURACY)) {
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
        Intent intent = getLaunchOMKIntent();
        mapActivityTR.launchActivity(intent);
        try {
            Thread.sleep(UI_LONG_WAIT_TIME);
            Activity activity1 = getActivityInstance();
            if(activity1 instanceof MapActivity) {
                MapActivity mapActivity = (MapActivity) activity1;
                mapActivity.clickMbtilesPositiveButton();

                //launch the TagSwipeActivity by trying to add a new node
                Thread.sleep(UI_STANDARD_WAIT_TIME);
                Espresso.onView(ViewMatchers.withId(R.id.nodeModeButton)).perform(ViewActions.click());
                Espresso.onView(ViewMatchers.withId(R.id.addNodeBtn)).perform(ViewActions.click());
                Espresso.onView(ViewMatchers.withText("Spray Status")).perform(ViewActions.click());

                //check if the current activity is TagSwipeActivity
                Activity activity2 = getActivityInstance();
                if(activity2 instanceof TagSwipeActivity) {
                    onPostLaunchActivity.run(activity2);
                } else {
                    Assert.assertTrue("Current context is not TagSwipeActivity", false);
                }
            } else {
                Assert.assertTrue("Current context is not MapActivity", false);
            }
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

    /**
     * This method creates an intent similar to the one used to launch OpenMapKit from OpenDataKit
     *
     * @return  Intent similar to the one used to launch OpenMapKit from OpenDataKit
     */
    private Intent getLaunchOMKIntent() {
        File odkInstanceDir = new File("/storage/emulated/0/odk/instances/omk_functional_test");
        odkInstanceDir.mkdirs();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra("FORM_FILE_NAME", "omk_functional_test");
        intent.putExtra("FORM_ID", "-1");
        intent.putExtra("INSTANCE_ID", "uuid:6004201f-9942-429d-bfa4-e65b683da37b");
        intent.putExtra("INSTANCE_DIR", "/storage/emulated/0/odk/instances/omk_functional_test");
        intent.putExtra("OSM_EDIT_FILE_NAME", "omk_functional_test.osm");
        ArrayList<String> tagKeys = new ArrayList<>();
        tagKeys.add("spray_status");
        intent.putExtra("TAG_KEYS", tagKeys);
        intent.putExtra("TAG_LABEL.spray_status", "Spray Status");
        intent.putExtra("TAG_VALUES.spray_status", "null");
        intent.putExtra("TAG_VALUE_LABEL.spray_status.undefined", "Undefined");
        intent.putExtra("TAG_VALUE_LABEL.spray_status.yes", "Yes");
        intent.putExtra("TAG_VALUE_LABEL.spray_status.no", "No");
        intent.putExtra(MapActivity.BUNDLE_KEY_IS_TESTING, true);

        return intent;
    }

    private interface OnPostLaunchActivity {
        void run(Activity activity);
    }
}