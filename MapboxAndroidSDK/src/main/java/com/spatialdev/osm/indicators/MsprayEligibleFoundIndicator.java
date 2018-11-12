package com.spatialdev.osm.indicators;

import android.content.Context;

import com.mapbox.mapboxsdk.R;
import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.lang.Math;

/**
 * Total Number of eligible structures "Found" = (Number of structures "Sprayed" + Number of
 * enumerated structures "Not Sprayed" + Number of duplicate structures "Sprayed") - Number of Not Sprayable Structures
 */
public class MsprayEligibleFoundIndicator extends OSMIndicator {
    public static final String NAME = "MsprayEligibleFoundIndicator";

    public MsprayEligibleFoundIndicator(Context context, Map<String, Map<Long, OSMElement>> mappedData) {
        super(context, NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) throws IndicatorCalculationException {
        if (indicators.get(MspraySprayedIndicator.NAME) != null
                && indicators.get(MsprayNotSprayedIndicator.NAME) != null
                && indicators.get(MsprayNotSprayableIndicator.NAME) != null) {
            double val = indicators.get(MspraySprayedIndicator.NAME).calculate(indicators)
                        + indicators.get(MsprayNotSprayedIndicator.NAME).calculate(indicators);

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
        return context.getResources().getString(R.string.indicatorMsprayEligibleFound);
    }
}