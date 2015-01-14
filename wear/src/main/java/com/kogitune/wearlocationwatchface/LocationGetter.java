package com.kogitune.wearlocationwatchface;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by takam on 2015/01/13.
 */
public class LocationGetter implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private final GoogleApiClient googleAPIClient;

    public LocationGetter(Context context) {
        googleAPIClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleAPIClient.connect();
    }

    public Location getLastLocation() {
        if (!googleAPIClient.isConnected()) {
            return null;
        }
        return LocationServices.FusedLocationApi.getLastLocation(googleAPIClient);
    }

    /**
     * updateLocation if GoogleAPIClient is connected.
     */
    public void updateLocation() {
        if(!googleAPIClient.isConnected()){
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleAPIClient, buildLocationRequest(), new LocationListener() {


            @Override
            public void onLocationChanged(Location location) {
            }
        });
    }

    private LocationRequest buildLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setNumUpdates(1);
        locationRequest.setInterval(300000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    @Override
    public void onConnected(Bundle bundle) {
        updateLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
