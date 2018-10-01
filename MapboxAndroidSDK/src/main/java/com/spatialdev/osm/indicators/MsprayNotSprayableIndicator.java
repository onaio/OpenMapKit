package com.spatialdev.osm.indicators;

import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Number of structures "not sprayable" = Number of Unique structures "not eligible"
 */
public class MsprayNotSprayableIndicator extends OSMIndicator {
    public static final String NAME = "MsprayNotSprayableIndicator";
    private static final String NOT_SPRAYABLE_VALUE = "noteligible";

    public MsprayNotSprayableIndicator(Map<String, Map<Long, OSMElement>> mappedData) {
        super(NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) {
        if (mappedData.containsKey(NOT_SPRAYABLE_VALUE)
                && mappedData.get(NOT_SPRAYABLE_VALUE) != null) {
            return mappedData.get(NOT_SPRAYABLE_VALUE).size();
        }

        return 0d;
    }
}