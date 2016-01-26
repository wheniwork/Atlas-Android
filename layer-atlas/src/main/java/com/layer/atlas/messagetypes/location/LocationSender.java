package com.layer.atlas.messagetypes.location;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.layer.atlas.R;
import com.layer.atlas.messagetypes.AttachmentSender;
import com.layer.atlas.util.Log;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessageOptions;
import com.layer.sdk.messaging.MessagePart;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * LocationSender creates JSON MessagePart with latitude, longitude, and label.  Google's fused
 * location API is used for gathering location at send time and may trigger a dialog for updating
 * Google Play Services.
 */
public class LocationSender extends AttachmentSender {
    private static final int GOOGLE_API_REQUEST_CODE = 47000;
    public static final int LOCATION_REQUEST_CODE = 47;

    private static GoogleApiClient sGoogleApiClient;
    private final WeakReference<Activity> mActivity;

    public LocationSender(int titleResId, Integer iconResId, Activity activity) {
        this(activity.getString(titleResId), iconResId, activity);
    }

    public LocationSender(String title, Integer iconResId, Activity activity) {
        super(title, iconResId);
        mActivity = new WeakReference<Activity>(activity);
        init(activity);
    }

    /**
     * Asynchronously requests a fresh location and sends a location Message.
     *
     * @return `true`
     */
    @Override
    public boolean requestSend() {
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Sending location");
        if (hasLocationPermission()) {
            return getFreshLocation(new SenderLocationListener(this));
        } else {
            Activity activity = mActivity.get();
            if (activity == null) {
                if (Log.isLoggable(Log.ERROR)) Log.e("Activity went out of scope.");
                return false;
            } else {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_REQUEST_CODE);
            }
        }
        return true;
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getFreshLocation(new SenderLocationListener(this));
            } else {
                if (Log.isLoggable(Log.VERBOSE)) Log.v("Location permission denied");
            }
        }
        return true;
    }

    @Override
    public boolean onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode != GOOGLE_API_REQUEST_CODE) return false;
        init(activity);
        return true;
    }

    private void onLocationChanged(Location location) {
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Got fresh location");
        try {
            String myName = getParticipantProvider().getParticipant(getLayerClient().getAuthenticatedUserId()).getName();
            JSONObject o = new JSONObject()
                    .put(LocationCellFactory.KEY_LATITUDE, location.getLatitude())
                    .put(LocationCellFactory.KEY_LONGITUDE, location.getLongitude())
                    .put(LocationCellFactory.KEY_LABEL, myName);
            String notification = getContext().getString(R.string.atlas_notification_location, myName);
            MessagePart part = getLayerClient().newMessagePart(LocationCellFactory.MIME_TYPE, o.toString().getBytes());
            Message message = getLayerClient().newMessage(new MessageOptions().pushNotificationMessage(notification), part);
            send(message);
        } catch (JSONException e) {
            if (Log.isLoggable(Log.ERROR)) {
                Log.e(e.getMessage(), e);
            }
        }
    }

    private boolean hasLocationPermission() {
        Activity activity = mActivity.get();
        if (activity == null) {
            if (Log.isLoggable(Log.ERROR)) Log.e("Activity went out of scope.");
            return false;
        } else {
            int hasFineLocationPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);
            return hasFineLocationPermission == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void init(final Activity activity) {
        // If the client has already been created, ensure connected and return.
        if (sGoogleApiClient != null) {
            if (!sGoogleApiClient.isConnected()) connectGoogleApi();
            return;
        }

        int errorCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity);

        // If the correct Google Play Services are available, connect and return. 
        if (errorCode == ConnectionResult.SUCCESS) {
            GoogleApiCallbacks googleApiCallbacks = new GoogleApiCallbacks();
            sGoogleApiClient = new GoogleApiClient.Builder(activity)
                    .addConnectionCallbacks(googleApiCallbacks)
                    .addOnConnectionFailedListener(googleApiCallbacks)
                    .addApi(LocationServices.API)
                    .build();
            connectGoogleApi();
            return;
        }

        // If the correct Google Play Services are not available, redirect to proper solution.
        if (GooglePlayServicesUtil.isUserRecoverableError(errorCode)) {
            GoogleApiAvailability.getInstance()
                    .getErrorDialog(activity, errorCode, GOOGLE_API_REQUEST_CODE)
                    .show();
            return;
        }

        if (Log.isLoggable(Log.ERROR)) Log.e("Cannot update Google Play Services: " + errorCode);
    }

    private static void connectGoogleApi() {
        sGoogleApiClient.connect();
    }

    private static void disconnectGoogleApi() {
        sGoogleApiClient.disconnect();
    }

    private static boolean getFreshLocation(LocationListener listener) {
        if (sGoogleApiClient == null) {
            if (Log.isLoggable(Log.ERROR)) Log.e("GoogleApiClient not initialized");
            return false;
        }
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Getting fresh location");
        LocationRequest r = new LocationRequest()
                .setNumUpdates(1)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setExpirationDuration(10000)
                .setMaxWaitTime(10000);
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(sGoogleApiClient, r, listener);
            return true;
        } catch (IllegalStateException e) {
            if (Log.isLoggable(Log.ERROR)) {
                Log.e(e.getMessage(), e);
            }
        }
        return false;
    }

    private static class SenderLocationListener implements LocationListener {

        private final WeakReference<LocationSender> mLocationSenderReference;

        public SenderLocationListener(LocationSender locationsender) {
            this.mLocationSenderReference = new WeakReference<LocationSender>(locationsender);
        }

        @Override
        public void onLocationChanged(Location location) {
            LocationSender locationSender = mLocationSenderReference.get();
            if (locationSender != null) {
                locationSender.onLocationChanged(location);
            }
        }
    }

    private static class GoogleApiCallbacks implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle bundle) {
            if (Log.isLoggable(Log.VERBOSE)) Log.v("GoogleApiClient connected");
        }

        @Override
        public void onConnectionSuspended(int i) {
            if (Log.isLoggable(Log.VERBOSE)) Log.v("GoogleApiClient suspended");
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            if (Log.isLoggable(Log.VERBOSE)) Log.v("GoogleApiClient failed: " + connectionResult);
        }
    }
}
