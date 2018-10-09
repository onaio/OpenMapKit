package com.spatialdev.osm.indicators;

import android.content.Context;

import com.mapbox.mapboxsdk.R;
import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.lang.Math;

/**
 * Total Number of eligible structures in Spray Area = (Number of structures enumerated in Spray Area + New Structures) - Number of "Not Sprayable" Structures
 */
public class MsprayTotalEligibleIndicator extends OSMIndicator {
    public static final String NAME = "MsprayTotalEligibleIndicator";

    public MsprayTotalEligibleIndicator(Context context, Map<String, Map<Long, OSMElement>> mappedData) {
        super(context, NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) throws IndicatorCalculationException {
        if (indicators.get(TotalIndicator.NAME) != null
                && indicators.get(MsprayNotSprayableIndicator.NAME) != null) {
            double val = indicators.get(TotalIndicator.NAME).calculate(indicators)
                    - indicators.get(MsprayNotSprayableIndicator.NAME).calculate(indicators);

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
        return context.getResources().getString(R.string.indicatorMsprayTotalEligible);
    }
}