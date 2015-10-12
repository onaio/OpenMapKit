package io.ona.openmapkit;

import android.content.Context;
import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.ContextThemeWrapper;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by coder on 8/4/15.
 */
public class MapActivityTest extends ActivityUnitTestCase<MapActivity> {
    MapActivity mapActivity;
    private Context mActivityContext;

    public MapActivityTest() {
        super(MapActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // default value for target context, as a default
        mActivityContext = getInstrumentation().getTargetContext();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test whether map gets the correct setting from proximity_settings.xml file.
     */
    @SmallTest
    public void testMapActivityConfig() {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ContextThemeWrapper contextTheme = new ContextThemeWrapper(mActivityContext, R.style.AppTheme);
                setActivityContext(contextTheme);
                Intent intent = new Intent(getInstrumentation().getTargetContext(), MapActivity.class);
                intent.setAction("android.intent.action.REC");
                mapActivity = startActivity(intent, null, null);
                assertEquals(mapActivity.getProximityRadius(), 100.0);
                assertEquals(mapActivity.getCheckProximity(), true);
            }
        });
    }

    /**
     * The user location should always be enabled in order to draw proximity circle.
     */
    @SmallTest
    public void testUserLocationEnabled() {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ContextThemeWrapper contextTheme = new ContextThemeWrapper(mActivityContext, R.style.AppTheme);
                setActivityContext(contextTheme);
                Intent intent = new Intent(getInstrumentation().getTargetContext(), MapActivity.class);
                intent.setAction("android.intent.action.REC");
                mapActivity = startActivity(intent, null, null);
                MapView mapView = mapActivity.getMapView();
                assertEquals(mapView.getUserLocationEnabled(), true);
            }
        });
    }

    /**
     * Test whether the given distance in meters returns the expected angle.
     */
    @SmallTest
    public void testGetCentralAngleDegreeDistance() {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ContextThemeWrapper contextTheme = new ContextThemeWrapper(mActivityContext, R.style.AppTheme);
                setActivityContext(contextTheme);
                Intent intent = new Intent(getInstrumentation().getTargetContext(), MapActivity.class);
                intent.setAction("android.intent.action.REC");
                mapActivity = startActivity(intent, null, null);
                //Change double value to integer for comparison.
                int dist = (int) (mapActivity.getCentralAngleDegreeDistance(200) * 100000);
                assertEquals(179, dist);
                dist = (int) (mapActivity.getCentralAngleDegreeDistance(50) * 1000000);
                assertEquals(449, dist);
            }
        });
    }

    /**
     * Test whether 2 places are within the proximity radius.
     */
    @SmallTest
    public void testIsWithinDistance() {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                MapActivity mapActivity = mock(MapActivity.class);

                GeometryFactory geometryFactory = new GeometryFactory();
                double userLat = 1.1;
                double userLong = 1.2;
                LatLng userPos = new LatLng(userLat, userLong);
                when(mapActivity.getUserLocation()).thenReturn(userPos);
                when(mapActivity.getProximityRadius()).thenReturn(100.0);
                when(mapActivity.getCentralAngleDegreeDistance(any(Double.class))).thenCallRealMethod();
                when(mapActivity.isWithinDistance(any(Geometry.class))).thenCallRealMethod();

                //Structure is at same location as user.
                double structureLat = 1.1;
                double structureLong = 1.2;
                Coordinate cord = new Coordinate(structureLong, structureLat);
                Geometry tappedStructure = geometryFactory.createPoint(cord);
                assertTrue(mapActivity.isWithinDistance(tappedStructure));

                //Structure is within 100m of the user location.
                structureLat = 1.1;
                structureLong = 1.2005;
                cord = new Coordinate(structureLong, structureLat);
                tappedStructure = geometryFactory.createPoint(cord);
                assertTrue(mapActivity.isWithinDistance(tappedStructure));

                //Structure is more than 100m of the user location.
                structureLat = 1.1;
                structureLong = 1.5;
                cord = new Coordinate(structureLong, structureLat);
                tappedStructure = geometryFactory.createPoint(cord);
                assertFalse(mapActivity.isWithinDistance(tappedStructure));
            }
        });
    }
}