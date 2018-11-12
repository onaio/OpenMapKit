/**
 * Created by Nicholas Hallahan on 1/7/15.
 * nhallahan@spatialdev.com
 */

package com.spatialdev.osm;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.events.MapListener;
import com.mapbox.mapboxsdk.events.RotateEvent;
import com.mapbox.mapboxsdk.events.ScrollEvent;
import com.mapbox.mapboxsdk.events.ZoomEvent;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.Overlay;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.MapViewListener;
import com.spatialdev.osm.events.OSMSelectionListener;
import com.spatialdev.osm.marker.OSMMarker;
import com.spatialdev.osm.model.JTSModel;
import com.spatialdev.osm.model.OSMColorConfig;
import com.spatialdev.osm.model.OSMElement;
import com.spatialdev.osm.model.OSMNode;
import com.spatialdev.osm.renderer.OSMOverlay;
import com.spatialdev.osm.indicators.*;
import com.vividsolutions.jts.geom.Envelope;

import org.fieldpapers.model.FPAtlas;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class OSMMap implements MapViewListener, MapListener {

    // DEBUG MODE - SHOW ENVELOPE AROUND TAP ON MAP
    private static final boolean DEBUG = false;

    private final OSMColorConfig osmColorConfig;
    private MapView mapView;
    private JTSModel jtsModel;
    private OSMSelectionListener selectionListener;
    private OSMOverlay osmOverlay;
    private Map<String, Map<Long, OSMElement>> mappedElements;

    private PathOverlay debugTapEnvelopePath;

    public OSMMap(MapView mapView, JTSModel jtsModel, OSMSelectionListener selectionListener, OSMColorConfig osmColorConfig) {
        this(mapView, jtsModel, osmColorConfig);
        this.selectionListener = selectionListener;
    }

    public OSMMap(MapView mapView, JTSModel jtsModel, OSMSelectionListener selectionListener, float minVectorRenderZoom, OSMColorConfig osmColorConfig) {
        this(mapView, jtsModel, minVectorRenderZoom, osmColorConfig);
        this.selectionListener = selectionListener;
    }

    // Only paint and render vectors at zoom levels greater than or equal to this level.
    public OSMMap(MapView mapView, JTSModel jtsModel, float minVectorRenderZoom, OSMColorConfig osmColorConfig) {
        if (minVectorRenderZoom > 0) {
            osmOverlay = new OSMOverlay(jtsModel, minVectorRenderZoom);
        } else {
            osmOverlay = new OSMOverlay(jtsModel);
        }
        this.mapView = mapView;
        this.jtsModel = jtsModel;
        updateBoundingBox();
        osmOverlay.updateZoom(mapView.getZoomLevel());
        mapView.setMapViewListener(this);
        mapView.addListener(this);
        mapView.getOverlays().add(osmOverlay);
        mapView.invalidate();
        this.osmColorConfig = osmColorConfig;
    }
    
    public OSMMap(MapView mapView, JTSModel jtsModel, OSMColorConfig osmColorConfig) {
        this.mapView = mapView;
        this.jtsModel = jtsModel;
        osmOverlay = new OSMOverlay(jtsModel);
        updateBoundingBox();
        mapView.setMapViewListener(this);
        mapView.addListener(this);
        mapView.getOverlays().add(osmOverlay);
        mapView.invalidate();
        this.osmColorConfig = osmColorConfig;
    }

    public void setSelectionListener(OSMSelectionListener selectionListener) {
        this.selectionListener = selectionListener;
    }
    
    /**
     * MapViewListener Methods
     */

    @Override
    public void onShowMarker(MapView pMapView, Marker pMarker) {

    }

    @Override
    public void onHideMarker(MapView pMapView, Marker pMarker) {

    }

    /**
     * When the user selects a marker on the map, we want to pan
     * the map to where the marker is for selection.
     *
     * @param pMapView the map
     * @param pMarker  the marker
     */
    @Override
    public void onTapMarker(MapView pMapView, Marker pMarker) {
        LatLng latLng = pMarker.getPoint();
        pMapView.getController().animateTo(latLng);
        OSMNode node = ((OSMMarker)pMarker).getNode();
        OSMElement.deselectAll();
        node.select();
        if (OSMElement.hasSelectedElementsChanged() && selectionListener != null) {
            selectionListener.selectedElementsChanged(OSMElement.getSelectedElements());
        }
    }

    @Override
    public void onLongPressMarker(MapView pMapView, Marker pMarker) {

    }

    @Override
    public void onTapMap(MapView pMapView, ILatLng pPosition) {
        FPAtlas atlas = FPAtlas.singleton();
        if (atlas != null) {
            atlas.onTapMap(pMapView, pPosition);
        }

        float zoom = pMapView.getZoomLevel();

        OSMElement.deselectAll();

        OSMElement element = jtsModel.queryFromTap(pPosition, zoom);
        if (element != null) {
            element.select();
        }

        // DEBUG MODE - SHOW ENVELOPE AROUND TAP ON MAP
        if (DEBUG) {
            drawDebugTapEnvelope(pMapView, pPosition, zoom);
        }

        mapView.invalidate();
        
        // check to see if the selected elements has changed and
        // notify selection listeners if they exist
        if (OSMElement.hasSelectedElementsChanged() && selectionListener != null) {
            selectionListener.selectedElementsChanged(OSMElement.getSelectedElements());
        }
    }

    private void drawDebugTapEnvelope(MapView pMapView, ILatLng pPosition, float zoom) {
        Envelope env = jtsModel.createTapEnvelope(pPosition, zoom);
        PathOverlay path;
        if (debugTapEnvelopePath == null) {
            path = new PathOverlay();
            debugTapEnvelopePath = path;
            pMapView.getOverlays().add(path);
        } else {
            path = debugTapEnvelopePath;
        }
        Paint paint = path.getPaint();
        paint.setStrokeWidth(0); // hairline mode
        paint.setARGB(200, 0, 255, 255);
        double maxX = env.getMaxX();
        double maxY = env.getMaxY();
        double minX = env.getMinX();
        double minY = env.getMinY();
        path.clearPath();
        path.addPoint(minY, minX);
        path.addPoint(maxY, minX);
        path.addPoint(maxY, maxX);
        path.addPoint(minY, maxX);
        path.addPoint(minY, minX);
    }

    @Override
    public void onLongPressMap(MapView pMapView, ILatLng pPosition) {
//        float zoom = pMapView.getZoomLevel();
//        jtsModel.queryFromTap(pPosition, zoom);
    }

    /**
     * MapListener Methods
     */

    @Override
    public void onScroll(ScrollEvent event) {
        updateBoundingBox();
    }

    @Override
    public void onZoom(ZoomEvent event) {
        updateBoundingBox();
        osmOverlay.updateZoom(event.getZoomLevel());
//        Log.i("ZOOM", String.valueOf(event.getZoomLevel()));
    }

    @Override
    public void onRotate(RotateEvent event) {
        updateBoundingBox();
    }
    
    private void updateBoundingBox() {
        BoundingBox bbox = mapView.getBoundingBox();
        if (bbox != null) {
            osmOverlay.updateBoundingBox(bbox);
        }
    }

    public OSMNode addNode() {
        LatLng center = mapView.getCenter();
        OSMNode node = new OSMNode(center, osmColorConfig);
        jtsModel.addOSMStandaloneNode(node);
        mapView.invalidate();
        return node;
    }

    public OSMNode addNode(OSMNode node) {
        Marker marker = node.getMarker();
        if (marker != null) {
            marker.setVisibility(true);
        }
        jtsModel.addOSMStandaloneNode(node);
        mapView.invalidate();
        return node;
    }

    public OSMNode moveNode() {
        LatLng center = mapView.getCenter();
        OSMNode selectedNode = (OSMNode)OSMElement.getSelectedElements().getFirst();
        selectedNode.move(jtsModel, center);
        mapView.invalidate();
        return selectedNode;
    }

    public OSMNode deleteNode() {
        OSMNode selectedNode = (OSMNode)OSMElement.getSelectedElements().getFirst();
        selectedNode.delete(jtsModel);
        mapView.invalidate();
        return selectedNode;
    }

    public Map<String, OSMIndicator> getIndicators(Context context, String mapReduceTagKey, String filterTagKey, String filterTagValue) {
        if (mappedElements == null) {
            mappedElements = OSMIndicator.getMappedData(mapReduceTagKey, filterTagKey, filterTagValue, jtsModel);
        }

        Map<String, OSMIndicator> indicators = new HashMap<String, OSMIndicator>();
        indicators.put(MsprayEligibleFoundIndicator.NAME,
                new MsprayEligibleFoundIndicator(context, mappedElements));
        indicators.put(MsprayFoundCoverageIndicator.NAME,
                new MsprayFoundCoverageIndicator(context, mappedElements));
        indicators.put(MsprayNotSprayableIndicator.NAME,
                new MsprayNotSprayableIndicator(context, mappedElements));
        indicators.put(MsprayNotSprayedIndicator.NAME,
                new MsprayNotSprayedIndicator(context, mappedElements));
        indicators.put(MspraySprayCoverageIndicator.NAME,
                new MspraySprayCoverageIndicator(context, mappedElements));
        indicators.put(MspraySprayedIndicator.NAME,
                new MspraySprayedIndicator(context, mappedElements));
        indicators.put(MspraySprayEffectivenessIndicator.NAME,
                new MspraySprayEffectivenessIndicator(context, mappedElements));
        indicators.put(MsprayTotalEligibleIndicator.NAME,
                new MsprayTotalEligibleIndicator(context, mappedElements));
        indicators.put(MsprayStructuresRequiredTo90PcIndicator.NAME,
                new MsprayStructuresRequiredTo90PcIndicator(context, mappedElements));
        indicators.put(TotalIndicator.NAME,
                new TotalIndicator(context, mappedElements));
        indicators.put(MacepaAllEligibleIndicator.NAME,
                new MacepaAllEligibleIndicator(context, mappedElements));
        indicators.put(MacepaAllReceivedIndicator.NAME,
                new MacepaAllReceivedIndicator(context, mappedElements));
        indicators.put(MacepaFoundCoverageIndicator.NAME,
                new MacepaFoundCoverageIndicator(context, mappedElements));
        indicators.put(MacepaFoundIndicator.NAME,
                new MacepaFoundIndicator(context, mappedElements));
        indicators.put(MacepaNoneReceivedIndicator.NAME,
                new MacepaNoneReceivedIndicator(context, mappedElements));
        indicators.put(MacepaNotFoundYetIndicator.NAME,
                new MacepaNotFoundYetIndicator(context, mappedElements));
        indicators.put(MacepaNotResidentialIndicator.NAME,
                new MacepaNotResidentialIndicator(context, mappedElements));
        indicators.put(MacepaReceivedCoverageIndicator.NAME,
                new MacepaReceivedCoverageIndicator(context, mappedElements));
        indicators.put(MacepaReceivedIndicator.NAME,
                new MacepaReceivedIndicator(context, mappedElements));
        indicators.put(MacepaReceivedSuccessRateIndicator.NAME,
                new MacepaReceivedSuccessRateIndicator(context, mappedElements));
        indicators.put(MacepaSomeReceivedIndicator.NAME,
                new MacepaSomeReceivedIndicator(context, mappedElements));
        indicators.put(MacepaStructuresRequiredTo90PcIndicator.NAME,
                new MacepaStructuresRequiredTo90PcIndicator(context, mappedElements));


        return indicators;
    }
}
