package com.kogitune.wearlocationwatchface.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import rx.Observable;
import rx.android.lifecycle.LifecycleEvent;
import rx.subjects.BehaviorSubject;

/**
 * Created by takam on 2015/03/24.
 */
public class RxActionBarActivity extends ActionBarActivity{

    private final BehaviorSubject<LifecycleEvent> lifecycleSubject = BehaviorSubject.create();


    public Observable<LifecycleEvent> lifecycle() {
        return lifecycleSubject.asObservable();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lifecycleSubject.onNext(LifecycleEvent.CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        lifecycleSubject.onNext(LifecycleEvent.START);
    }

    @Override
    protected void onResume() {
        super.onResume();
        lifecycleSubject.onNext(LifecycleEvent.RESUME);
    }

    @Override
    protected void onPause() {
        lifecycleSubject.onNext(LifecycleEvent.PAUSE);
        super.onPause();
    }

    @Override
    protected void onStop() {
        lifecycleSubject.onNext(LifecycleEvent.STOP);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        lifecycleSubject.onNext(LifecycleEvent.DESTROY);
        super.onDestroy();
    }
}
