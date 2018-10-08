package org.redcross.openmapkit.odkcollect;

import android.util.Log;

import com.spatialdev.osm.model.OSMDataSet;
import com.spatialdev.osm.model.OSMElement;
import com.spatialdev.osm.model.OSMNode;
import com.spatialdev.osm.model.OSMWay;
import com.spatialdev.osm.model.OSMXmlWriter;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.redcross.openmapkit.ExternalStorage;
import org.redcross.openmapkit.MapActivity;
import org.redcross.openmapkit.OSMXmlParserInOSMMapBuilder;
import org.redcross.openmapkit.odkcollect.tag.ODKTag;
import org.redcross.openmapkit.odkcollect.tag.ODKTagItem;
import org.redcross.openmapkit.Settings;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Nicholas Hallahan on 2/9/15.
 * nhallahan@spatialdev.com
 * * * 
 */
public class ODKCollectData {
    private static final String TAG = ODKCollectData.class.getSimpleName();
    public static final String APP_NAME = "OpenMapKit Android";
            
    private String formId;
    private String formFileName;
    private String instanceId;
    private String instanceDir;
    private String previousOSMEditFileName;
    private LinkedHashMap<String, ODKTag> requiredTags;
    private ArrayList<Long> previousOSMEditFileStructureIds;
    private List<File> editedOSM = new ArrayList<>();
    
    private String editedXml;
    private String checksum;
    private String appVersion;
    private String geoContext;

    public ODKCollectData ( String formId,
                            String formFileName,
                            String instanceId,
                            String instanceDir,
                            String previousOSMEditFileName,
                            String geoContext,
                            LinkedHashMap<String, ODKTag> requiredTags) {
        this.formId = formId;
        this.formFileName = formFileName;
        this.instanceId = instanceId;
        this.instanceDir = instanceDir;
        this.previousOSMEditFileName = previousOSMEditFileName;
        this.requiredTags = requiredTags;
        this.appVersion = MapActivity.getVersion();
        this.geoContext = geoContext;
        findEditedOSMForForm(formFileName);
        if (Settings.singleton() != null) {
            ArrayList<String> extraInstanceDirs = Settings.singleton().getExtraOdkInstanceDirectories();

            for (String curDir : extraInstanceDirs) {
                findEditedOSMForForm(curDir, null, false);
            }
        }
        extractOsmIdsFromPreviousEdit();
    }

    private void extractOsmIdsFromPreviousEdit() {
        previousOSMEditFileStructureIds = new ArrayList<>();
        try {
            File file = getPreviousEditedFile();
            if (file != null) {
                InputStream inputStream = new FileInputStream(file);
                OSMDataSet ds = OSMXmlParserInOSMMapBuilder.parseFromInputStream(inputStream);
                if (ds != null) {
                    if (ds.getNodes() != null) {
                        for (OSMNode curNode : ds.getNodes().values()) {
                            Log.d(TAG, "Adding "+curNode.getId() + " to list");
                            previousOSMEditFileStructureIds.add(curNode.getId());
                        }
                    } else {
                        Log.d(TAG, "Nodes is null");
                    }

                    if (ds.getWays() != null) {
                        for (OSMWay curWay : ds.getWays().values()) {
                            Log.d(TAG, "Adding "+curWay.getId() + " to list");
                            previousOSMEditFileStructureIds.add(curWay.getId());
                        }
                    } else {
                        Log.d(TAG, "Ways is null");
                    }
                } else {
                    Log.d(TAG, "DS is null");
                }
            } else {
                Log.d(TAG, "File is null");
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private void findEditedOSMForForm(String formFileName) {
        if (formFileName == null) {
            return;
        }

        String instances = new File(instanceDir).getParent();

        findEditedOSMForForm(instances, formFileName, true);
    }

    private void findEditedOSMForForm(String instances, String formFileName, boolean checkIfFromSameForm) {
        File[] instancesDirs = new File(instances).listFiles();
        String instanceDirName = new File(instanceDir).getName();

        if (instancesDirs != null) {
            for (int i = 0; i < instancesDirs.length; ++i) {
                File dir = instancesDirs[i];
                if (!dir.isDirectory()) {
                    continue;
                }
                // check if the instance dir is for the form we are dealing with
                // it is 0 if the form file name is the first substring of the dirname
                if (checkIfFromSameForm && dir.getName().indexOf(formFileName) != 0) {
                    continue;
                }

                String[] files = dir.list();
                for (int j = 0; j < files.length; ++j) {
                    String fname = files[j];
                    if (fname.lastIndexOf(".osm") > -1) {
                    /*
                    determine whether the current file j is in the same instance directory as
                    previousOSMEditFileName. If so, don't add it
                     */
                        if(dir.getName().equals(instanceDirName)) {
                            if(previousOSMEditFileName == null) {
                                //means that none of the OSM files in the ODK instance directory have been
                                //marked as previous. Very fishy. Check if the file is the youngest in the instance directory
                                if(!isLastModifiedOsmFileInDirectory(dir, fname)) {
                                    continue;
                                }
                            }
                            else if(!fname.equals(previousOSMEditFileName)) {
                                //means that the current file is something the user entered then overwrote
                                //with the previousOSMEditFileName
                                continue;
                            }
                        } else {
                            if(!isLastModifiedOsmFileInDirectory(dir, fname)) {
                                continue;
                            }
                        }
                        File osmFile = new File(dir, fname);
                        editedOSM.add(osmFile);
                    }
                }
            }
        }
    }

    /**
     * This method checks whether the provided file is the youngest in the directory
     *
     * @param directory The directory to check for the youngest file
     * @param fileName  The name of the file you want to validate if youngest
     * @return  TRUE if the file was the last to be modified in the directory
     */
    public static boolean isLastModifiedOsmFileInDirectory(File directory, String fileName) {
        if(directory != null && fileName != null) {
            FileFilter fileFilter = new WildcardFileFilter("*.osm");
            File[] dirFiles = directory.listFiles(fileFilter);
            if(dirFiles.length > 0) {
                Arrays.sort(dirFiles, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
                if(dirFiles[0].getName().equals(fileName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public ArrayList<Long> getPreviousOSMEditFileStructureIds() {
        return previousOSMEditFileStructureIds;
    }
    
    public List<File> getEditedOSM() {
        return editedOSM;
    }

    public String getFormId() {
        return formId;
    }

    public String getFormFileName() {
        return formFileName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getInstanceDir() {
        return instanceDir;
    }

    public Collection<ODKTag> getRequiredTags() {
        return requiredTags.values();
    }

    public String getGeoContext() {
        return this.geoContext;
    }

    /**
     * Returns the ODK defined label for a OSM tag key if exists
     * * * 
     * @param key
     * @return
     */
    public String getTagKeyLabel(String key) {
        ODKTag tag = requiredTags.get(key);
        if (tag != null) {
            return tag.getLabel();
        }
        return null;
    }

    /**
     * Returns the ODK defined label for an OSM tag value if exists
     * * * 
     * @param key
     * @param value
     * @return
     */
    public String getTagValueLabel(String key, String value) {
        ODKTag tag = requiredTags.get(key);
        if (tag != null) {
            ODKTagItem item = tag.getItem(value);
            return item.getLabel();
        }
        return null;
    }
    
    public void consumeOSMElement(OSMElement el, String osmUserName) throws IOException {
        checksum = el.checksum();
        editedXml = OSMXmlWriter.elementToString(el, osmUserName, APP_NAME + " " + appVersion);
    }
    
    public void deleteOldOSMEdit() {
        if (previousOSMEditFileName == null) {
            return;
        }
        File f = getPreviousEditedFile();
        if (f != null && f.exists()) {
            f.delete();
        }
    }

    private File getPreviousEditedFile() {
        if (previousOSMEditFileName != null && instanceDir != null) {
            String path = instanceDir + '/' + previousOSMEditFileName;
            return new File(path);
        }

        return null;
    }
    
    public void writeXmlToOdkCollectInstanceDir() throws IOException {
        if ( ! isODKCollectInstanceDirectoryAvailable() ) {
            throw new IOException("The ODK Collect Instance Directory cannot be accessed!");
        }
        File f = new File( getOSMFileFullPath() );
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        OutputStreamWriter writer = new OutputStreamWriter(fos);
        writer.append(editedXml);
        writer.close();
        fos.close();
    }

    public String getOSMFileName() {
        return checksum + ".osm";
    }
    
    public String getOSMFileFullPath() {
        return instanceDir + "/" + getOSMFileName();
    }

    
    private boolean isODKCollectInstanceDirectoryAvailable() {
        if ( ! ExternalStorage.isWritable() ) {
            return false;
        }
        File dir = new File(instanceDir);
        if (dir.exists()) {
            return true;
        }
        return false;
    }
    
}
