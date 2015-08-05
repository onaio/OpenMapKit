package org.redcross.openmapkit;

import android.content.Context;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;
import android.test.mock.MockContext;
import android.test.suitebuilder.annotation.SmallTest;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.mapbox.mapboxsdk.overlay.LocationXMLParser;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.spatialdev.osm.renderer.util.ColorXmlParser;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import static android.app.PendingIntent.getActivity;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Created by coder on 7/20/15.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class LocationXmlParserTest {
    @Test
    public void testLocationXmlTagNames() {
        assertEquals(LocationXMLParser.FILENAME, "proximity_settings.xml");
        assertEquals(LocationXMLParser.PROXIMITY_RADIUS, "proximity_radius");
        assertEquals(LocationXMLParser.PROXIMITY_CHECK, "proximity_check");
    }

    @Test
    public void testLocationProximityRadius() throws IOException, XmlPullParserException {
        Context context = new MockContext();
        LocationXMLParser.parseXML(context);
        int radius  = (int) LocationXMLParser.getProximityRadius();
        assertEquals(radius, 100);
    }

    @Test
    public void testLocationProximityCheck() throws IOException, XmlPullParserException {
        Context context = new MockContext();
        LocationXMLParser.parseXML(context);
        assertEquals(LocationXMLParser.getProximityCheck(), true);
        assertEquals(LocationXMLParser.isProximityEnabled(), false);
        LocationXMLParser.setProximityEnabled(true);
        assertEquals(LocationXMLParser.isProximityEnabled(), true);
    }
}
