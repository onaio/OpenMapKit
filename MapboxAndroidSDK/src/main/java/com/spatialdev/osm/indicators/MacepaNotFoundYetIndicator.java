package com.spatialdev.osm.indicators;

import android.content.Context;

import com.mapbox.mapboxsdk.R;
import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.lang.Math;

/**
 * All unique structures where MDA Status = ''
 */
public class MacepaNotFoundYetIndicator extends OSMIndicator {
    public static final String NAME = "MacepaNotFoundYetIndicator";
    private static final String NOT_FOUND_YET_VALUE = "";

    public MacepaNotFoundYetIndicator(Context context, Map<String, Map<Long, OSMElement>> mappedData) {
        super(context, NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) throws IndicatorCalculationException {
        if (mappedData.containsKey(NOT_FOUND_YET_VALUE)
                && mappedData.get(NOT_FOUND_YET_VALUE) != null) {
            return mappedData.get(NOT_FOUND_YET_VALUE).size();
        }

        return 0d;
    }

    @Override
    public String getFormattedCalculation(Map<String, OSMIndicator> indicators) {
        try {
            double calculation = calculate(indicators);
            return String.valueOf(Math.round(calculation));
        } catch (IndicatorCalculationException e) {

        }

        return NULL_VALUE;
    }

    @Override
    public String getTitle() {
        return context.getResources().getString(R.string.indicatorMacepaNotFoundYet);
    }
}