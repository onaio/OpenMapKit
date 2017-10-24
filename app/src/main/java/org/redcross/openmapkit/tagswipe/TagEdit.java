package org.redcross.openmapkit.tagswipe;

import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.spatialdev.osm.model.OSMElement;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

import org.redcross.openmapkit.Constraints;
import org.redcross.openmapkit.Settings;
import org.redcross.openmapkit.odkcollect.ODKCollectData;
import org.redcross.openmapkit.odkcollect.ODKCollectHandler;
import org.redcross.openmapkit.odkcollect.tag.ODKTag;
import org.redcross.openmapkit.odkcollect.tag.ODKTagItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Nicholas Hallahan on 3/4/15.
 * nhallahan@spatialdev.com
 * * *
 */
public class TagEdit {
    private static LinkedHashMap<String, TagEdit> tagEditHash;
    private static LinkedHashMap<String, TagEdit> tagEditHiddenHash;
    private static List<TagEdit> tagEdits;
    private static OSMElement osmElement;
    private static TagSwipeActivity tagSwipeActivity;
    
    private final String tagKey; // a given TagEdit always associates to an immutable key
    private String tagVal;
    private ODKTag odkTag;
    private boolean readOnly;
    private boolean checkBoxMode = false;
    private EditText editText;
    private RadioGroup radioGroup;

    /**
     * For CheckBox mode.
     */
    private CheckBox editTextCheckBox;
    private EditText checkBoxEditText;

    /**
     * Factory Method that gives collection of instances.
     *
     * @return
     */
    public static List<TagEdit> buildTagEdits() {
        tagEditHash = new LinkedHashMap<>();
        tagEditHiddenHash = new LinkedHashMap<>();
        tagEdits = new ArrayList<>();
        osmElement = OSMElement.getSelectedElements().getFirst();

        Map<String, String> tags = osmElement.getTags();
        
        // Tag Edits for ODK Collect Mode
        if (ODKCollectHandler.isODKCollectMode()) {
            Map<String, String> readOnlyTags = new HashMap<>(tags);
            ODKCollectData odkCollectData = ODKCollectHandler.getODKCollectData();
            Collection<ODKTag> requiredTags = odkCollectData.getRequiredTags();
            for (ODKTag odkTag : requiredTags) {
                String tagKey = odkTag.getKey();
                TagEdit tagEdit = new TagEdit(tagKey, tagValueOrDefaultValue(tags, tagKey), odkTag, false);
                String implicitVal = Constraints.singleton().implicitVal(tagKey);
                if (implicitVal != null) {
                    tagEditHiddenHash.put(tagKey, tagEdit);
                    osmElement.addOrEditTag(tagKey, implicitVal);
                } else if (Constraints.singleton().tagShouldBeShown(tagKey, osmElement)) {
                    tagEditHash.put(tagKey, tagEdit);
                    tagEdits.add(tagEdit);
                } else {
                    tagEditHiddenHash.put(tagKey, tagEdit);
                }
                readOnlyTags.remove(tagKey);
            }
            Set<String> readOnlyKeys = readOnlyTags.keySet();
            for (String readOnlyKey : readOnlyKeys) {
                TagEdit tagEdit = new TagEdit(readOnlyKey, readOnlyTags.get(readOnlyKey), true);
                if(Constraints.singleton().tagIsHidden(readOnlyKey) == true) {
                    tagEditHiddenHash.put(readOnlyKey, tagEdit);
                } else {
                    tagEditHash.put(readOnlyKey, tagEdit);
                    tagEdits.add(tagEdit);
                }
            }
        }
        
        // Tag Edits for Standalone Mode
        else {
            Set<String> keys = tags.keySet();
            for (String key : keys) {
                TagEdit tagEdit = new TagEdit(key, tags.get(key), false);
                tagEditHash.put(key, tagEdit);
                tagEdits.add(tagEdit);
            }
        }

        cleanUserLocationTags();

        return tagEdits;
    }

    /**
     * This method is intended for use in only tests. It repopulates the tagEditHash hashMap with
     * data. The data includes user location tags
     */
    public static void mockTagEditHash() {
        tagEditHash = new LinkedHashMap<>();
        tagEditHash.put("test_tag_1", new TagEdit("test_tag_1", "test", new ODKTag(), false));
        tagEditHash.put("test_tag_2", new TagEdit("test_tag_2", "test", new ODKTag(), true));
        tagEditHash.put("test_tag_3", new TagEdit("test_tag_3", null, new ODKTag(), false));
        tagEditHash.put("test_tag_4", new TagEdit("test_tag_4", null, new ODKTag(), true));
        tagEditHash.put("test_tag_5", new TagEdit("test_tag_5", "test", false));
        tagEditHash.put("test_tag_6", new TagEdit("test_tag_6", null, false));
        tagEditHash.put("test_tag_7", new TagEdit("test_tag_7", "test", true));
        tagEditHash.put("test_tag_8", new TagEdit("test_tag_8", null, true));

        tagEditHiddenHash = new LinkedHashMap<>();
        tagEditHiddenHash.put(Settings.singleton().getUserLatLngName(), new TagEdit(Settings.singleton().getUserLatLngName(), "-1.22321132,36.32234233", true));
        tagEditHiddenHash.put(Settings.singleton().getUserAccuracyName(), new TagEdit(Settings.singleton().getUserAccuracyName(), distanceToString(32f), true));
        tagEditHiddenHash.put(Settings.singleton().getUserDistanceName(), new TagEdit(Settings.singleton().getUserDistanceName(), distanceToString(8), true));
    }

    /**
     * This method is intended for use in only tests. It repopulates the tagEditHash hashMap with
     * data. The data does not include user location tags
     */
    public static void mockTagEditHashWithoutUserLocTags() {
        tagEditHash = new LinkedHashMap<>();
        tagEditHash.put("test_tag_1", new TagEdit("test_tag_1", "test", new ODKTag(), false));
        tagEditHash.put("test_tag_2", new TagEdit("test_tag_2", "test", new ODKTag(), true));
        tagEditHash.put("test_tag_3", new TagEdit("test_tag_3", null, new ODKTag(), false));
        tagEditHash.put("test_tag_4", new TagEdit("test_tag_4", null, new ODKTag(), true));
        tagEditHash.put("test_tag_5", new TagEdit("test_tag_5", "test", false));
        tagEditHash.put("test_tag_6", new TagEdit("test_tag_6", null, false));
        tagEditHash.put("test_tag_7", new TagEdit("test_tag_7", "test", true));
        tagEditHash.put("test_tag_8", new TagEdit("test_tag_8", null, true));

        tagEditHiddenHash = new LinkedHashMap<>();
    }

    /**
     * This method is intended for use in only tests. It repopulates the tagEditHash hashMap with
     * data. The data includes user location tags that have null values
     */
    public static void mockTagEditHashWithNullUserLocTags() {
        tagEditHash = new LinkedHashMap<>();
        tagEditHash.put("test_tag_1", new TagEdit("test_tag_1", "test", new ODKTag(), false));
        tagEditHash.put("test_tag_2", new TagEdit("test_tag_2", "test", new ODKTag(), true));
        tagEditHash.put("test_tag_3", new TagEdit("test_tag_3", null, new ODKTag(), false));
        tagEditHash.put("test_tag_4", new TagEdit("test_tag_4", null, new ODKTag(), true));
        tagEditHash.put("test_tag_5", new TagEdit("test_tag_5", "test", false));
        tagEditHash.put("test_tag_6", new TagEdit("test_tag_6", null, false));
        tagEditHash.put("test_tag_7", new TagEdit("test_tag_7", "test", true));
        tagEditHash.put("test_tag_8", new TagEdit("test_tag_8", null, true));

        tagEditHiddenHash = new LinkedHashMap<>();
        tagEditHiddenHash.put(Settings.singleton().getUserLatLngName(), new TagEdit(Settings.singleton().getUserLatLngName(), null, true));
        tagEditHiddenHash.put(Settings.singleton().getUserAccuracyName(), new TagEdit(Settings.singleton().getUserAccuracyName(), "", true));
        tagEditHiddenHash.put(Settings.singleton().getUserDistanceName(), new TagEdit(Settings.singleton().getUserDistanceName(), "", true));
    }

    /**
     * This method cleans the values in the user location and user location accuracy tags
     */
    public static void cleanUserLocationTags() {
        if(Settings.singleton().getUserLatLngName() != null) {
            if(tagEditHiddenHash.containsKey(Settings.singleton().getUserLatLngName())) {
                tagEditHiddenHash.get(Settings.singleton().getUserLatLngName()).tagVal = null;
                if(tagEditHiddenHash.get(Settings.singleton().getUserLatLngName()).editText != null) {
                    tagEditHiddenHash.get(Settings.singleton().getUserLatLngName()).editText.setText(null);
                }
            }
        }

        if(Settings.singleton().getUserAccuracyName() != null) {
            if(tagEditHiddenHash.containsKey(Settings.singleton().getUserAccuracyName())) {
                tagEditHiddenHash.get(Settings.singleton().getUserAccuracyName()).tagVal = null;
                if(tagEditHiddenHash.get(Settings.singleton().getUserAccuracyName()).editText != null) {
                    tagEditHiddenHash.get(Settings.singleton().getUserAccuracyName()).editText.setText(null);
                }
            }
        }

        if(Settings.singleton().getUserDistanceName() != null) {
            if(tagEditHiddenHash.containsKey(Settings.singleton().getUserDistanceName())) {
                tagEditHiddenHash.get(Settings.singleton().getUserDistanceName()).tagVal = null;
                if(tagEditHiddenHash.get(Settings.singleton().getUserDistanceName()).editText != null) {
                    tagEditHiddenHash.get(Settings.singleton().getUserDistanceName()).editText.setText(null);
                }
            }
        }
    }

    /**
     * This method update the values for the user location tags
     *
     * @param location  The location object to be used to update the user's location and location
     *                  accuracy
     * @see Location
     */
    public static void updateUserLocationTags(Location location, double distance) {
        if(location != null) {
            tagSwipeActivity.hideGpsSearchingProgressDialog();

            if(Settings.singleton().getUserLatLngName() != null) {
                if (tagEditHiddenHash.containsKey(Settings.singleton().getUserLatLngName())) {
                    tagEditHiddenHash.get(Settings.singleton().getUserLatLngName()).tagVal = locationToString(location);
                }
            }

            if(Settings.singleton().getUserAccuracyName() != null) {
                if (tagEditHiddenHash.containsKey(Settings.singleton().getUserAccuracyName())) {
                    tagEditHiddenHash.get(Settings.singleton().getUserAccuracyName()).tagVal = distanceToString(location.getAccuracy());
                }
            }

            if(Settings.singleton().getUserDistanceName() != null) {
                if (tagEditHiddenHash.containsKey(Settings.singleton().getUserDistanceName())) {
                    Log.d("userdistance", "Distance is " + distance);
                    tagEditHiddenHash.get(Settings.singleton().getUserDistanceName()).tagVal = distanceToString(distance);
                }
            }
        }
    }

    /**
     * This method converts a location object to a string containing the latitude and longitude
     * (separated using a comma i.e latitude,longitude)
     *
     * @param location  The location to be converted
     * @return  String representing the latitude and longitude for the location or NULL if location
     *          is NULL
     * @see Location
     */
    public static String locationToString(Location location) {
        if(location != null) {
            return String.valueOf(location.getLatitude())+","+String.valueOf(location.getLongitude());
        }
        return null;
    }

    public static String distanceToString(double accuracy) {
        return String.valueOf((float) accuracy) + " m";
    }

    private static String tagValueOrDefaultValue(Map<String,String> tags, String tagKey) {
        String tagVal = tags.get(tagKey);
        if (tagVal == null) {
            // null if there is no default
            tagVal = Constraints.singleton().tagDefaultValue(tagKey);
            if (tagVal != null) {
                osmElement.addOrEditTag(tagKey, tagVal);
            }
        }
        return tagVal;
    }

    public static void setTagSwipeActivity(TagSwipeActivity tagSwipeActivity) {
        TagEdit.tagSwipeActivity = tagSwipeActivity;
    }
    
    public static TagEdit getTag(int idx) {
        return tagEdits.get(idx);
    }
    
    public static TagEdit getTag(String key) {
        if(tagEditHash != null && tagEditHash.containsKey(key)) {
            return tagEditHash.get(key);
        }

        return null;
    }

    public static TagEdit getHiddenTag(String key) {
        if(tagEditHiddenHash != null && tagEditHiddenHash.containsKey(key)) {
            return tagEditHiddenHash.get(key);
        }

        return null;
    }

    public static Set<String> hiddenTagKeys() {
        return tagEditHash.keySet();
    }

    public static Set<String> shownTagKeys() {
        return tagEditHiddenHash.keySet();
    }

    public static int getIndexForTagKey(String key) {
        TagEdit tagEdit = tagEditHash.get(key);
        if (tagEdit != null) {
            return tagEdits.indexOf(tagEdit);
        }
        // If its not there, just go to the first TagEdit
        return 0;
    }

    /**
     * This method checks whether the user location and user location accuracy tags have been set
     *
     * @return  TRUE if the user location and user location accuracy tags have been set
     */
    public static boolean checkUserLocationTags() {
        if(Settings.singleton().isUserLocationTagsEnabled()) {
            if(Settings.singleton().getUserLatLngName() != null) {//lat_lng required
                if(!tagEditHiddenHash.containsKey(Settings.singleton().getUserLatLngName())
                        || tagEditHiddenHash.get(Settings.singleton().getUserLatLngName()).tagVal == null
                        || tagEditHiddenHash.get(Settings.singleton().getUserLatLngName()).tagVal.length() == 0) {
                    return false;
                }
            }

            if(Settings.singleton().getUserAccuracyName() != null) {//accuracy required
                if(!tagEditHiddenHash.containsKey(Settings.singleton().getUserAccuracyName())
                        || tagEditHiddenHash.get(Settings.singleton().getUserAccuracyName()).tagVal == null
                        || tagEditHiddenHash.get(Settings.singleton().getUserAccuracyName()).tagVal.length() == 0) {
                    return false;
                }
            }

            if(Settings.singleton().getUserDistanceName() != null) {//distance required
                if(!tagEditHiddenHash.containsKey(Settings.singleton().getUserDistanceName())
                        || tagEditHiddenHash.get(Settings.singleton().getUserDistanceName()).tagVal == null
                        || tagEditHiddenHash.get(Settings.singleton().getUserDistanceName()).tagVal.length() == 0) {
                    return false;
                }
            }
        }

        return true;
    }
    
    public static boolean saveToODKCollect(String osmUserName) {
        updateTagsInOSMElement();

        Set<String> missingTags = Constraints.singleton().requiredTagsNotMet(osmElement);
        tagSwipeActivity.setOsmFilePath(null);
        if (missingTags.size() > 0) {
            tagSwipeActivity.notifyMissingTags(missingTags);
            return false;
        } else if(checkUserLocationTags() == false) {
            tagSwipeActivity.notifyMissingUserLocation();
            return false;
        } else {
            tagSwipeActivity.setOsmFilePath(
                    ODKCollectHandler.saveXmlInODKCollect(osmElement, osmUserName));
            return true;
        }
    }

    private static void removeTag(String key, String activeTagKey) {
        if (tagEditHash.get(key) == null) return;
        int idx = getIndexForTagKey(key);
        TagEdit tagEdit = tagEditHash.remove(key);
        tagEditHiddenHash.put(key, tagEdit);
        tagEdits.remove(idx);
        if (tagSwipeActivity != null) {
            tagSwipeActivity.updateUI(activeTagKey);
        }
    }

    private static void addTag(String key, String activeTagKey) {
        if (tagEditHiddenHash.get(key) == null) return;
        int idx = getIndexForTagKey(activeTagKey) + 1;
        TagEdit tagEdit = tagEditHiddenHash.remove(key);
        tagEditHash.put(key, tagEdit);
        tagEdits.add(idx, tagEdit);
        if (tagSwipeActivity != null) {
            tagSwipeActivity.updateUI(activeTagKey);
        }
    }
    
    private static void updateTagsInOSMElement() {
        Collection<ODKTag> requiredTags = ODKCollectHandler.getODKCollectData().getRequiredTags();
        ArrayList<String> requiredTagNames = new ArrayList<>();
        for (ODKTag curTag : requiredTags) {
            requiredTagNames.add(curTag.getKey());
        }

        for (TagEdit tagEdit : tagEdits) {
            tagEdit.updateTagInOSMElement();
        }

        if(Settings.singleton().isUserLocationTagsEnabled()) {
            if(Settings.singleton().getUserLatLngName() != null
                    && tagEditHiddenHash.containsKey(Settings.singleton().getUserLatLngName())) {
                requiredTagNames.remove(Settings.singleton().getUserLatLngName());
                tagEditHiddenHash.get(Settings.singleton().getUserLatLngName()).updateTagInOSMElement();
            }
            if(Settings.singleton().getUserAccuracyName() != null
                    && tagEditHiddenHash.containsKey(Settings.singleton().getUserAccuracyName())) {
                requiredTagNames.remove(Settings.singleton().getUserAccuracyName());
                tagEditHiddenHash.get(Settings.singleton().getUserAccuracyName()).updateTagInOSMElement();
            }
            if(Settings.singleton().getUserDistanceName() != null
                    && tagEditHiddenHash.containsKey(Settings.singleton().getUserDistanceName())) {
                requiredTagNames.remove(Settings.singleton().getUserDistanceName());
                tagEditHiddenHash.get(Settings.singleton().getUserDistanceName()).updateTagInOSMElement();
            }
        }

        // set all hidden tags that are supposed to be collected but hidden to null
        if (tagEditHiddenHash != null) {
            for (TagEdit curTagEdit : tagEditHiddenHash.values()) {
                if (requiredTagNames.contains(curTagEdit.getTagKey())) {
                    curTagEdit.getOsmElement().addOrEditTag(curTagEdit.getTagKey(), "");
                }
            }
        }
    }
    
    private TagEdit(String tagKey, String tagVal, ODKTag odkTag, boolean readOnly) {
        this.tagKey = tagKey;
        this.tagVal = tagVal;
        this.odkTag = odkTag;
        this.readOnly = readOnly;
    }
    
    private TagEdit(String tagKey, String tagVal, boolean readOnly) {
        this.tagKey = tagKey;
        this.tagVal = tagVal;
        this.readOnly = readOnly;
    }

    /**
     * The EditText widget from StringTagValueFragment gets passed into here
     * so that the value can be retrieved on save.
     * * * *
     * @param editText
     */
    public void setEditText(EditText editText) {
        this.editText = editText;
    }
    
    public void setRadioGroup(RadioGroup radioGroup) {
        this.radioGroup = radioGroup;
    }

    public void setupEditCheckbox(CheckBox cb, EditText et) {
        checkBoxMode = true;
        editTextCheckBox = cb;
        checkBoxEditText = et;
    }
    
    public ODKTag getODKTag() {
        return odkTag;
    }
    
    public void updateTagInOSMElement() {
        // check boxes
        if (odkTag != null && checkBoxMode) {
            boolean editTextCheckBoxChecked = editTextCheckBox.isChecked();
            if (odkTag.hasCheckedTagValues() || editTextCheckBoxChecked) {
                if (editTextCheckBoxChecked) {
                    tagVal = odkTag.getSemiColonDelimitedTagValues(checkBoxEditText.getText().toString());
                } else {
                    tagVal = odkTag.getSemiColonDelimitedTagValues(null);
                }
                addOrEditTag(tagKey, tagVal);
            } else {
                deleteTag(tagKey);
            }
            return;
        }
        // radio buttons
        if (radioGroup != null && odkTag != null) {
            View v = radioGroup.getChildAt(radioGroup.getChildCount() - 1);
            int checkedId = radioGroup.getCheckedRadioButtonId();
            // has custom value input
            if (v instanceof LinearLayout) {
                LinearLayout customLL = (LinearLayout)v;
                RadioButton customRadio = (RadioButton)customLL.getChildAt(0);
                if (customRadio.isChecked()) {
                    EditText et = (EditText)customLL.getChildAt(1);
                    tagVal = et.getText().toString();
                    addOrEditTag(tagKey, tagVal);
                } else if (checkedId != -1) {
                    tagVal = odkTag.getTagItemValueFromButtonId(checkedId);
                    addOrEditTag(tagKey, tagVal);
                } else {
                    deleteTag(tagKey);
                }
            }
            // no custom value input
            else {
                if (checkedId != -1) {
                    tagVal = odkTag.getTagItemValueFromButtonId(checkedId);
                    addOrEditTag(tagKey, tagVal);
                } else {
                    deleteTag(tagKey);
                }
            }
        }
        // edit text
        else if (editText != null) {
            tagVal = editText.getText().toString();
            addOrEditTag(tagKey, tagVal);
        }
        else if(tagKey.equals(Settings.singleton().getUserLatLngName())
                || tagKey.equals(Settings.singleton().getUserAccuracyName())
                || tagKey.equals(Settings.singleton().getUserDistanceName())) {
            if(tagVal != null) {
                addOrEditTag(tagKey, tagVal);
            } else {
                addOrEditTag(tagKey, "");
            }
        }
    }

    private void addOrEditTag(String tagKey, String tagVal) {
        osmElement.addOrEditTag(tagKey, tagVal);
        Constraints.TagAction tagAction = Constraints.singleton().tagAddedOrEdited(tagKey, tagVal);
        executeTagAction(tagAction);
    }

    private void deleteTag(String tagKey) {
        osmElement.deleteTag(tagKey);
        tagVal = null;
        Constraints.TagAction tagAction = Constraints.singleton().tagDeleted(tagKey);
        executeTagAction(tagAction);
    }

    private void executeTagAction(Constraints.TagAction tagAction) {
        for (String tag : tagAction.hide) {
            removeTag(tag, tagKey);
        }
        for (String tag : tagAction.show) {
            addTag(tag, tagKey);
        }
    }
    
    public String getTitle() {
        return tagKey;
    }
    
    public String getTagKeyLabel() {
        if (odkTag != null) {
            return odkTag.getLabel();
        }
        return null;
    }

    public String getTagKey() {
        return tagKey;
    }
    
    public String getTagValLabel() {
        if (odkTag == null) return null;
        ODKTagItem item = odkTag.getItem(tagVal);
        if (item != null) {
            return item.getLabel();
        }
        return null;
    }
    
    public String getTagVal() {
        return tagVal;
    }

    public Set<String> getTagVals() {
        Set<String> tagVals = new HashSet<>();
        if (tagVal == null || tagVal.length() < 1) {
            return tagVals;
        }
        String[] vals = tagVal.trim().split(";");
        for (int i = 0; i < vals.length; i++) {
            String val = vals[i];
            tagVals.add(val);
        }
        return tagVals;
    }
    
    public boolean isReadOnly() {
        return readOnly;
    }
    
    public boolean isSelectOne() {
        return  !readOnly &&
                odkTag != null &&
                odkTag.getItems().size() > 0 &&
                !Constraints.singleton().tagIsSelectMultiple(odkTag.getKey());
    }

    public boolean isSelectMultiple() {
        return  !readOnly &&
                odkTag != null &&
                odkTag.getItems().size() > 0 &&
                Constraints.singleton().tagIsSelectMultiple(odkTag.getKey());
    }

    public static OSMElement getOsmElement() {
        return osmElement;
    }
    
}
