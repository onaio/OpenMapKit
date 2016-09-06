package org.redcross.openmapkit;

import android.content.Context;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by Jason Rogena - jrogena@ona.io on 9/6/16.
 */
@RunWith(AndroidJUnit4.class)
public class OSMMapBuilderTest {

    Context context;

    @Before
    public void setup() throws Exception {
        ApplicationTest.simulateODKLaunch();
        context = InstrumentationRegistry.getContext();
    }

    /**
     * This method tests whether the sortOsmFiles method sorts files accordingly
     */
    @Test
    public void testSortOsmFiles() {
        String sdCardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String testDirPath = sdCardPath + "/" + ExternalStorage.APP_DIR + "/tests";
        File testDir = new File(testDirPath);
        testDir.mkdirs();
        ArrayList<File> orderedList = new ArrayList<>();
        orderedList.add(new File(testDir, "fileone"));
        orderedList.add(new File(testDir, "filetwo"));
        orderedList.add(new File(testDir, "filethree"));
        orderedList.add(new File(testDir, "filefour"));
        orderedList.add(new File(testDir, "filefive"));
        orderedList.add(new File(testDir, "filesix"));
        createTestFilesInFS(orderedList);

        Set<File> unorderedSet = new HashSet<>();
        unorderedSet.add(new File(testDir, "filefive"));
        unorderedSet.add(new File(testDir, "fileone"));
        unorderedSet.add(new File(testDir, "filethree"));
        unorderedSet.add(new File(testDir, "filesix"));
        unorderedSet.add(new File(testDir, "filetwo"));
        unorderedSet.add(new File(testDir, "filefour"));

        ArrayList<File> reorderedList = OSMMapBuilder.sortOsmFiles(unorderedSet);
        for(int i = 0; i < orderedList.size(); i++) {
            assertEquals(orderedList.get(i).getAbsolutePath(), reorderedList.get(i).getAbsolutePath());
        }
    }

    /**
     * This method creates test files on the filesystem in the order their in on the list one second
     * apart
     */
    private void createTestFilesInFS(ArrayList<File> files) {
        for(File curFile : files) {
            try
            {
                if (!curFile.exists()) {
                    new FileOutputStream(curFile).close();
                }
            }
            catch (IOException e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}