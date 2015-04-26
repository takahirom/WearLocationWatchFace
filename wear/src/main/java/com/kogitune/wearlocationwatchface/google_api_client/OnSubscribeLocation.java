package com.kogitune.wearlocationwatchface.google_api_client;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by takam on 2015/01/13.
 */
public class OnSubscribeLocation implements Observable.OnSubscribe<Location>,LocationListener {
    private GoogleApiClient googleAPIClient;
    private Subscriber<? super Location> subscriber;
    private LocationRequest mLocationRequest;


    public OnSubscribeLocation(GoogleApiClient client) {
        this.googleAPIClient = client;
        createLocationRequest();
    }

    @Override
    public void call(Subscriber<? super Location> subscriber) {
        this.subscriber = subscriber;
        updateLocation();
    }

    /**
     * updateLocation if GoogleAPIClient is connected.
     */
    public void updateLocation() {
        if (!googleAPIClient.isConnected()) {
            return;
        }

        final Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleAPIClient);
        if (lastLocation != null && System.currentTimeMillis() - lastLocation.getTime() < 1000 * 60 * 10) {
            Log.d("LocationWatch", "use last location");
            // use last location
            subscriber.onNext(lastLocation);
            subscriber.onCompleted();
            return;
        }
        Log.d("LocationWatch", "update location");

        LocationServices.FusedLocationApi.requestLocationUpdates(googleAPIClient, mLocationRequest, this);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setNumUpdates(1);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    @Override
    public void onLocationChanged(Location location) {
        subscriber.onNext(location);
        subscriber.onCompleted();
    }
}
