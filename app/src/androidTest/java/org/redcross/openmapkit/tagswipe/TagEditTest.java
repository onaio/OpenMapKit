package org.redcross.openmapkit.tagswipe;

import android.content.Context;
import android.location.Location;
import android.support.test.InstrumentationRegistry;
import android.test.AndroidTestCase;

import org.junit.Before;
import org.junit.Test;
import org.redcross.openmapkit.ApplicationTest;
import org.redcross.openmapkit.Settings;

/**
 * Created by Jason Rogena - jrogena@ona.io on 7/26/16.
 */
public class TagEditTest extends AndroidTestCase {
    Context context;

    @Before
    public void init() {
        ApplicationTest.simulateODKLaunch();
        context = InstrumentationRegistry.getContext();
    }

    @Test
    public void testCheckUserLocationTags() throws Exception {
        TagEdit.mockTagEditHash();//method inserts user location tags with values
        assertTrue(TagEdit.checkUserLocationTags());

        /*
        mockTagEditHashWithoutUserLocTags mocks a tag hash without user location tags
         */
        TagEdit.mockTagEditHashWithoutUserLocTags();
        assertFalse(TagEdit.checkUserLocationTags());

        /*
        mockTagEditHashWithoutUserLocTags mocks a tag hash with user location tags with null and
        empty values
         */
        TagEdit.mockTagEditHashWithNullUserLocTags();
        assertFalse(TagEdit.checkUserLocationTags());
    }

    @Test
    public void testCleanUserLocationTags() throws Exception {
        TagEdit.mockTagEditHash();
        //test to see if initially the user location tags have values
        assertTrue(TagEdit.checkUserLocationTags());//checks whether tags exist and have values

        TagEdit.cleanUserLocationTags();
        /*
        cleanUserLocationTags should have made null the values of the user location tags
         */

        assertFalse(TagEdit.checkUserLocationTags());
    }

    @Test
    public void testGetReadOnlyValue() throws Exception {
        assertTrue(TagEdit.getReadOnlyValue(Settings.singleton().getUserLatLngName()));
        assertTrue(TagEdit.getReadOnlyValue(Settings.singleton().getUserAccuracyName()));
        assertFalse(TagEdit.getReadOnlyValue("test_tag"));
        assertFalse(TagEdit.getReadOnlyValue(null));
    }

    @Test
    public void testLocationToString() throws Exception {
        // Set up your test
        Location loc1 = new Location("Test");
        loc1.setLatitude(-10.0);
        loc1.setLongitude(10.0);
        assertTrue(TagEdit.locationToString(loc1).equals("-10.0,10.0"));

        Location loc2 = new Location("Test");
        loc2.setLatitude(10.0);
        loc2.setLongitude(-10.0);
        assertTrue(TagEdit.locationToString(loc2).equals("10.0,-10.0"));

        Location loc3 = new Location("Test");
        loc3.setLatitude(0.0);
        loc3.setLongitude(0.0);
        assertTrue(TagEdit.locationToString(loc3).equals("0.0,0.0"));

        Location loc4 = new Location("Test");
        loc4.setLatitude(0.42342343);
        loc4.setLongitude(-179.2342343);
        assertTrue(TagEdit.locationToString(loc4).equals("0.42342343,-179.2342343"));

        assertTrue(TagEdit.locationToString(null) == null);
    }
}