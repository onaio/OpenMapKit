package org.redcross.openmapkit;

import android.content.Context;
import android.location.LocationManager;

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
    private static final String SUB_USER_LOCATION_TAGS = "user_location_tags";
    private static final String SUB_PULL_CSV = "pull_csv";

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
    public static final String DEFAULT_NODE_NAME = "node";
    public static final ArrayList<String> DEFAULT_HIDDEN_MENU_ITEMS = new ArrayList<>();
    public static final String DEFAULT_USER_LOCATION_TAGS_LAT_LNG = null;
    public static final String DEFAULT_USER_LOCATION_TAGS_ACCURACY = null;
    public static final boolean DEFAULT_CLICKABLE_TAGS = true;
    public static final JSONArray DEFAULT_PULL_CSV_TAGS = new JSONArray();
    public static final String DEFAULT_PULL_CSV_PK_COLUMN = null;
    public static final String DEFAULT_PULL_CSV_FILENAME = null;
    public static final float DEFAULT_MIN_VECTOR_RENDER_ZOOM = OSMMapBuilder.DEFAULT_MIN_VECTOR_RENDER_ZOOM;
    public static final boolean DEFAULT_SHOW_ADD_NODE = true;
    public static final String DEFAULT_ADMIN_PASSWORD = null;


    private static Settings instance;
    private static boolean proximityEnabled = DEFAULT_PROXIMITY_ENABLED;

    private JSONObject data;
    private boolean gpsEnabled;

    public static Settings initialize(Context context) {
        instance = new Settings(context);
        return instance;
    }

    /**
     * Constructor to be called whenever the settings object is to be initialized before {@link ODKCollectHandler}
     * has initialized an instance of {@link org.redcross.openmapkit.odkcollect.ODKCollectData}
     * @param formFileName
     * @return
     */
    public static Settings initialize(Context context, String formFileName) {
        instance = new Settings(context, formFileName);
        return instance;
    }

    private Settings(Context context) {
        proximityEnabled = DEFAULT_PROXIMITY_ENABLED;
        updateGpsEnabled(context);
        data = new JSONObject();
        if(ODKCollectHandler.isODKCollectMode()) {
            String formFileName = ODKCollectHandler.getODKCollectData().getFormFileName();
            completeInit(formFileName);
        }
    }

    public void updateGpsEnabled(Context context) {
        gpsEnabled = false;
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if ( manager.isProviderEnabled(MapActivity.preferredLocationProvider) ) {
            gpsEnabled = true;
        } else {
            gpsEnabled = false;
        }
    }

    private Settings(Context context, String formFileName) {
        proximityEnabled = DEFAULT_PROXIMITY_ENABLED;
        updateGpsEnabled(context);
        data = new JSONObject();
        completeInit(formFileName);
    }

    private void completeInit(String formFileName) {
        ExternalStorage.copyFormFileFromOdkMediaDir(formFileName, ExternalStorage.SETTINGS_FILE_NAME_ON_ODK);
        try {
            File file = ExternalStorage.fetchSettingsFile(formFileName);
            String settingsString = FileUtils.readFileToString(file);
            data = new JSONObject(settingsString);
        } catch (Exception e) {
            e.printStackTrace();
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

    private JSONObject getPullCsvSub() throws JSONException {
        if(!data.has(SUB_PULL_CSV)) {
            data.put(SUB_PULL_CSV, new JSONObject());
        }

        //set the default values
        if(!data.getJSONObject(SUB_PULL_CSV).has("tags")) {
            data.getJSONObject(SUB_PULL_CSV).put("tags", DEFAULT_PULL_CSV_TAGS);
        }
        if(!data.getJSONObject(SUB_PULL_CSV).has("filename")
                || data.getJSONObject(SUB_PULL_CSV).getString("filename") == null
                || data.getJSONObject(SUB_PULL_CSV).getString("filename").length() == 0) {
            data.getJSONObject(SUB_PULL_CSV).put("filename", DEFAULT_PULL_CSV_FILENAME);
        }
        if(!data.getJSONObject(SUB_PULL_CSV).has("pk_column")
                || data.getJSONObject(SUB_PULL_CSV).getString("pk_column") == null
                || data.getJSONObject(SUB_PULL_CSV).getString("pk_column").length() == 0) {
            data.getJSONObject(SUB_PULL_CSV).put("pk_column", DEFAULT_PULL_CSV_PK_COLUMN);
        }
        return data.getJSONObject(SUB_PULL_CSV);
    }

    private JSONObject getProximitySub() throws JSONException {
        if(!data.has(SUB_PROXIMITY)) {
            data.put(SUB_PROXIMITY, new JSONObject());
        }

        return data.getJSONObject(SUB_PROXIMITY);
    }

    private JSONObject getUserLocationTagsSub() throws JSONException {
        if(!data.has(SUB_USER_LOCATION_TAGS)) {
            data.put(SUB_USER_LOCATION_TAGS, new JSONObject());
        }

        return data.getJSONObject(SUB_USER_LOCATION_TAGS);
    }

    public JSONArray getPullCsvTags() {
        JSONArray tags = DEFAULT_PULL_CSV_TAGS;
        if(data != null) {
            try {
                JSONObject pullCsvSub = getPullCsvSub();
                if(pullCsvSub.has("tags")) {
                    tags = pullCsvSub.getJSONArray("tags");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return tags;
    }

    public String getPullCsvFilename() {
        String filename = DEFAULT_PULL_CSV_FILENAME;
        if(data != null) {
            try {
                JSONObject pullCsvSub = getPullCsvSub();
                if(pullCsvSub.has("filename")) {
                    filename = pullCsvSub.getString("filename");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return  filename;
    }

    public String getPullCsvPkColumn() {
        String pkColumn = DEFAULT_PULL_CSV_PK_COLUMN;
        if(data != null) {
            try {
                JSONObject pullCsvSub = getPullCsvSub();
                if(pullCsvSub.has("pk_column")) {
                    pkColumn = pullCsvSub.getString("pk_column");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return  pkColumn;
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

    public void setOSMFromODKUsername(String username) {
        if(data != null) {
            try {
                JSONObject osmFromODK = getOSMFromODKSub();
                osmFromODK.put("username", username);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void setOSMFromODKPassword(String password) {
        if(data != null) {
            try {
                JSONObject osmFromODK = getOSMFromODKSub();
                osmFromODK.put("password", password);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void setOSMFromODKQuery(String query) {
        if(data != null) {
            try {
                JSONObject osmFromODK = getOSMFromODKSub();
                osmFromODK.put("query", query);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void setOSMFromODKServer(String server) {
        if(data != null) {
            try {
                JSONObject osmFromODK = getOSMFromODKSub();
                osmFromODK.put("server", server);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void setOSMFromODKForms(JSONArray forms) {
        if(data != null) {
            try {
                JSONObject osmFromODK = getOSMFromODKSub();
                osmFromODK.put("forms", forms);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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
            if(isGpsEnabled() && proximitySettings.has("proximity_check")) {
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

    public String getNodeName() {
        String nodeName = DEFAULT_NODE_NAME;
        if(data.has("node_name")) {
            try {
                nodeName = data.getString("node_name");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return nodeName;
    }

    public boolean shouldShowAddNode() {
        boolean result = DEFAULT_SHOW_ADD_NODE;
        if(data.has("show_add_node")) {
            try {
                result = data.getBoolean("show_add_node");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * This method returns lowercase text for menu items that should be hidden
     *
     * @return  A list of all lowercase labels for menu items that should be hidden
     */
    public ArrayList<String> getHiddenMenuItems() {
        ArrayList<String> hiddenMenuItems = DEFAULT_HIDDEN_MENU_ITEMS;
        if(data.has("hidden_menu_items")) {
            try {
                JSONArray hiddenMenuItemsJA = data.getJSONArray("hidden_menu_items");
                hiddenMenuItems = new ArrayList<>();
                for(int i = 0; i < hiddenMenuItemsJA.length(); i++) {
                    hiddenMenuItems.add(hiddenMenuItemsJA.getString(i).toLowerCase());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return hiddenMenuItems;
    }

    /**
     * This method checks whether user location tags are enabled
     *
     * @return TRUE if user location tags are supposed to be inserted into the OSM data
     */
    public boolean isUserLocationTagsEnabled() {
        return isUserLocationTagsEnabled(false);
    }

    /**
     * This method checks whether user location tags are enabled
     *
     * @param shouldIgnoreGpsStatus Whether to ignore if gps is on or off
     * @return  TRUE if user location tags are supposed to be inserted into the OSM data
     */
    public boolean isUserLocationTagsEnabled(boolean shouldIgnoreGpsStatus) {
        boolean result = false;
        if(getUserLatLngName() != null
                || getUserAccuracyName() != null) {
            if(shouldIgnoreGpsStatus == true || isGpsEnabled()) result = true;
        }
        return result;
    }

    public String getUserLatLngName() {
        String latLng = DEFAULT_USER_LOCATION_TAGS_LAT_LNG;
        try {
            JSONObject userLocationTags = getUserLocationTagsSub();
            if(userLocationTags.has("lat_lng")
                    && userLocationTags.getJSONObject("lat_lng").has("name")) {
                latLng = userLocationTags.getJSONObject("lat_lng").getString("name");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return latLng;
    }

    public String getUserLatLngLabel() {
        String latLng = getUserLatLngName();
        try {
            JSONObject userLocationTags = getUserLocationTagsSub();
            if(userLocationTags.has("lat_lng")
                    && userLocationTags.getJSONObject("lat_lng").has("label")) {
                latLng = userLocationTags.getJSONObject("lat_lng").getString("label");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return latLng;
    }

    public String getUserAccuracyName() {
        String accuracy = DEFAULT_USER_LOCATION_TAGS_ACCURACY;
        try {
            JSONObject userLocationTags = getUserLocationTagsSub();
            if(userLocationTags.has("accuracy")
                    && userLocationTags.getJSONObject("accuracy").has("name")) {
                accuracy = userLocationTags.getJSONObject("accuracy").getString("name");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return accuracy;
    }

    public String getUserAccuracyLabel() {
        String accuracy = getUserAccuracyName();
        try {
            JSONObject userLocationTags = getUserLocationTagsSub();
            if(userLocationTags.has("accuracy")
                    && userLocationTags.getJSONObject("accuracy").has("label")) {
                accuracy = userLocationTags.getJSONObject("accuracy").getString("label");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return accuracy;
    }

    public boolean isUserLocationTag(String name) {
        if(isUserLocationTagsEnabled(true)) {
            if((getUserLatLngName() != null && name.equals(getUserLatLngName()))
                    || (getUserAccuracyName() != null && name.equals(getUserAccuracyName()))) {
                return true;
            }
        }

        return false;
    }

    /**
     * This method returns whether tags in the MapActivity are clickable (hence sending the user to
     * the TagSwipeActivity with the clicked tag in focus)
     *
     * @return  TRUE if tags in the MapActivity are clickable
     */
    public boolean getClickableTags() {
        boolean response = DEFAULT_CLICKABLE_TAGS;
        if(data.has("clickable_tags")) {
            try {
                response = data.getBoolean("clickable_tags");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    /**
     * This method returns the no GPS authorization password
     *
     * @return The no_gps_password field in the settings, or @link{DEFAULT_ADMIN_PASSWORD} if not
     * defined in the settings file
     */
    public String getAdminPassword() {
        String response = DEFAULT_ADMIN_PASSWORD;
        if(data.has("admin_password")) {
            try {
                response = data.getString("admin_password");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    /**
     * This method temporarily sets the value of clickable_tags. Value will not persist when settings
     * singleton is re-instantiated
     * @param value
     */
    public void setCickableTags(boolean value) {
        try {
            data.put("clickable_tags", value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public float getMinVectorRenderZoom() {
        float response = DEFAULT_MIN_VECTOR_RENDER_ZOOM;
        if(data.has("min_vector_render_zoom")) {
            try {
                response = (float)data.getDouble("min_vector_render_zoom");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    public boolean isGpsEnabled() {
        return gpsEnabled;
    }
}
