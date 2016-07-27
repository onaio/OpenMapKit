package org.redcross.openmapkit.odkcollect;

import org.junit.Test;
import org.redcross.openmapkit.odkcollect.tag.ODKTag;
import org.redcross.openmapkit.tagswipe.TagEdit;

import java.util.LinkedHashMap;

import static org.junit.Assert.*;

/**
 * Created by Jason Rogena - jrogena@ona.io on 7/25/16.
 */
public class ODKCollectHandlerTest {
    @Test
    public void testAddUserLocationTags() {
        //test with empty map
        LinkedHashMap<String, ODKTag> firstMap = new LinkedHashMap<>();
        firstMap = ODKCollectHandler.addUserLocationTags(firstMap);
        assertTrue(firstMap.size() == 2);
        assertTrue(firstMap.containsKey(TagEdit.TAG_KEY_USER_LOCATION));
        assertTrue(firstMap.get(TagEdit.TAG_KEY_USER_LOCATION) != null);
        assertTrue(firstMap.containsKey(TagEdit.TAG_KEY_USER_LOCATION_ACCURACY));
        assertTrue(firstMap.get(TagEdit.TAG_KEY_USER_LOCATION_ACCURACY) != null);

        //test with non-empty map
        LinkedHashMap<String, ODKTag> secondMap = new LinkedHashMap<>();
        secondMap.put("test_key_1", new ODKTag());
        secondMap = ODKCollectHandler.addUserLocationTags(secondMap);
        assertTrue(secondMap.size() == 3);
        assertTrue(secondMap.containsKey(TagEdit.TAG_KEY_USER_LOCATION));
        assertTrue(secondMap.get(TagEdit.TAG_KEY_USER_LOCATION) != null);
        assertTrue(secondMap.containsKey(TagEdit.TAG_KEY_USER_LOCATION_ACCURACY));
        assertTrue(secondMap.get(TagEdit.TAG_KEY_USER_LOCATION_ACCURACY) != null);
    }
}