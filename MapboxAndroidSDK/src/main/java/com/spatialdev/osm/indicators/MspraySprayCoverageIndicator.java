package com.spatialdev.osm.indicators;

import android.content.Context;

import com.mapbox.mapboxsdk.R;
import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.lang.Math;

/**
 * Spray Coverage = Number of structures "Sprayed" / Total Number of eligible structures "Found"
 */
public class MspraySprayCoverageIndicator extends OSMIndicator {
    public static final String NAME = "MspraySprayCoverageIndicator";

    public MspraySprayCoverageIndicator(Context context, Map<String, Map<Long, OSMElement>> mappedData) {
        super(context, NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) {
        if (indicators.get(MspraySprayedIndicator.NAME) != null
                && indicators.get(MsprayEligibleFoundIndicator.NAME) != null) {
            return indicators.get(MspraySprayedIndicator.NAME).calculate(indicators)
                    / indicators.get(MsprayEligibleFoundIndicator.NAME).calculate(indicators);
        }

        return 0d;
    }

    @Override
    public String getFormattedCalculation(Map<String, OSMIndicator> indicators) {
        double percent = calculate(indicators) * 100;
        return String.valueOf(Math.round(percent)) + "%";
    }

    @Override
    public String getTitle() {
        return context.getResources().getString(R.string.indicatorMspraySprayCoverage);
    }
}