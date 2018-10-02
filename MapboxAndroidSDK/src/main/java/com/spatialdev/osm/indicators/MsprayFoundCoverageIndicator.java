package com.spatialdev.osm.indicators;

import android.content.Context;

import com.mapbox.mapboxsdk.R;
import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.lang.Math;

/**
 * Found Coverage = Total Number of eligible structures "Found" / Total Number of eligible structures in Spray Area
 */
public class MsprayFoundCoverageIndicator extends OSMIndicator {
    public static final String NAME = "MsprayFoundCoverageIndicator";

    public MsprayFoundCoverageIndicator(Context context, Map<String, Map<Long, OSMElement>> mappedData) {
        super(context, NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) {
        if (indicators.get(MsprayEligibleFoundIndicator.NAME) != null
                && indicators.get(MsprayTotalEligibleIndicator.NAME) != null) {
            return indicators.get(MsprayEligibleFoundIndicator.NAME).calculate(indicators)
                    / indicators.get(MsprayTotalEligibleIndicator.NAME).calculate(indicators);
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
        return context.getResources().getString(R.string.indicatorMsprayFoundCoverage);
    }
}