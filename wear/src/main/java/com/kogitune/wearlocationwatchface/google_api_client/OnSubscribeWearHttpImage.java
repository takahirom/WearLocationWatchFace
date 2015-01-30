package com.kogitune.wearlocationwatchface.google_api_client;


import android.content.Context;
import android.graphics.Bitmap;

import com.kogitune.wearhttp.WearGetImage;
import com.kogitune.wearhttp.WearGetText;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by takam on 2015/01/30.
 */
public class OnSubscribeWearHttpImage implements Observable.OnSubscribe<Bitmap> {
    private final WearGetImage wearGetImage;
    private final String url;
    private final int timeout;

    public OnSubscribeWearHttpImage(Context context, String url, int timeout){
        wearGetImage = new WearGetImage(context);
        this.url = url;
        this.timeout = timeout;
    }
    @Override
    public void call(Subscriber<? super Bitmap> subscriber) {
        wearGetImage.get(url, new WearGetImage.WearGetCallBack() {
            @Override
            public void onGet(Bitmap bitmap) {
                subscriber.onNext(bitmap);
                subscriber.onCompleted();
            }

            @Override
            public void onFail(Exception e) {
                subscriber.onError(e);
                subscriber.onCompleted();
            }
        });
    }
}
