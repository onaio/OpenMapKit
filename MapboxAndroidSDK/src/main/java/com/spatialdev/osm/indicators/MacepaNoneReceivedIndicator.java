package com.spatialdev.osm.indicators;

import android.content.Context;

import com.mapbox.mapboxsdk.R;
import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.lang.Math;

/**
 * All unique structures where MDA Status = 'none_received'
 */
public class MacepaNoneReceivedIndicator extends OSMIndicator {
    public static final String NAME = "MacepaNoneReceivedIndicator";
    private static final String NONE_RECEIVED_VALUE = "none_received";

    public MacepaNoneReceivedIndicator(Context context, Map<String, Map<Long, OSMElement>> mappedData) {
        super(context, NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) throws IndicatorCalculationException {
        if (mappedData.containsKey(NONE_RECEIVED_VALUE)
                && mappedData.get(NONE_RECEIVED_VALUE) != null) {
            return mappedData.get(NONE_RECEIVED_VALUE).size();
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
        return context.getResources().getString(R.string.indicatorMacepaNoneReceived);
    }
}