package com.spatialdev.osm.indicators;

import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Number of structures "not sprayed" = Number of Unique structures "not sprayed"
 */
public class MsprayNotSprayedIndicator extends OSMIndicator {
    public static final String NAME = "MsprayNotSprayedIndicator";
    private static final String NOT_SPRAYED_VALUE = "notsprayed";

    public MsprayNotSprayedIndicator(Map<String, Map<Long, OSMElement>> mappedData) {
        super(NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) {
        if (mappedData.containsKey(NOT_SPRAYED_VALUE)
                && mappedData.get(NOT_SPRAYED_VALUE) != null) {
            return mappedData.get(NOT_SPRAYED_VALUE).size();
        }

        return 0d;
    }
}