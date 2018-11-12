package com.spatialdev.osm.indicators;

import android.content.Context;

import com.mapbox.mapboxsdk.R;
import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.lang.Math;

/**
 * Found / All Eligible
 */
public class MacepaFoundCoverageIndicator extends OSMIndicator {
    public static final String NAME = "MacepaFoundCoverageIndicator";

    public MacepaFoundCoverageIndicator(Context context, Map<String, Map<Long, OSMElement>> mappedData) {
        super(context, NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) throws IndicatorCalculationException {
        if (indicators.get(MacepaFoundIndicator.NAME) != null
                && indicators.get(MacepaAllEligibleIndicator.NAME) != null) {
            double denominator = indicators.get(MacepaAllEligibleIndicator.NAME).calculate(indicators);

            if (denominator == 0d) throw new IndicatorCalculationException("Division by 0 error");

            double val = indicators.get(MacepaFoundIndicator.NAME).calculate(indicators)
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
        return context.getResources().getString(R.string.indicatorMacepaFoundCoverage);
    }
}