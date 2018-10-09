package org.redcross.openmapkit;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.spatialdev.osm.indicators.OSMIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class IndicatorListAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private Map<String, OSMIndicator> indicators;
    private ArrayList<String> displayedIndicatorNames;

    public IndicatorListAdapter(Activity activity, Map<String, OSMIndicator> indicators) {

        this.indicators = indicators;

        displayedIndicatorNames = new ArrayList<>();
        for (String curName : getIndicatorsToDisplay()) {
            if (indicators.containsKey(curName) && indicators.get(curName) != null) {
                displayedIndicatorNames.add(curName);
            }
        }

        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    private ArrayList<String> getIndicatorsToDisplay() {
        return Settings.singleton().getIndicators();

    }

    /**
     * Implementing Adapter inherited abstract methods
     */
    public int getCount() {
        return displayedIndicatorNames.size();
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
        if (displayedIndicatorNames == null || displayedIndicatorNames.size() == 0) {
            return true;
        }
        return false;
    }

    /**
     * Implementing Adapter inherited abstract methods
     */
    public View getView(int position, View convertView, ViewGroup parent) {

        //create the view for an individual list view item ...

        View view = convertView;

        ViewHolder mViewHolder;
        if(convertView == null) {

            view = inflater.inflate(R.layout.taglistviewitem, null);

            mViewHolder = new ViewHolder();
            mViewHolder.textViewTagKey = (TextView)view.findViewById(R.id.textViewTagKey); //left side tag key
            mViewHolder.textViewTagValue = (TextView)view.findViewById(R.id.textViewTagValue); //left side tag value

            view.setTag(mViewHolder);
        }
        else {

            mViewHolder = (ViewHolder)view.getTag();
        }

        //tag key and value
        String indicatorName = displayedIndicatorNames.get(position);

        mViewHolder.textViewTagKey.setText(indicators.get(indicatorName).getTitle());
        mViewHolder.textViewTagValue.setText(indicators.get(indicatorName).getFormattedCalculation(indicators));

        return view;
    }

    static class ViewHolder{
        TextView textViewTagKey;
        TextView textViewTagValue;
    }
    
}


