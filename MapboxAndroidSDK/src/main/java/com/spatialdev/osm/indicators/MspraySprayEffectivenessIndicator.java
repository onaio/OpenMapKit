package com.spatialdev.osm.indicators;

import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Spray Effectiveness = Number of structures "Sprayed" / Total Number of eligible structures in Spray Area
 */
public class MspraySprayEffectivenessIndicator extends OSMIndicator {
    public static final String NAME = "MspraySprayEffectivenessIndicator";

    public MspraySprayEffectivenessIndicator(Map<String, Map<Long, OSMElement>> mappedData) {
        super(NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) {
        if (indicators.get(MspraySprayedIndicator.NAME) != null
                && indicators.get(MsprayTotalEligibleIndicator.NAME) != null) {
            return indicators.get(MspraySprayedIndicator.NAME).calculate(indicators)
                    / indicators.get(MsprayTotalEligibleIndicator.NAME).calculate(indicators);
        }

        return 0d;
    }
}