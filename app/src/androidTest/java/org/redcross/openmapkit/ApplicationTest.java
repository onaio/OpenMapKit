package org.redcross.openmapkit;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.test.ApplicationTestCase;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.redcross.openmapkit.odkcollect.ODKCollectHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    /**
     * This method performs actions that simulate a launch from ODK including:
     *  - cleans up the sdcard directory for the app
     *  - creating an intent similar to the one used to launch OpenMapKit from OpenDataKit
     *  - calling ODKCollectHandler.registerIntent with the created intent
     *  - initializes constraints and settings
     *
     * @return  Intent similar to the one used to launch OpenMapKit from OpenDataKit
     */
    public static Intent simulateODKLaunch() {
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

        Context context = InstrumentationRegistry.getContext();

        //delete and recreate the OpenMapKit directory on the SDCard
        File appDir = new File(sdcardPath+"/"+ExternalStorage.APP_DIR);
        if(appDir.exists()) {
            appDir.delete();
        }

        // create directory structure for app if needed
        ExternalStorage.checkOrCreateAppDirs();

        // Move constraints assets to ExternalStorage if necessary
        ExternalStorage.copyConstraintsToExternalStorageIfNeeded(context);

        ODKCollectHandler.registerIntent(context, intent);

        // Initialize the constraints singleton.
        // Loads up all the constraints JSON configs.
        Constraints.initialize();

        addTagsToIntent(intent);

        return intent;
    }

    private static void addTagsToIntent(Intent intent) {
        String formFileName = ODKCollectHandler.getODKCollectData().getFormFileName();
        try {
            File formConstraintsFile = ExternalStorage.fetchConstraintsFile(formFileName);
            String formConstraintsStr = FileUtils.readFileToString(formConstraintsFile);
            JSONObject formConstraintsJson = new JSONObject(formConstraintsStr);
            ArrayList<String> tagKeys = new ArrayList<>();

            Iterator<String> keyIterator = formConstraintsJson.keys();
            while (keyIterator.hasNext()) {
                String key = keyIterator.next();
                tagKeys.add(key);
                intent.putExtra("TAG_LABEL." + key, key);
                intent.putExtra("TAG_VALUE_LABEL."+key+".yes", "Yes");
                intent.putExtra("TAG_VALUE_LABEL." + key + ".no", "No");
            }
            intent.putExtra("TAG_KEYS", tagKeys);
        } catch (Exception e) {

        }
    }
}