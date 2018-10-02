package com.spatialdev.osm.indicators;

import android.content.Context;

import com.mapbox.mapboxsdk.R;
import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.lang.Math;

/**
 * Number of structures "not sprayable" = Number of Unique structures "not eligible"
 */
public class MsprayNotSprayableIndicator extends OSMIndicator {
    public static final String NAME = "MsprayNotSprayableIndicator";
    private static final String NOT_SPRAYABLE_VALUE = "noteligible";

    public MsprayNotSprayableIndicator(Context context, Map<String, Map<Long, OSMElement>> mappedData) {
        super(context, NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) {
        if (mappedData.containsKey(NOT_SPRAYABLE_VALUE)
                && mappedData.get(NOT_SPRAYABLE_VALUE) != null) {
            return mappedData.get(NOT_SPRAYABLE_VALUE).size();
        }

        return 0d;
    }

    @Override
    public String getFormattedCalculation(Map<String, OSMIndicator> indicators) {
        double calculation = calculate(indicators);
        return String.valueOf(Math.round(calculation));
    }

    @Override
    public String getTitle() {
        return context.getResources().getString(R.string.indicatorMsprayNotSprayable);
    }
}