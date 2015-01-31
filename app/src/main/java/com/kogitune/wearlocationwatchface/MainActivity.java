package com.kogitune.wearlocationwatchface;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.kogitune.wearlocationwatchface.util.LUtils;
import com.kogitune.wearlocationwatchface.util.UIUtils;
import com.kogitune.wearlocationwatchface.widget.CheckableFrameLayout;
import com.kogitune.wearlocationwatchface.widget.ObservableScrollView;
import com.kogitune.wearsharedpreference.WearSharedPreference;

import java.io.File;


public class MainActivity extends ActionBarActivity implements ObservableScrollView.Callbacks {

    private static final String TAG = "MainActivity";
    private Toolbar toolbar;
    private WearSharedPreference wearSharedPreference;
    private ObservableScrollView scrollView;
    private int mPhotoHeightPixels;
    private View mHeaderBox;
    private CheckableFrameLayout mFabButton;
    private boolean mStarred;
    private LUtils lUtil;
    private ImageView beforePhoto;
    private boolean hasPhoto = false;
    private static final float PHOTO_ASPECT_RATIO = 1.5f;
    private int maxHeaderElevation;
    private int fabElevation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lUtil = LUtils.getInstance(this);

        scrollView = (ObservableScrollView) findViewById(R.id.scroll_view);
        scrollView.addCallbacks(this);
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);

        mHeaderBox = findViewById(R.id.toolbar);
        maxHeaderElevation = getResources().getDimensionPixelSize(
                R.dimen.session_detail_max_header_elevation);
        fabElevation = getResources().getDimensionPixelSize(R.dimen.fab_elevation);

        mDetailsContainer = findViewById(R.id.details_container);

        mFabButton = (CheckableFrameLayout) findViewById(R.id.fab_button);
        mFabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean starred = !mStarred;
                showStarred(starred, true);
                String beforePhotoUrl = new WearSharedPreference(MainActivity.this).get(getString(R.string.key_preference_photo_url), "");
                downloadFile(beforePhotoUrl);
            }
        });


        beforePhoto = (ImageView) findViewById(R.id.beforePhoto);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(new WearSharedPreference(this).get(getString(R.string.key_preference_photo_title), ""));
        toolbar.getMenu().clear();
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


    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener
            = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            recomputePhotoAndScrollingMetrics();
        }
    };
    private int mHeaderHeightPixels;
    private View mDetailsContainer;

    private void recomputePhotoAndScrollingMetrics() {
        mHeaderHeightPixels = mHeaderBox.getHeight();

        mPhotoHeightPixels = 0;
        if (hasPhoto) {
            mPhotoHeightPixels = (int) (beforePhoto.getWidth() / PHOTO_ASPECT_RATIO);
            mPhotoHeightPixels = Math.min(mPhotoHeightPixels, scrollView.getHeight() * 2 / 3);
        }

        ViewGroup.LayoutParams lp;
        lp = beforePhoto.getLayoutParams();
        if (lp.height != mPhotoHeightPixels) {
            lp.height = mPhotoHeightPixels;
            //beforePhoto.setLayoutParams(lp);
        }

        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)
                mDetailsContainer.getLayoutParams();
        if (mlp.topMargin != mHeaderHeightPixels + mPhotoHeightPixels) {
            mlp.topMargin = mHeaderHeightPixels + mPhotoHeightPixels;
            mDetailsContainer.setLayoutParams(mlp);
        }

        onScrollChanged(0, 0); // trigger scroll handling
    }



    private void showStarred(boolean starred, boolean allowAnimate) {
        mStarred = starred;
        mFabButton.setChecked(mStarred, allowAnimate);

        ImageView iconView = (ImageView) mFabButton.findViewById(R.id.add_schedule_icon);
        lUtil.setOrAnimatePlusCheckIcon(iconView, starred, allowAnimate);
//        mFabButton.setContentDescription(getString(starred
//                ? R.string.remove_from_schedule_desc
//                : R.string.add_to_schedule_desc));
    }

    private void setupPhotoAndApplyTheme() {
        String beforePhotoUrl = new WearSharedPreference(this).get(getString(R.string.key_preference_photo_url), "");
        Log.d(TAG, "beforePhotoUrl:" + beforePhotoUrl);
        if (beforePhotoUrl.length() == 0) {
            return;
        }
        Glide.with(this)
                .load(beforePhotoUrl)
                .asBitmap()
                .into(new BitmapImageViewTarget(beforePhoto) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                        super.onResourceReady(bitmap, anim);

                        hasPhoto = true;
                        Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener() {
                            @Override
                            public void onGenerated(Palette palette) {
                                applyTheme(palette);
                            }
                        });
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
    public void onScrollChanged(int deltaX, int deltaY) {
        int scrollY = scrollView.getScrollY();

        float newTop = Math.max(mPhotoHeightPixels, scrollY);
        mHeaderBox.setTranslationY(newTop);
        mFabButton.setTranslationY(newTop + mHeaderHeightPixels
                - mFabButton.getHeight() / 2);

        float gapFillProgress = 1;
        if (mPhotoHeightPixels != 0) {
            gapFillProgress = Math.min(Math.max(UIUtils.getProgress(scrollY,
                    0,
                    mPhotoHeightPixels), 0), 1);
        }

        ViewCompat.setElevation(mHeaderBox, gapFillProgress * maxHeaderElevation);
        ViewCompat.setElevation(mFabButton, gapFillProgress * maxHeaderElevation
                + fabElevation);

        // Move background photo (parallax effect)
        beforePhoto.setTranslationY(scrollY * 0.5f);
    }

    public void downloadFile(String url) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        Uri downloadUri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(
                downloadUri);

        request.setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI
                        | DownloadManager.Request.NETWORK_MOBILE);

        downloadManager.enqueue(request);

    }

}
