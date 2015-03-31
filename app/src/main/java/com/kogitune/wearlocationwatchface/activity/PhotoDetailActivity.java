package com.kogitune.wearlocationwatchface.activity;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.kogitune.activity_transition.ActivityTransition;
import com.kogitune.activity_transition.ExitActivityTransition;
import com.kogitune.wearlocationwatchface.R;
import com.kogitune.wearlocationwatchface.common.OnSubscribeWearSharedPreferences;
import com.kogitune.wearlocationwatchface.observable.FlickrObservable;
import com.kogitune.wearlocationwatchface.util.LUtils;
import com.kogitune.wearlocationwatchface.util.UIUtils;
import com.kogitune.wearlocationwatchface.widget.CheckableFrameLayout;
import com.kogitune.wearlocationwatchface.widget.ObservableScrollView;
import com.kogitune.wearsharedpreference.WearSharedPreference;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;
import rx.Observable;
import rx.Subscriber;
import rx.android.content.ContentObservable;
import rx.functions.Action1;

public class PhotoDetailActivity extends ActionBarActivity implements ObservableScrollView.Callbacks {

    private static final String TAG = "MainActivity";
    private static final float PHOTO_ASPECT_RATIO = 1.5f;
    public static final int DURATION = 500;
    // views
    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    @InjectView(R.id.scroll_view)
    ObservableScrollView scrollView;
    @InjectView(R.id.beforePhoto)
    ImageView photoImageView;
    @InjectView(R.id.fab_button)
    CheckableFrameLayout fabButton;
    @InjectView(R.id.details_container)
    View detailsContainer;
    private WearSharedPreference wearPref;
    private LUtils lUtil;
    private int photoHeightPixels;

    // flags
    private boolean starred;
    private boolean hasPhoto = false;

    // sizes
    private int maxHeaderElevation;
    private int fabElevation;
    private int headerHeightPixels;

    private ViewTreeObserver.OnPreDrawListener mGlobalLayoutListener = this::recomputePhotoAndScrollingMetrics;
    private int oldPhotoHashCode;
    private FlickrObservable.PhotoShowInfo photoShowInfo;
    private ExitActivityTransition exitActivityTransition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);
        ButterKnife.inject(this);
        photoShowInfo = FlickrObservable.PhotoShowInfo.parseBundle(getIntent().getExtras());

        lUtil = LUtils.getInstance(this);
        wearPref = new WearSharedPreference(this);

        exitActivityTransition = ActivityTransition.with(getIntent()).to(photoImageView).duration(DURATION).start(savedInstanceState);
        setupViews();
    }

    @Override
    public void onBackPressed() {
        toolbar.setAlpha(0);
        detailsContainer.setAlpha(0);

        int[] screenLocation = new int[2];
        fabButton.getLocationOnScreen(screenLocation);
        fabButton.animate()
                .setDuration(DURATION)
                .scaleX(0).scaleY(0);

        if (scrollView.getScrollY() != 0) {
            super.onBackPressed();
            return;
        }
        exitActivityTransition.exit(this);
    }

    private void setupViews() {
        Resources res = getResources();

        toolbar.setTitle(photoShowInfo.title);
        toolbar.getMenu().clear();
        setSupportActionBar(toolbar);


        scrollView.addCallbacks(this);
        scrollView.getViewTreeObserver().addOnPreDrawListener(mGlobalLayoutListener);

        maxHeaderElevation = getResources().getDimensionPixelSize(
                R.dimen.session_detail_max_header_elevation);
        fabElevation = getResources().getDimensionPixelSize(R.dimen.fab_elevation);
        fabButton.setScaleX(0);
        fabButton.setScaleY(0);
        fabButton.animate().scaleX(1).scaleY(1).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(DURATION);

        fabButton.setOnClickListener(view -> {
            if (!starred) {
                downloadAndOpen(photoShowInfo.url);
            }
            boolean starredReverse = !PhotoDetailActivity.this.starred;
            showStarred(starredReverse, true);
        });


        setupPhotoAndApplyTheme();
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
                        Observable.create(new OnSubscribeWearSharedPreferences(PhotoDetailActivity.this, getString(R.string.key_preference_time_text_accent), b)))
                .subscribe((subscriber) -> {
                }, throwable -> {
                    throwable.printStackTrace();
                });

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

    private boolean recomputePhotoAndScrollingMetrics() {
        scrollView.getViewTreeObserver().removeOnPreDrawListener(mGlobalLayoutListener);
        headerHeightPixels = toolbar.getHeight();

        photoHeightPixels = 0;
        photoHeightPixels = photoImageView.getHeight();

        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)
                detailsContainer.getLayoutParams();
        mlp.topMargin = headerHeightPixels + photoHeightPixels;
        detailsContainer.setLayoutParams(mlp);

        onScrollChanged(0, 0); // trigger scroll handling
        toolbar.setTranslationY(UIUtils.getScreenHeight(this));
        detailsContainer.setTranslationY(UIUtils.getScreenHeight(this));
        toolbar.animate()
                .translationY(photoHeightPixels)
                .setDuration(300)
                .setStartDelay(300);
        detailsContainer.animate().translationY(0).setDuration(300)
                .setStartDelay(400);
        return true;
    }

    private void startAnimationIfImageChanged() {
        Drawable drawable = photoImageView.getDrawable();
        if (drawable == null) {
            return;
        }
        final int newPhotoHashCode = drawable.hashCode();
        if (oldPhotoHashCode != newPhotoHashCode) {
            startCircularRevealAnimation(photoHeightPixels);
        }
        oldPhotoHashCode = newPhotoHashCode;
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
        String beforePhotoUrl = photoShowInfo.url;
        Log.d(TAG, "beforePhotoUrl:" + beforePhotoUrl);
        if (beforePhotoUrl.length() == 0) {
            return;
        }

        //setPhotoAndApplyTheme(beforePhotoUrl);
    }

    private void setPhotoAndApplyTheme(String beforePhotoUrl) {
        Glide.with(this)
                .load(beforePhotoUrl)
                .asBitmap()
                .into(new BitmapImageViewTarget(photoImageView) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                        super.onResourceReady(bitmap, anim);
                        hasPhoto = true;
                        Palette.generateAsync(bitmap, PhotoDetailActivity.this::applyTheme);
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
        int finalRadius = Math.max(photoImageView.getWidth(), newHeight);

        int cx = (photoImageView.getLeft() + photoImageView.getRight()) / 2;
        int cy = (photoImageView.getTop() * 2 + newHeight) / 2;

        SupportAnimator animator =
                ViewAnimationUtils.createCircularReveal(photoImageView, cx, cy, 0, finalRadius);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(1500);
        animator.start();
    }


    @Override
    public void onScrollChanged(int deltaX, int deltaY) {
        int scrollY = scrollView.getScrollY();
        float newTop = Math.max(photoHeightPixels, scrollY);

        toolbar.setTranslationY(newTop);
        fabButton.setTranslationY(newTop + headerHeightPixels
                - fabButton.getHeight() / 2);


        float gapFillProgress = 1;
        if (photoHeightPixels != 0) {
            gapFillProgress = Math.min(Math.max(UIUtils.getProgress(scrollY,
                    0,
                    photoHeightPixels), 0), 1);
        }

        ViewCompat.setElevation(toolbar, gapFillProgress * maxHeaderElevation);
        ViewCompat.setElevation(fabButton, gapFillProgress * maxHeaderElevation
                + fabElevation);

        // Move background photoImageView (parallax effect)
        photoImageView.setTranslationY(scrollY * 0.5f);
    }

    protected void downloadAndOpen(String url) {
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

                Toast toast = Toast.makeText(PhotoDetailActivity.this,
                        "Downloading of data just finished", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 25, 400);
                toast.show();

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        wearPref.registerOnPreferenceChangeListener(new WearSharedPreference.OnPreferenceChangeListener() {
            @Override
            public void onPreferenceChange(WearSharedPreference preference, String key, Bundle bundle) {
                if (!TextUtils.equals(getString(R.string.key_preference_photo_ids), key)) {
                    return;
                }
                final String photoUrl = bundle.getString(key);
                //setPhotoAndApplyTheme(photoUrl);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        wearPref.unregisterOnPreferenceChangeListener();
    }
}
