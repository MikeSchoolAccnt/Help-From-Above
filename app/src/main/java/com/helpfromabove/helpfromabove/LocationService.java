package com.helpfromabove.helpfromabove;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Stack;

public class LocationService extends Service {
    private static final String TAG = "LocationService";

    private final IBinder mBinder = new LocationServiceBinder();

    private LocationManager locationManager;
    private LocationListener locationServiceLocationListener;
    private Criteria locationCriteria = new Criteria();
    private Stack<Location> hhmdLocations = new Stack<>();
    private Stack<Location> uasLocations = new Stack<>();
    private Stack<Location> waypointLocations = new Stack<>();
    private int heightOffset;

    private int accurateCount = 0;
    private boolean calibrationComplete = false;
    private boolean sessionActive = false;

    private static final int MIN_ACCURACY_DISTANCE = 100;
    private static final int MIN_ACCURATE_COUNT = 3;
    protected static final int CONSTANT_LOCATION_UPDATE_SECONDS = 3;

    public LocationService() {
        super();
        Log.d(TAG, "LocationService");
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationCriteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    protected void startSession() {
        Log.d(TAG, "startSession");

        sessionActive = true;
        setCalibrationComplete(false);
        resetHeightOffset();
        clearLocations();
        askLocationPermissionsAndRequestLocationUpdates();
    }

    protected void onLocationCalibrationComplete() {
        Log.d(TAG, "onLocationCalibrationComplete");

        setCalibrationComplete(true);
    }

    private synchronized boolean isCalibrationComplete() {
        return calibrationComplete;
    }

    private synchronized void setCalibrationComplete(boolean calibrationComplete) {
        this.calibrationComplete = calibrationComplete;
    }

    private void testCalibration (Location location) {
        Log.d(TAG, "testCalibration: location.getAccuracy()=" + location.getAccuracy());

        if (location.getAccuracy() <= MIN_ACCURACY_DISTANCE) {
            accurateCount++;
        } else {
            accurateCount = 0;
        }

        if (accurateCount >= MIN_ACCURATE_COUNT) {
            CommandService.notifyLocationCalibrationComplete(getApplicationContext());
        }
    }

    private void resetHeightOffset() {
        Log.d(TAG, "resetHeightOffset");

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int startHeight = Integer.parseInt(sharedPref.getString(getString(R.string.pref_key_uas_start_height), getString(R.string.pref_value_uas_start_height_default)));
        setHeightOffset(startHeight);
    }

    private void askLocationPermissionsAndRequestLocationUpdates() {
        Log.d(TAG, "askLocationPermissionsAndRequestLocationUpdates");

        locationServiceLocationListener = new LocationServiceLocationListener();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int response = getApplicationContext().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);
            if (response == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onCreate: permission FEATURE_LOCATION is GRANTED");
                locationManager.requestLocationUpdates(1000 * CONSTANT_LOCATION_UPDATE_SECONDS, 0, locationCriteria, locationServiceLocationListener, getMainLooper());
            } else {
                Log.w(TAG, "onCreate: permission FEATURE_LOCATION is DENIED");
                // TODO : Request permission from user on devices at or above Android M
                // Because the user can explicitly modify the permissions of apps, if the
                // user denies Location from this app, then a dialog box should be shown
                // to the user to give this access to Location. This should send a
                // broadcast that gets received from all activities. When an activity
                // gets the broadcast, it should then request the permission.
            }
        } else {
            locationManager.requestLocationUpdates(1000 * CONSTANT_LOCATION_UPDATE_SECONDS, 0, locationCriteria, locationServiceLocationListener, getMainLooper());
        }
    }

    private void clearLocations() {
        Log.d(TAG, "clearLocations");

        hhmdLocations.clear();
        uasLocations.clear();
        waypointLocations.clear();
    }

    protected void stopSession() {
        Log.d(TAG, "stopSession");

        sessionActive = false;
        calibrationComplete = false;
        accurateCount = 0;
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates");

        locationManager.removeUpdates(locationServiceLocationListener);
    }

    protected boolean isSessionActive() {
        return sessionActive;
    }

    protected void pushUasLocation(Location uasLocation) {
        Log.d(TAG, "pushUasLocation: uasLocation=" + uasLocation);
        uasLocations.push(uasLocation);
    }

    private Location getLastUasLocation() {
        Log.d(TAG, "getLastUasLocation");

        Location uasLocation = null;
        if (!uasLocations.isEmpty()) {
            uasLocation = uasLocations.peek();
        } else {
            Log.w(TAG, "getLastUasLocation: uasLocations is empty.");
        }

        return uasLocation;
    }

    private void pushHhmdLocation(Location hhmdLocation) {
        Log.d(TAG, "pushHhmdLocation: hhmdLocation=" + hhmdLocation);
        hhmdLocations.push(hhmdLocation);
    }

    protected Location getLastHhmdLocation() {
        Log.d(TAG, "getLastHhmdLocation");

        Location hhmdLocation = null;
        if (!hhmdLocations.isEmpty()) {
            hhmdLocation = hhmdLocations.peek();
        } else {
            Log.w(TAG, "getLastUasLocation: hhmdLocations is empty.");
        }

        return hhmdLocation;
    }

    private void pushWaypointLocation(Location waypoint) {
        Log.d(TAG, "pushWaypointLocation");
        waypointLocations.push(waypoint);
    }

    protected Location getLastWaypointLocation() {
        Log.d(TAG, "getLastWaypointLocation");

        Location waypoint = null;
        if (!waypointLocations.isEmpty()) {
            waypoint = waypointLocations.peek();
        } else {
            Log.w(TAG, "getLastWaypointLocation: waypointLocations is empty.");
        }

        return waypoint;
    }


    private Location getPreviousHhmdLocation() {
        Log.d(TAG, "getPreviousHhmdLocation");

        Location previousHhmdLocation = null;
        if (hhmdLocations.size() >= 2) {
            Location lastHhmdLocation = hhmdLocations.pop();
            previousHhmdLocation = hhmdLocations.peek();
            hhmdLocations.push(lastHhmdLocation);
        } else {
            Log.w(TAG, "getPreviousHhmdLocation: hhmdLocations does not have enough locations.");
        }

        return previousHhmdLocation;
    }

    private Location generateWaypoint() {
        Log.d(TAG, "generateWaypoint");

        Location lastHhmd = getLastHhmdLocation();
        Location previousHhmd = getPreviousHhmdLocation();
        Location diff = getLocationDiff(lastHhmd, previousHhmd);

        Location lastUas = getLastUasLocation();
        Location waypoint = addLocations(lastUas, diff);

        return addHeightOffset(waypoint);
    }

    private Location getLocationDiff(Location newLocation, Location oldLocation) {
        Log.d(TAG, "getLocationDiff");

        Location diff = null;
        if (newLocation != null && oldLocation != null) {
            diff = new Location(getClass().getName());

            double diffLong = newLocation.getLongitude() - oldLocation.getLongitude();
            double diffLat = newLocation.getLatitude() - oldLocation.getLatitude();
            double diffAlt = newLocation.getAltitude() - oldLocation.getAltitude();
            float diffAcc = newLocation.getAccuracy() + newLocation.getAccuracy();

            diff.setLongitude(diffLong);
            diff.setLatitude(diffLat);
            diff.setAltitude(diffAlt);
            diff.setAccuracy(diffAcc);
        } else {
            Log.e(TAG, "getLocationSum: Locations cannot be subtracted because a location object is null.");
        }

        return diff;
    }

    private Location addLocations(Location location1, Location location2) {
        Log.d(TAG, "addLocations");

        Location sum = null;
        if (location1 != null && location2 != null) {
            sum = new Location(getClass().getName());
            double retLong = location1.getLongitude() + location2.getLongitude();
            double retLat = location1.getLatitude() + location2.getLatitude();
            double retAlt = location1.getAltitude() + location2.getAltitude();
            float retAcc = location1.getAccuracy() + location2.getAccuracy();

            sum.setLongitude(retLong);
            sum.setLatitude(retLat);
            sum.setAltitude(retAlt);
            sum.setAccuracy(retAcc);

        } else {
            Log.e(TAG, "addLocations: Locations cannot be added because a location object is null.");
        }

        return sum;
    }

    private Location addHeightOffset(Location location) {
        Log.d(TAG, "addHeightOffset");

        if (location != null) {
            location.setAltitude(location.getAltitude() + getHeightOffset());
            clearHeightOffset();
        }
        return location;
    }

    private synchronized int getHeightOffset() {
        Log.d(TAG, "getHeightOffset");

        return heightOffset;
    }

    private synchronized void clearHeightOffset() {
        Log.d(TAG, "clearHeightOffset");

        setHeightOffset(0);
    }

    private synchronized void setHeightOffset(int i) {
        Log.d(TAG, "setHeightOffset: i=" + i);

        heightOffset = i;
    }

    protected void incrementHeightOffset() {
        Log.d(TAG, "incrementHeightOffset");

        setHeightOffset(++heightOffset);
    }

    protected void decrementHeightOffset() {
        Log.d(TAG, "decrementHeightOffset");

        setHeightOffset(--heightOffset);
    }

    protected class LocationServiceBinder extends Binder {
        protected LocationService getService() {
            return LocationService.this;
        }
    }

    private class LocationServiceLocationListener implements LocationListener {
        private static final String TAG = "LocationServ...Listener";

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged");

            if (isCalibrationComplete()) {
                pushHhmdLocation(location);
                Location waypoint = generateWaypoint();
                pushWaypointLocation(waypoint);
                CommandService.notifyNewWaypointAvailable(getApplicationContext());
            } else {
                testCalibration(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "onStatusChanged: status=" + status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "onProviderEnabled: provider=" + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "onProviderDisabled: provider=" + provider);
        }
    }

}
