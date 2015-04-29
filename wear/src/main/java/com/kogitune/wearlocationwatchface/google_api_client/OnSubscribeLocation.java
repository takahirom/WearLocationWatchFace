package com.kogitune.wearlocationwatchface.google_api_client;

import android.location.Location;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by takam on 2015/01/13.
 */
public class OnSubscribeLocation extends LocationCallback implements Observable.OnSubscribe<Location> {
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
        Log.d("LocationWatch", SystemClock.uptimeMillis()%100000+"update location");

        LocationServices.FusedLocationApi.requestLocationUpdates(googleAPIClient, mLocationRequest, this, Looper.myLooper());
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest()
            .setInterval(100)
            .setFastestInterval(0)
            .setMaxWaitTime(10000)
            .setNumUpdates(1)
            .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    @Override
    public void onLocationResult(LocationResult result) {
        super.onLocationResult(result);
        Log.d("LocationWatch", SystemClock.uptimeMillis()%100000+"get location!");
        subscriber.onNext(result.getLastLocation());
        subscriber.onCompleted();
    }

    @Override
    public void onLocationAvailability(LocationAvailability locationAvailability) {
        super.onLocationAvailability(locationAvailability);
        if (!locationAvailability.isLocationAvailable()){
            Log.d("LocationWatch", SystemClock.uptimeMillis()%100000+"not available");
            subscriber.onError(new LocationNotAvailableException(locationAvailability.toString()));
            subscriber.onCompleted();
        }
    }
}
