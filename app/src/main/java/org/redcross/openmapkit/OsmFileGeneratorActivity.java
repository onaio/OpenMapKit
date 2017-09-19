package org.redcross.openmapkit;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spatialdev.osm.model.OSMNode;

import org.redcross.openmapkit.odkcollect.ODKCollectData;
import org.redcross.openmapkit.odkcollect.ODKCollectHandler;
import org.redcross.openmapkit.odkcollect.tag.ODKTag;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates an OSM file
 */
public class OsmFileGeneratorActivity extends Activity {
    private static final String TAG = OsmFileGeneratorActivity.class.getSimpleName();
    public static final String KEY_FORM_FILE_NAME = "FORM_FILE_NAME";
    public static final String KEY_FORM_ID = "FORM_ID";
    public static final String KEY_INSTANCE_ID = "INSTANCE_ID";
    public static final String KEY_INSTANCE_DIR = "INSTANCE_DIR";
    public static final String KEY_GPS = "gps";
    public static final String KEY_FILENAME = "filename";
    public static final String VALUE_GPSLATLNG = "value:gps_latlng";
    public static final String VALUE_GPSACCURACY = "value:gps_accuracy";
    private static final Pattern ODK_GPS_PATTERN = Pattern.compile(
            "([\\-\\+]?[\\d\\.]+)\\s+([\\-\\+]?[\\d\\.]+)\\s+([\\d\\.]+)\\s+([\\d\\.]+)");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_osm_generator);
        if (getIntent().getExtras() != null) {
            extractData(getIntent().getExtras());
        }
    }

    private void extractData(Bundle bundle) {
        String formId = null;
        String formFileName = null;
        String instanceId = null;
        String instanceDir = null;
        String gps = null;
        String filename = null;
        String userLocTag = null;
        HashMap<String, String> tags = new HashMap<>();
        for (String curKey : bundle.keySet()) {
            if (curKey.equals(KEY_FORM_FILE_NAME)) {
                formFileName = bundle.getString(curKey);
            } else if (curKey.equals(KEY_FORM_ID)) {
                formId = bundle.getString(curKey);
            } else if (curKey.equals(KEY_INSTANCE_ID)) {
                instanceId = bundle.getString(curKey);
            } else if (curKey.equals(KEY_INSTANCE_DIR)) {
                instanceDir = bundle.getString(curKey);
            } else if (curKey.equals(KEY_GPS)) {
                gps = bundle.getString(curKey);
            } else if (curKey.equals(KEY_FILENAME)) {
                filename = bundle.getString(curKey);
            } else {
                tags.put(curKey, bundle.getString(curKey));
            }
        }

        constructOsmFile(formId, formFileName, instanceId, instanceDir, filename, gps, tags, userLocTag);
    }

    private void constructOsmFile(String formId, String formFileName, String instanceId,
                                  String instanceDir, String filename, String gps,
                                  HashMap<String, String> tags, String userLocTag) {
        if (instanceDir != null) Log.d(TAG, "instance_dir : " + instanceDir);
        if (filename != null) Log.d(TAG, "filename : " + filename);
        if (gps != null) Log.d(TAG, "gps : " + gps);
        for (String curKey : tags.keySet()) {
            Log.d(TAG, curKey + " : " + tags.get(curKey));
        }

        String latitude = null;
        String longitude = null;
        String accuracy = null;
        Matcher gpsMatcher = ODK_GPS_PATTERN.matcher(gps);
        if (gpsMatcher.find()) {
            latitude = gpsMatcher.group(1);
            longitude = gpsMatcher.group(2);
            accuracy = gpsMatcher.group(4);
        }

        if (!TextUtils.isEmpty(latitude) && !TextUtils.isEmpty(longitude)) {
            LatLng latLng = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
            OSMNode node = new OSMNode(latLng, null);

            for (String curKey : tags.keySet()) {
                String curValue = tags.get(curKey);
                if (!TextUtils.isEmpty(curValue)) {
                    if (curValue.equals(VALUE_GPSLATLNG)) {
                        node.addOrEditTag(curKey, latitude + "," + longitude);
                    } else if (curValue.equals(VALUE_GPSACCURACY)) {
                        node.addOrEditTag(curKey, accuracy + " m");
                    } else {
                        node.addOrEditTag(curKey, tags.get(curKey));
                    }
                }
            }

            ODKCollectData odkCollectData = new ODKCollectData(
                    formId,
                    formFileName,
                    instanceId,
                    instanceDir,
                    null,
                    new LinkedHashMap<String, ODKTag>());
            ODKCollectHandler.saveXmlInOdkCollect(odkCollectData, node, "odk_collect");
        }
    }
}
