package com.kogitune.wearlocationwatchface;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.kogitune.wearlocationwatchface.widget.ObservableScrollView;
import com.kogitune.wearsharedpreference.WearSharedPreference;


public class MainActivity extends ActionBarActivity implements ObservableScrollView.Callbacks {

    private static final String TAG = "MainActivity";
    private Toolbar toolbar;
    private WearSharedPreference wearSharedPreference;
    private ObservableScrollView mScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScrollView = (ObservableScrollView) findViewById(R.id.scroll_view);
        mScrollView.addCallbacks(this);


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(new WearSharedPreference(this).get(getString(R.string.key_preference_photo_title), ""));
        setSupportActionBar(toolbar);

        setupPhotoAndApplyTheme();
        SeekBar searchRadiusSeekBar = (SeekBar) findViewById(R.id.search_radius);
        wearSharedPreference = new WearSharedPreference(this);
        final int radius = wearSharedPreference.get(getString(R.string.key_preference_search_range), getResources().getInteger(R.integer.search_range_default));
        searchRadiusSeekBar.setProgress(radius);
        searchRadiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public int startProgress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                this.startProgress = seekBar.getProgress();
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                wearSharedPreference.put(getString(R.string.key_preference_search_range), seekBar.getProgress());
                wearSharedPreference.sync(new WearSharedPreference.OnSyncListener() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onFail(Exception e) {
                        Toast.makeText(MainActivity.this, "Sync failed search radius", Toast.LENGTH_LONG).show();
                        seekBar.setProgress(startProgress);
                    }
                });
            }
        });
    }

    private void setupPhotoAndApplyTheme() {
        final ImageView beforePhoto = (ImageView) findViewById(R.id.beforePhoto);
        String beforePhotoUrl = new WearSharedPreference(this).get(getString(R.string.key_preference_photo_url), "");
        Log.d(TAG, "beforePhotoUrl:" + beforePhotoUrl);
        if (beforePhotoUrl.length() == 0) {
            return;
        }
        Glide.with(this).load(beforePhotoUrl).into((Target) new SimpleTarget<Bitmap>(beforePhoto.getWidth(), (int) (beforePhoto.getWidth() / 1.5)) {
            @Override
            public void onLoadStarted(Drawable placeholder) {
            }

            @Override
            public void onLoadFailed(Exception e, Drawable errorDrawable) {
            }

            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                beforePhoto.setImageBitmap(resource);
                applyTheme(Palette.generate(resource));
            }
        });
    }

    private void applyTheme(Palette generate) {
        if (generate == null) {
            return;
        }
        final Palette.Swatch darkVibrantSwatch = generate.getDarkVibrantSwatch();
        if (darkVibrantSwatch != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(darkVibrantSwatch.getRgb());
            }
        }
        final Palette.Swatch vibrantSwatch = generate.getVibrantSwatch();
        if (vibrantSwatch != null) {
            toolbar.setBackgroundColor(vibrantSwatch.getRgb());
            toolbar.setTitleTextColor(vibrantSwatch.getTitleTextColor());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onScrollChanged(int deltaX, int deltaY) {
        int scrollY = mScrollView.getScrollY();

//        float newTop = Math.max(mPhotoHeightPixels, scrollY);
//        mHeaderBox.setTranslationY(newTop);
//        mAddScheduleButton.setTranslationY(newTop + mHeaderHeightPixels
//                - mAddScheduleButtonHeightPixels / 2);
//
//        float gapFillProgress = 1;
//        if (mPhotoHeightPixels != 0) {
//            gapFillProgress = Math.min(Math.max(UIUtils.getProgress(scrollY,
//                    0,
//                    mPhotoHeightPixels), 0), 1);
//        }
//
//        ViewCompat.setElevation(mHeaderBox, gapFillProgress * mMaxHeaderElevation);
//        ViewCompat.setElevation(mAddScheduleButton, gapFillProgress * mMaxHeaderElevation
//                + mFABElevation);
//
//        // Move background photo (parallax effect)
//        mPhotoViewContainer.setTranslationY(scrollY * 0.5f);

    }
}
