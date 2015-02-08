package com.kogitune.wearlocationwatchface;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.kogitune.wearlocationwatchface.util.LUtils;
import com.kogitune.wearlocationwatchface.util.UIUtils;
import com.kogitune.wearlocationwatchface.widget.CheckableFrameLayout;
import com.kogitune.wearlocationwatchface.widget.ObservableScrollView;
import com.kogitune.wearsharedpreference.WearSharedPreference;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;
import rx.android.content.ContentObservable;
import rx.functions.Action1;

public class MainActivity extends ActionBarActivity implements ObservableScrollView.Callbacks {

    private static final int GLIDE_DISK_CACHE_SIZE_IN_BYTES = 128 * 1024 * 1024;
    private static final String TAG = "MainActivity";
    private Toolbar toolbar;
    private WearSharedPreference wearSharedPreference;
    private ObservableScrollView scrollView;
    private int photoHeightPixels;
    private View headerBox;
    private CheckableFrameLayout fabButton;
    private boolean starred;
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
        if (!Glide.isSetup()) {
            Glide.setup(new GlideBuilder(this)
                    .setDiskCache(DiskLruCacheWrapper.get(Glide.getPhotoCacheDir(this), GLIDE_DISK_CACHE_SIZE_IN_BYTES))
                    .setDecodeFormat(DecodeFormat.PREFER_RGB_565));
        }
        lUtil = LUtils.getInstance(this);

        scrollView = (ObservableScrollView) findViewById(R.id.scroll_view);
        scrollView.addCallbacks(this);
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);

        headerBox = findViewById(R.id.toolbar);
        maxHeaderElevation = getResources().getDimensionPixelSize(
                R.dimen.session_detail_max_header_elevation);
        fabElevation = getResources().getDimensionPixelSize(R.dimen.fab_elevation);

        detailsContainer = findViewById(R.id.details_container);

        fabButton = (CheckableFrameLayout) findViewById(R.id.fab_button);
        fabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean starred = !MainActivity.this.starred;
                showStarred(starred, true);
                String beforePhotoUrl = new WearSharedPreference(MainActivity.this).get(getString(R.string.key_preference_photo_url), "");
                downloadAndOpen(beforePhotoUrl);
            }
        });


        beforePhoto = (ImageView) findViewById(R.id.beforePhoto);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(new WearSharedPreference(this).get(getString(R.string.key_preference_photo_title), ""));
        toolbar.getMenu().clear();
        setSupportActionBar(toolbar);

        setupPhotoAndApplyTheme();
        DiscreteSeekBar searchRadiusSeekBar = (DiscreteSeekBar) findViewById(R.id.search_radius);
        wearSharedPreference = new WearSharedPreference(this);
        final int firstRadius = wearSharedPreference.get(getString(R.string.key_preference_search_range), getResources().getInteger(R.integer.search_range_default));
        searchRadiusSeekBar.setProgress(firstRadius);
        searchRadiusSeekBar.setOnProgressChangeListener((seekBar, value, fromUser) -> {
            wearSharedPreference.put(getString(R.string.key_preference_search_range), seekBar.getProgress());
            wearSharedPreference.sync(new WearSharedPreference.OnSyncListener() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFail(Exception e) {
                    Toast.makeText(MainActivity.this, "Sync failed search radius", Toast.LENGTH_LONG).show();
                    seekBar.setProgress(firstRadius);
                }
            });
        });
       }


    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener
            = this::recomputePhotoAndScrollingMetrics;
    private int headerHeightPixels;
    private View detailsContainer;

    private void recomputePhotoAndScrollingMetrics() {
        headerHeightPixels = headerBox.getHeight();

        photoHeightPixels = 0;
        if (hasPhoto) {
            photoHeightPixels = (int) (beforePhoto.getWidth() / PHOTO_ASPECT_RATIO);
        }

        ViewGroup.LayoutParams lp;
        lp = beforePhoto.getLayoutParams();
        if (lp.height != photoHeightPixels) {
            lp.height = photoHeightPixels;
            beforePhoto.setLayoutParams(lp);
            startCircularRevealAnimation(photoHeightPixels);
        }

        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)
                detailsContainer.getLayoutParams();
        if (mlp.topMargin != headerHeightPixels + photoHeightPixels) {
            mlp.topMargin = headerHeightPixels + photoHeightPixels;
            detailsContainer.setLayoutParams(mlp);
        }

        onScrollChanged(0, 0); // trigger scroll handling
    }


    private void showStarred(boolean starred, boolean allowAnimate) {
        this.starred = starred;
        fabButton.setChecked(this.starred, allowAnimate);

        ImageView iconView = (ImageView) fabButton.findViewById(R.id.add_schedule_icon);
        lUtil.setOrAnimatePlusCheckIcon(iconView, starred, allowAnimate);
//        fabButton.setContentDescription(getString(starred
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
                        Palette.generateAsync(bitmap, MainActivity.this::applyTheme);
                    }
                });
    }

    private void applyTheme(Palette palette) {
        if (palette == null) {
            return;
        }
        final Palette.Swatch darkVibrantSwatch = palette.getDarkVibrantSwatch();
        if (darkVibrantSwatch != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(darkVibrantSwatch.getRgb());
            }
        }
        final Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
        if (vibrantSwatch != null) {
            toolbar.setBackgroundColor(vibrantSwatch.getRgb());
            toolbar.setTitleTextColor(vibrantSwatch.getTitleTextColor());
        }
    }

    private void startCircularRevealAnimation(int newHeight) {
        // get the final radius for the clipping circle
        int finalRadius = Math.max(beforePhoto.getWidth(), newHeight);

        int cx = (beforePhoto.getLeft() + beforePhoto.getRight()) / 2;
        int cy = (beforePhoto.getTop() * 2 + newHeight) / 2;

        SupportAnimator animator =
                ViewAnimationUtils.createCircularReveal(beforePhoto, cx, cy, 0, finalRadius);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(1500);
        animator.start();
    }


    @Override
    public void onScrollChanged(int deltaX, int deltaY) {
        int scrollY = scrollView.getScrollY();

        float newTop = Math.max(photoHeightPixels, scrollY);
        headerBox.setTranslationY(newTop);
        fabButton.setTranslationY(newTop + headerHeightPixels
                - fabButton.getHeight() / 2);

        float gapFillProgress = 1;
        if (photoHeightPixels != 0) {
            gapFillProgress = Math.min(Math.max(UIUtils.getProgress(scrollY,
                    0,
                    photoHeightPixels), 0), 1);
        }

        ViewCompat.setElevation(headerBox, gapFillProgress * maxHeaderElevation);
        ViewCompat.setElevation(fabButton, gapFillProgress * maxHeaderElevation
                + fabElevation);

        // Move background photo (parallax effect)
        beforePhoto.setTranslationY(scrollY * 0.5f);
    }

    public void downloadAndOpen(String url) {
        final DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        Uri downloadUri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(
                downloadUri);

        request.setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI
                        | DownloadManager.Request.NETWORK_MOBILE);

        final long downloadId = downloadManager.enqueue(request);
        ContentObservable.fromBroadcast(this, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)).subscribe(new Action1<Intent>() {
            @Override
            public void call(Intent intent) {
                long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId != referenceId) {
                    return;
                }

                Intent openFileIntent = new Intent();
                openFileIntent.setAction(Intent.ACTION_VIEW);
                openFileIntent.setDataAndType(downloadManager.getUriForDownloadedFile(downloadId), "image/*");
                startActivity(openFileIntent);

                Toast toast = Toast.makeText(MainActivity.this,
                        "Downloading of data just finished", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 25, 400);
                toast.show();

            }
        });

    }

}
