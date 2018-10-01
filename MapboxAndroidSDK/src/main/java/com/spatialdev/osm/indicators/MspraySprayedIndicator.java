package com.spatialdev.osm.indicators;

import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Number of structures "Sprayed" = Number of Unique structures "Sprayed"
 */
public class MspraySprayedIndicator extends OSMIndicator {
    public static final String NAME = "MspraySprayedIndicator";
    private static final String SPRAYED_VALUE = "sprayed";

    public MspraySprayedIndicator(Map<String, Map<Long, OSMElement>> mappedData) {
        super(NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) {
        if (mappedData.containsKey(SPRAYED_VALUE)
                && mappedData.get(SPRAYED_VALUE) != null) {
            return mappedData.get(SPRAYED_VALUE).size();
        }

        return 0d;
    }
}