package com.kogitune.wearlocationwatchface.google_api_client;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;

import com.google.android.gms.common.api.GoogleApiClient;

import rx.Observable;

/**
 * Created by takam on 2015/01/31.
 */
public class GoogleApiClientObservable {

    private static final String TAG = "ApiClientObservable";

    public static Observable<GoogleApiClient> connection(Context context) {
        return Observable.create(new OnSubscribeGoogleAPIClientConnection(context));
    }

    public static Observable<Location> location(GoogleApiClient client) {
        return Observable.create(new OnSubscribeLocation(client));
    }

    public static Observable<String> fetchText(Context context, String url) {
        return Observable.create(new OnSubscribeWearHttpText(context, url));
    }

    public static Observable<Bitmap> fetchImage(Context context, String url, int timeout) {
        return Observable.create(new OnSubscribeWearHttpImage(context, url, timeout));
    }

}
