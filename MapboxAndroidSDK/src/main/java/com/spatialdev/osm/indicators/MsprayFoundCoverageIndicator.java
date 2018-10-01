package com.spatialdev.osm.indicators;

import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Found Coverage = Total Number of eligible structures "Found" / Total Number of eligible structures in Spray Area
 */
public class MsprayFoundCoverageIndicator extends OSMIndicator {
    public static final String NAME = "MsprayFoundCoverageIndicator";

    public MsprayFoundCoverageIndicator(Map<String, Map<Long, OSMElement>> mappedData) {
        super(NAME, mappedData);
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
}