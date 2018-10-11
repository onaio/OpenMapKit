package com.spatialdev.osm.indicators;

import android.content.Context;

import com.mapbox.mapboxsdk.R;
import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.lang.Math;

/**
 * Total Number of eligible structures "Found" = (Total number of eligible structures in spray area x 90%) - Number of structures sprayed
 */
public class MsprayStructuresRequiredTo90PcIndicator extends OSMIndicator {
    public static final String NAME = "MsprayStructuresRequiredTo90PcIndicator";

    public MsprayStructuresRequiredTo90PcIndicator(Context context, Map<String, Map<Long, OSMElement>> mappedData) {
        super(context, NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) throws IndicatorCalculationException {
        if (indicators.get(MsprayTotalEligibleIndicator.NAME) != null
                && indicators.get(MspraySprayedIndicator.NAME) != null) {

            double val = (indicators.get(MsprayTotalEligibleIndicator.NAME).calculate(indicators) * 0.9)
                    - indicators.get(MspraySprayedIndicator.NAME).calculate(indicators);

            if (val < 0d) val = 0d;

            return val;
        }

        return 0d;
    }

    @Override
    public String getFormattedCalculation(Map<String, OSMIndicator> indicators) {
        try {
            double value = calculate(indicators);
            return String.valueOf(Math.round(value));
        } catch (IndicatorCalculationException e) {
        }

        return NULL_VALUE;
    }

    @Override
    public String getTitle() {
        return context.getResources().getString(R.string.indicatorMsprayStructuresRequiredTo90Pc);
    }
}