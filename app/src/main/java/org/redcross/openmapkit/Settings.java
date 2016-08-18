package org.redcross.openmapkit;

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
    private static Settings instance;

    //sub settings
    private static final String SUB_OSM_FROM_ODK = "osm_from_odk";


    private static final String DEFAULT_OSM_FROM_ODK_SERVER = "https://api.ona.io/api/v1/osm/";

    private JSONObject data;

    public static Settings initialize() {
        instance = new Settings();
        return instance;
    }

    private Settings() {
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

    public ArrayList<Form> getOSMFromODKForms() {
        ArrayList<Form> forms = new ArrayList<>();
        if(data != null) {
            try {
                JSONObject osmFromODK = getOSMFromODKSub();
                if(osmFromODK.has("forms")) {
                    JSONArray formsJsonArray = osmFromODK.getJSONArray("forms");
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
        String server = null;
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
        String username = null;
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
        String password = null;
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
        String query = null;
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
}
