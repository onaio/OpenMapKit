package org.redcross.openmapkit;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.content.Intent;

import com.spatialdev.osm.model.OSMColorConfig;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.redcross.openmapkit.odkcollect.ODKCollectHandler;

import static org.junit.Assert.*;

/**
 * Created by Jason Rogena - jrogena@ona.io on 8/1/16.
 */
public class ConstraintsTest {
    private Context context;

    @Before
    public void initConstraints() {
        Intent intent = ApplicationTest.getLaunchOMKIntent();
        ODKCollectHandler.registerIntent(intent);

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
    /**
     * This method tests whether the 'hide' constraint works
     */
    @Test
    public void testHideConstraint() {
        assertTrue(Constraints.singleton().tagIsHidden("hidden_tag_1"));
        assertFalse(Constraints.singleton().tagIsHidden("shown_tag_1"));
    }

    /**
     * This method tests whether the 'hide' constraint works in tags that also have the 'required'
     * constraint
     */
    @Test
    public void testHideConstraint_Required() {
        assertTrue(Constraints.singleton().tagIsHidden("hidden_tag_2"));
        assertFalse(Constraints.singleton().tagIsHidden("shown_tag_2"));
    }

    /**
     * This method tests whether the 'hide' constraint works in tags that also have the 'hide_if'
     * constraint
     */
    @Test
    public void testHideConstraint_HideIf() {
        assertTrue(Constraints.singleton().tagIsHidden("hidden_tag_3"));
        assertFalse(Constraints.singleton().tagIsHidden("shown_tag_3"));
    }
}