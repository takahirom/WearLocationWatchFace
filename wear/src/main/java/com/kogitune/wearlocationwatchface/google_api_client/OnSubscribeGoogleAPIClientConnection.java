package com.kogitune.wearlocationwatchface.google_api_client;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by takam on 2015/01/29.
 */
public class OnSubscribeGoogleAPIClientConnection implements Observable.OnSubscribe<GoogleApiClient> {
    private final GoogleApiClient googleAPIClient;

    public OnSubscribeGoogleAPIClientConnection(Context context) {
        googleAPIClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void call(Subscriber<? super GoogleApiClient> subscriber) {
        googleAPIClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                subscriber.onNext(googleAPIClient);
                subscriber.onCompleted();
            }

            @Override
            public void onConnectionSuspended(int i) {
                subscriber.onCompleted();
            }
        });
        googleAPIClient.registerConnectionFailedListener(connectionResult -> {
            subscriber.onError(new RuntimeException(connectionResult.toString()));
            subscriber.onCompleted();
        });
        googleAPIClient.connect();
    }

}
