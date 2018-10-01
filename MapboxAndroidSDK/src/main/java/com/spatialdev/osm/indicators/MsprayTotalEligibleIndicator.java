package com.spatialdev.osm.indicators;

import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Total Number of eligible structures in Spray Area = (Number of structures enumerated in Spray Area + New Structures) - Number of "Not Sprayable" Structures
 */
public class MsprayTotalEligibleIndicator extends OSMIndicator {
    public static final String NAME = "MsprayTotalEligibleIndicator";

    public MsprayTotalEligibleIndicator(Map<String, Map<Long, OSMElement>> mappedData) {
        super(NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) {
        if (indicators.get(TotalIndicator.NAME) != null
                && indicators.get(MsprayNotSprayableIndicator.NAME) != null) {
            return indicators.get(TotalIndicator.NAME).calculate(indicators)
                    - indicators.get(MsprayNotSprayableIndicator.NAME).calculate(indicators);
        }

        return 0d;
    }
}