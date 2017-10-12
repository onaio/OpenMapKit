package org.redcross.openmapkit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TouchDelegate;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.spatialdev.osm.model.OSMWay;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import com.spatialdev.osm.OSMMap;
import com.spatialdev.osm.events.OSMSelectionListener;
import com.spatialdev.osm.model.OSMElement;
import com.spatialdev.osm.model.OSMNode;
import com.vividsolutions.jts.geom.Point;

import org.fieldpapers.listeners.FPListener;
import org.fieldpapers.model.FPAtlas;
import org.json.JSONException;
import org.redcross.openmapkit.deployments.DeploymentsActivity;
import org.redcross.openmapkit.odkcollect.Form;
import org.redcross.openmapkit.odkcollect.FormOSMDownloader;
import org.redcross.openmapkit.odkcollect.ODKCollectHandler;
import org.redcross.openmapkit.odkcollect.tag.ODKTag;
import org.redcross.openmapkit.server.MBTilesServer;
import org.redcross.openmapkit.tagswipe.TagSwipeActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MapActivity extends AppCompatActivity implements OSMSelectionListener, FPListener, FormOSMDownloader.OnFileDownload {
    protected static final String PREVIOUS_LAT = "org.redcross.openmapkit.PREVIOUS_LAT";
    protected static final String PREVIOUS_LNG = "org.redcross.openmapkit.PREVIOUS_LNG";
    protected static final String PREVIOUS_ZOOM = "org.redcross.openmapkit.PREVIOUS_ZOOM";
    protected static final String ODK_OMK_QUERY_VAL = "org.redcross.openmapkit.ODK_QUERY_VAL";
    public static final ArrayList<Integer> MENU_ITEM_IDS;
    static{
        MENU_ITEM_IDS = new ArrayList<>();
        MENU_ITEM_IDS.add(R.id.deployments);
        MENU_ITEM_IDS.add(R.id.basemaps);
        MENU_ITEM_IDS.add(R.id.osmcredentials);
        MENU_ITEM_IDS.add(R.id.osmdownloader);
        MENU_ITEM_IDS.add(R.id.osmXmlLayers);
        MENU_ITEM_IDS.add(R.id.osmFromODK);
        MENU_ITEM_IDS.add(R.id.info);
    }
    
    private static String version = "";

    protected MapView mapView;
    protected OSMMap osmMap;
    protected TextView fieldPapersMsg;
    protected ListView mTagListView;
    protected ImageButton mCloseListViewButton;
    protected ImageButton tagButton;
    protected ImageButton moveButton;
    protected ImageButton deleteButton;
    protected Button nodeModeButton;
    protected Button addTagsButton;
    protected LinearLayout mTopLinearLayout;
    protected LinearLayout mBottomLinearLayout;
    protected TextView mTagTextView;
    protected Basemap basemap;
    protected TagListAdapter tagListAdapter;
    protected Menu menu;
    protected TextView gpsAccuracyView;

    private boolean nodeMode = false;
    private boolean moveNodeMode = false;
    private Dialog gpsCountdownDialog;
    private AlertDialog mbtilesDialog;
    private AlertDialog deleteNodeDialog;
    public static final int TASK_INTERVAL_IN_MILLIS = 1000;
    private Timer mTimer;
    protected TimerTask timerTask;
    protected int initialCountdownValue;
    private LocationListener locationListener;
    private android.support.v7.app.AlertDialog gpsProviderAlertDialog;
    private Location lastLocation;
    private Location userLocation;//the location to be recorded in the user location OSM tags

    private AppCompatDialog odkQueryDialog;
    private HashMap<Integer, Form> downloadingForms, successfulForms;
    private ProgressDialog progressDialog;
    private DownloadManager downloadManager;
    private boolean locationPreviouslyEnabled;// Used to track in onResume whether onPause disabled location providers

    /**
     * Which GPS provider should be used to get the User's current location
     */
    public static String preferredLocationProvider = LocationManager.GPS_PROVIDER;

    /**
     * intent request codes
     */
    private static final int ODK_COLLECT_TAG_ACTIVITY_CODE = 2015;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Turn on MBTiles HTTP server.
        /**
         * We are waiting to enable this until we need it for a new map renderer.
         */
//        initializeMBTilesServer();

        determineVersion();
        
        if(android.os.Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.osm_light_green));
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.mipmap.ic_omk_nobg);
        }


        // create directory structure for app if needed
        ExternalStorage.checkOrCreateAppDirs();

        // Move constraints assets to ExternalStorage if necessary
        ExternalStorage.copyConstraintsToExternalStorageIfNeeded(this);

        // Move OSM files in assets to external storage
        ExternalStorage.copyOsmToExternalStorageIfNeeded(this);

        // Register the intent to the ODKCollect handler
        // This will determine if we are in ODK Collect Mode or not.
        ODKCollectHandler.registerIntent(getApplicationContext(), getIntent());

        // Copy over OSM files from ODK media directory
        if(ODKCollectHandler.isODKCollectMode()) {
            ArrayList<File[]> mediaFilesToCopy = new ArrayList<>();
            mediaFilesToCopy.addAll(ExternalStorage.getMbtilesFilesToCopyFromOdkMediaDir(ODKCollectHandler.getODKCollectData().getFormFileName()));
            mediaFilesToCopy.addAll(ExternalStorage.getOsmFilesToCopyFromOdkMediaDir(ODKCollectHandler.getODKCollectData().getFormFileName()));
            if(mediaFilesToCopy.size() > 0) {
                new ExternalStorage.FileCopyAsyncTask(this, mediaFilesToCopy).execute();
            }
        }

        // Initialize the constraints singleton.
        // Loads up all the constraints JSON configs.
        Constraints.initialize();

        //set layout
        setContentView(R.layout.activity_map);

        //get the layout the ListView is nested in
        mBottomLinearLayout = (LinearLayout)findViewById(R.id.bottomLinearLayout);

        //the ListView from layout
        mTagListView = (ListView)findViewById(R.id.tagListView);

        //the ListView close image button
        mCloseListViewButton = (ImageButton)findViewById(R.id.imageViewCloseList);

        //get the layout the Map is nested in
        mTopLinearLayout = (LinearLayout)findViewById(R.id.topLinearLayout);

        //get map from layout
        mapView = (MapView)findViewById(R.id.mapView);

        // get Field Papers Message
        fieldPapersMsg = (TextView)findViewById(R.id.fieldPapersMsg);
        
        // initialize basemap object
        basemap = new Basemap(this);

        initializeFP();

        initializeOsmXml();

        // add user location toggle button
        initializeLocationButton();

        // setup move button
        initializeMoveButton();

        initializeNodeModeButton();
        initializeAddNodeButtons();
        initializeMoveNodeButtons();
        initializeDeleteNodeButton();
        initializeGpsAccuracyView();

        positionMap();

        initializeListView();
        initLocationListener();

        // Proximity is disabled until there is a GPS fix.
        Settings.setProximityEnabled(false);
        if (Settings.singleton().getProximityCheck()) {
            mapView.setProximityRadius(Settings.singleton().getProximityRadius());
            // Start GPS progress
            initialCountdownValue = Settings.singleton().getGpsTimerDelay();
            showProgressDialog();
        }

        locationPreviouslyEnabled = false;
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        cleanOsmFilesFromOdkInstances();
        checkIfOsmFilesNeedLoading();
    }

    /**
     * This method checks if there are currently no loaded OSM files
     */
    private void checkIfOsmFilesNeedLoading() {
        final File[] osmFiles = ExternalStorage.fetchOSMXmlFiles();
        if(osmFiles.length > 0) {
            final boolean[] checkedOsmFiles = OSMMapBuilder.isFileArraySelected(osmFiles);
            boolean needsPresenting = true;
            for(int i = 0; i < checkedOsmFiles.length; i++) {
                if(checkedOsmFiles[i]) {
                    needsPresenting = false;
                    break;
                }
            }

            if(needsPresenting) {
                presentOSMOptions();
            }
        }
    }

    private void initLocationListener() {
        checkLocationProviderEnabled();
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                lastLocation = location;
                if(lastLocation != null) {
                    gpsAccuracyView.setText("GPS Accuracy "+lastLocation.getAccuracy()+"m");
                } else {
                    gpsAccuracyView.setText("GPS Accuracy Unknown");
                }

                if(Settings.singleton().getProximityCheck()) {
                    if(location.getAccuracy() <= Settings.singleton().getGpsProximityAccuracy()) {
                        if(!Settings.singleton().isProximityEnabled()) {
                            //means this is the first time a location fix for the user has been gotten
                            goToUserLocation();
                        }
                        Settings.singleton().setProximityEnabled(true);
                    }
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                checkLocationProviderEnabled();
                Settings.singleton().updateGpsEnabled(MapActivity.this);
            }

            @Override
            public void onProviderEnabled(String s) {
                Log.d("GPSTest", "onProviderEnabled called");
                Settings.singleton().updateGpsEnabled(MapActivity.this);
            }

            @Override
            public void onProviderDisabled(String s) {
                Log.d("GPSTest", "onProviderDisabled called");
                checkLocationProviderEnabled();
                Settings.singleton().updateGpsEnabled(MapActivity.this);
            }
        };

        mapView.addLocationListener(locationListener);
    }


    /**
     * This method checks whether the preferred location provider is enabled in the device and shows
     * the gpsProviderAlertDialog if not
     *
     * @return TRUE if the preferred location provider is enabled
     */
    public boolean checkLocationProviderEnabled() {
        if(isGPSEnabled()) {
            if(gpsProviderAlertDialog != null) {
                gpsProviderAlertDialog.dismiss();
            }
            return true;
        }

        //if we've reached this point, it means the location provider is not enabled
        //show the enable location provider dialog
        if(gpsProviderAlertDialog == null) {
            gpsProviderAlertDialog = new android.support.v7.app.AlertDialog.Builder(MapActivity.this)
                    .setMessage(getResources().getString(R.string.enable_gps))
                    .setPositiveButton("Enable GPS", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (isGPSEnabled()) {
                                dialogInterface.dismiss();
                            } else {
                                MapActivity.this.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                            MapActivity.this.finish();
                        }
                    })
                    .setNegativeButton("Continue", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .setCancelable(false)
                    .create();
        }
        gpsProviderAlertDialog.show();

        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationPreviouslyEnabled) {
            mapView.setUserLocationEnabled(true, getLocationStrategy());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveMapPosition();
        locationPreviouslyEnabled = mapView.isUserLocationEnabled();
        if (locationPreviouslyEnabled) {
            mapView.setUserLocationEnabled(false, getLocationStrategy());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void saveMapPosition() {
        LatLng c = mapView.getCenter();
        float lat = (float) c.getLatitude();
        float lng = (float) c.getLongitude();
        float z = mapView.getZoomLevel();

        SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
        editor.putFloat(PREVIOUS_LAT, lat);
        editor.putFloat(PREVIOUS_LNG, lng);
        editor.putFloat(PREVIOUS_ZOOM, z);
        editor.apply();
    }

    protected void positionMap() {
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        double lat = (double) pref.getFloat(PREVIOUS_LAT, -999);
        double lng = (double) pref.getFloat(PREVIOUS_LNG, -999);
        float z = pref.getFloat(PREVIOUS_ZOOM, -999);

        // no shared pref
        if (lat == -999 || lng == -999 || z == -999) {
            mapView.setUserLocationEnabled(true, getLocationStrategy());
            mapView.goToUserLocation(true);
        }
        // there is a shared pref
        else {
            LatLng c = new LatLng(lat, lng);
            mapView.setCenter(c);
            mapView.setZoom(z);
        }
    }
    
    /**
     * For initializing the ListView of tags
     */
    protected void initializeListView() {

        //the ListView title
        mTagTextView = (TextView)findViewById(R.id.tagTextView);
        mTagTextView.setText(R.string.tagListViewTitle);

        //hide the ListView by default
        proportionMapAndList(100, 0);

        //handle when user taps on the close button in the list view
        mCloseListViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                proportionMapAndList(100, 0);
            }
        });

        //increase the 'hit area' of the down arrow
        View parent = findViewById(R.id.bottomLinearLayout);
        parent.post(new Runnable() {
            public void run() {

                Rect delegateArea = new Rect();
                ImageButton delegate = mCloseListViewButton;
                delegate.getHitRect(delegateArea);
                delegateArea.top -= 100;
                delegateArea.bottom += 100;
                delegateArea.left -= 100;
                delegateArea.right += 100;

                TouchDelegate expandedArea = new TouchDelegate(delegateArea, delegate);

                if (View.class.isInstance(delegate.getParent())) {
                    ((View) delegate.getParent()).setTouchDelegate(expandedArea);
                }
            }
        });

        View.OnClickListener tagSwipeLaunchListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ODKCollectHandler.isODKCollectMode()) {
                    if(!Settings.singleton().isUserLocationTagsEnabled() || userLocation != null) {
                        //launch the TagSwipeActivity
                        Intent tagSwipe = new Intent(getApplicationContext(), TagSwipeActivity.class);
                        if(userLocation != null) tagSwipe.putExtra(TagSwipeActivity.KEY_USER_LOCATION, userLocation);
                        startActivityForResult(tagSwipe, ODK_COLLECT_TAG_ACTIVITY_CODE);
                    } else {
                        Toast.makeText(MapActivity.this, R.string.waiting_for_accurate_location, Toast.LENGTH_LONG).show();
                    }
                } else {
                    launchODKCollectSnackbar();
                }
            }
        };
        // tag button
        tagButton = (ImageButton)findViewById(R.id.tagButton);
        tagButton.setOnClickListener(tagSwipeLaunchListener);

        // add tags button
        addTagsButton = (Button) findViewById(R.id.addTagsBtn);
        addTagsButton.setOnClickListener(tagSwipeLaunchListener);

        //handle list view item taps
        mTagListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (ODKCollectHandler.isODKCollectMode()) {
                    if (Settings.singleton().getClickableTags()) {
                        if (!Settings.singleton().isUserLocationTagsEnabled() || userLocation != null) {
                            String tappedKey = tagListAdapter.getTagKeyForIndex(position);
                            Intent tagSwipe = new Intent(getApplicationContext(), TagSwipeActivity.class);
                            tagSwipe.putExtra("TAG_KEY", tappedKey);
                            tagSwipe.putExtra(TagSwipeActivity.KEY_USER_LOCATION, userLocation);
                            startActivityForResult(tagSwipe, ODK_COLLECT_TAG_ACTIVITY_CODE);
                        } else {
                            Toast.makeText(MapActivity.this, R.string.waiting_for_accurate_location, Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    launchODKCollectSnackbar();
                }
            }
        });
    }

    private void initializeGpsAccuracyView() {
        gpsAccuracyView = (TextView) findViewById(R.id.gpsAccuracyView);
        if(Settings.singleton().getProximityCheck()) {
            gpsAccuracyView.setVisibility(View.VISIBLE);
        } else {
            gpsAccuracyView.setVisibility(View.GONE);
        }
    }

    /**
     * For identifying an OSM element and presenting it's tags in the ListView
     * @param osmElement The target OSMElement.
     */
    protected void identifyOSMFeature(OSMElement osmElement) {
        Log.d("ColorTest", "identifyOSMFeature called");
        // only open it if we render the OSM vectors,
        // otherwise it is confusing for the user
        if (mapView.getZoomLevel() < Settings.singleton().getMinVectorRenderZoom()) {
            return;
        }

        //enable the delete button for newly added features
        if(osmElement.getId() < 0) {
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            deleteButton.setVisibility(View.GONE);
        }

        int numRequiredTags = 0;
        if (ODKCollectHandler.isODKCollectMode()) {
            Collection<ODKTag> requiredTags = ODKCollectHandler.getODKCollectData().getRequiredTags();
            numRequiredTags = requiredTags.size();
        }
        int tagCount = osmElement.getTags().size();

        if (tagCount > 0 || numRequiredTags > 0) {
            mTagListView.setVisibility(View.VISIBLE);
            addTagsButton.setVisibility(View.GONE);
        } else {
            mTagListView.setVisibility(View.GONE);
            addTagsButton.setVisibility(View.VISIBLE);
        }

        /**
         * If we have a node that is selected, we want to activate the
         * move button. Ways should not be editable.
         */
        if (osmElement instanceof OSMNode) {
            moveButton.setVisibility(View.VISIBLE);
        } else {
            moveButton.setVisibility(View.GONE);
        }

        //pass the tags to the list adapter
        tagListAdapter = new TagListAdapter(this, osmElement);

        //set the ListView's adapter
        mTagListView.setAdapter(tagListAdapter);

        //show the ListView under the map
        proportionMapAndList(50, 50);
    }

    private void launchDeleteDialog() {
        if(deleteNodeDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(String.format(getResources().getString(R.string.confirm_delete_node), Settings.singleton().getNodeName()));
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    deleteNode();
                    dialogInterface.dismiss();
                    proportionMapAndList(100, 0);
                }
            });

            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            deleteNodeDialog = builder.show();
        } else {
            deleteNodeDialog.show();
        }
    }

    /**
     * For setting the proportions of the Map weight and the ListView weight for dual display
     * @param topWeight Refers to the layout weight.  Note, topWeight + bottomWeight must equal the weight sum of 100
     * @param bottomWeight Referes to the layotu height.  Note, bottomWeight + topWeight must equal the weight sum of 100
     */
    protected void proportionMapAndList(int topWeight, int bottomWeight) {

        LinearLayout.LayoutParams topLayoutParams = (LinearLayout.LayoutParams)mTopLinearLayout.getLayoutParams();
        LinearLayout.LayoutParams bottomLayoutParams = (LinearLayout.LayoutParams)mBottomLinearLayout.getLayoutParams();

        //update weight of top and bottom linear layouts
        mTopLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(topLayoutParams.width, topLayoutParams.height, topWeight));
        mBottomLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(bottomLayoutParams.width, bottomLayoutParams.height, bottomWeight));
    }

    /**
     * Adds FieldPapers Overlay to the map (if we have one).
     */
    protected void initializeFP() {
        try {
            FPAtlas.addToMap(this, mapView);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads OSM XML stored on the device.
     */
    protected void initializeOsmXml() {
        try {
            OSMMapBuilder.buildMapFromExternalStorage(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * For instantiating the location button and setting up its tap event handler
     */
    protected void initializeLocationButton() {
        final ImageButton locationButton = (ImageButton)findViewById(R.id.locationButton);

        //set tap event
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleUserLocation(true);
            }
        });
    }

    private boolean isUserLocationEnabled() {
        return mapView.getUserLocationEnabled();
    }

    private GpsLocationProvider.LocationStrategy getLocationStrategy() {
        if (ODKCollectHandler.isODKCollectMode() && Settings.singleton() != null) {
            return Settings.singleton().getLocationStrategy();
        }

        return GpsLocationProvider.LocationStrategy.LOCATION_MANAGER;
    }

    private void toggleUserLocation(boolean goToUserLocation) {
        final ImageButton locationButton = (ImageButton)findViewById(R.id.locationButton);
        boolean userLocationIsEnabled = isUserLocationEnabled();
        if (userLocationIsEnabled) {
            mapView.setUserLocationEnabled(false, getLocationStrategy());
            locationButton.setBackground(getResources().getDrawable(R.drawable.roundedbutton));
        } else {
            mapView.setUserLocationEnabled(true, getLocationStrategy());
            if(goToUserLocation) mapView.goToUserLocation(true);
            locationButton.setBackground(getResources().getDrawable(R.drawable.roundedbutton_blue));
        }
    }

    protected void initializeMoveButton() {
        moveButton = (ImageButton)findViewById(R.id.moveNodeModeBtn);
        moveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMoveNodeMode();
            }
        });
    }
    
    protected void initializeNodeModeButton() {
        nodeModeButton = (Button)findViewById(R.id.nodeModeButton);
        nodeModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNodeMode();
            }
        });
        if(Settings.singleton().shouldShowAddNode() == false) {
            nodeModeButton.setVisibility(View.GONE);
        }
    }

    protected void initializeAddNodeButtons() {
        final Button addNodeBtn = (Button)findViewById(R.id.addNodeBtn);
        addNodeBtn.setText(String.format(getResources().getString(R.string.add_node), Settings.singleton().getNodeName()));
        final ImageButton addNodeMarkerBtn = (ImageButton)findViewById(R.id.addNodeMarkerBtn);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OSMNode node = osmMap.addNode();
                toggleNodeMode();
                node.select();
                identifyOSMFeature(node);
            }
        };
        addNodeMarkerBtn.setOnClickListener(listener);
        addNodeBtn.setOnClickListener(listener);
    }

    protected void initializeMoveNodeButtons() {
        final Button moveNodeBtn = (Button)findViewById(R.id.moveNodeBtn);
        final ImageButton moveNodeMarkerBtn = (ImageButton)findViewById(R.id.moveNodeMarkerBtn);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                osmMap.moveNode();
                toggleMoveNodeMode();
            }
        };
        moveNodeBtn.setOnClickListener(listener);
        moveNodeMarkerBtn.setOnClickListener(listener);
    }

    protected void initializeDeleteNodeButton() {
        deleteButton = (ImageButton)findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchDeleteDialog();
            }
        });
    }

    private void toggleNodeMode() {
        final Button addNodeBtn = (Button)findViewById(R.id.addNodeBtn);
        final ImageButton addNodeMarkerBtn = (ImageButton)findViewById(R.id.addNodeMarkerBtn);
        if (nodeMode) {
            addNodeBtn.setVisibility(View.GONE);
            addNodeMarkerBtn.setVisibility(View.GONE);
            nodeModeButton.setBackground(getResources().getDrawable(R.drawable.roundedbutton));
            mapView.setInteractionEnabled(true);
        } else {
            if(recordUserLocation()) {
                if (Settings.singleton().getProximityCheck()) {
                    //check user's last location is accurate enough
                    if (lastLocation != null && lastLocation.getAccuracy() <= Settings.singleton().getGpsProximityAccuracy()) {
                        goToUserLocation();
                        mapView.goToUserLocation(true);
                        mapView.setInteractionEnabled(false);
                    } else {
                        Toast.makeText(this, getResources().getString(R.string.waiting_for_accurate_location), Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                addNodeBtn.setVisibility(View.VISIBLE);
                addNodeMarkerBtn.setVisibility(View.VISIBLE);
                nodeModeButton.setBackground(getResources().getDrawable(R.drawable.roundedbutton_green));
                OSMElement.deselectAll();
                mapView.invalidate();
            }
        }
        nodeMode = !nodeMode;
    }

    private void goToUserLocation() {
        if (!isUserLocationEnabled()) {
            toggleUserLocation(true);
        } else {
            mapView.goToUserLocation(true);
        }
    }

    /**
     * This method records the user's current location if the user location tags setting is turned on
     *
     * @return  TRUE if the user location tags setting is turned off or if the user's location is
     *          successfully recorded
     */
    private boolean recordUserLocation() {
        boolean result = true;
        //make sure the user's location is being gotten if user location tags are going to be collected
        if(Settings.singleton().isUserLocationTagsEnabled()) {
            userLocation = null;
            //make sure we can get a user location
            if(!isUserLocationEnabled()) toggleUserLocation(false);

            if(lastLocation != null && (Calendar.getInstance().getTimeInMillis() - lastLocation.getTime()) < 60000l) {
                userLocation = new Location(lastLocation);
            } else if(lastLocation != null
                    && Settings.singleton().getProximityCheck()
                    && (Calendar.getInstance().getTimeInMillis() - lastLocation.getTime()) >= 60000l) {
                gpsAccuracyView.setText("GPS Location Expired");
                double timeDiff = ((double)(Calendar.getInstance().getTimeInMillis() - lastLocation.getTime())/60000);
                Toast.makeText(this, "Last location was collected "+((int)timeDiff)+"min ago", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.waiting_for_accurate_location, Toast.LENGTH_LONG).show();
            }

            if(userLocation == null) {
                proportionMapAndList(100, 0);
                result = false;
            }
        }

        return result;
    }

    private void deleteNode() {
        final OSMNode deletedNode = osmMap.deleteNode();

        Snackbar.make(findViewById(R.id.mapActivity),
                String.format(getResources().getString(R.string.deleted_node), Settings.singleton().getNodeName()),
                Snackbar.LENGTH_LONG)
                .setAction("UNDO", new View.OnClickListener() {
                    // undo action
                    @Override
                    public void onClick(View v) {
                        osmMap.addNode(deletedNode);
                    }
                })
                .setActionTextColor(Color.rgb(126,188,111))
                .show();
    }

    private void toggleMoveNodeMode() {
        if(!Settings.singleton().getProximityCheck()) {
            final ImageButton moveNodeModeBtn = (ImageButton)findViewById(R.id.moveNodeModeBtn);
            final ImageButton moveNodeMarkerBtn = (ImageButton)findViewById(R.id.moveNodeMarkerBtn);
            final Button moveNodeBtn = (Button)findViewById(R.id.moveNodeBtn);
            if (moveNodeMode) {
                moveNodeMarkerBtn.setVisibility(View.GONE);
                moveNodeBtn.setVisibility(View.GONE);
                moveNodeModeBtn.setBackground(getResources().getDrawable(R.drawable.roundedbutton));
                showSelectedMarker();
            } else {
                moveNodeMarkerBtn.setVisibility(View.VISIBLE);
                moveNodeBtn.setVisibility(View.VISIBLE);
                moveNodeModeBtn.setBackground(getResources().getDrawable(R.drawable.roundedbutton_orange));
                hideSelectedMarker();
                proportionMapAndList(100, 0);
            }
            moveNodeMode = !moveNodeMode;
        }
    }

    private void hideSelectedMarker() {
        LinkedList<OSMElement> selectedElements = OSMElement.getSelectedElements();
        if (selectedElements.size() < 1) return;
        OSMNode node = (OSMNode)selectedElements.getFirst();
        Marker marker = node.getMarker();
        if (marker != null) {
            node.getMarker().setVisibility(false);
        }
        mapView.invalidate();
    }

    private void showSelectedMarker() {
        LinkedList<OSMElement> selectedElements = OSMElement.getSelectedElements();
        if (selectedElements.size() < 1) return;
        OSMNode node = (OSMNode)selectedElements.getFirst();
        Marker marker = node.getMarker();
        if (marker != null) {
            node.getMarker().setVisibility(true);
        }
        mapView.invalidate();
    }

    /**
     * For presenting a dialog to allow the user to choose which OSM XML files to use.
     */
    private void presentOSMOptions() {
        final File[] osmFiles = ExternalStorage.fetchOSMXmlFiles();
        String[] osmFileNames = ExternalStorage.fetchOSMXmlFileNames();
        final boolean[] checkedOsmFiles = OSMMapBuilder.isFileArraySelected(osmFiles);
        final Set<File> filesToAdd = new HashSet<>();
        final Set<File> filesToRemove = new HashSet<>();

        if (osmFileNames.length > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.osmChooserDialogTitle));
            builder.setMultiChoiceItems(osmFileNames, checkedOsmFiles, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i, boolean isChecked) {
                    // load the file
                    if (isChecked) {
                        File fileToAdd = osmFiles[i];
                        filesToAdd.add(fileToAdd);
                    }
                    // remove the file
                    else {
                        File fileToRemove = osmFiles[i];
                        filesToRemove.add(fileToRemove);
                    }
                }
            });
            //handle OK tap event of dialog
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    OSMMapBuilder.removeOSMFilesFromModel(filesToRemove);
                    OSMMapBuilder.addOSMFilesToModel(filesToAdd);
                }
            });

            //handle cancel button tap event of dialog
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    //user clicked cancel
                }
            });
            builder.show();
        } else {
            Snackbar.make(findViewById(R.id.mapActivity),
                    "You have not yet downloaded any OSM data. Please check out a deployment or add .osm files to external storage.",
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK", new View.OnClickListener() {
                        // undo action
                        @Override
                        public void onClick(View v) {

                        }
                    })
                    .setActionTextColor(Color.rgb(126, 188, 111))
                    .show();
        }
    }

    private void inputOSMCredentials() {
        final SharedPreferences userNamePref = getSharedPreferences("org.redcross.openmapkit.USER_NAME", Context.MODE_PRIVATE);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("OpenStreetMap User Name");
        builder.setMessage("Please enter your OpenStreetMap user name.");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        String userName = userNamePref.getString("userName", null);
        if (userName != null) {
            input.setText(userName);
        }
        builder.setView(input);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // just dismiss
            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String userName = input.getText().toString();
                SharedPreferences.Editor editor = userNamePref.edit();
                editor.putString("userName", userName);
                editor.apply();
            }
        });
        builder.show();
    }

    private void askIfDownloadOSM() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.downloadOSMTitle);
        builder.setMessage(R.string.downloadOSMMessage);
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // just dismiss
            }
        });
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                downloadOSM();
            }
        });
        builder.show();
    }

    private void downloadOSM() {
        BoundingBox bbox = mapView.getBoundingBox();
        OSMDownloader downloader = new OSMDownloader(this, bbox);
        downloader.execute();
    }

    private void showInfo() {
        Snackbar.make(findViewById(R.id.mapActivity),
                "Version: " + MapActivity.getVersion(),
                Snackbar.LENGTH_INDEFINITE)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                })
                .setActionTextColor(Color.rgb(126, 188, 111))
                .show();
    }

    /**
     * OSMMapBuilder sets a reference to OSMMap in this class.
     *
     * @param osmMap
     */
    public void setOSMMap(OSMMap osmMap) {
        this.osmMap = osmMap;
    }

    /**
     * For adding action items to the action bar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map, menu);
        this.menu = menu;
        hideMenuItems();
        return true;
    }

    public Menu getMenu() {
        return menu;
    }

    private void hideMenuItems() {
        if(!ODKCollectHandler.isODKCollectMode()
                || Settings.singleton().getOSMFromODKForms().size() == 0) {
            menu.findItem(R.id.osmFromODK).setVisible(false);
        }

        ArrayList<String> hiddenMenuItems = Settings.singleton().getHiddenMenuItems();
        if(hiddenMenuItems.size() > 0) {
            int hiddenItems = 0;
            for (int currMenuItemId : MENU_ITEM_IDS) {
                if (hiddenMenuItems.contains(menu.findItem(currMenuItemId).getTitle().toString().toLowerCase())) {
                    menu.findItem(currMenuItemId).setVisible(false);
                    hiddenItems++;
                }
            }

            if(hiddenItems == MENU_ITEM_IDS.size()) {
                menu.clear();
            }
        }
    }

    /**
     * For handling when a user taps on a menu item (top right)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        super.onOptionsItemSelected(item);
                
        int id = item.getItemId();

        if (id == R.id.deployments) {
            launchDeploymentsActivity();
            return true;
        } else if (id == R.id.osmdownloader) {
            askIfDownloadOSM();
            return true;
        } else if (id == R.id.basemaps) {
            basemap.presentBasemapsOptions();
            return true;
        } else if (id == R.id.osmcredentials) {
            inputOSMCredentials();
            return true;
        } else if (id == R.id.osmXmlLayers) {
            presentOSMOptions();
            return true;
        } else if (id == R.id.info) {
            showInfo();
            return true;
        } else if (id == R.id.action_save_to_odk_collect) {
            saveToODKCollectAndExit();
            return true;
        } else if (id == R.id.osmFromODK) {
            showOdkQueryDialog();
        }
        return false;
    }

    private void launchDeploymentsActivity() {
        Intent deploymentsActivity = new Intent(getApplicationContext(), DeploymentsActivity.class);
        startActivity(deploymentsActivity);
    }

    public void clickMbtilesPositiveButton() {
        if(mbtilesDialog != null) {
            mbtilesDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        }
    }

    public void zoomToRecommendedLevel() {
        mapView.setZoom(Settings.singleton().getMinVectorRenderZoom());
    }

    @Override
    public void selectedElementsChanged(LinkedList<OSMElement> selectedElements) {
        if(recordUserLocation()) {
            if (selectedElements != null && selectedElements.size() > 0) {
                //fetch the tapped feature
                OSMElement tappedOSMElement = selectedElements.get(0);

                //zoom into location
                Geometry geometry = tappedOSMElement.getJTSGeom();
                Point elementCentroid = geometry.getCentroid();
                LatLng centroidLatLng = new LatLng(elementCentroid.getCoordinate().y, elementCentroid.getCoordinate().x);
                mapView.setCenter(centroidLatLng);

                //check whether the user is within the proximity
                if (isWithinUserProximity(tappedOSMElement)) {
                    //present OSM Feature tags in bottom ListView
                    identifyOSMFeature(tappedOSMElement);
                } else {
                    String warning = String.format(getResources().getString(R.string.need_to_be_close_node), Settings.singleton().getProximityRadius() + "m");
                    Toast.makeText(this, warning, Toast.LENGTH_LONG).show();
                    proportionMapAndList(100, 0);
                }
            }
        }
    }

    /**
     * This method checks whether the user is within the required proximity to the provided
     * {@link com.spatialdev.osm.model.OSMElement}
     *
     * @param tappedOSMOsmElement
     * @return TRUE if proximity settings are off or the user is within the set maximum proximity to
     *          the {@link com.spatialdev.osm.model.OSMElement}
     */
    public boolean isWithinUserProximity(OSMElement tappedOSMOsmElement) {
        if(Settings.singleton().getProximityCheck()) {
            if (tappedOSMOsmElement != null) {
                Geometry geometry = tappedOSMOsmElement.getJTSGeom();
                if(geometry != null && lastLocation != null) {
                    Coordinate[] coordinates = geometry.getCoordinates();
                    LatLng lastLocLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    for(int i = 0; i < coordinates.length; i++) {
                        Coordinate curCoordinate = coordinates[i];
                        LatLng curLatLng = new LatLng(curCoordinate.y, curCoordinate.x);
                        if(curLatLng.distanceTo(lastLocLatLng) <= Settings.singleton().getProximityRadius()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ( requestCode == ODK_COLLECT_TAG_ACTIVITY_CODE ) {
            if(resultCode == RESULT_OK) {
                saveToODKCollectAndExit();
            }
        }
    }

    protected void saveToODKCollectAndExit() {
        final String osmXmlFileFullPath = ODKCollectHandler.getODKCollectData().getOSMFileFullPath();
        final String osmXmlFileName = ODKCollectHandler.getODKCollectData().getOSMFileName();
        if(osmXmlFileName != null && !osmXmlFileName.equals("null.osm")) {
            PullCsvBuilder.updateCsv(OSMElement.getSelectedElements().getFirst(), osmXmlFileName, new PullCsvBuilder.OnPostUpdateListener() {
                private void sendData() {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("OSM_PATH", osmXmlFileFullPath);
                    resultIntent.putExtra("OSM_FILE_NAME", osmXmlFileName);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }

                @Override
                public void onUpdate(boolean status) {
                    sendData();
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MapActivity.this, "An error occurred while trying to save Pull CSV data", Toast.LENGTH_LONG).show();
                    sendData();
                }
            });
        } else {
            Intent resultIntent = new Intent();
            Toast.makeText(this, String.format(getResources().getString(R.string.an_error_occurred_retag), Settings.singleton().getNodeName()), Toast.LENGTH_LONG).show();
            setResult(Activity.RESULT_CANCELED, resultIntent);
            finish();
        }
    }
    
    public MapView getMapView() {
        return mapView;
    }
    
    private void determineVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public static String getVersion() {
        return version;
    }

    private void initializeMBTilesServer() {
        try {
            MBTilesServer.singleton().start();
        } catch(IOException ioe) {
            Log.w("Httpd", "MBTiles HTTP server could not start.");
        }
        Log.w("MBTilesServer", "MBTiles HTTP server initialized.");
    }

    private void launchODKCollectSnackbar() {
        if (isAppInstalled("org.odk.collect.android")) {
            Snackbar.make(findViewById(R.id.mapActivity),
                    "To edit tags, OpenMapKit must be launched from within an ODK Collect survey.",
                    Snackbar.LENGTH_LONG)
                    .setAction("Launch ODK Collect", new View.OnClickListener() {
                        // undo action
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setClassName("org.odk.collect.android", "org.odk.collect.android.activities.SplashScreenActivity");
                            startActivity(intent);
                        }
                    })
                    .setActionTextColor(Color.rgb(126, 188, 111))
                    .show();
        } else {
            Snackbar.make(findViewById(R.id.mapActivity),
                    "Please install ODK Collect.",
                    Snackbar.LENGTH_LONG)
                    .setAction("Launch Play Store", new View.OnClickListener() {
                        // undo action
                        @Override
                        public void onClick(View v) {
                            final String appPackageName = "org.odk.collect.android";
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                            } catch (android.content.ActivityNotFoundException anfe) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                            }
                        }
                    })
                    .setActionTextColor(Color.rgb(126, 188, 111))
                    .show();
        }

    }

    private boolean isAppInstalled(String uri) {
        PackageManager pm = getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

    @Override
    public void onMapCenterPageChangeMessage(String msg) {
        if (msg != null) {
            fieldPapersMsg.setText(msg);
            fieldPapersMsg.setVisibility(View.VISIBLE);
        } else {
            fieldPapersMsg.setVisibility(View.GONE);
        }
    }

    /**
     *
     * @return true if GPS is enabled.
     */
    private boolean isGPSEnabled() {
        LocationManager manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if ( manager.isProviderEnabled(preferredLocationProvider) ) {
            return true;
        }
        return false;
    }

    private void showProgressDialog() {
        goToUserLocation();
        // custom dialog
        gpsCountdownDialog = new Dialog(this);
        gpsCountdownDialog.setContentView(R.layout.dialog_gps_countdown);
        gpsCountdownDialog.setTitle(getResources().getString(R.string.getting_gps_fix));
        gpsCountdownDialog.setCancelable(false);
        gpsCountdownDialog.show();

        mTimer = new Timer();
        doCountDown();
    }

    private void doCountDown() {
        boolean foundGpsLocation = Settings.isProximityEnabled();
        if (initialCountdownValue-- == 0 || foundGpsLocation) {
            gpsCountdownDialog.dismiss();
            if (foundGpsLocation) {
                MapActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        goToUserLocation();
                    }
                });
            }
        } else {
            //Initialize timer textview
            final TextView text = (TextView) gpsCountdownDialog.findViewById(R.id.timer);
            text.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            text.setText(String.valueOf(initialCountdownValue));
                        }
                    }
            );
            timerTask = new TimerTask() {
                public void run() {
                    doCountDown();
                }
            };
            mTimer.schedule(timerTask, TASK_INTERVAL_IN_MILLIS);
        }
    }

    public GpsLocationProvider getGpsLocationProvider() {
        if(mapView.getGpsLocationProvider() != null) {
            return mapView.getGpsLocationProvider();
        }
        return null;
    }

    public void setMbtilesDialog(AlertDialog mbtilesDialog) {
        this.mbtilesDialog = mbtilesDialog;
    }

    public boolean isMapInteractionEnabled () {
        return mapView.isInteractionEnabled();
    }

    public AlertDialog getDeleteNodeDialog() {
        return deleteNodeDialog;
    }

    /**
     * This method removes all OSM files that were loaded from ODK instances but not in the list
     * editedOSM in {@link org.redcross.openmapkit.odkcollect.ODKCollectData}
     */
    private void cleanOsmFilesFromOdkInstances() {
        if (ODKCollectHandler.isODKCollectMode()
                && Constraints.singleton().getFirstColorConfig().isEnabled()) {
            //remove all osm files loaded from ODK's instance directory and not in the ODKCollectData.editedOSM object
            Set<String> loadedOsmFiles = OSMMapBuilder.getLoadedOSMFiles();
            Set<File> filesToRemove = new HashSet<>();
            Set<File> filesToAdd = new HashSet<>();
            File parentInstanceDir = new File(ODKCollectHandler.getODKCollectData().getInstanceDir()).getParentFile();
            List<File> loadedODKInstanceOSMFiles = ODKCollectHandler.getODKCollectData().getEditedOSM();
            for(String curPath : loadedOsmFiles) {
                File curFile = new File(curPath);
                filesToRemove.add(curFile);
                if(curFile.getAbsolutePath().startsWith(parentInstanceDir.getAbsolutePath())) {
                    //is omk file in ODK's instance directory
                    if(loadedODKInstanceOSMFiles.contains(curFile)) {
                        filesToAdd.add(curFile);
                    }
                } else {
                    filesToAdd.add(curFile);
                }
            }

            OSMMapBuilder.removeOSMFilesFromModel(filesToRemove);
            //Calling resetModifiedElements prevents OMK from hiding a polygon that was tagged (but
            // is now not tagged due to retagging)
            OSMWay.resetModifiedElements();
            OSMMapBuilder.addOSMFilesToModel(filesToAdd);
        }
    }

    public void showOdkQueryDialog() {
        if(Settings.singleton().getOSMFromODKQuery() != null) {
            final SharedPreferences sharedPreferences = getSharedPreferences("org.redcross.openmapkit.USER_NAME", Context.MODE_PRIVATE);
            if (odkQueryDialog == null) {
                final ContextThemeWrapper themedContext;
                themedContext = new ContextThemeWrapper(this, R.style.CustomDialogStyle);
                odkQueryDialog = new AppCompatDialog(themedContext);
                odkQueryDialog.setContentView(R.layout.dialog_odk_query);
                odkQueryDialog.setTitle(R.string.osm_data_from_odk);

                Button cancelB = (Button) odkQueryDialog.findViewById(R.id.cancelB);
                cancelB.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                        odkQueryDialog.dismiss();
                    }
                });

                Button okB = (Button) odkQueryDialog.findViewById(R.id.okB);
                okB.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                        EditText filterET = (EditText) odkQueryDialog.findViewById(R.id.filterET);

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(ODK_OMK_QUERY_VAL, filterET.getText().toString());
                        editor.commit();

                        odkQueryDialog.dismiss();

                        startDownload();
                    }
                });
            }
            String queryVal = sharedPreferences.getString(ODK_OMK_QUERY_VAL, null);
            String instructions = null;
            if (queryVal == null) {
                instructions = String.format(getResources().getString(R.string.odk_query_instructions), "enter", Settings.singleton().getOSMFromODKQuery(), "");
            } else {
                instructions = String.format(getResources().getString(R.string.odk_query_instructions), "confirm that", Settings.singleton().getOSMFromODKQuery(), " is:");
            }
            TextView instructionsTV = (TextView) odkQueryDialog.findViewById(R.id.instructionsTV);
            instructionsTV.setText(instructions);
            EditText filterET = (EditText) odkQueryDialog.findViewById(R.id.filterET);
            filterET.setText(queryVal);

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(filterET.getWindowToken(), 0);

            odkQueryDialog.show();
        } else {
            startDownload();
        }
    }

    public Dialog getOdkQuDialog() {
        return odkQueryDialog;
    }

    public ProgressDialog getOSMFromODKProgressDialog() {
        return progressDialog;
    }

    private void startDownload() {
        //stop all previous downloads
        FormOSMDownloader.clearOngoingDownloads(getApplicationContext());

        ArrayList<Form> forms = Settings.singleton().getOSMFromODKForms();
        downloadingForms = new HashMap<>();
        successfulForms = new HashMap<>();
        for(Form curForm : forms) {
            downloadingForms.put(curForm.getId(), curForm);
            final SharedPreferences sharedPreferences = getSharedPreferences("org.redcross.openmapkit.USER_NAME", Context.MODE_PRIVATE);
            new FormOSMDownloader(getApplicationContext(), curForm, this, sharedPreferences.getString(ODK_OMK_QUERY_VAL, null)).execute();
        }

        updateOsmFromOdkProgress();
    }

    @Override
    public void onStart(Form form) {}

    @Override
    public void onFail(final Form form) {
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setMessage("An OSM file failed to download. Do you want to try download it again?");
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    downloadingForms.put(form.getId(), form);
                    final SharedPreferences sharedPreferences = getSharedPreferences("org.redcross.openmapkit.USER_NAME", Context.MODE_PRIVATE);
                    new FormOSMDownloader(MapActivity.this.getApplicationContext(), form, MapActivity.this, sharedPreferences.getString(ODK_OMK_QUERY_VAL, null)).execute();
                    updateOsmFromOdkProgress();
                }
            });

            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    downloadingForms.remove(form.getId());
                    updateOsmFromOdkProgress();
                }
            });

            builder.setCancelable(false);
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSuccess(Form form) {
        try {
            downloadingForms.remove(form.getId());
            successfulForms.put(form.getId(), form);
            updateOsmFromOdkProgress();

            if(downloadingForms.size() == 0) {//no more forms downloading
                loadOSMFromODK();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadOSMFromODK() {
        ArrayList<Form> forms = Settings.singleton().getOSMFromODKForms();
        Set<File> osmFiles = new HashSet<>();
        for(Form curForm : forms) {
            if(curForm.getLocalOsmFile().exists()) {
                osmFiles.add(curForm.getLocalOsmFile());
            }
        }

        if(osmFiles.size() == forms.size()) {
            //might be better to user OSMMapBuilder.prepareMapToShowOnlyTheseOSM
            OSMMapBuilder.removeOSMFilesFromModel(osmFiles);
            OSMMapBuilder.addOSMFilesToModel(osmFiles);
        }
    }

    private void updateOsmFromOdkProgress() {
        if(progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getResources().getString(R.string.downloading_osm_from_odk));
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setIndeterminate(false);
        }

        if(downloadingForms != null
                && successfulForms != null
                &&(downloadingForms.size() + successfulForms.size()) > 0) {
            progressDialog.setMax(downloadingForms.size() + successfulForms.size());
            progressDialog.setProgress(successfulForms.size());

            if(downloadingForms.size() == 0) {
                progressDialog.dismiss();
            } else {
                progressDialog.show();
            }
        } else {
            progressDialog.dismiss();
        }
    }
}
