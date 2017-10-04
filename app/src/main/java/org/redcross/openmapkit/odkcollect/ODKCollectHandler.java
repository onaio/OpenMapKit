package org.redcross.openmapkit.odkcollect;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.spatialdev.osm.model.OSMElement;

import org.redcross.openmapkit.Constraints;
import org.redcross.openmapkit.ExternalStorage;
import org.redcross.openmapkit.Settings;
import org.redcross.openmapkit.odkcollect.tag.ODKTag;
import org.redcross.openmapkit.odkcollect.tag.ODKTagItem;
import org.redcross.openmapkit.tagswipe.TagEdit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Nicholas Hallahan on 2/9/15.
 * nhallahan@spatialdev.com
 * * * 
 */
public class ODKCollectHandler {

    private static ODKCollectData odkCollectData;
    
    public static void registerIntent(Context context, Intent intent) {
        String action = intent.getAction();
        if(action != null && (action.equals("android.intent.action.SEND")
                || action.equals("org.odk.collect.android.osm.action.GENERATE"))) {
            if (intent.getType().equals("text/plain")) {
                Bundle extras = intent.getExtras();
                if(extras != null) {
                    // extract data from intent extras
                    String formId = extras.getString("FORM_ID");
                    String formFileName = extras.getString("FORM_FILE_NAME");
                    String instanceId = extras.getString("INSTANCE_ID");
                    String instanceDir = extras.getString("INSTANCE_DIR");
                    String previousOSMEditFileName = extras.getString("OSM_EDIT_FILE_NAME");

                    if (action.equals("android.intent.action.SEND")) {
                        //initialize settings
                        ExternalStorage.copyAssetsFileOrDirToExternalStorage(context, ExternalStorage.SETTINGS_DIR);
                        Settings.initialize(context, formFileName);
                    }

                    LinkedHashMap<String, ODKTag> requiredTags = generateRequiredOSMTagsFromBundle(extras, action.equals("android.intent.action.SEND"));
                    odkCollectData = new ODKCollectData(formId,
                            formFileName,
                            instanceId,
                            instanceDir,
                            previousOSMEditFileName,
                            requiredTags);
                }
            }
        } else {
            ExternalStorage.copyAssetsFileOrDirToExternalStorage(context, ExternalStorage.SETTINGS_DIR);
            Settings.initialize(context);
        }
    }
    
    public static boolean isODKCollectMode() {
        if (odkCollectData != null) {
            return true;
        }
        return false;
    }
    
    public static boolean isStandaloneMode() {
        if (odkCollectData == null) {
            return true;
        }
        return false;
    }
    
    public static ODKCollectData getODKCollectData() {
        return odkCollectData;
    }

    /**
     * Saves an OSM Element as XML in ODK Collect.
     * * * 
     * @param el
     * @return The full path of the saved OSM XML File
     */
    public static String saveXmlInODKCollect(OSMElement el, String osmUserName) {
        return saveXmlInOdkCollect(odkCollectData, el, osmUserName);
    }

    /**
     * Saves an OSM Element as XML in ODK Collect.
     * * *
     * @param el
     * @return The full path of the saved OSM XML File
     */
    public static String saveXmlInOdkCollect(ODKCollectData odkCollectData, OSMElement el, String osmUserName) {
        try {
            odkCollectData.consumeOSMElement(el, osmUserName);
            odkCollectData.deleteOldOSMEdit();
            odkCollectData.writeXmlToOdkCollectInstanceDir();
            return odkCollectData.getOSMFileFullPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static LinkedHashMap<String, ODKTag> generateRequiredOSMTagsFromBundle(
            Bundle extras, boolean addUserLocationTags) {
        List<String> tagKeys = extras.getStringArrayList("TAG_KEYS");
        if (tagKeys == null || tagKeys.size() == 0) {
            return null;
        }
        LinkedHashMap<String, ODKTag> tags = new LinkedHashMap<>();
        for (String key : tagKeys) {
            ODKTag tag = new ODKTag();
            tags.put(key, tag);
            tag.setKey(key);
            String label = extras.getString("TAG_LABEL." + key);
            if (label != null) {
                tag.setLabel(label);
            }
            List<String> values = extras.getStringArrayList("TAG_VALUES." + key);
            if (values != null && values.size() > 0) {
                for (String value : values) {
                    ODKTagItem tagItem = new ODKTagItem();
                    tagItem.setValue(value);
                    String valueLabel = extras.getString("TAG_VALUE_LABEL." + key + "." + value);
                    if (valueLabel != null) {
                        tagItem.setLabel(valueLabel);
                    }
                    tag.addItem(tagItem);
                }
            }
        }

        if (addUserLocationTags) tags = addUserLocationTags(tags);

        return tags;
    }

    /**
     * This method adds the user location tags
     *
     * @param tags  All the tags without the user location tag
     * @return  List of all the tags, including the user location tags
     */
    public static LinkedHashMap<String, ODKTag> addUserLocationTags(LinkedHashMap<String, ODKTag> tags) {
        if(Settings.singleton().isUserLocationTagsEnabled()) {
            if(Settings.singleton().getUserLatLngName() != null) {
                ODKTag userLocation = new ODKTag();
                userLocation.setKey(Settings.singleton().getUserLatLngName());
                userLocation.setLabel(Settings.singleton().getUserLatLngLabel());
                tags.put(Settings.singleton().getUserLatLngName(), userLocation);
            }

            if(Settings.singleton().getUserAccuracyName() != null) {
                ODKTag userLocationAccuracy = new ODKTag();
                userLocationAccuracy.setKey(Settings.singleton().getUserAccuracyName());
                userLocationAccuracy.setLabel(Settings.singleton().getUserAccuracyLabel());
                tags.put(Settings.singleton().getUserAccuracyName(), userLocationAccuracy);
            }
        }
        return tags;
    }
}
