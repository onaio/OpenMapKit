package com.spatialdev.osm.renderer;

import android.graphics.Paint;
import android.graphics.Path;

import com.mapbox.mapboxsdk.views.MapView;
import com.spatialdev.osm.model.OSMWay;
import com.spatialdev.osm.renderer.util.ColorElement;
import com.spatialdev.osm.renderer.util.ColorXmlParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Nicholas Hallahan on 1/22/15.
 * nhallahan@spatialdev.com 
 */
public class OSMPolygon extends OSMPath {

    private static List<ColorElement> colorElements = new ArrayList<>();
    private static boolean initializedColors = false;
    // ALPHA
    private static final int DEFAULT_ALPHA = 50;
    private static final int HEX_RADIX = 16;

    // GOLD
    private static final int DEFAULT_SELECTED_A = 180;
    private static final int DEFAULT_SELECTED_R = 255;
    private static final int DEFAULT_SELECTED_G = 140;
    private static final int DEFAULT_SELECTED_B = 0;

    private int a;
    private int r;
    private int g;
    private int b;
    
    /**
     * This should only be constructed by
     * OSMPath.createOSMPath
     * * * *
     * @param w
     */
    protected OSMPolygon(final OSMWay w, final MapView mv) {
        super(w, mv);

        // color polygon according to values in tags.
        Map<String, String> tags = w.getTags();
        loadColorElements(mv);
        String colorCode;
        for (ColorElement el : colorElements) {
            String key = el.getKey();
            if (tags.containsKey(key)) {
                if (tags.get(key).equals(el.getValue())) {
                    //Choose highest priority coloring and exit loop.
                    colorCode = el.getColorCode();
                    r = Integer.parseInt(colorCode.substring(1, 3), HEX_RADIX);
                    g = Integer.parseInt(colorCode.substring(3, 5), HEX_RADIX);
                    b = Integer.parseInt(colorCode.substring(5, 7), HEX_RADIX);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setARGB(DEFAULT_ALPHA, r, g, b);
                    break;
                }
            }
        }
    }

    @Override
    public void select() {
        paint.setARGB(DEFAULT_SELECTED_A, DEFAULT_SELECTED_R, DEFAULT_SELECTED_G, DEFAULT_SELECTED_B);
    }

    @Override
    public void deselect() {
        paint.setARGB(a, r, g, b);
    }
    


    /**
     * For now, we are drawing all of the polygons, even those outside of the canvas.
     * 
     * This isn't too much of a problem for now, because the Spatial Index will give us
     * only polygons that intersect.
     * 
     * This can be problematic for very large polygons.
     * * * * * * * * 
     * @param path
     * @param projectedPoint
     * @param nextProjectedPoint
     * @param screenPoint
     */
    @Override
    void clipOrDrawPath(Path path, double[] projectedPoint, double[] nextProjectedPoint, double[] screenPoint) {
        if (pathLineToReady) {
            path.lineTo( (float) screenPoint[0], (float) screenPoint[1] );
        } else {
            path.moveTo( (float) screenPoint[0], (float) screenPoint[1] );
            pathLineToReady = true;
        }
    }

    private static void loadColorElements(MapView mv) {
        if (!initializedColors) {
            try {
                colorElements = ColorXmlParser.parseXML(mv.getContext());
                initializedColors = true;
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
