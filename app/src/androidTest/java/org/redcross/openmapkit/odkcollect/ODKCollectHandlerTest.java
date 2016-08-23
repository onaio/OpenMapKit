package org.redcross.openmapkit.odkcollect;

import org.junit.Before;
import org.junit.Test;
import org.redcross.openmapkit.ApplicationTest;
import org.redcross.openmapkit.Settings;
import org.redcross.openmapkit.odkcollect.tag.ODKTag;

import java.util.LinkedHashMap;

import static org.junit.Assert.*;

/**
 * Created by Jason Rogena - jrogena@ona.io on 7/25/16.
 */
public class ODKCollectHandlerTest {
    @Before
    public void init() {
        ApplicationTest.simulateODKLaunch();
    }

    @Test
    public void testAddUserLocationTags() {
        //test with empty map
        LinkedHashMap<String, ODKTag> firstMap = new LinkedHashMap<>();
        firstMap = ODKCollectHandler.addUserLocationTags(firstMap);
        assertTrue(firstMap.size() == 2);
        assertTrue(firstMap.containsKey(Settings.singleton().getUserLatLngName()));
        assertTrue(firstMap.get(Settings.singleton().getUserLatLngName()) != null);
        assertTrue(firstMap.containsKey(Settings.singleton().getUserAccuracyName()));
        assertTrue(firstMap.get(Settings.singleton().getUserAccuracyName()) != null);

        //test with non-empty map
        LinkedHashMap<String, ODKTag> secondMap = new LinkedHashMap<>();
        secondMap.put("test_key_1", new ODKTag());
        secondMap = ODKCollectHandler.addUserLocationTags(secondMap);
        assertTrue(secondMap.size() == 3);
        assertTrue(secondMap.containsKey(Settings.singleton().getUserLatLngName()));
        assertTrue(secondMap.get(Settings.singleton().getUserLatLngName()) != null);
        assertTrue(secondMap.containsKey(Settings.singleton().getUserAccuracyName()));
        assertTrue(secondMap.get(Settings.singleton().getUserAccuracyName()) != null);
    }
}