package org.redcross.openmapkit;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.PositionAssertions;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.view.View;

import org.hamcrest.Matcher;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by Jason Rogena|Mark Ekisa on 7/22/16.
 */
@RunWith(AndroidJUnit4.class)
public class MapActivityTest {
    private static final String TAG = "OMK.MapActivityTest";
    private static final long INITIAL_SLEEP_TIME = 5000l;

    private static final String ROUNDED_BUTTON_LABEL = "+  Add Structure";

    @Before
    public void waitForSomeTime() {
        Log.d(TAG, "Sleeping for 5 seconds before running tests");
        try {
            Log.d(TAG, "Running tests");
            Thread.sleep(INITIAL_SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Rule
    public ActivityTestRule<MapActivity> mActivityRule = new ActivityTestRule<>(MapActivity.class);

    /**
     * This method tests the position of the 'locate-me' button in the activity
     */
    @Test
    public void locationButtonPosition() {
        //check initial position
        Espresso.onView(ViewMatchers.withId(R.id.locationButton))
                .check(PositionAssertions
                        .isAbove(ViewMatchers.withId(R.id.nodeModeButton)));
        Espresso.onView(ViewMatchers.withId(R.id.locationButton))
                .check(PositionAssertions
                        .isRightAlignedWith(ViewMatchers.withId(R.id.nodeModeButton)));
    }

    @Test
    public void roundedButtonLabel() {
        //show the add structure marker by clicking the '+' button
        Espresso.onView(ViewMatchers.withId(R.id.nodeModeButton)).perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withId(R.id.addNodeBtn)).check(ViewAssertions.matches(ViewMatchers.withText(ROUNDED_BUTTON_LABEL)));
    }
}
