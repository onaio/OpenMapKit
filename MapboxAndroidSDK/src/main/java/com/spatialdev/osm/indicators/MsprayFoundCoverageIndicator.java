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
    public double calculate(Map<String, OSMIndicator> indicators) throws IndicatorCalculationException {
        if (indicators.get(MsprayEligibleFoundIndicator.NAME) != null
                && indicators.get(MsprayTotalEligibleIndicator.NAME) != null) {
            double denominator = indicators.get(MsprayTotalEligibleIndicator.NAME).calculate(indicators);

            if (denominator == 0d) throw new IndicatorCalculationException("Division by 0 error");

            double val = indicators.get(MsprayEligibleFoundIndicator.NAME).calculate(indicators)
                    / denominator;

            if (val > 1d) val = 1d;

            return val;
        }

        return 0d;
    }

    @Override
    public String getFormattedCalculation(Map<String, OSMIndicator> indicators) {
        try {
            double percent = calculate(indicators) * 100;
            return String.valueOf(Math.round(percent)) + "%";
        } catch (IndicatorCalculationException e) {

        }

        return NULL_VALUE;
    }

    @Override
    public String getTitle() {
        return context.getResources().getString(R.string.indicatorMsprayFoundCoverage);
    }
}