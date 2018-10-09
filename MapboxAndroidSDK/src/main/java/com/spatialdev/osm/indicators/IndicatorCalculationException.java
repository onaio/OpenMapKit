package com.spatialdev.osm.indicators;

public class IndicatorCalculationException  extends Exception {
    public IndicatorCalculationException(final String message) {
        super(message);
    }

    public IndicatorCalculationException(final Throwable throwable) {
        super(throwable);
    }
}