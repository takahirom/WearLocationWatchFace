package com.kogitune.wearlocationwatchface.google_api_client;


import android.content.Context;

import com.kogitune.wearhttp.WearGetText;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by takam on 2015/01/30.
 */
public class OnSubscribeWearHttpText implements Observable.OnSubscribe<String> {
    private final WearGetText wearGetText;
    private final String url;

    public OnSubscribeWearHttpText(Context context, String url){
        wearGetText = new WearGetText(context);
        this.url = url;
    }
    @Override
    public void call(Subscriber<? super String> subscriber) {
        wearGetText.get(url,new WearGetText.WearGetCallBack() {
            @Override
            public void onGet(String s) {
                subscriber.onNext(s);
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
