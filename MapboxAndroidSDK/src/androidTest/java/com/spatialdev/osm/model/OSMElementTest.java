package com.spatialdev.osm.model;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created by Jason Rogena - jrogena@ona.io on 9/9/16.
 */
@RunWith(AndroidJUnit4.class)
public class OSMElementTest {
    /**
     * This method tests whether the getUniqueNegativeId method generates a unique with enough random
     * digits to avoid a collision. Requirements for an ID are:
     *  - It must be unique on the device
     *  - It must be negative
     *  - It must be long enough to guarantee uniqueness across several devices
     */
    @Test
    public void testGetUniqueNegativeId() {
        ArrayList<Long> randomIds = new ArrayList<>();
        for(int i =0; i < 10000; i++) {
            long curId = OSMElement.getUniqueNegativeId();
            assertFalse(randomIds.contains(curId));
            assertTrue(curId < 0);
            assertTrue(String.valueOf(curId).length() > 11);
            randomIds.add(curId);
        }
    }
}