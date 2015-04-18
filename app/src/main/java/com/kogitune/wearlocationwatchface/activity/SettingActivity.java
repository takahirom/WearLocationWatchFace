package com.kogitune.wearlocationwatchface.activity;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;

import com.kogitune.wearlocationwatchface.R;
import com.kogitune.wearlocationwatchface.common.OnSubscribeWearSharedPreferences;
import com.kogitune.wearsharedpreference.WearSharedPreference;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscriber;

/**
 * Created by takam on 2015/03/24.
 */
public class SettingActivity extends RxActionBarActivity {
    private WearSharedPreference wearPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.inject(this);
        wearPref = new WearSharedPreference(this);
        final Resources res = getResources();
        DiscreteSeekBar searchRadiusSeekBar = (DiscreteSeekBar) findViewById(R.id.search_radius);

        final SwitchCompat textAccentSwitch = (SwitchCompat) findViewById(R.id.switch_text_accent);
        final boolean timeTextAccentEnabled = wearPref.get(getString(R.string.key_preference_time_text_accent), res.getBoolean(R.bool.time_text_accent_default));
        textAccentSwitch.setChecked(timeTextAccentEnabled);
        Observable
                .create(new Observable.OnSubscribe<Boolean>() {
                    @Override
                    public void call(Subscriber<? super Boolean> subscriber) {
                        textAccentSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            subscriber.onNext(Boolean.valueOf(isChecked));
                        });
                    }
                })
                .flatMap(b ->
                        Observable.create(new OnSubscribeWearSharedPreferences(SettingActivity.this, getString(R.string.key_preference_time_text_accent), b)))
                .subscribe((subscriber) -> {
                }, Throwable::printStackTrace);

        int firstRadius = wearPref.get(getString(R.string.key_preference_search_range), res.getInteger(R.integer.search_range_default));
        searchRadiusSeekBar.setProgress(firstRadius);
        searchRadiusSeekBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            private int startRadius;

            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {
                startRadius = seekBar.getProgress();
            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                wearPref.put(getString(R.string.key_preference_search_range), seekBar.getProgress());
                wearPref.sync(new WearSharedPreference.OnSyncListener() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onFail(Exception e) {
                        seekBar.setProgress(startRadius);
                    }
                });
            }
        });
    }
}
