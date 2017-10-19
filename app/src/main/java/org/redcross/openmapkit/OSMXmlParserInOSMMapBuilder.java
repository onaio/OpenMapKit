package org.redcross.openmapkit;

import android.support.annotation.Nullable;

import com.spatialdev.osm.model.OSMDataSet;
import com.spatialdev.osm.model.OSMXmlParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Nicholas Hallahan on 1/29/15.
 *
 * You should use this instead of OSMXmlParser if you want
 * to parse OSM XML within OSMMapBuilder. This class will notify
 * the task of the parsing activity as the data is being parsed.
 * * * * * * 
 */
public class OSMXmlParserInOSMMapBuilder extends OSMXmlParser {

    private OSMMapBuilder osmMapBuilder;

    public static OSMDataSet parseFromInputStream(InputStream in) throws IOException {
        OSMXmlParser osmXmlParser = new OSMXmlParserInOSMMapBuilder();
        parse(osmXmlParser, in);
        return osmXmlParser.getDataSet();
    }

    public static OSMDataSet parseFromInputStream(InputStream in, OSMMapBuilder osmMapBuilder)
            throws IOException {
        OSMXmlParser osmXmlParser = new OSMXmlParserInOSMMapBuilder(osmMapBuilder);
        parse(osmXmlParser, in);
        return osmXmlParser.getDataSet();
    }

    private static void parse(OSMXmlParser osmXmlParser, InputStream in) throws IOException {
        try {
            osmXmlParser.parse(in);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private OSMXmlParserInOSMMapBuilder() {
        super(null);
    }

    private OSMXmlParserInOSMMapBuilder(OSMMapBuilder osmMapBuilder) {
        super(osmMapBuilder.getOsmColorConfig());
        this.osmMapBuilder = osmMapBuilder;
    }

    @Override
    protected void notifyProgress() {
        if (osmMapBuilder != null) {
            osmMapBuilder.updateFromParser(
                    elementReadCount, nodeReadCount, wayReadCount, relationReadCount, tagReadCount);
        }
    }
}