package com.spatialdev.osm.indicators;

import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Total Number of eligible structures "Found" = (Number of structures "Sprayed" + Number of
 * enumerated structures "Not Sprayed" + Number of duplicate structures "Sprayed") - Number of Not Sprayable Structures
 */
public class MsprayEligibleFoundIndicator extends OSMIndicator {
    public static final String NAME = "MsprayEligibleFoundIndicator";

    public MsprayEligibleFoundIndicator(Map<String, Map<Long, OSMElement>> mappedData) {
        super(NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) {
        if (indicators.get(MspraySprayedIndicator.NAME) != null
                && indicators.get(MsprayNotSprayedIndicator.NAME) != null
                && indicators.get(MsprayNotSprayableIndicator.NAME) != null) {
            return (indicators.get(MspraySprayedIndicator.NAME).calculate(indicators)
                        + indicators.get(MsprayNotSprayedIndicator.NAME).calculate(indicators))
                    - indicators.get(MsprayNotSprayableIndicator.NAME).calculate(indicators);
        }

        return 0d;
    }
}