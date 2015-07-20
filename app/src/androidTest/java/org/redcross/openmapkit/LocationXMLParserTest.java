package org.redcross.openmapkit;

import android.os.Bundle;
import android.test.ActivityTestCase;

import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Created by coder on 7/20/15.
 */
public class LocationXMLParserTest extends ActivityTestCase {
    @Test
    public static void testMapActivityConfig() {
        MapActivity mapActivity;
        mapActivity = Mockito.mock(MapActivity.class);
        assertEquals(mapActivity.getRadius(), 50);
        assertEquals(mapActivity.getCheckProximity(), false);
    }

    @Test
    public static void testUserLocationEnabled() {
        MapActivity mapActivity = Mockito.mock(MapActivity.class);
        Bundle bundle = Mockito.mock(Bundle.class);
        mapActivity.onCreate(bundle);
        //verify method positionMap is called.
        Mockito.verify(mapActivity).positionMap();
        MapView mapView  = mapActivity.getMapView();
        assertEquals(mapView.getUserLocationEnabled(), true);

        GpsLocationProvider gpsLocationProvider = Mockito.mock(GpsLocationProvider.class);
        UserLocationOverlay userLocationOverlay = new UserLocationOverlay(gpsLocationProvider, mapView);
        assertTrue(userLocationOverlay.isMyLocationEnabled());
    }
}
