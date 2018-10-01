package com.spatialdev.osm.indicators;

import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class TotalIndicator extends OSMIndicator {
    public static final String NAME = "TotalIndicator";

    public TotalIndicator(Map<String, Map<Long, OSMElement>> mappedData) {
        super(NAME, mappedData);
    }

    @Override
    public double calculate(Map<String, OSMIndicator> indicators) {
        double total = 0d;
        for (String curMapKey : mappedData.keySet()) {
            if (mappedData.get(curMapKey) != null) {
                total = total + mappedData.get(curMapKey).size();
            }
        }

        return total;
    }
}