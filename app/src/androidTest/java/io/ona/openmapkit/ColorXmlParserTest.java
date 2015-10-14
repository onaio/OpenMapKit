package io.ona.openmapkit;

import android.content.Context;
import android.test.mock.MockContext;

import com.spatialdev.osm.renderer.util.ColorElement;
import com.spatialdev.osm.renderer.util.ColorXmlParser;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;

/**
 * Created by coder on 8/4/15.
 */
public class ColorXmlParserTest {
    @Test
    public void testColorXmlTagNames() {
        assertEquals(ColorXmlParser.FILENAME, "coloring.xml");
        assertEquals(ColorXmlParser.COLOR, "color");
        assertEquals(ColorXmlParser.COLOR_CODE, "color_code");
        assertEquals(ColorXmlParser.PRIORITY, "priority");
        assertEquals(ColorXmlParser.KEY, "tag");
        assertEquals(ColorXmlParser.VALUE, "value");
    }

    /**
     * Test both the size and the ranking according to priority for tag colors.
     * @throws IOException
     * @throws XmlPullParserException
     */
    @Test
    public void testColorElements() throws IOException, XmlPullParserException {
        Context context = new MockContext();
        List<ColorElement> list = ColorXmlParser.parseXML(context);
        assertEquals(list.size(), 4);
        //test priority
        for (int i = 0; i < list.size()-1; i++) {
            assertTrue(list.get(i).getPriority() >= list.get(i+1).getPriority());
        }
    }

    /**
     * Ensure color codes are in the right format.
     * @throws IOException
     * @throws XmlPullParserException
     */
    @Test
    public void testColorCodes() throws IOException, XmlPullParserException {
        Context context = new MockContext();
        List<ColorElement> list = ColorXmlParser.parseXML(context);
        for (ColorElement el : list) {
            String colorCode = el.getColorCode();
            assertEquals(colorCode.charAt(0), '#');
            assertEquals(colorCode.length(), 7);
            int hex;
            //Code should be in hexadecimals.
            for (int i = 1; i < colorCode.length(); i++) {
                hex = Integer.parseInt(Character.toString(colorCode.charAt(i)), 16);
                assertTrue(hex >= 0 && hex <= 15);
            }
        }
    }
}
