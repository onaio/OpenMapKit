/**
 * Created by Nicholas Hallahan on 1/7/15.
 * nhallahan@spatialdev.com
 */

package com.spatialdev.osm.model;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JTSModel {

    private static final int TAP_PIXEL_TOLERANCE = 24;

    private Map<Long, OSMElement> elementHashMap;
    private Map<String, OSMDataSet> dataSetHash;
    private GeometryFactory geometryFactory;
    private Quadtree spatialIndex;

    public JTSModel() {
        geometryFactory = new GeometryFactory();
        spatialIndex = new Quadtree();
        dataSetHash = new ConcurrentHashMap<>();
        elementHashMap = new ConcurrentHashMap<>();
    }

    public synchronized void addOSMDataSet(String filePath, OSMDataSet ds) {
        dataSetHash.put(filePath, ds);
        addOSMClosedWays(ds);
        addOSMOpenWays(ds);
        addOSMStandaloneNodes(ds);
    }
    
    public synchronized void mergeEditedOSMDataSet(String absPath, OSMDataSet ds) {
        Collection<OSMDataSet> dataSets = dataSetHash.values();
        for (OSMDataSet existingDataSet : dataSets) {
            // closed ways
            removeWaysFromExistingDataSet(existingDataSet, ds.getClosedWays());
            //open ways
            removeWaysFromExistingDataSet(existingDataSet, ds.getOpenWays());
        }
        addOSMDataSet(absPath, ds);
    }

    /**
     * Removes a specific OSM XML Data Set based off of the path of the file.
     * * * 
     * @param absoluteFilePath
     */
    public void removeDataSet(String absoluteFilePath) {
        OSMDataSet ds = dataSetHash.get(absoluteFilePath);
        List<OSMWay> closedWays = ds.getClosedWays();
        List<OSMWay> openWays = ds.getOpenWays();
        List<OSMNode> standaloneNodes = ds.getStandaloneNodes();
        for (OSMWay w : closedWays) {
            removeOSMElement(w);
        }
        for (OSMWay w : openWays) {
            removeOSMElement(w);
        }
        for (OSMNode n : standaloneNodes) {
            removeOSMElement(n);
        }
    }
    
    private void removeWaysFromExistingDataSet(OSMDataSet existingDataSet, List<OSMWay> ways) {
        for (OSMWay w : ways) {
            OSMWay oldWay = existingDataSet.getWay(w.getId());
            if (oldWay != null) {
                removeOSMElement(oldWay);
            }
        }
    }
    
    public Envelope createTapEnvelope(ILatLng latLng, float zoom) {
        return createTapEnvelope(latLng.getLatitude(), latLng.getLongitude(), zoom);
    }

    public Envelope createTapEnvelope(double lat, double lng, float zoom) {
        Coordinate coord = new Coordinate(lng, lat);
        return createTapEnvelope(coord, lat, lng, zoom);
    }

    public List<OSMElement> queryFromEnvelope(Envelope envelope) {
        List<OSMElement> results = spatialIndex.query(envelope);
        return results;
    }
    
    public OSMElement queryFromTap(ILatLng latLng, float zoom) {
        double lat = latLng.getLatitude();
        double lng = latLng.getLongitude();
        Coordinate coord = new Coordinate(lng, lat);
        Envelope envelope = createTapEnvelope(coord, lat, lng, zoom);

        List results = spatialIndex.query(envelope);

        int len = results.size();
        if (len == 0 ) {
            return null;
        }
        if (len == 1) {
            return (OSMElement) results.get(0);
        }

        Point clickPoint = geometryFactory.createPoint(coord);
        OSMElement closestElement = null;
        double closestDist = Double.POSITIVE_INFINITY; // should be replaced in first for loop iteration
        for (Object res : results) {
            OSMElement el = (OSMElement) res;
            if (closestElement == null) {
                closestElement = el;
                closestDist = el.getJTSGeom().distance(clickPoint);
                continue;
            }
            Geometry geom = el.getJTSGeom();
            double dist = geom.distance(clickPoint);

            if (dist > closestDist) {
                continue;
            }

            if (dist < closestDist) {
                closestElement = el;
                closestDist = dist;
                continue;
            }

            // If we are here, then the distances are the same,
            // so we prioritize which element is better based on their type.
            closestElement = prioritizeElementByType(closestElement, el);

        }

        Geometry closestElementGeom = closestElement.getJTSGeom();
        if (closestElementGeom != null && closestElementGeom.intersects(geometryFactory.createPoint(coord))) {
            return closestElement;
        }
        return null;
    }
    
    private Envelope createTapEnvelope(Coordinate coord, double lat, double lng, float zoom) {
        Envelope envelope = new Envelope(coord);

        // Creating a reasonably sized envelope around the tap location.
        // Tweak the TAP_PIXEL_TOLERANCE to get a better sized box for your needs.
        double degreesLngPerPixel = degreesLngPerPixel(zoom);
        double deltaX = degreesLngPerPixel * TAP_PIXEL_TOLERANCE;
        double deltaY = scaledLatDeltaForMercator(deltaX, lat);
        envelope.expandBy(deltaX, deltaY);
        return envelope;
    }

    /**
     * Prioritizes points over lines over polygons.
     * @param el1
     * @param el2
     * @return the priority OSMElement type
     */
    private OSMElement prioritizeElementByType(OSMElement el1, OSMElement el2) {
        if (el1 instanceof OSMNode) {
            return el1;
        }
        if (el2 instanceof OSMNode) {
            return el2;
        }
        // It's gotta be a Way at this point...
        if ( ! ((OSMWay)el1).isClosed() ) {
            return el1;
        }
        return el2;
    }

    private void addOSMClosedWays(OSMDataSet ds) {
        List<OSMWay> closedWays = ds.getClosedWays();
        for (OSMWay w : closedWays) {
            if(elementAlreadyAdded(elementHashMap, w)) {
                continue;
            }
            if (!w.isModified() && OSMWay.containsModifiedWay(w.getId())) {
                continue;    
            }
            // Don't render or index ways that do not have all of their referenced nodes.
            if (w.incomplete()) {
                continue;
            }

            if(elementHashMap.containsKey(w.getId())) {
                removeOSMElement(elementHashMap.get(w.getId()));
            }

            List<OSMNode> nodes = w.getNodes();
            Coordinate[] coords = coordArrayFromNodeList(nodes);
            Polygon poly = geometryFactory.createPolygon(coords);
            w.setJTSGeom(poly);
            Envelope envelope = poly.getEnvelopeInternal();
            spatialIndex.insert(envelope, w);
            elementHashMap.put(w.getId(), w);
        }
    }

    private void addOSMOpenWays(OSMDataSet ds) {
        List<OSMWay> openWays = ds.getOpenWays();
        for (OSMWay w : openWays) {
            if(elementAlreadyAdded(elementHashMap, w)) {
                continue;
            }
            if (!w.isModified() && OSMWay.containsModifiedWay(w.getId())) {
                continue;
            }
            // Don't render or index ways that do not have all of their referenced nodes.
            if (w.incomplete()) {
                continue;
            }

            if(elementHashMap.containsKey(w.getId())) {
                removeOSMElement(elementHashMap.get(w.getId()));
            }

            List<OSMNode> nodes = w.getNodes();
            Coordinate[] coords = coordArrayFromNodeList(nodes);
            LineString line = geometryFactory.createLineString(coords);
            w.setJTSGeom(line);
            Envelope envelope = line.getEnvelopeInternal();
            spatialIndex.insert(envelope, w);
            elementHashMap.put(w.getId(), w);
        }
    }

    private Coordinate[] coordArrayFromNodeList(List<OSMNode> nodes) {
        Coordinate[] coords = new Coordinate[nodes.size()];
        int i = 0;
        for (OSMNode node : nodes) {
            double lat = node.getLat();
            double lng = node.getLng();
            Coordinate coord = new Coordinate(lng, lat);
            coords[i++] = coord;
        }
        return coords;
    }

    private void addOSMStandaloneNodes(OSMDataSet ds) {
        List<OSMNode> standaloneNodes = ds.getStandaloneNodes();
        for (OSMNode n : standaloneNodes) {
            if(elementAlreadyAdded(elementHashMap, n)) {
                continue;
            }

            if(elementHashMap.containsKey(n.getId())) {
                removeOSMElement(elementHashMap.get(n.getId()));
            }

            double lat = n.getLat();
            double lng = n.getLng();
            Coordinate coord = new Coordinate(lng, lat);
            Point point = geometryFactory.createPoint(coord);
            n.setJTSGeom(point);
            Envelope envelope = point.getEnvelopeInternal();
            spatialIndex.insert(envelope, n);
            elementHashMap.put(n.getId(), n);
        }
    }

    /**
     * When the user adds an OSMNode POI to the map, we want to add that new
     * node to JTSModel. This is done in OSMMap.
     *
     * @param n - the OSMNode
     */
    public void addOSMStandaloneNode(OSMNode n) {
        double lat = n.getLat();
        double lng = n.getLng();
        Coordinate coord = new Coordinate(lng, lat);
        Point point = geometryFactory.createPoint(coord);
        n.setJTSGeom(point);
        Envelope envelope = point.getEnvelopeInternal();
        spatialIndex.insert(envelope, n);
    }

    /**
     * Removes the OSMElement from the Spatial Index
     * if the element there.
     *
     * @param el - any OSMElement
     */
    public void removeOSMElement(OSMElement el) {
        Geometry geom = el.getJTSGeom();
        if (geom != null) {
            try {
                Envelope env = geom.getEnvelopeInternal();
                spatialIndex.remove(env, el);
                if(elementHashMap.containsKey(el.getId())) {
                    elementHashMap.remove(el.getId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method checks whether an updated version of the {@link OSMElement} is already rendered
     *
     * @param elementHashMap    A {@link Map} of all the elements already added to the model (rendered on the map)
     * @param el                The {@link OSMElement} to check if already rendered
     * @return  TRUE if there as already an up-to-date version of the element rendered
     */
    public static boolean elementAlreadyAdded(Map<Long, OSMElement> elementHashMap, OSMElement el) {
        if(el != null) {
            if(elementHashMap.containsKey(el.getId())) {
                Date newElDate = el.getTimestampDate();
                Date existingElDate = elementHashMap.get(el.getId()).getTimestampDate();
                if(newElDate == null) {
                    return true;
                } else if(existingElDate == null || newElDate.getTime() > existingElDate.getTime()) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * This is how degrees wide a given pixel is for a given zoom.
     *
     * @param zoom
     * @return
     */
    private static double degreesLngPerPixel(float zoom) {
        double degreesPerTile = 360 / Math.pow(2, zoom);
        return degreesPerTile / 256;
    }

    /**
     * This is how many degrees high a given Lat Delta is for a given
     * zoom in Spherical Mercator.
     *
     * http://en.wikipedia.org/wiki/Mercator_projection#Scale_factor
     *
     * @param deltaDeg, lng
     * @return
     */
    private static double scaledLatDeltaForMercator(double deltaDeg, double lat) {
        double scale =  1 / Math.cos(Math.toRadians(lat));
        return deltaDeg / scale;
    }

}
