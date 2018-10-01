package com.spatialdev.osm.indicators;

import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Spray Coverage = Number of structures "Sprayed" / Total Number of eligible structures "Found"
 */
public class MspraySprayCoverageIndicator extends OSMIndicator {
    public static final String NAME = "MspraySprayCoverageIndicator";

    public MspraySprayCoverageIndicator(Map<String, Map<Long, OSMElement>> mappedData) {
        super(NAME, mappedData);
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
}