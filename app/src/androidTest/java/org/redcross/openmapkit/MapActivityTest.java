package org.redcross.openmapkit;

import android.content.Intent;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityUnitTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by coder on 8/4/15.
 */
public class MapActivityTest extends ActivityUnitTestCase<MapActivity> {
    MapActivity mapActivity;

    public MapActivityTest() {
        super(MapActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Starts the MapActivity of the target application
        startActivity(new Intent(getInstrumentation().getTargetContext(), MapActivity.class), null, null);

        // Getting a reference to the MapActivity of the target application
        mapActivity = getActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @SmallTest
    public void testMapActivityConfig() {
        assertEquals(mapActivity.getProximityRadius(), 200);
        assertEquals(mapActivity.getCheckProximity(), true);
    }

    @SmallTest
    public void testUserLocationEnabled() {
        MapView mapView  = mapActivity.getMapView();
        assertEquals(mapView.getUserLocationEnabled(), true);
        GpsLocationProvider gpsLocationProvider = mock(GpsLocationProvider.class);
        UserLocationOverlay userLocationOverlay = new UserLocationOverlay(gpsLocationProvider, mapView);
        assertTrue(userLocationOverlay.isMyLocationEnabled());
    }

    /**
     * Test whether the given distance in meters returns the expected angle.
     */
    @SmallTest
    public void testGetCentralAngleDegreeDistance() {
        System.out.println("dist "+mapActivity.getCentralAngleDegreeDistance(20000));
        System.out.println("dist "+mapActivity.getCentralAngleDegreeDistance(2000));
        System.out.println("dist "+mapActivity.getCentralAngleDegreeDistance(990000));
        assertEquals(mapActivity.getCentralAngleDegreeDistance(200), 0.00179);
        assertEquals(mapActivity.getCentralAngleDegreeDistance(50), 0.000449);
    }

    @SmallTest
    public void testIsWithinDistance() {
        GeometryFactory geometryFactory = new GeometryFactory();
        double userLat = 1.1;
        double userLong = 1.2;
        LatLng userPos = new LatLng(userLat, userLong);
        when(mapActivity.getUserLocation()).thenReturn(userPos);

        //Structure is at same location as user.
        double structureLat = 1.1;
        double structureLong = 1.2;
        Coordinate cord = new Coordinate(structureLat, structureLong);
        Geometry tappedStructure = geometryFactory.createPoint(cord);
        assertTrue(mapActivity.isWithinDistance(tappedStructure));

        //Structure is within 50m of the user location.
        structureLat = 1.1;
        structureLong = 1.2123;
        cord = new Coordinate(structureLat, structureLong);
        tappedStructure = geometryFactory.createPoint(cord);
        when(mapActivity.getUserLocation()).thenReturn(userPos);
        assertTrue(mapActivity.isWithinDistance(tappedStructure));

        //Structure is 300m of the user location.
        structureLat = 1.2;
        structureLong = 1.5;
        cord = new Coordinate(structureLat, structureLong);
        tappedStructure = geometryFactory.createPoint(cord);
        when(mapActivity.getUserLocation()).thenReturn(userPos);
        assertFalse(mapActivity.isWithinDistance(tappedStructure));
    }
}
