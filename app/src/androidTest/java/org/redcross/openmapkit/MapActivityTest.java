package org.redcross.openmapkit;

import android.location.Criteria;
import android.location.LocationManager;
import android.os.Environment;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.PositionAssertions;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.util.Log;
import android.widget.Button;

import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Created by Jason Rogena - jrogena@ona.io on 7/27/16.
 */
@RunWith(AndroidJUnit4.class)
public class MapActivityTest {
    private static final String TAG = "MapActivityTest";
    private static final String ROUNDED_BUTTON_LABEL = "Add Structure";
    public static final long GPS_DIALOG_TIMEOUT = 10100l;
    private static final long UI_STANDARD_WAIT_TIME = 100l;//standard time to wait for a UI update
    private Activity currentActivity;
    /*
    Launch the MapActivity with touch mode set to true (nothing is in focus and nothing is initially
    selected) and launchActivity set to false so that the activity is launched for each test
     */
    @Rule
    public ActivityTestRule<MapActivity> mapActivityTR = new ActivityTestRule<MapActivity>(MapActivity.class, true, false);

    @Before
    public void setup() {
        //get instrumented (not device) context so as to fetch files from androidTest/assets
        Context context = InstrumentationRegistry.getContext();
        copySettingsDir(context);
    }

    private void copySettingsDir(Context context) {
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
     * This method tests the position of the 'locate-me' button in the activity
     */
    @Test
    public void locationButtonPosition() {
        startMapActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                //check initial position
                try {
                    Thread.sleep(GPS_DIALOG_TIMEOUT);
                    Espresso.onView(ViewMatchers.withId(R.id.locationButton))
                            .check(PositionAssertions
                                    .isAbove(ViewMatchers.withId(R.id.nodeModeButton)));
                    Espresso.onView(ViewMatchers.withId(R.id.locationButton))
                            .check(PositionAssertions
                                    .isRightAlignedWith(ViewMatchers.withId(R.id.nodeModeButton)));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * This method tests whether the text on the 'add node' button corresponds to ROUNDED_BUTTON_LABEL
     */
    @Test
    public void roundedButtonLabel() {
        startMapActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                MapActivity mapActivity = (MapActivity) activity;
                GpsLocationProvider gpsLocationProvider = mapActivity.getGpsLocationProvider();
                String testProvider = createTestLocationProvider(mapActivity);

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

                //show the add structure marker by clicking the '+' button
                try {
                    Thread.sleep(GPS_DIALOG_TIMEOUT);
                    Espresso.onView(ViewMatchers.withId(R.id.nodeModeButton)).perform(ViewActions.click());
                    Button addNodeButton = (Button) mapActivity.findViewById(R.id.addNodeBtn);
                    assertTrue(addNodeButton.getText().toString().contains(ROUNDED_BUTTON_LABEL));

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                gpsLocationProvider.getLocationManager().removeTestProvider(testProvider);
            }
        });
    }

    /**
     * This method tests whether the GPS countdown dialog is shown when the MapActivity is first
     * started and a GPS fix is not gotten
     */
    @Test
    @Ignore//passes on physical devices but fails in emulators
    public void testLoadingGpsDialogShown() {
        startMapActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                MapActivity mapActivity = (MapActivity) activity;
                GpsLocationProvider gpsLocationProvider = mapActivity.getGpsLocationProvider();
                String testProvider = createTestLocationProvider(mapActivity);

                Location location = new Location(testProvider);
                location.setLatitude(-0.3212321d);
                location.setLongitude(36.324324d);
                location.setAccuracy(30.0f);//accuracy in settings set to 10
                location.setTime(System.currentTimeMillis());
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                }

                gpsLocationProvider.getLocationManager().setTestProviderLocation(testProvider, location);
                Espresso.onView(ViewMatchers.withText(R.string.getting_gps_fix))
                        .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

                gpsLocationProvider.getLocationManager().removeTestProvider(testProvider);
            }
        });
    }

    /**
     * This method tests whether the GPS countdown dialog stops showing after a location with an
     * accuracy below the gps_proximity_accuracy is obtained
     */
    @Test
    public void testLoadingGpsDialogNotShownGpsAccurate() {
        startMapActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                final MapActivity mapActivity = (MapActivity) activity;
                GpsLocationProvider gpsLocationProvider = mapActivity.getGpsLocationProvider();
                String testProvider = createTestLocationProvider(mapActivity);

                Location location = new Location(testProvider);
                location.setLatitude(-0.3212321d);
                location.setLongitude(36.324324d);
                location.setAccuracy(9f);//accuracy in settings set to 10
                location.setTime(System.currentTimeMillis());
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                }
                gpsLocationProvider.getLocationManager().setTestProviderLocation(testProvider, location);

                try {
                    Thread.sleep(GPS_DIALOG_TIMEOUT + UI_STANDARD_WAIT_TIME);
                    Espresso.onView(ViewMatchers.withText(R.string.getting_gps_fix))
                            .check(ViewAssertions.doesNotExist());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                gpsLocationProvider.getLocationManager().removeTestProvider(testProvider);
            }
        });
    }

    /**
     * This method tests whether the GPS countdown dialog stops showing the timeout expires
     */
    @Test
    public void testLoadingGpsDialogNotShownAfterTimeout() {
        startMapActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                final MapActivity mapActivity = (MapActivity) activity;
                GpsLocationProvider gpsLocationProvider = mapActivity.getGpsLocationProvider();
                String testProvider = createTestLocationProvider(mapActivity);

                Location location = new Location(testProvider);
                location.setLatitude(-0.3212321d);
                location.setLongitude(36.324324d);
                location.setAccuracy(20f);//accuracy in settings set to 10
                location.setTime(System.currentTimeMillis());
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                }
                gpsLocationProvider.getLocationManager().setTestProviderLocation(testProvider, location);

                try {
                    Thread.sleep(GPS_DIALOG_TIMEOUT);//wait for 10s timeout to expire
                    Espresso.onView(ViewMatchers.withText(R.string.getting_gps_fix))
                            .check(ViewAssertions.doesNotExist());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                gpsLocationProvider.getLocationManager().removeTestProvider(testProvider);
            }
        });
    }

    /**
     * This method test whether a user is allowed to add a new location with an accurate location
     * and proximity_settings enabled
     */
    @Test
    public void testAddNode() {
        startMapActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                final MapActivity mapActivity = (MapActivity) activity;
                GpsLocationProvider gpsLocationProvider = mapActivity.getGpsLocationProvider();
                String testProvider = createTestLocationProvider(mapActivity);

                //set current location with an accuracy that is larger than the one set in
                //proximity_settings
                Location location = new Location(testProvider);
                location.setLatitude(-0.3212321d);
                location.setLongitude(36.324324d);
                location.setAccuracy(9f);//accuracy in settings set to 10
                location.setTime(System.currentTimeMillis());
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                }
                gpsLocationProvider.getLocationManager().setTestProviderLocation(testProvider, location);

                try {
                    //wait until after the next time the GPS dialog timer runs
                    Thread.sleep(MapActivity.TASK_INTERVAL_IN_MILLIS + UI_STANDARD_WAIT_TIME);

                    //click the '+' button and check whether views responsible for adding a new
                    //node show (shouldn't be the case)
                    Espresso.onView(ViewMatchers.withId(R.id.nodeModeButton)).perform(ViewActions.click());
                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    Espresso.onView(ViewMatchers.withId(R.id.addNodeBtn))
                            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                gpsLocationProvider.getLocationManager().removeTestProvider(testProvider);
            }
        });
    }

    /**
     * This method test whether a user is allowed to add a new location with an inaccurate location
     * and proximity_settings enabled (shouldn't be the case)
     */
    @Test
    public void testAddNode_InaccurateLocation() {
        startMapActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                final MapActivity mapActivity = (MapActivity) activity;
                GpsLocationProvider gpsLocationProvider = mapActivity.getGpsLocationProvider();
                String testProvider = createTestLocationProvider(mapActivity);

                //set current location with an accuracy that is larger than the one set in
                //proximity_settings
                Location location = new Location(testProvider);
                location.setLatitude(-0.3212321d);
                location.setLongitude(36.324324d);
                location.setAccuracy(20f);//accuracy in settings set to 10
                location.setTime(System.currentTimeMillis());
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                }
                gpsLocationProvider.getLocationManager().setTestProviderLocation(testProvider, location);

                try {
                    //wait for the searching gps dialog times out
                    Thread.sleep(GPS_DIALOG_TIMEOUT);

                    //click the '+' button and check whether views responsible for adding a new
                    //node show (shouldn't be the case)
                    Espresso.onView(ViewMatchers.withId(R.id.nodeModeButton)).perform(ViewActions.click());
                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    Espresso.onView(ViewMatchers.withId(R.id.addNodeBtn))
                            .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isDisplayed())));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                gpsLocationProvider.getLocationManager().removeTestProvider(testProvider);
            }
        });
    }

    /**
     * This method tests whether a user is able to interact with the map (pan around or zoom in and
     * out) when proximity_settings are enabled and they press the '+' button
     */
    @Test
    public void testMapInteraction_AddingNewNode() {
        startMapActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                final MapActivity mapActivity = (MapActivity) activity;
                GpsLocationProvider gpsLocationProvider = mapActivity.getGpsLocationProvider();
                String testProvider = createTestLocationProvider(mapActivity);

                //set current location with an accuracy that is larger than the one set in
                //proximity_settings
                Location location = new Location(testProvider);
                location.setLatitude(-0.3212321d);
                location.setLongitude(36.324324d);
                location.setAccuracy(9f);//accuracy in settings set to 10
                location.setTime(System.currentTimeMillis());
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                }
                gpsLocationProvider.getLocationManager().setTestProviderLocation(testProvider, location);

                try {
                    //wait until after the next time the GPS dialog timer runs
                    Thread.sleep(MapActivity.TASK_INTERVAL_IN_MILLIS + UI_STANDARD_WAIT_TIME);

                    //click the '+' button and check whether map interactions are enabled (shouldn't be)
                    Espresso.onView(ViewMatchers.withId(R.id.nodeModeButton)).perform(ViewActions.click());
                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    assertFalse(mapActivity.isMapInteractionEnabled());

                    //click the add node button and check whether map interactions re-enabled
                    Espresso.onView(ViewMatchers.withId(R.id.addNodeBtn)).perform(ViewActions.click());
                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    assertTrue(mapActivity.isMapInteractionEnabled());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                gpsLocationProvider.getLocationManager().removeTestProvider(testProvider);
            }
        });
    }

    /**
     * This method tests whether a user is able to move the node when adding a new node and
     * proximity_settings is turned on (shouldn't be able to)
     */
    @Test
    public void moveNode_AddingNewNode() {
        startMapActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                final MapActivity mapActivity = (MapActivity) activity;
                GpsLocationProvider gpsLocationProvider = mapActivity.getGpsLocationProvider();
                String testProvider = createTestLocationProvider(mapActivity);

                //set current location with an accuracy that is larger than the one set in
                //proximity_settings
                Location location = new Location(testProvider);
                location.setLatitude(-0.3212321d);
                location.setLongitude(36.324324d);
                location.setAccuracy(9f);//accuracy in settings set to 10
                location.setTime(System.currentTimeMillis());
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                }
                gpsLocationProvider.getLocationManager().setTestProviderLocation(testProvider, location);

                try {
                    //wait until after the next time the GPS dialog timer runs
                    Thread.sleep(MapActivity.TASK_INTERVAL_IN_MILLIS + UI_STANDARD_WAIT_TIME);

                    //click the '+' button and then add node button
                    Espresso.onView(ViewMatchers.withId(R.id.nodeModeButton)).perform(ViewActions.click());
                    Espresso.onView(ViewMatchers.withId(R.id.addNodeBtn)).perform(ViewActions.click());
                    Thread.sleep(UI_STANDARD_WAIT_TIME);

                    //click the move node button and check if moveNodeBtn is made visible (shouldn't be)
                    Espresso.onView(ViewMatchers.withId(R.id.moveNodeModeBtn)).perform(ViewActions.click());
                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    Espresso.onView(ViewMatchers.withId(R.id.moveNodeBtn))
                            .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isDisplayed())));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                gpsLocationProvider.getLocationManager().removeTestProvider(testProvider);
            }
        });
    }

    private void startMapActivity(OnPostLaunchActivity onPostLaunchActivity) {
        Intent intent = ApplicationTest.getLaunchOMKIntent();
        mapActivityTR.launchActivity(intent);
        Activity activity = getActivityInstance();
        if(activity instanceof MapActivity) {
            final MapActivity mapActivity = (MapActivity) activity;
            mapActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapActivity.clickMbtilesPositiveButton();
                    mapActivity.zoomToRecommendedLevel();
                }
            });
            onPostLaunchActivity.run(activity);
        } else {
            assertTrue("Current activity is not the MapActivity", false);
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
     * @param mapActivity   Activity containing the LocationManager to attach the test provider
     * @return  The name of the test provider created
     *
     * @see android.location.LocationManager
     * @see com.mapbox.mapboxsdk.overlay.GpsLocationProvider
     */
    public static String createTestLocationProvider(MapActivity mapActivity) {
        final GpsLocationProvider gpsLocationProvider = mapActivity.getGpsLocationProvider();
        if(gpsLocationProvider != null) {
            final LocationManager locationManager = gpsLocationProvider.getLocationManager();
            locationManager.removeUpdates(gpsLocationProvider);
            final String providerName = "test_provider" + String.valueOf(Calendar.getInstance().getTimeInMillis());
            if(locationManager.getProvider(providerName) == null) {//provider has not yet been created
                locationManager.addTestProvider(providerName, true, false, false, false, true, true,
                        true, Criteria.POWER_MEDIUM, Criteria.ACCURACY_FINE);
                locationManager.setTestProviderEnabled(providerName, true);
                mapActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        locationManager.requestLocationUpdates(providerName,
                                gpsLocationProvider.getLocationUpdateMinTime(),
                                gpsLocationProvider.getLocationUpdateMinDistance(),
                                gpsLocationProvider);
                    }
                });
            }
            return providerName;
        }
        return null;
    }
}
