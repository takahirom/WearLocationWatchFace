package com.kogitune.wearlocationwatchface.common;

import android.content.Context;
import android.util.Log;

import com.kogitune.wearsharedpreference.WearSharedPreference;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by takam on 2015/02/08.
 */
public class OnSubscribeWearSharedPreferences implements Observable.OnSubscribe<Void> {
    private final WearSharedPreference wearSharedPreference;

    public OnSubscribeWearSharedPreferences(Context context, String key, String value) {
        wearSharedPreference = new WearSharedPreference(context);
        wearSharedPreference.put(key, value);
    }

    public OnSubscribeWearSharedPreferences(Context context, String key, int value) {
        wearSharedPreference = new WearSharedPreference(context);
        wearSharedPreference.put(key, value);
    }

    public OnSubscribeWearSharedPreferences(Context context, String key, boolean value) {
        wearSharedPreference = new WearSharedPreference(context);
        wearSharedPreference.put(key, value);
    }


    @Override
    public void call(Subscriber<? super Void> subscriber) {
        wearSharedPreference.sync(new WearSharedPreference.OnSyncListener() {
            @Override
            public void onSuccess() {
                subscriber.onNext(null);
                subscriber.onCompleted();
            }

            @Override
            public void onFail(Exception e) {
                subscriber.onError(e);
            }
        });
    }
}
