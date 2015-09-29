package com.mapbox.mapboxsdk.util;

import android.os.Environment;

/**
 * For accessing the settings directory.
 */
public class ExternalStorage {

    /**
     * Created to prevent circular dependency on the app module
     * ExternalStorage class. The naming should be kept in sync.
     * * * 
     */
    public static final String APP_DIR = "openmapkit";
    public static final String SETTINGS_DIR = "settings";

    /**
     *
     * @return the settings directory of the app.
     */
    public static String getSettingsDir() {
        return Environment.getExternalStorageDirectory() + "/"
                + APP_DIR + "/"
                + SETTINGS_DIR + "/";
    }
}
