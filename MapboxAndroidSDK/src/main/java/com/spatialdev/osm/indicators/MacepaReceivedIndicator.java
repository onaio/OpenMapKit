package com.spatialdev.osm.indicators;

import android.content.Context;

import com.mapbox.mapboxsdk.R;
import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.lang.Math;

/**
 * ('mda_status'='all_received' +'mda_status'='some_received')
 */
public class MacepaReceivedIndicator extends OSMIndicator {
    public static final String NAME = "MacepaReceivedIndicator";

    public MacepaReceivedIndicator(Context context, Map<String, Map<Long, OSMElement>> mappedData) {
        super(context, NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) throws IndicatorCalculationException {
        if (indicators.get(MacepaAllReceivedIndicator.NAME) != null
                && indicators.get(MacepaSomeReceivedIndicator.NAME) != null) {
            double val = indicators.get(MacepaAllReceivedIndicator.NAME).calculate(indicators)
                    + indicators.get(MacepaSomeReceivedIndicator.NAME).calculate(indicators);

            if (val < 0d) val = 0d;

            return val;
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
        return context.getResources().getString(R.string.indicatorMacepaReceived);
    }
}