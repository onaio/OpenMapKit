package com.spatialdev.osm.indicators;

import android.content.Context;

import com.mapbox.mapboxsdk.R;
import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.lang.Math;

public class TotalIndicator extends OSMIndicator {
    public static final String NAME = "TotalIndicator";

    public TotalIndicator(Context context, Map<String, Map<Long, OSMElement>> mappedData) {
        super(context, NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) {
        double total = 0d;
        for (String curMapKey : mappedData.keySet()) {
            if (mappedData.get(curMapKey) != null) {
                total = total + mappedData.get(curMapKey).size();
            }
        }

        return total;
    }

    @Override
    public String getFormattedCalculation(Map<String, OSMIndicator> indicators) {
        double calculation = calculate(indicators);
        return String.valueOf(Math.round(calculation));
    }

    @Override
    public String getTitle() {
        return context.getResources().getString(R.string.indicatorTotalStructures);
    }
}