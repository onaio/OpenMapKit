package com.spatialdev.osm.model;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Jason Rogena - jrogena@ona.io on 9/14/16.
 */
@RunWith(AndroidJUnit4.class)
public class JTSModelTest {

    /**
     * This method tests whether the elementAlreadyAdded works well in these scenarios:
     *  - The {@link OSMElement} being queried is null
     *  - No element corresponding to the provided {@link OSMElement} exists in the provided {@link Map}
     *  - The {@link OSMElement} being queried has a null timestamp
     *  - The element in the {@link Map} with the corresponding id to the element being queried has
     *    a null timestamp
     *  - The element being queried has a more recent timestamp that the element in the map
     *  - The in the map has a more recent timestamp than the element being queried
     */
    @Test
    public void testElementAlreadyAdded() {
        //test the effect of providing a null OSMElement
        long testId = 2343244323432l;
        Map<Long, OSMElement> test1Map = new ConcurrentHashMap<>();
        OSMElement test1Element = new OSMNode(
                String.valueOf(testId),
                "-23423432",
                "36.342323",
                "1",
                "2016-09-15T13:25:25Z",
                "30092734",
                null,
                "testUser",
                "update",
                OSMColorConfig.getDefaultConfig());
        test1Map.put(test1Element.getId(), test1Element);
        assertTrue(JTSModel.elementAlreadyAdded(test1Map, null));

        //test the effect of not having an element in the map corresponding to the provided element
        Map<Long, OSMElement> test2Map = new ConcurrentHashMap<>();
        OSMElement test2Element = new OSMNode(
                String.valueOf(testId),
                "-23423432",
                "36.342323",
                "1",
                "2016-09-15T13:25:25Z",
                "30092734",
                null,
                "testUser",
                "update",
                OSMColorConfig.getDefaultConfig());
        assertFalse(JTSModel.elementAlreadyAdded(test2Map, test2Element));

        //test null timestamp in query OSMElement
        Map<Long, OSMElement> test3Map = new ConcurrentHashMap<>();
        OSMElement test3QueryElement = new OSMNode(
                String.valueOf(testId),
                "-23423432",
                "36.342323",
                "1",
                "2016-09-15T1rwe3",//badly formatted date
                "30092734",
                null,
                "testUser",
                "update",
                OSMColorConfig.getDefaultConfig());
        OSMElement test3Element = new OSMNode(
                String.valueOf(testId),
                "-23423432",
                "36.342323",
                "1",
                "2016-09-15T13:25:25Z",//well formatted date
                "30092734",
                null,
                "testUser",
                "update",
                OSMColorConfig.getDefaultConfig());
        test3Map.put(test3Element.getId(), test3Element);
        assertTrue(JTSModel.elementAlreadyAdded(test3Map, test3QueryElement));

        //test null timestamp in OSMElement in map
        Map<Long, OSMElement> test4Map = new ConcurrentHashMap<>();
        OSMElement test4QueryElement = new OSMNode(
                String.valueOf(testId),
                "-23423432",
                "36.342323",
                "1",
                "2016-09-15T13:25:25Z",//well formatted date
                "30092734",
                null,
                "testUser",
                "update",
                OSMColorConfig.getDefaultConfig());
        OSMElement test4Element = new OSMNode(
                String.valueOf(testId),
                "-23423432",
                "36.342323",
                "1",
                "2016-09-15T13:25:fdsfd",//badly formatted date
                "30092734",
                null,
                "testUser",
                "update",
                OSMColorConfig.getDefaultConfig());
        test4Map.put(test4Element.getId(), test4Element);
        assertFalse(JTSModel.elementAlreadyAdded(test4Map, test4QueryElement));

        //query OSMElement has a more recent timestamp than what is in the map
        Map<Long, OSMElement> test5Map = new ConcurrentHashMap<>();
        OSMElement test5QueryElement = new OSMNode(
                String.valueOf(testId),
                "-23423432",
                "36.342323",
                "1",
                "2016-09-15T13:25:26Z",
                "30092734",
                null,
                "testUser",
                "update",
                OSMColorConfig.getDefaultConfig());
        OSMElement test5Element = new OSMNode(
                String.valueOf(testId),
                "-23423432",
                "36.342323",
                "1",
                "2016-09-15T13:25:25Z",
                "30092734",
                null,
                "testUser",
                "update",
                OSMColorConfig.getDefaultConfig());
        test5Map.put(test5Element.getId(), test5Element);
        assertFalse(JTSModel.elementAlreadyAdded(test5Map, test5QueryElement));

        //OSMElement in the map has a more recent timestamp than the query OSMElement
        Map<Long, OSMElement> test6Map = new ConcurrentHashMap<>();
        OSMElement test6QueryElement = new OSMNode(
                String.valueOf(testId),
                "-23423432",
                "36.342323",
                "1",
                "2016-09-15T13:25:25Z",
                "30092734",
                null,
                "testUser",
                "update",
                OSMColorConfig.getDefaultConfig());
        OSMElement test6Element = new OSMNode(
                String.valueOf(testId),
                "-23423432",
                "36.342323",
                "1",
                "2016-09-15T13:25:26Z",
                "30092734",
                null,
                "testUser",
                "update",
                OSMColorConfig.getDefaultConfig());
        test6Map.put(test6Element.getId(), test6Element);
        assertTrue(JTSModel.elementAlreadyAdded(test6Map, test6QueryElement));
    }
}