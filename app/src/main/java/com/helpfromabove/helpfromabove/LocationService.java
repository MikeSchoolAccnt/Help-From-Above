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

import java.util.ArrayDeque;
import java.util.Stack;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final int MIN_ACCURACY_DISTANCE = 1000;
    private static final int MIN_ACCURATE_COUNT = 3;
    private static final int CONSTANT_LOCATION_UPDATE_SECONDS = 3;
    private final IBinder mBinder = new LocationServiceBinder();
    private LocationManager locationManager;
    private LocationListener locationServiceLocationListener;
    private Criteria locationCriteria = new Criteria();
    private ArrayDeque<Location> tmpHhmdLocations = new ArrayDeque<>();
    private ArrayDeque<Location> tmpUasLocations = new ArrayDeque<>();
    private Stack<Location> sessionHhmdLocations = new Stack<>();
    private Stack<Location> sessionUasLocations = new Stack<>();
    private Stack<Location> sessionWaypointLocations = new Stack<>();
    private int heightOffset;
    private int accurateCount = 0;
    private boolean calibrationComplete = false;

    public LocationService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationCriteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    protected void startSession() {
        setCalibrationComplete(false);
        resetHeightOffset();
        clearLocations();
        askLocationPermissionsAndRequestLocationUpdates();
    }

    protected void onLocationCalibrationComplete() {
        setCalibrationComplete(true);
    }

    private synchronized boolean isCalibrationComplete() {
        return calibrationComplete;
    }

    private synchronized void setCalibrationComplete(boolean calibrationComplete) {
        this.calibrationComplete = calibrationComplete;
    }

    private void testCalibration(Location location) {
        if (location.getAccuracy() <= MIN_ACCURACY_DISTANCE) {
            accurateCount++;
        } else {
            accurateCount = 0;
        }

        if (accurateCount >= MIN_ACCURATE_COUNT) {
            CommandService.notifyLocationHhmdCalibrationComplete(getApplicationContext());
        }
    }

    private void resetHeightOffset() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int startHeight = Integer.parseInt(sharedPref.getString(getString(R.string.pref_key_uas_start_height), getString(R.string.pref_value_uas_start_height_default)));
        setHeightOffset(startHeight);
    }

    private void askLocationPermissionsAndRequestLocationUpdates() {
        locationServiceLocationListener = new LocationServiceLocationListener();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int response = getApplicationContext().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);
            if (response == PackageManager.PERMISSION_GRANTED) {
                CommandService.notifyLocationCalibrating(getApplicationContext());
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
            CommandService.notifyLocationCalibrating(getApplicationContext());
            locationManager.requestLocationUpdates(1000 * CONSTANT_LOCATION_UPDATE_SECONDS, 0, locationCriteria, locationServiceLocationListener, getMainLooper());
        }
    }

    private void clearLocations() {
        sessionHhmdLocations.clear();
        sessionUasLocations.clear();
        sessionWaypointLocations.clear();
    }

    protected void stopSession() {
        calibrationComplete = false;
        accurateCount = 0;
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        locationManager.removeUpdates(locationServiceLocationListener);
    }

    protected void pushUasLocation(Location uasLocation) {
        sessionUasLocations.push(uasLocation);
        tmpUasLocations.addFirst(uasLocation);
    }

    private void pushHhmdLocation(Location hhmdLocation) {
        sessionHhmdLocations.push(hhmdLocation);
        tmpHhmdLocations.push(hhmdLocation);
    }

    protected Location getLastHhmdLocation() {
        Location hhmdLocation = null;
        if (!sessionHhmdLocations.isEmpty()) {
            hhmdLocation = sessionHhmdLocations.peek();
        } else {
            Log.w(TAG, "getLastUasLocation: sessionHhmdLocations is empty.");
        }

        return hhmdLocation;
    }

    private void pushWaypointLocation(Location waypoint) {
        sessionWaypointLocations.push(waypoint);
        CommandService.notifyNewWaypointAvailable(getApplicationContext());
    }

    protected Location getLastWaypointLocation() {
        Location waypoint = null;
        if (!sessionWaypointLocations.isEmpty()) {
            waypoint = sessionWaypointLocations.peek();
        } else {
            Log.w(TAG, "getLastWaypointLocation: sessionWaypointLocations is empty.");
        }

        return waypoint;
    }

    private void generateWaypoint() {
        if ((!tmpHhmdLocations.isEmpty()) &&
                (!tmpUasLocations.isEmpty()) &&
                (tmpHhmdLocations.peekFirst() != null) &&
                (tmpHhmdLocations.peekLast() != null) &&
                (tmpHhmdLocations.peekFirst() != tmpHhmdLocations.peekLast()) &&
                (tmpUasLocations.peekLast() != null)) {
            Location oldHhmd = tmpHhmdLocations.getLast();
            Location newHhmd = tmpHhmdLocations.getFirst();
            Location diff = getLocationDiff(newHhmd, oldHhmd);

            Location lastUas = tmpUasLocations.getLast();
            Location waypoint = addLocations(lastUas, diff);
            waypoint = addHeightOffset(waypoint);

            Log.i(TAG, "----------------------------------------");
            Log.i(TAG, "New Waypoint Generated");
            Log.i(TAG, "----------------------------------------");
            Log.i(TAG, "HHMD Location 1          = (" + oldHhmd.getLatitude() + ", " + oldHhmd.getLongitude() + ") Accuracy = " + oldHhmd.getAccuracy() + "m");
            Log.i(TAG, "HHMD Location n          = (" + newHhmd.getLatitude() + ", " + newHhmd.getLongitude() + ") Accuracy = " + newHhmd.getAccuracy() + "m");
            Log.i(TAG, "HHMD Location difference = (" + diff.getLatitude() + ", " + diff.getLongitude() + ")");
            Log.i(TAG, "Waypoint Location        = (" + waypoint.getLatitude() + ", " + waypoint.getLongitude() + ")");
            Log.i(TAG, "----------------------------------------");

            tmpUasLocations.clear();
            tmpHhmdLocations.clear();
            pushWaypointLocation(waypoint);
        }
    }

    private Location getLocationDiff(Location newLocation, Location oldLocation) {
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
        if (location != null) {
            location.setAltitude(location.getAltitude() + getHeightOffset());
            clearHeightOffset();
        }
        return location;
    }

    private synchronized int getHeightOffset() {
        return heightOffset;
    }

    private synchronized void setHeightOffset(int i) {
        heightOffset = i;
    }

    private synchronized void clearHeightOffset() {
        setHeightOffset(0);
    }

    protected void incrementHeightOffset() {
        setHeightOffset(++heightOffset);
    }

    protected void decrementHeightOffset() {
        setHeightOffset(--heightOffset);
    }

    protected class LocationServiceBinder extends Binder {
        protected LocationService getService() {
            return LocationService.this;
        }
    }

    private class LocationServiceLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            if (isCalibrationComplete()) {
                pushHhmdLocation(location);
                generateWaypoint();
            } else {
                testCalibration(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }

}
