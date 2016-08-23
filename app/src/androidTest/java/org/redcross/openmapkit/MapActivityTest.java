package org.redcross.openmapkit;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.location.Criteria;
import android.location.LocationManager;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.PositionAssertions;
import android.app.Activity;
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
import android.view.Menu;
import android.widget.Button;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.spatialdev.osm.model.OSMColorConfig;
import com.spatialdev.osm.model.OSMElement;
import com.spatialdev.osm.model.OSMNode;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import org.hamcrest.CoreMatchers;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redcross.openmapkit.odkcollect.FormOSMDownloader;

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
    private static final long UI_LONG_WAIT_TIME = 500l;//standard time to wait for a UI update
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
        ApplicationTest.simulateODKLaunch();
    }

    /**
     * This method tests the position of the 'locate-me' button in the activity
     */
    @Test
    public void locationButtonPosition() {
        Log.i(TAG, "Running test locationButtonPosition");
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
        Log.i(TAG, "Running test roundedButtonLabel");
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
        Log.i(TAG, "Running test testLoadingGpsDialogShown");
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
        Log.i(TAG, "Running test testLoadingGpsDialogNotShowingGpsAccurate");
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
        Log.i(TAG, "Running test testLoadingGpsDialogNotShowingAfterTimeout");
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
        Log.i(TAG, "Running test testAddNode");
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
                    Thread.sleep(GPS_DIALOG_TIMEOUT);

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
        Log.i(TAG, "Running test testAddNode_InaccurateLocation");
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
        Log.i(TAG, "Running test testMapInteraction_AddingNewNode");
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
                    Thread.sleep(GPS_DIALOG_TIMEOUT);

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
        Log.i(TAG, "Running test moveNode_AddingNewNode");
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
                    Thread.sleep(GPS_DIALOG_TIMEOUT);

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

    /**
     * This method tests whether the proximity_radius preference in the proximity_settings works
     */
    @Test
    public void testIsWithinUserProximity() {
        Log.i(TAG, "Running test testIsWithinUserProximity");
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
                    Thread.sleep(GPS_DIALOG_TIMEOUT);
                    GeometryFactory geometryFactory = new GeometryFactory();

                    //test close-by node
                    LatLng latLng1 = new LatLng(-0.3212321, 36.324224);
                    OSMElement osmElement1 = new OSMNode(latLng1, OSMColorConfig.getDefaultConfig());
                    Coordinate coord1 = new Coordinate(latLng1.getLongitude(), latLng1.getLatitude());
                    Point point1 = geometryFactory.createPoint(coord1);
                    osmElement1.setJTSGeom(point1);
                    assertTrue(mapActivity.isWithinUserProximity(osmElement1));

                    //test far way node
                    LatLng latLng2 = new LatLng(-0.3212321, 36.323224);
                    OSMElement osmElement2 = new OSMNode(latLng2, OSMColorConfig.getDefaultConfig());//should be further than 100m from the provider location
                    Coordinate coord2 = new Coordinate(latLng2.getLongitude(), latLng2.getLatitude());
                    Point point2 = geometryFactory.createPoint(coord2);
                    osmElement2.setJTSGeom(point2);
                    assertFalse(mapActivity.isWithinUserProximity(osmElement2));

                    gpsLocationProvider.getLocationManager().removeTestProvider(testProvider);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * This method tests whether tags with the 'hide' constraints in the testing constraints json
     * file (androidTest/constraints/omk_functional_test.json) are shown
     */
    @Test
    @Ignore//test already being done in ConstraintsTest
    public void testHideTagConstraint() {
        Log.i(TAG, "Running test testHideTagConstraint");
        startMapActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                MapActivity mapActivity = (MapActivity) activity;
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
                    Thread.sleep(GPS_DIALOG_TIMEOUT);
                    Espresso.onView(ViewMatchers.withId(R.id.nodeModeButton)).perform(ViewActions.click());
                    Espresso.onView(ViewMatchers.withId(R.id.addNodeBtn)).perform(ViewActions.click());

                    //check which tags are shown
                    Espresso.onView(ViewMatchers.withText("hidden_tag_1"))
                            .check(ViewAssertions.doesNotExist());
                    Espresso.onView(ViewMatchers.withText("hidden_tag_2"))
                            .check(ViewAssertions.doesNotExist());
                    Espresso.onView(ViewMatchers.withText("hidden_tag_3"))
                            .check(ViewAssertions.doesNotExist());

                    Espresso.onView(ViewMatchers.withText("shown_tag_1"))
                            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
                    Espresso.onView(ViewMatchers.withText("shown_tag_2"))
                            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
                    Espresso.onView(ViewMatchers.withText("shown_tag_3"))
                            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                gpsLocationProvider.getLocationManager().removeTestProvider(testProvider);
            }
        });
    }

    /**
     * This method tests whether the UI logic for deleting a newly added node works
     */
    @Test
    public void testDeleteNode() {
        Log.i(TAG, "Running test testDeleteNode");
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
                    Thread.sleep(GPS_DIALOG_TIMEOUT);

                    //launch the noe details by clicking on add node/structure
                    Espresso.onView(ViewMatchers.withId(R.id.nodeModeButton))
                            .perform(ViewActions.click());
                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    Espresso.onView(ViewMatchers.withId(R.id.addNodeBtn))
                            .perform(ViewActions.click());

                    //check if the delete node button exists
                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    Espresso.onView(ViewMatchers.withId(R.id.deleteButton))
                            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

                    //click on the delete button and check if confirmation dialog shows
                    Espresso.onView(ViewMatchers.withId(R.id.deleteButton))
                            .perform(ViewActions.click());

                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    assertTrue(mapActivity.getDeleteNodeDialog() != null);
                    assertTrue(mapActivity.getDeleteNodeDialog().isShowing());

                    //click on the no button and check if dialog disappears but node still in focus
                    mapActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mapActivity.getDeleteNodeDialog()
                                    .getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
                        }
                    });

                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    assertFalse(mapActivity.getDeleteNodeDialog().isShowing());
                    Espresso.onView(ViewMatchers.withId(R.id.tagListView))
                            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));


                    //click on the delete button again
                    Espresso.onView(ViewMatchers.withId(R.id.deleteButton))
                            .perform(ViewActions.click());

                    //click on the yes button and see if node disappears
                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    mapActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mapActivity.getDeleteNodeDialog()
                                    .getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                        }
                    });

                    Thread.sleep(UI_STANDARD_WAIT_TIME);
                    Espresso.onView(ViewMatchers.withId(R.id.tagListView))
                            .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isDisplayed())));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                gpsLocationProvider.getLocationManager().removeTestProvider(testProvider);
            }
        });
    }

    /**
     * This method checks whether the menu items configured to be hidden in assets/settings/omk_functional_test.json
     * are actually hidden
     */
    @Test
    public void testHiddenMenuItems() {
        Log.i(TAG, "Running test testHiddenMenuItems");
        startMapActivity(new OnPostLaunchActivity() {
            @Override
            public void run(Activity activity) {
                final MapActivity mapActivity = (MapActivity) activity;
                try {
                    Thread.sleep(GPS_DIALOG_TIMEOUT);
                    Menu menu = mapActivity.getMenu();
                    for(int currMenuItemId : MapActivity.MENU_ITEM_IDS) {
                        assertEquals(Settings.singleton().getHiddenMenuItems().contains(menu.findItem(currMenuItemId).getTitle().toString().toLowerCase()), !menu.findItem(currMenuItemId).isVisible());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * This method tests whether the OSM Form ODK query dialog is shown when the OSM From ODK menu
     * item is clicked.
     */
    @Test
    public void testOSMFromODK_QueryDialogShowing() {
        Log.i(TAG, "Running test testOSMFromODK_QueryDialogShowing");
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
                    Thread.sleep(GPS_DIALOG_TIMEOUT);

                    mapActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mapActivity.showOdkQueryDialog();
                        }
                    });

                    Thread.sleep(UI_LONG_WAIT_TIME);

                    //check if the odkQueryDialog is showing
                    final Dialog queryDialog = mapActivity.getOdkQuDialog();
                    assertTrue(queryDialog != null);
                    assertTrue(queryDialog.isShowing());

                    Settings.singleton().setOSMFromODKUsername(Settings.DEFAULT_OSM_FROM_ODK_USERNAME);
                    Settings.singleton().setOSMFromODKPassword(Settings.DEFAULT_OSM_FROM_ODK_PASSWORD);
                    Settings.singleton().setOSMFromODKQuery(Settings.DEFAULT_OSM_FROM_ODK_QUERY);
                    Settings.singleton().setOSMFromODKServer(Settings.DEFAULT_OSM_FROM_ODK_SERVER);
                    JSONArray formArray = new JSONArray();
                    formArray.put(79639);
                    formArray.put(80159);
                    Settings.singleton().setOSMFromODKForms(formArray);

                    //clear all ongoing downloads
                    FormOSMDownloader.clearOngoingDownloads(mapActivity.getApplicationContext());
                    assertEquals(FormOSMDownloader.getOngoingDownloadIds().size(), 0);

                    //press the ok button and check if the downloads have started
                    mapActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Button okButton = (Button) queryDialog.findViewById(R.id.okB);
                            okButton.performClick();
                        }
                    });

                    Thread.sleep(UI_LONG_WAIT_TIME);

                    ProgressDialog progressDialog = mapActivity.getOSMFromODKProgressDialog();

                    assertTrue(progressDialog != null);
                    assertTrue(progressDialog.isShowing());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                gpsLocationProvider.getLocationManager().removeTestProvider(testProvider);
            }
        });
    }

    private void startMapActivity(OnPostLaunchActivity onPostLaunchActivity) {
        Intent intent = ApplicationTest.simulateODKLaunch();
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
        if (gpsLocationProvider != null) {
            final String providerName = "test_provider" + String.valueOf(Calendar.getInstance().getTimeInMillis());
            Runnable uiRunnable = new Runnable() {
                @Override
                public void run() {
                    LocationManager locationManager = gpsLocationProvider.getLocationManager();
                    if(locationManager.getProvider(providerName) == null) {
                        locationManager.removeUpdates(gpsLocationProvider);

                        locationManager.addTestProvider(providerName, true, false, false, false, true, true,
                                true, Criteria.POWER_MEDIUM, Criteria.ACCURACY_FINE);
                        locationManager.setTestProviderEnabled(providerName, true);
                        locationManager.requestLocationUpdates(providerName,
                                gpsLocationProvider.getLocationUpdateMinTime(),
                                gpsLocationProvider.getLocationUpdateMinDistance(),
                                gpsLocationProvider);
                    }

                    //notify all other threads that are waiting that we're done
                    synchronized (this) {
                        this.notify();
                    }
                }
            };

            //run code that needs to be run on the UI thread but wait for that to finish
            synchronized (uiRunnable) {
                mapActivity.runOnUiThread(uiRunnable);

                try {
                    uiRunnable.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return providerName;
        }
        return null;
    }
}
