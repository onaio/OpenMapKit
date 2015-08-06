package org.redcross.openmapkit;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.spatialdev.osm.model.OSMElement;

import org.redcross.openmapkit.odkcollect.ODKCollectData;
import org.redcross.openmapkit.odkcollect.ODKCollectHandler;
import org.redcross.openmapkit.odkcollect.tag.ODKTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class TagListAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private Map<String, String> tagMap;
    private ArrayList<String> tagKeys;

    public TagListAdapter(Activity activity, OSMElement osmElement) {

        tagMap = new LinkedHashMap<>();

        if (ODKCollectHandler.isODKCollectMode()) {
            Map<String,String> tags = osmElement.getTags();
            Map<String, String> readOnlyTags = new LinkedHashMap<>(tags);
            ODKCollectData odkCollectData = ODKCollectHandler.getODKCollectData();
            Collection<ODKTag> requiredTags = odkCollectData.getRequiredTags();
            for (ODKTag odkTag : requiredTags) {
                String key = odkTag.getKey();
                String val = tags.get(key);
                tagMap.put(key, val);
                readOnlyTags.remove(key);
            }
            Set<String> readOnlyKeys = readOnlyTags.keySet();
            for (String readOnlyKey : readOnlyKeys) {
                tagMap.put(readOnlyKey, tags.get(readOnlyKey));
            }
        } else {
            tagMap = osmElement.getTags();
        }
        
        Set<String> keys = tagMap.keySet();
        tagKeys = new ArrayList<>();
        for(String key: keys) {
            tagKeys.add(key);
        }

        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * Implementing Adapter inherited abstract methods
     */
    public int getCount() {
        return tagMap.size();
    }

    /**
     * Implementing Adapter inherited abstract methods
     */
    public Object getItem(int arg0) {
        return null;
    }

    /**
     * Implementing Adapter inherited abstract methods
     */
    public long getItemId(int position) {
        return 0;
    }

    public boolean isEmpty() {
        if (tagMap == null || tagMap.size() == 0) {
            return true;
        }
        return false;
    }
    
    public String getTagKeyForIndex(int idx) {
        String[] keys = tagMap.keySet().toArray(new String[tagMap.size()]);
        return keys[idx];
    }
    
    /**
     * Implementing Adapter inherited abstract methods
     */
    public View getView(int position, View convertView, ViewGroup parent) {

        //create the view for an individual list view item ...

        View view = convertView;

        if (userVisibleTag(tagKeys.get(position))) {
            ViewHolder mViewHolder;
            if (convertView == null) {

                view = inflater.inflate(R.layout.taglistviewitem, null);

                mViewHolder = new ViewHolder();
                mViewHolder.textViewTagKey = (TextView) view.findViewById(R.id.textViewTagKey); //left side tag key
                mViewHolder.textViewTagValue = (TextView) view.findViewById(R.id.textViewTagValue); //left side tag value

                view.setTag(mViewHolder);
            } else {

                mViewHolder = (ViewHolder) view.getTag();
            }

            //tag key and value
            String currentTagKey = tagKeys.get(position);
            String currentTagValue = tagMap.get(currentTagKey);

            //labels for tag key and value
            String currentTagKeyLabel = null;
            String currentTagValueLabel = null;

            //attempt to assign labels for tag key and value if available from ODK Collect Mode
            if (ODKCollectHandler.isODKCollectMode()) {

                ODKCollectData odkCollectData = ODKCollectHandler.getODKCollectData();

                if (odkCollectData != null) {

                    try {

                        currentTagKeyLabel = odkCollectData.getTagKeyLabel(currentTagKey);
                        currentTagValueLabel = odkCollectData.getTagValueLabel(currentTagKey, currentTagValue);
                    } catch (Exception ex) {

                        Log.e("error", "exception raised when calling getTagKeyLabel or getTagValueLabel with tag key: '" + currentTagKey + "' and tag value: '" + currentTagValue + "'");
                    }
                }
            }

            //present tag key
            if (currentTagKeyLabel != null) {
                mViewHolder.textViewTagKey.setText(currentTagKeyLabel);
            } else {
                mViewHolder.textViewTagKey.setText(currentTagKey);
            }

            //present tag value
            if (currentTagValueLabel != null) {
                mViewHolder.textViewTagValue.setText(currentTagValueLabel);
            } else {
                mViewHolder.textViewTagValue.setText(currentTagValue);
            }
        } else {//Hide tags which should not be seen by user.
            view = inflater.inflate(R.layout.taglistviewnull, null);
        }

        return view;
    }

    static class ViewHolder{
        TextView textViewTagKey;
        TextView textViewTagValue;
    }

    private boolean userVisibleTag(String key) {
        if (key.equals(MapActivity.USER_LNG) || key.equals(MapActivity.USER_LAT)
                || key.equals(MapActivity.USER_ALT) || key.equals(MapActivity.GPS_ACCURACY)) {
            return false;
        }
        return true;
    }
    
}


