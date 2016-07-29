package org.redcross.openmapkit;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.spatialdev.osm.model.OSMColorConfig;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Created by Jason Rogena - jrogena@ona.io on 8/1/16.
 */
public class ConstraintsTest {
    private Context context;

    @Before
    public void initConstraints() {
        context = InstrumentationRegistry.getContext();
        ExternalStorage.copyConstraintsToExternalStorageIfNeeded(context);

        Constraints.initialize();
    }

    /**
     * This method tests whether the color constraint is parsed OK
     */
    @Test
    public void testColorConstraint() {
        OSMColorConfig osmColorConfig = Constraints.singleton().getFirstColorConfig();
        assertEquals(osmColorConfig.getOsmTagKey(), "building");
        assertEquals(osmColorConfig.getDefaultArgb().getIntValue(), new OSMColorConfig.ARGB("#654321").getIntValue());
        HashMap<String, String> tagValueColors = osmColorConfig.getValueColors();
        assertEquals(tagValueColors.size(), 2);
        assertTrue(tagValueColors.containsKey("yes"));
        assertEquals(tagValueColors.get("yes"), "#83b26e");
    }
}