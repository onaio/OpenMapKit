package org.redcross.openmapkit.odkcollect;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.redcross.openmapkit.ApplicationTest;
import org.redcross.openmapkit.ExternalStorage;
import org.redcross.openmapkit.Settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created by Jason Rogena - jrogena@ona.io on 8/18/16.
 */
public class FormOSMDownloaderTest {
    Context context;

    @Before
    public void setup() throws Exception {
        ApplicationTest.simulateODKLaunch();
        context = InstrumentationRegistry.getContext();
    }

    private void copySettingsDir() {
        //first remove the existing dir if already exists
        File dir = new File(ExternalStorage.getSettingsDir());
        dir.mkdirs();
        if (dir.exists() && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                new File(dir, children[i]).delete();
            }
        }

        //add the fresh settings dir
        ExternalStorage.copyAssetsFileOrDirToExternalStorage(context, ExternalStorage.SETTINGS_DIR);
    }

    /**
     * This method tests:
     *  - whether the OSM file for a form is deleted before the download is started
     *  - the OSM file is actually downloaded to the designated location after onSuccess of the
     *    OnFileDownload interface is called
     */
    @Test
    public void testDownloadProcess() {
        ArrayList<Form> testForms = Settings.singleton().getOSMFromODKForms();
        if(testForms.size() > 0) {
            final Form testForm = testForms.get(0);
            //touch the test form file in the sdcard
            try {
                new FileOutputStream(testForm.getLocalOsmFile()).close();
            } catch (IOException e) {
                assertTrue(e.getMessage(), false);
            }

            //check if the file exists
            assertTrue(testForm.getLocalOsmFile().exists());

            FormOSMDownloader downloader = new FormOSMDownloader(context, testForm, new FormOSMDownloader.OnFileDownload() {
                @Override
                public void onStart(Form form) {
                    //check if the osm file exists on the sdcard (it shouldn't)
                    assertFalse(testForm.getLocalOsmFile().exists());
                }

                @Override
                public void onFail(Form form) {
                }

                @Override
                public void onSuccess(Form form) {
                    assertTrue(testForm.getLocalOsmFile().exists());
                }
            }, null);
            downloader.execute();
        } else {
            assertTrue("No test forms defined in settings", false);
        }
    }
}