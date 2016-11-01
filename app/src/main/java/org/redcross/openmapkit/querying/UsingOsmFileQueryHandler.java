package org.redcross.openmapkit.querying;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.spatialdev.osm.model.OSMColorConfig;
import com.spatialdev.osm.model.OSMDataSet;
import com.spatialdev.osm.model.OSMNode;
import com.spatialdev.osm.model.OSMWay;

import org.redcross.openmapkit.ExternalStorage;
import org.redcross.openmapkit.OSMXmlParserInOSMMapBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jason Rogena - jrogena@ona.io on 25/10/2016.
 */

public class UsingOsmFileQueryHandler extends QueryHandler {
    public static final String KEY_OSM_FILE = "osm_file";
    public static final String KEY_TAG = "tag";
    private final Context context;

    public UsingOsmFileQueryHandler(Context context) {
        this.context = context;
    }

    @Override
    public void handle(Intent intent, OnQueryCompleteListener onQueryCompleteListener) {
        new Task(intent, onQueryCompleteListener).execute();
    }

    private static String getTagInOsmFile(File osmFile, String tag) {
        try {
            InputStream inputStream = new FileInputStream(osmFile);
            OSMDataSet osmDataSet = OSMXmlParserInOSMMapBuilder.parseFromInputStream(inputStream, OSMColorConfig.getDefaultConfig());
            List<OSMWay> closedWays = osmDataSet.getClosedWays();
            for(OSMWay curWay : closedWays) {
                if(curWay.getTags().containsKey(tag)
                        && curWay.getTags().get(tag) != null
                        && curWay.getTags().get(tag).length() > 0) {
                    return curWay.getTags().get(tag);
                }
            }

            List<OSMWay> openWays = osmDataSet.getOpenWays();
            for(OSMWay curWay : openWays) {
                if(curWay.getTags().containsKey(tag)
                        && curWay.getTags().get(tag) != null
                        && curWay.getTags().get(tag).length() > 0) {
                    return curWay.getTags().get(tag);
                }
            }

            List<OSMNode> standaloneNodes = osmDataSet.getStandaloneNodes();
            for(OSMNode curNode : standaloneNodes) {
                if(curNode.getTags().containsKey(tag)
                        && curNode.getTags().get(tag) != null
                        && curNode.getTags().get(tag).length() > 0) {
                    return curNode.getTags().get(tag);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DEFAULT_RESULT;
    }

    @Override
    public boolean canHandle(Intent intent) {
        if(intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            if(bundle.containsKey(KEY_OSM_FILE)
                    && bundle.getString(KEY_OSM_FILE) != null
                    && bundle.getString(KEY_OSM_FILE).length() > 0) {
                return true;
            }
        }
        return false;
    }

    private static class Task extends AsyncTask<Void, Void, Intent> {
        private final Intent queryIntent;
        private final OnQueryCompleteListener onQueryCompleteListener;

        public Task(Intent queryIntent, OnQueryCompleteListener onQueryCompleteListener) {
            this.queryIntent = queryIntent;
            this.onQueryCompleteListener = onQueryCompleteListener;
        }

        @Override
        protected Intent doInBackground(Void... voids) {
            final String queryOsmFile = queryIntent.getExtras().getString(KEY_OSM_FILE);
            final ArrayList<String> queryTags = new ArrayList<>();
            String mainQueryTag = null;
            for(String currExtra : queryIntent.getExtras().keySet()) {
                if(currExtra.equals(KEY_TAG)) {
                    if(queryIntent.getExtras().getString(KEY_TAG) != null
                            && queryIntent.getExtras().getString(KEY_TAG).length() > 0) {
                        mainQueryTag = queryIntent.getExtras().getString(KEY_TAG);
                        queryTags.add(queryIntent.getExtras().getString(KEY_TAG));
                    }
                } else if(!currExtra.equals(KEY_OSM_FILE)) {
                    queryTags.add(currExtra);
                }
            }

            Intent result = new Intent();
            result.putExtra(KEY_INTENT_RESULT, DEFAULT_RESULT);
            //get a list of all the available OSM files
            List<File> osmFiles = new ArrayList<>();
            ExternalStorage.findAllOsmFileInDir(ExternalStorage.odkDirectory(), osmFiles);
            ExternalStorage.findAllOsmFileInDir(new File(ExternalStorage.getOSMDir()), osmFiles);

            for(File curOsmFile : osmFiles) {
                if(curOsmFile.getName().trim().equals(queryOsmFile.trim())) {
                    for(String  queryTag : queryTags) {
                        String tagValue = getTagInOsmFile(curOsmFile, queryTag);
                        if(tagValue != null) {
                            if (mainQueryTag != null && mainQueryTag.equals(queryTag)) {
                                result.putExtra(KEY_INTENT_RESULT, tagValue);
                            } else {
                                result.putExtra(queryTag, tagValue);
                            }
                        }
                    }
                    break;
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Intent intent) {
            super.onPostExecute(intent);
            onQueryCompleteListener.onSuccess(intent);
        }
    }
}
