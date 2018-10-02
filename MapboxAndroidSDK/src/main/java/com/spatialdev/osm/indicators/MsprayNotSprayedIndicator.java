package com.spatialdev.osm.indicators;

import android.content.Context;

import com.mapbox.mapboxsdk.R;
import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.lang.Math;

/**
 * Number of structures "not sprayed" = Number of Unique structures "not sprayed"
 */
public class MsprayNotSprayedIndicator extends OSMIndicator {
    public static final String NAME = "MsprayNotSprayedIndicator";
    private static final String NOT_SPRAYED_VALUE = "notsprayed";

    public MsprayNotSprayedIndicator(Context context, Map<String, Map<Long, OSMElement>> mappedData) {
        super(context, NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) {
        if (mappedData.containsKey(NOT_SPRAYED_VALUE)
                && mappedData.get(NOT_SPRAYED_VALUE) != null) {
            return mappedData.get(NOT_SPRAYED_VALUE).size();
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
        return context.getResources().getString(R.string.indicatorMsprayNotSprayed);
    }
}