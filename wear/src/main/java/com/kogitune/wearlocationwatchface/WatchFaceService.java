package com.kogitune.wearlocationwatchface;

/**
 * Created by takam on 2014/12/29.
 */

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Toast;

import com.kogitune.wearlocationwatchface.google_api_client.GoogleApiClientObservable;
import com.kogitune.wearlocationwatchface.google_api_client.LocationNotAvailableException;
import com.kogitune.wearsharedpreference.WearSharedPreference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import rx.schedulers.Schedulers;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

public class WatchFaceService extends CanvasWatchFaceService implements WearSharedPreference.OnPreferenceChangeListener {
    private static final String TAG = "WatchFaceService";
    private static final int INTERVAL_SETTING_PHOTO = 60 * 60 * 1000;
    public long beforeRefreshTime;

    private WatchFaceEngine engine;

    private FloatingActionBarManager floatingActionBarManager;
    private String photoId;
    private WearSharedPreference wearPref;

    @Override
    public Engine onCreateEngine() {

        wearPref = new WearSharedPreference(this);
        floatingActionBarManager = new FloatingActionBarManager(this);
        floatingActionBarManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRefresh();
            }
        });
        wearPref.registerOnPreferenceChangeListener(this);

        engine = new WatchFaceEngine(this);
        return engine;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wearPref.unregisterOnPreferenceChangeListener();
    }

    @Override
    public void onPreferenceChange(WearSharedPreference preference, String key, Bundle bundle) {
        Toast.makeText(this, key + ":" + bundle.get(key), Toast.LENGTH_LONG).show();
        if (TextUtils.equals(key, getString(R.string.key_preference_time_text_accent))) {
        }
    }

    void refreshIfNeed() {
        if (beforeRefreshTime + INTERVAL_SETTING_PHOTO < System.currentTimeMillis()) {
            startRefresh();
        }
    }

    public void startRefresh() {
        floatingActionBarManager.startRefresh();
        GoogleApiClientObservable.connection(this)
                .flatMap(GoogleApiClientObservable::location)
                .map(location -> {
                    int range = new WearSharedPreference(this).get(getString(R.string.key_preference_search_range), getResources().getInteger(R.integer.search_range_default));
                    return "https://api.flickr.com/services/rest/?method=flickr.photos.search&group_id=1463451@N25&api_key=" + BuildConfig.FLICKR_API_KEY + "&license=1%2C2%2C3%2C4%2C5%2C6&lat=" + location.getLatitude() + "&lon=" + location.getLongitude() + "&radius=" + range + "&extras=url_n,url_l&per_page=30&format=json&nojsoncallback=1";
                }).flatMap(url -> GoogleApiClientObservable.fetchText(this, url))
                .timeout(50, TimeUnit.SECONDS)
                .observeOn(mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::applyView,
                        e -> {
                            if (e instanceof LocationNotAvailableException) {
                                try {
                                    final Intent intent = new Intent("android.settings.LOCATION_SOURCE_SETTINGS");
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    return;
                                } catch (final ActivityNotFoundException e1) {
                                }
                            }
                            floatingActionBarManager.stopRefresh();
                            Toast.makeText(WatchFaceService.this,"Can't get photo in time.",Toast.LENGTH_SHORT);
                            e.printStackTrace();
                        });
    }

    private void applyView(String jsonString) {
        try {
            final JSONArray photosArray = new JSONObject(jsonString).getJSONObject("photos").getJSONArray("photo");
            if (photosArray.length() == 0) {
                Toast.makeText(WatchFaceService.this, "Photo not found", Toast.LENGTH_LONG).show();
                floatingActionBarManager.stopRefresh();
                return;
            }
            final int nextIndex = new Random().nextInt(photosArray.length());
            String photoUrl = photosArray.getJSONObject(nextIndex).getString("url_n").toString();

            fetchAndApplyPhotoBitmap(photoUrl);

            photoId = photosArray.getJSONObject(nextIndex).getString("id").toString();
        } catch (JSONException e) {
            Log.d(TAG, "json" + jsonString);
            e.printStackTrace();
            floatingActionBarManager.stopRefresh();
        }
    }

    public void fetchAndApplyPhotoBitmap(String url) {
        GoogleApiClientObservable.fetchImage(this, url, 50).subscribe(bitmap1 -> {
            engine.setBitmap(bitmap1);
            engine.invalidate();
            beforeRefreshTime = System.currentTimeMillis();
            floatingActionBarManager.stopRefresh();

            savePhotoId();
        }, throwable -> {
            throwable.printStackTrace();
            Toast.makeText(WatchFaceService.this, "Can't get photo.Please retry later.", Toast.LENGTH_LONG).show();
            floatingActionBarManager.problemStopRefresh();
        });

    }

    private void savePhotoId() {
        final WearSharedPreference preference = new WearSharedPreference(this);
        final String photoIdsKey = getString(R.string.key_preference_photo_ids);
        final String historyPhotoIdsString = photoId + "," + preference.get(photoIdsKey, "");
        final String[] historyPhotoIdsArray = historyPhotoIdsString.split(",");
        final StringBuilder savingPhotoIds = new StringBuilder();
        for (int i = 0; i < historyPhotoIdsArray.length && i < 10; i++) {
            savingPhotoIds.append(historyPhotoIdsArray[i]);
            savingPhotoIds.append(",");
        }
        preference.put(photoIdsKey, savingPhotoIds.toString());
        preference.sync(new WearSharedPreference.OnSyncListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFail(Exception e) {
            }
        });
    }

    public void updateFab(@ColorRes int color, int x, int y) {
        floatingActionBarManager.update(color, x, y);
    }

    class WatchFaceEngine extends WatchFaceService.Engine {

        private final WatchFaceDrawer watchFaceDrawer;
        private final WatchFaceStyle watchFaceStyle;

        public WatchFaceEngine(WatchFaceService watchFaceService) {
            watchFaceDrawer = new WatchFaceDrawer(this, watchFaceService);
            watchFaceStyle = new WatchFaceStyle.Builder(WatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                    .build();
            setWatchFaceStyle(watchFaceStyle);
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setWatchFaceStyle(watchFaceStyle);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            watchFaceDrawer.setInsets(insets);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onPeekCardPositionUpdate(Rect rect) {
            setWatchFaceStyle(watchFaceStyle);
            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            floatingActionBarManager.setVisible(visible);
            if (visible) {
                refreshIfNeed();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            watchFaceDrawer.onDraw(canvas, bounds);
        }

        public void setBitmap(Bitmap bitmap) {
            watchFaceDrawer.setBitmap(bitmap);
        }
    }


}
