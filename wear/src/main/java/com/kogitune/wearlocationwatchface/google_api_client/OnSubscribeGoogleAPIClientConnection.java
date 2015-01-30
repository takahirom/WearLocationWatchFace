package com.kogitune.wearlocationwatchface.google_api_client;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by takam on 2015/01/29.
 */
public class OnSubscribeGoogleAPIClientConnection implements Observable.OnSubscribe<GoogleApiClient>, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final GoogleApiClient googleAPIClient;
    private Subscriber<? super GoogleApiClient> subscriber;

    public OnSubscribeGoogleAPIClientConnection(Context context){
        googleAPIClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void call(Subscriber<? super GoogleApiClient> subscriber) {
        Log.d("ApiConnection", "call");
        this.subscriber = subscriber;
        googleAPIClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        subscriber.onNext(googleAPIClient);
    }

    @Override
    public void onConnectionSuspended(int i) {
        subscriber.onCompleted();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        subscriber.onError(new RuntimeException(connectionResult.toString()));
    }
}
