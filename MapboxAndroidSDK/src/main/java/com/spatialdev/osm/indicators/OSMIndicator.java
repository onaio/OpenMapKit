package com.spatialdev.osm.indicators;

import android.content.Context;
import android.util.Log;

import com.spatialdev.osm.model.JTSModel;
import com.spatialdev.osm.model.OSMElement;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

public abstract class OSMIndicator {
    protected final Map<String, Map<Long, OSMElement>> mappedData;
    protected final String name;
    protected final Context context;

    public OSMIndicator(Context context, String name, Map<String, Map<Long, OSMElement>> mappedData) {
        this.context = context;
        this.name = name;
        this.mappedData = mappedData;
    }

    public static Map<String, Map<Long, OSMElement>> getMappedData(String mapReduceTagkey,
                                                                           String filterTagKey,
                                                                           String filterTagValue,
                                                                           JTSModel jtsModel) {
        String formattedFilterValue = "";
        if (filterTagValue != null) {
            formattedFilterValue = filterTagValue.toLowerCase().trim();
        }

        Map<String, Map<Long, OSMElement>> mappedData = new HashMap<String, Map<Long, OSMElement>>();

        if (mapReduceTagkey != null && mapReduceTagkey.length() > 0) {
            Map<Long, OSMElement> elementHashMap = jtsModel.getElementHashMap();
            Log.d("TestIndicators", "Number of features = " + String.valueOf(elementHashMap.size()));

            for (Long curId : elementHashMap.keySet()) {
                OSMElement curElement = elementHashMap.get(curId);
                Map<String, String> curTags = curElement.getTags();
                String curFilterValue = "";
                if (curTags.containsKey(filterTagKey)
                        && curTags.get(filterTagKey) != null) {
                    curFilterValue = curTags.get(filterTagKey).toLowerCase().trim();
                }

                if (filterTagKey == null || curFilterValue.equals(formattedFilterValue)) {
                    String mapReduceTagValue = "";
                    if (curTags.containsKey(mapReduceTagkey)
                            && curTags.get(mapReduceTagkey) != null) {
                        mapReduceTagValue = curTags.get(mapReduceTagkey);
                    }

                    if (mappedData.get(mapReduceTagValue) == null) {
                        mappedData.put(mapReduceTagValue, new HashMap<Long, OSMElement>());
                    }

                    mappedData.get(mapReduceTagValue).put(curId, curElement);
                }
            }
        }

        return mappedData;
    }

    public abstract double calculate(Map<String, OSMIndicator> indicators);
    public abstract String getTitle();
    public abstract String getFormattedCalculation(Map<String, OSMIndicator> indicators);
}