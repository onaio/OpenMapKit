package org.redcross.openmapkit.tagswipe;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import org.redcross.openmapkit.R;
import org.redcross.openmapkit.odkcollect.ODKCollectHandler;

public class TagSwipeActivity extends ActionBarActivity {
    /*
    Intent bundle key used to determine whether the activity has been started for testing purposes
     */
    public static final String BUNDLE_KEY_IS_TESTING = "is_testing";

    private List<TagEdit> tagEdits;
    private SharedPreferences userNamePref;
    private LocationListener locationListener;
    private LocationManager locationManager;
    /*
    Which GPS provider should be used to get the User's current location
     */
    private String preferredLocationProvider = LocationManager.GPS_PROVIDER;
    private AlertDialog gpsProviderAlertDialog;
    private ProgressDialog gpsSearchingProgressDialog;
    private boolean forTesting;
    private AlertDialog insertOsmUsernameDialog;
    private EditText osmUsernameEditText;
    private String osmFilePath;

    private void setupModel() {
        tagEdits = TagEdit.buildTagEdits();
        TagEdit.setTagSwipeActivity(this);
        userNamePref = getSharedPreferences("org.redcross.openmapkit.USER_NAME", Context.MODE_PRIVATE);
    }


    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_swipe);
        forTesting = false;
        Intent startActivityIntent = getIntent();
        if(startActivityIntent != null) {
            Bundle extras = startActivityIntent.getExtras();
            if(extras != null) {
                if(extras.containsKey(BUNDLE_KEY_IS_TESTING)){
                    forTesting = extras.getBoolean(BUNDLE_KEY_IS_TESTING);
                }
            }
        }

        setupModel();
        
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.tagSwipeActivity);
        mViewPager.setAdapter(mSectionsPagerAdapter);
    
        pageToCorrectTag();
        initLocationManager();
    }

    @Override
    protected void onDestroy() {
        if(locationManager != null) {
            locationManager.removeUpdates(locationListener);
            if(forTesting) {
                locationManager.removeTestProvider(preferredLocationProvider);
            }
        }
        super.onDestroy();
    }

    private void initLocationManager() {
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if(forTesting) {
            preferredLocationProvider = "test_provider_"+String.valueOf(Calendar.getInstance().getTimeInMillis());
            if(locationManager.getProvider(preferredLocationProvider) != null) {
                locationManager.removeTestProvider(preferredLocationProvider);
            }
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                TagEdit.updateUserLocationTags(location);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                checkLocationProviderEnabled();
            }

            @Override
            public void onProviderEnabled(String s) {
                updateUsersLocation();
            }

            @Override
            public void onProviderDisabled(String s) {
                TagEdit.cleanUserLocationTags();
                checkLocationProviderEnabled();
            }
        };

        if(forTesting == false) {
            locationManager.requestLocationUpdates(preferredLocationProvider, 30000, 4, locationListener);
        } else {
            Toast.makeText(this, "App launched for automated tests", Toast.LENGTH_LONG).show();
            locationManager.addTestProvider(preferredLocationProvider
                    , true, false, false, false, true, true, true,
                    Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
            changeTestProviderEnabled(true);
            locationManager.requestLocationUpdates(preferredLocationProvider, 0, 0, locationListener);
        }

        updateUsersLocation();
    }

    /**
     * This method changes the state of the test provider initialized in this activity.
     * This method is intended to only be used in tests.
     *
     * @param enable    TRUE of you want to enable the test provider
     */
    public void changeTestProviderEnabled(boolean enable) {
        if(locationManager != null) {
            locationManager.setTestProviderEnabled(preferredLocationProvider, enable);
        } else {
            Log.w("TagSwipeActivity", "Location manager is null, cannot enable/disable the test provider");
        }
    }

    /**
     * This method changes the status of the test provider for the locationManager initialized in
     * this activity.
     * This method is intended to only be used in tests.
     *
     * @param status    Status similar to those provided in android.location.LocationProvider
     * @see android.location.LocationProvider
     */
    public void changeTestProviderStatus(int status) {
        if(locationManager != null) {
            locationManager.setTestProviderStatus(
                    preferredLocationProvider,
                    status,
                    null,
                    Calendar.getInstance().getTimeInMillis());
        } else {
            Log.w("TagSwipeActivity", "Location manager is null, cannot change the status of the test provider");
        }
    }

    /**
     * This method changes the location passed by the test provider registered in the
     * locationManager initialized in this activity.
     * This method is intended to only be used in tests.
     *
     * @param location  The location to be provided by the locationManager
     */
    public void changeTestProviderLocation(Location location) {
        if(locationManager != null) {
            locationManager.setTestProviderLocation(preferredLocationProvider, location);
        } else {
            Log.w("TagSwipeActivity", "Location manager is null, cannot change the location in the test provider");
        }
    }

    public String getPreferredLocationProvider() {
        return preferredLocationProvider;
    }

    /**
     * This method updates the user's location (and osm location tags)
     *
     * @return TRUE if location was successfully updated
     */
    public boolean updateUsersLocation() {
        if(checkLocationProviderEnabled()) {
            Log.w("GPSTest", "LocationManager is not null");
            Location location = locationManager.getLastKnownLocation(preferredLocationProvider);
            if(location == null) {
                Log.w("GPSTest", "Last location is null");
                Log.w("GPSTest", "Location providers = "+locationManager.getProviders(true).toString());
            }
            TagEdit.updateUserLocationTags(location);
            return true;
        } else {
            Log.w("GPSTest", "LocationManager is null");
        }
        return false;
    }

    /**
     * This method checks whether the preferred location provider is enabled in the device and shows
     * the gpsProviderAlertDialog if not
     *
     * @return TRUE if the preferred location provider is enabled
     */
    public boolean checkLocationProviderEnabled() {
        if(getLocationProviderStatus() == true) {
            if(gpsProviderAlertDialog != null) {
                gpsProviderAlertDialog.dismiss();
            }
            return true;
        }

        //if we've reached this point, it means the location provider is not enabled
        //show the enable location provider dialog
        if(gpsProviderAlertDialog == null) {
            gpsProviderAlertDialog = new AlertDialog.Builder(TagSwipeActivity.this)
                    .setMessage(getResources().getString(R.string.enable_gps))
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (getLocationProviderStatus() == true) {
                                dialogInterface.dismiss();
                            } else {
                                TagSwipeActivity.this.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                        }
                    })
                    .setCancelable(false)
                    .create();
        }
        gpsProviderAlertDialog.show();

        return false;
    }

    public boolean isGpsProviderAlertDialogShowing() {
        if(gpsProviderAlertDialog != null) {
            return gpsProviderAlertDialog.isShowing();
        }
        return false;
    }

    /**
     * This method checks whether the preferred location provider is available
     *
     * @return  TRUE if the preferred location provider is available
     */
    private boolean getLocationProviderStatus() {
        if(locationManager != null) {
            if(locationManager.isProviderEnabled(preferredLocationProvider)){
                return true;
            }
        }
        return false;
    }

    public void setOsmFilePath(String path) {
        osmFilePath = path;
    }

    public String getOsmFilePath() {
        return osmFilePath;
    }

    public void setOsmUsername(String username) {
        if(insertOsmUsernameDialog != null && osmUsernameEditText != null) {
            osmUsernameEditText.setText(username);
            insertOsmUsernameDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        }
    }

    private void showGpsSearchingProgressDialog() {
        if(gpsSearchingProgressDialog == null) {
            gpsSearchingProgressDialog = ProgressDialog.show(TagSwipeActivity.this, "", getResources().getString(R.string.waiting_for_user_location), true, false);
        } else {
            gpsSearchingProgressDialog.show();
        }
    }

    public void hideGpsSearchingProgressDialog() {
        if(gpsSearchingProgressDialog != null) {
            gpsSearchingProgressDialog.dismiss();
        }
    }

    private void pageToCorrectTag() {
        String tagKey = getIntent().getStringExtra("TAG_KEY");
        if (tagKey == null) return;
        int idx = TagEdit.getIndexForTagKey(tagKey);
        mViewPager.setCurrentItem(idx);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tag_swipe, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // save to odk collect action bar button
        if (id == R.id.action_save_to_odk_collect) {
            saveToODKCollect();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * We check to see if there is a saved user name. If there is not,
     * we present a dialog to ask for it. Otherwise, we just use what
     * is saved for writing OSM XML and saving to ODK Collect.
     */
    public void saveToODKCollect() {
        String userName = userNamePref.getString("userName", null);
        if (userName == null) {
            askForOSMUsername();
        } else {
            if (TagEdit.saveToODKCollect(userName)) {
                setResult(Activity.RESULT_OK);
                finish();
            }
        }
    }
    
    public void cancel() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    private void askForOSMUsername() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("OpenStreetMap User Name");
        builder.setMessage("Please enter your OpenStreetMap user name.");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String userName = input.getText().toString();
                SharedPreferences.Editor editor = userNamePref.edit();
                editor.putString("userName", userName);
                editor.apply();
                if (TagEdit.saveToODKCollect(userName)) {
                    setResult(Activity.RESULT_OK);
                    finish();
                }
            }
        });
        insertOsmUsernameDialog = builder.show();
        osmUsernameEditText = input;
    }

    public void updateUI(String activeTagKey) {
        mSectionsPagerAdapter.notifyDataSetChanged();
        int idx = TagEdit.getIndexForTagKey(activeTagKey);
        mViewPager.setCurrentItem(idx);
    }

    /**
     * Only call if you have more than one missing tag.
     *
     * @param missingTags - tags that are required that are missing.
     */
    public void notifyMissingTags(final Set<String> missingTags) {
        Snackbar.make(findViewById(R.id.tagSwipeActivity),
                "There are " + missingTags.size() + " required tags that you need to complete: " + missingTagsText(missingTags),
                Snackbar.LENGTH_LONG)
                .setAction("OK", new View.OnClickListener() {
                    // undo action
                    @Override
                    public void onClick(View v) {
                        try {
                            String missingTag = missingTags.iterator().next();
                            int idx = TagEdit.getIndexForTagKey(missingTag);
                            mViewPager.setCurrentItem(idx);
                        } catch (Exception e) {
                            // do nothing
                        }
                    }
                })
                .setActionTextColor(Color.rgb(126, 188, 111))
                .show();
    }

    public void notifyMissingUserLocation() {
        showGpsSearchingProgressDialog();
        int index = TagEdit.getIndexForTagKey(TagEdit.TAG_KEY_USER_LOCATION);
        updateUsersLocation();
        mViewPager.setCurrentItem(index);
    }

    private String missingTagsText(Set<String> missingTags) {
        String str = "";
        boolean first = true;
        for (String tag: missingTags) {
            if (first) {
                str += tag;
            } else {
                str += ", " + tag;
            }
            first = false;
        }
        return str;
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
        
        private Fragment fragment;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        private void hideKeyboard() {
            if (fragment != null && fragment instanceof StringTagValueFragment) {
                StringTagValueFragment stvf = (StringTagValueFragment) fragment;
                EditText et = stvf.getEditText();
                if (et != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
                }
            }
        }
        
        @Override
        public Fragment getItem(int position) {
            
            // hide keyboard if last fragment had a user edit text
            hideKeyboard();
            
            if (position < tagEdits.size()) {
                TagEdit tagEdit = tagEdits.get(position);
                if (tagEdit != null) {
                    if (tagEdit.isReadOnly()) {
                        fragment = ReadOnlyTagFragment.newInstance(position);
                        return fragment;
                    } else if (tagEdit.isSelectOne()) {
                        fragment = SelectOneTagValueFragment.newInstance(position);
                        return fragment;
                    }
                    else if (tagEdit.isSelectMultiple()) {
                        fragment = SelectMultipleTagValueFragment.newInstance(position);
                        return fragment;
                    } else {
                        fragment = StringTagValueFragment.newInstance(position);
                        return fragment;
                    }
                }
            }
            
            if (ODKCollectHandler.isODKCollectMode()) {
                return ODKCollectFragment.newInstance();    
            } else {
                return StandaloneFragment.newInstance("one", "two");
            }
        }

        @Override
        public int getCount() {
            return tagEdits.size() + 1;
        }

        //this is called when notifyDataSetChanged() is called
        @Override
        public int getItemPosition(Object object) {
            // refresh all fragments when data set changed
            return PagerAdapter.POSITION_NONE;
        }


        @Override
        public CharSequence getPageTitle(int position) {
            if (position < tagEdits.size()) {
                TagEdit tagEdit = tagEdits.get(position);
                if (tagEdit != null) {
                    return tagEdit.getTitle();
                }
            }
            Resources res = getResources();
            if (ODKCollectHandler.isODKCollectMode()) {
                return res.getString(R.string.odkcollect_fragment_title);
            } else {
                return res.getString(R.string.standalone_fragment_title);
            }
        }
    }

}
