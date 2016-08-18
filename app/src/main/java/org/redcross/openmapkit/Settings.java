package org.redcross.openmapkit;

import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.redcross.openmapkit.odkcollect.Form;
import org.redcross.openmapkit.odkcollect.ODKCollectHandler;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Jason Rogena - jrogena@ona.io on 8/16/16.
 */
public class Settings {
    //sub settings
    private static final String SUB_OSM_FROM_ODK = "osm_from_odk";
    private static final String SUB_PROXIMITY = "proximity";

    //defaults
    public static final double DEFAULT_PROXIMITY_RADIUS = 50d;
    public static final boolean DEFAULT_PROXIMITY_CHECK = false;
    public static final int DEFAULT_GPS_TIMER_DELAY = 0;
    public static final float DEFAULT_GPS_PROXIMITY_ACCURACY = 25f;
    public static final boolean DEFAULT_PROXIMITY_ENABLED = false;
    public static final String DEFAULT_OSM_FROM_ODK_QUERY = null;
    public static final String DEFAULT_OSM_FROM_ODK_SERVER = "https://api.ona.io/api/v1/osm/";
    public static final String DEFAULT_OSM_FROM_ODK_USERNAME = null;
    public static final String DEFAULT_OSM_FROM_ODK_PASSWORD = null;
    public static final ArrayList<Form> DEFAULT_OSM_FROM_ODK_FORMS = new ArrayList<>();


    private static Settings instance;
    private static boolean proximityEnabled = DEFAULT_PROXIMITY_ENABLED;

    private JSONObject data;

    public static Settings initialize() {
        instance = new Settings();
        return instance;
    }

    private Settings() {
        proximityEnabled = DEFAULT_PROXIMITY_ENABLED;
        data = new JSONObject();
        if(ODKCollectHandler.isODKCollectMode()) {
            String formFileName = ODKCollectHandler.getODKCollectData().getFormFileName();
            try {
                File file = ExternalStorage.fetchSettingsFile(formFileName);
                String settingsString = FileUtils.readFileToString(file);
                data = new JSONObject(settingsString);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Settings singleton() {
        return instance;
    }

    private JSONObject getOSMFromODKSub() throws JSONException {
        if(!data.has(SUB_OSM_FROM_ODK)) {
            data.put(SUB_OSM_FROM_ODK, new JSONObject());
        }

        //set the default server if non set
        if(!data.getJSONObject(SUB_OSM_FROM_ODK).has("server")
                || data.getJSONObject(SUB_OSM_FROM_ODK).getString("server") == null
                || data.getJSONObject(SUB_OSM_FROM_ODK).getString("server").trim().length() == 0) {
            data.getJSONObject(SUB_OSM_FROM_ODK).put("server", DEFAULT_OSM_FROM_ODK_SERVER);
        }
        return data.getJSONObject(SUB_OSM_FROM_ODK);
    }

    private JSONObject getProximitySub() throws JSONException {
        if(!data.has(SUB_PROXIMITY)) {
            data.put(SUB_PROXIMITY, new JSONObject());
        }

        return data.getJSONObject(SUB_PROXIMITY);
    }

    public ArrayList<Form> getOSMFromODKForms() {
        ArrayList<Form> forms = DEFAULT_OSM_FROM_ODK_FORMS;
        if(data != null) {
            try {
                JSONObject osmFromODK = getOSMFromODKSub();
                if(osmFromODK.has("forms")) {
                    JSONArray formsJsonArray = osmFromODK.getJSONArray("forms");
                    forms = new ArrayList<>();
                    for(int i = 0; i < formsJsonArray.length(); i++) {
                        forms.add(new Form(null, formsJsonArray.getInt(i)));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return forms;
    }

    public String getOSMFromODKServer() {
        String server = DEFAULT_OSM_FROM_ODK_SERVER;
        if(data != null) {
            try {
                JSONObject osmFromODK = getOSMFromODKSub();
                server = osmFromODK.getString("server");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return server;
    }

    public String getOSMFromODKUsername() {
        String username = DEFAULT_OSM_FROM_ODK_USERNAME;
        if(data != null) {
            try {
                JSONObject osmFromODK = getOSMFromODKSub();
                username = osmFromODK.getString("username");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return username;
    }

    public String getOSMFromODKPassword() {
        String password = DEFAULT_OSM_FROM_ODK_PASSWORD;
        if(data != null) {
            try {
                JSONObject osmFromODK = getOSMFromODKSub();
                password = osmFromODK.getString("password");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return password;
    }

    public String getOSMFromODKQuery() {
        String query = DEFAULT_OSM_FROM_ODK_QUERY;
        if(data != null) {
            try {
                JSONObject osmFromODK = getOSMFromODKSub();
                if(osmFromODK.has("query")) {
                    query = osmFromODK.getString("query");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return query;
    }

    /**
     *
     * @return proximity proximityRadius around user location.
     */
    public double getProximityRadius() {
        double proximityRadius = DEFAULT_PROXIMITY_RADIUS;
        try {
            JSONObject proximitySettings = getProximitySub();
            if(proximitySettings.has("proximity_radius")) {
                proximityRadius = proximitySettings.getDouble("proximity_radius");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return proximityRadius;
    }

    /**
     *
     * @return true if GPS must be enabled to show user location.
     */
    public boolean getProximityCheck() {
        boolean proximityCheck = DEFAULT_PROXIMITY_CHECK;
        try {
            JSONObject proximitySettings = getProximitySub();
            if(proximitySettings.has("proximity_check")) {
                proximityCheck = proximitySettings.getBoolean("proximity_check");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return proximityCheck;
    }

    /**
     *
     * @param value set whether to apply proximity settings.
     */
    public static void setProximityEnabled(boolean value) {
        proximityEnabled = value;
    }

    /**
     *
     * @return true if proximity settings should be applied.
     */
    public static boolean isProximityEnabled() {
        return proximityEnabled;
    }

    public int getGpsTimerDelay() {
        int gpsTimerDelay = DEFAULT_GPS_TIMER_DELAY;
        try {
            JSONObject proximitySettings = getProximitySub();
            if(proximitySettings.has("max_gps_timer_delay")) {
                gpsTimerDelay = proximitySettings.getInt("max_gps_timer_delay");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return gpsTimerDelay;
    }

    public double getGpsProximityAccuracy() {
        double gpsProximityAccuracy = DEFAULT_GPS_PROXIMITY_ACCURACY;
        try {
            JSONObject proximitySettings = getProximitySub();
            if(proximitySettings.has("gps_proximity_accuracy")) {
                gpsProximityAccuracy = proximitySettings.getDouble("gps_proximity_accuracy");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return gpsProximityAccuracy;
    }
}
