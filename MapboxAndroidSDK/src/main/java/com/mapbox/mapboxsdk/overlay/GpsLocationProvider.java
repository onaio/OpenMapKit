package com.mapbox.mapboxsdk.overlay;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.mapbox.mapboxsdk.util.NetworkLocationIgnorer;

import java.util.ArrayList;
import java.util.Calendar;

public class GpsLocationProvider implements LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {
    public static enum LocationStrategy {
        LOCATION_MANAGER,
        GOOGLE_PLAY_SERVICES
    }
    private static final long PLAY_SERVICES_LOCATION_UPDATE_MIN_TIME = 10000000l;
    private static final float PLAY_SERVICES_LOCATION_UPDATE_MIN_DIST = 100000000f;

    private final LocationManager mLocationManager;
    private Location mLocation;

    private UserLocationOverlay mMyLocationConsumer;
    private long mLocationUpdateMinTime = 0;
    private float mLocationUpdateMinDistance = 0.0f;
    private final NetworkLocationIgnorer mIgnorer = new NetworkLocationIgnorer();
    private final ArrayList<LocationListener> locationListeners;
    private final Context context;
    private LocationStrategy locationStrategy;
    private GoogleApiClient googleApiClient;
    private LocationCallback locationCallback;

    public GpsLocationProvider(Context context) {
        this.context = context;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationListeners = new ArrayList<>();
        this.locationStrategy = null;
    }

    public void addLocationListener(final LocationListener locationListener) {
        if(!locationListeners.contains(locationListener)) {
            this.locationListeners.add(locationListener);
        }
    }

    public void addLocationListeners(final ArrayList<LocationListener> locationListeners) {
        if(locationListeners!= null) {
            for (LocationListener currLocationListener : locationListeners) {
                addLocationListener(currLocationListener);
            }
        }
    }

    public long getLocationUpdateMinTime() {
        return mLocationUpdateMinTime;
    }

    /**
     * Set the minimum interval for location updates. See {@link
     * LocationManager.requestLocationUpdates(String, long, float, LocationListener)}. Note that
     * you
     * should call this before calling {@link enableMyLocation()}.
     */
    public void setLocationUpdateMinTime(final long milliSeconds) {
        mLocationUpdateMinTime = milliSeconds;
    }

    public float getLocationUpdateMinDistance() {
        return mLocationUpdateMinDistance;
    }

    /**
     * Set the minimum distance for location updates. See
     * {@link LocationManager.requestLocationUpdates}. Note that you should call this before
     * calling
     * {@link enableMyLocation()}.
     */
    public void setLocationUpdateMinDistance(final float meters) {
        mLocationUpdateMinDistance = meters;
    }

    /**
     * Enable location updates and show your current location on the map. By default this will
     * request location updates as frequently as possible, but you can change the frequency and/or
     * distance by calling {@link setLocationUpdateMinTime(long)} and/or {@link
     * setLocationUpdateMinDistance(float)} before calling this method.
     */
    public boolean startLocationProvider(UserLocationOverlay myLocationConsumer,
                                         @NonNull LocationStrategy locationStrategy) {
        mMyLocationConsumer = myLocationConsumer;

        if (this.locationStrategy != null && !this.locationStrategy.equals(locationStrategy)) {
            // Location strategy has changed, make sure you stop the previous strategy before
            // starting the new one
            if (this.locationStrategy.equals(LocationStrategy.GOOGLE_PLAY_SERVICES)) {
                disconnectGoogleApiClient();
            } else if (this.locationStrategy.equals(LocationStrategy.LOCATION_MANAGER)) {
                disconnectLocationManager();
            }
        }

        this.locationStrategy = locationStrategy;

        boolean result = false;
        if (locationStrategy.equals(LocationStrategy.LOCATION_MANAGER)) {
            result = connectLocationManager(myLocationConsumer, true);
        } else if (locationStrategy.equals(LocationStrategy.GOOGLE_PLAY_SERVICES)) {
            result = connectGoogleApiClient(myLocationConsumer);
        }

        return result;
    }

    public boolean isListeningForLocation() {
        return mMyLocationConsumer != null;// If location consumer is not null then we should be
        // listening for location
    }

    private boolean connectLocationManager(UserLocationOverlay mMyLocationConsumer,
                                           boolean getLocationUpdates) {
        boolean result = false;
        for (final String provider : mLocationManager.getProviders(true)) {
            if (LocationManager.GPS_PROVIDER.equals(provider)
                    || LocationManager.PASSIVE_PROVIDER.equals(provider)
                    || LocationManager.NETWORK_PROVIDER.equals(provider)) {
                result = true;
                if (mLocation == null) {
                    mLocation = mLocationManager.getLastKnownLocation(provider);
                    if (mLocation != null) {
                        mMyLocationConsumer.onLocationChanged(mLocation, this);
                    }
                }
                if (getLocationUpdates) {
                    mLocationManager.requestLocationUpdates(provider, mLocationUpdateMinTime,
                            mLocationUpdateMinDistance, this);
                } else {
                    mLocationManager.requestLocationUpdates(provider,
                            PLAY_SERVICES_LOCATION_UPDATE_MIN_TIME,
                            PLAY_SERVICES_LOCATION_UPDATE_MIN_DIST, this);
                }
            }
        }

        return result;
    }

    private boolean connectGoogleApiClient(UserLocationOverlay mMyLocationConsumer) {
        if (googleApiClient == null) {
            connectLocationManager(mMyLocationConsumer, true);
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult result) {
                    super.onLocationResult(result);
                }

                @Override
                public void onLocationAvailability(LocationAvailability locationAvailability) {
                    super.onLocationAvailability(locationAvailability);
                }
            };
            googleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        googleApiClient.connect();

        return true;
    }

    private void disconnectGoogleApiClient() {
        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }
    }

    private void disconnectLocationManager() {
        mLocationManager.removeUpdates(this);
    }

    public void stopLocationProvider() {
        mMyLocationConsumer = null;
        if (locationStrategy.equals(LocationStrategy.LOCATION_MANAGER)) {
            disconnectLocationManager();
        } else if (locationStrategy.equals(LocationStrategy.GOOGLE_PLAY_SERVICES)) {
            disconnectGoogleApiClient();
        }
    }

    public Location getLastKnownLocation() {
        return mLocation;
    }

    //
    // LocationListener
    //

    @Override
    public void onLocationChanged(final Location location) {
        locationChanged(location);
    }

    private void locationChanged(Location location) {
        // ignore temporary non-gps fix
        if (mIgnorer.shouldIgnore(location.getProvider(), System.currentTimeMillis())) {
            return;
        }

        mLocation = location;
        if (mMyLocationConsumer != null) {
            mMyLocationConsumer.onLocationChanged(mLocation, this);
        }

        for(int i = 0; i < locationListeners.size(); i++) {
            locationListeners.get(i).onLocationChanged(location);
        }
    }

    @Override
    public void onProviderDisabled(final String provider) {
        for(int i = 0; i < locationListeners.size(); i++) {
            locationListeners.get(i).onProviderDisabled(provider);
        }
    }

    @Override
    public void onProviderEnabled(final String provider) {
        for(int i = 0; i < locationListeners.size(); i++) {
            locationListeners.get(i).onProviderEnabled(provider);
        }
    }

    @Override
    public void onStatusChanged(final String provider, final int status, final Bundle extras) {
        for(int i = 0; i < locationListeners.size(); i++) {
            locationListeners.get(i).onStatusChanged(provider, status, extras);
        }
    }

    public LocationManager getLocationManager() {
        return mLocationManager;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationRequest mLocationRequest=new LocationRequest();

        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.FusedLocationApi
                .requestLocationUpdates(googleApiClient, mLocationRequest, this);

        if (mMyLocationConsumer != null) {
            mLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (mLocation != null) {
                mMyLocationConsumer.onLocationChanged(mLocation, this);
            }
        }

        for(int i = 0; i < locationListeners.size(); i++) {
            locationListeners.get(i).onProviderDisabled(null);
        }
    }

    @Override
    public void onConnectionSuspended(int status) {
        for(int i = 0; i < locationListeners.size(); i++) {
            locationListeners.get(i).onStatusChanged(null, status, null);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        for(int i = 0; i < locationListeners.size(); i++) {
            locationListeners.get(i).onProviderDisabled(null);
        }
    }
}
