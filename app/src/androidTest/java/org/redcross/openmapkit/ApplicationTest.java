package org.redcross.openmapkit;

import android.app.Application;
import android.content.Intent;
import android.os.Environment;
import android.test.ApplicationTestCase;

import java.io.File;
import java.util.ArrayList;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    /**
     * This method creates an intent similar to the one used to launch OpenMapKit from OpenDataKit
     *
     * @return  Intent similar to the one used to launch OpenMapKit from OpenDataKit
     */
    public static Intent getLaunchOMKIntent() {
        String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File odkInstanceDir = new File(sdcardPath + "/odk/instances/omk_functional_test");
        odkInstanceDir.mkdirs();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra("FORM_FILE_NAME", "omk_functional_test");
        intent.putExtra("FORM_ID", "-1");
        intent.putExtra("INSTANCE_ID", "uuid:6004201f-9942-429d-bfa4-e65b683da37b");
        intent.putExtra("INSTANCE_DIR", sdcardPath + "/odk/instances/omk_functional_test");
        intent.putExtra("OSM_EDIT_FILE_NAME", "omk_functional_test.osm");
        ArrayList<String> tagKeys = new ArrayList<>();
        tagKeys.add("spray_status");

        intent.putExtra("TAG_KEYS", tagKeys);
        intent.putExtra("TAG_LABEL.spray_status", "Spray Status");
        intent.putExtra("TAG_VALUES.spray_status", "null");
        intent.putExtra("TAG_VALUE_LABEL.spray_status.undefined", "Undefined");
        intent.putExtra("TAG_VALUE_LABEL.spray_status.yes", "Yes");
        intent.putExtra("TAG_VALUE_LABEL.spray_status.no", "No");

        return intent;
    }
}