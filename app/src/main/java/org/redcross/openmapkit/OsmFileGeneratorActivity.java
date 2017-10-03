package org.redcross.openmapkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spatialdev.osm.model.OSMElement;
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
    public static final String KEY_GPS = "gps";
    public static final String VALUE_GPSLATLNG = "value:gps_latlng";
    public static final String VALUE_GPSACCURACY = "value:gps_accuracy";
    private static final Pattern ODK_GPS_PATTERN = Pattern.compile(
            "([\\-\\+]?[\\d\\.]+)\\s+([\\-\\+]?[\\d\\.]+)\\s+([\\d\\.]+)\\s+([\\d\\.]+)");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_osm_generator);
        if (getIntent().getExtras() != null) {
            extractData();
        }
    }

    private void extractData() {
        Intent intent = getIntent();
        ODKCollectHandler.registerIntent(getApplicationContext(), intent);
        LinkedHashMap<String, ODKTag> requiredTags = ODKCollectHandler
                .generateRequiredOSMTagsFromBundle(intent.getExtras(), false);
        constructOsmFile(requiredTags);
    }

    private void constructOsmFile(LinkedHashMap<String, ODKTag> requiredTags) {
        String latitude = null;
        String longitude = null;
        String accuracy = null;
        if (requiredTags.containsKey(KEY_GPS)) {
            String gps = requiredTags.get(KEY_GPS).getLabel();
            Matcher gpsMatcher = ODK_GPS_PATTERN.matcher(gps);
            if (gpsMatcher.find()) {
                latitude = gpsMatcher.group(1);
                longitude = gpsMatcher.group(2);
                accuracy = gpsMatcher.group(4);
            }
            requiredTags.remove(KEY_GPS);
        }

        if (!TextUtils.isEmpty(latitude) && !TextUtils.isEmpty(longitude)) {
            LatLng latLng = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
            OSMNode node = new OSMNode(latLng, null);

            for (String curKey : requiredTags.keySet()) {
                String curValue = requiredTags.get(curKey).getLabel();
                if (!TextUtils.isEmpty(curValue)) {
                    if (curValue.contains(VALUE_GPSLATLNG)) {
                        node.addOrEditTag(curKey, latitude + "," + longitude);
                    } else if (curValue.contains(VALUE_GPSACCURACY)) {
                        node.addOrEditTag(curKey, accuracy + " m");
                    } else {
                        node.addOrEditTag(curKey, curValue);
                    }
                }
            }


            ODKCollectHandler.saveXmlInODKCollect(node, "odk_collect");

            final String osmXmlFileFullPath = ODKCollectHandler.getODKCollectData().getOSMFileFullPath();
            final String osmXmlFileName = ODKCollectHandler.getODKCollectData().getOSMFileName();
            if(osmXmlFileName != null && !osmXmlFileName.equals("null.osm")) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("OSM_PATH", osmXmlFileFullPath);
                resultIntent.putExtra("OSM_FILE_NAME", osmXmlFileName);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } else {
                Intent resultIntent = new Intent();
                Toast.makeText(this, String.format(getResources().getString(R.string.an_error_occurred_retag), Settings.singleton().getNodeName()), Toast.LENGTH_LONG).show();
                setResult(Activity.RESULT_CANCELED, resultIntent);
                finish();
            }
        }
    }
}
