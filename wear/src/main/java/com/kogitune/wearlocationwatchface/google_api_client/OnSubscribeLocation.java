package com.kogitune.wearlocationwatchface.google_api_client;

import android.location.Location;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by takam on 2015/01/13.
 */
public class OnSubscribeLocation implements Observable.OnSubscribe<Location> {
    private GoogleApiClient googleAPIClient;
    private Subscriber<? super Location> subscriber;


    public OnSubscribeLocation(GoogleApiClient client) {
        this.googleAPIClient = client;
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
        final long locationGetTime = lastLocation.getTime();
        if (lastLocation != null && System.currentTimeMillis() - locationGetTime < 1000 * 60 * 60) {
            // use last location
            subscriber.onNext(lastLocation);
            subscriber.onCompleted();
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleAPIClient, buildLocationRequest(), location -> {
            subscriber.onNext(location);
            subscriber.onCompleted();
        });


    }

    private LocationRequest buildLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setNumUpdates(1);
        locationRequest.setFastestInterval(5000)
                .setFastestInterval(5000L)
                .setInterval(10000L)
                .setSmallestDisplacement(75.0F);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }


}
