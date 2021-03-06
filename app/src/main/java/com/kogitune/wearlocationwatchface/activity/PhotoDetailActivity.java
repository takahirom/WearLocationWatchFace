package com.kogitune.wearlocationwatchface.activity;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
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
import android.widget.TextView;
import android.widget.Toast;

import com.kogitune.activity_transition.ActivityTransition;
import com.kogitune.activity_transition.ExitActivityTransition;
import com.kogitune.wearlocationwatchface.R;
import com.kogitune.wearlocationwatchface.data.PhotoShowInfo;
import com.kogitune.wearlocationwatchface.observable.FlickrObservable;
import com.kogitune.wearlocationwatchface.util.LUtils;
import com.kogitune.wearlocationwatchface.util.UIUtils;
import com.kogitune.wearlocationwatchface.widget.CheckableFrameLayout;
import com.kogitune.wearlocationwatchface.widget.ObservableScrollView;
import com.kogitune.wearsharedpreference.WearSharedPreference;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;
import rx.Observable;
import rx.android.content.ContentObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.android.view.ViewObservable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class PhotoDetailActivity extends ActionBarActivity implements ObservableScrollView.Callbacks {

    private static final String TAG = "PhotoDetailActivity";
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

    @InjectView(R.id.photo_description)
    TextView photoDescription;
    @InjectView(R.id.photo_owner)
    TextView photoOwner;
    @InjectView(R.id.photo_place)
    TextView photoPlace;

    private WearSharedPreference wearPref;
    private LUtils lUtil;
    private int photoHeightPixels;

    // flags
    private boolean starred;

    // sizes
    private int maxHeaderElevation;
    private int fabElevation;
    private int headerHeightPixels;

    private ViewTreeObserver.OnPreDrawListener mGlobalLayoutListener = this::recomputePhotoAndScrollingMetrics;
    private int oldPhotoHashCode;
    private PhotoShowInfo photoShowInfo;
    private ExitActivityTransition exitActivityTransition;
    private String lastGeoLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);
        ButterKnife.inject(this);
        photoShowInfo = PhotoShowInfo.parseBundle(getIntent().getExtras());

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
        toolbar.setTitle(photoShowInfo.title);
        toolbar.getMenu().clear();
        setSupportActionBar(toolbar);
        applyTheme();

        scrollView.addCallbacks(this);
        scrollView.getViewTreeObserver().addOnPreDrawListener(mGlobalLayoutListener);

        maxHeaderElevation = getResources().getDimensionPixelSize(
                R.dimen.session_detail_max_header_elevation);
        fabElevation = getResources().getDimensionPixelSize(R.dimen.fab_elevation);
        fabButton.setScaleX(0);
        fabButton.setScaleY(0);
        fabButton.animate().scaleX(1).scaleY(1).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(DURATION);

        ViewObservable.clicks(fabButton).subscribe(view -> {
            if (!starred) {
                downloadAndOpen(photoShowInfo.url);
            }
            boolean starredReverse = !PhotoDetailActivity.this.starred;
            showStarred(starredReverse, true);
        });

        photoDescription.setText(photoShowInfo.description);
        photoOwner.setText(photoShowInfo.username);
        setupPhotoAndApplyTheme();

        new FlickrObservable(this).fetchPhotoLocation(photoShowInfo.id).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(new Action1<Location>() {
            @Override
            public void call(Location location) {

                lastGeoLocation = location.getLatitude() + "," + location.getLongitude();
                photoPlace.setText(lastGeoLocation);
            }
        }, Throwable::printStackTrace);

        ViewObservable.clicks(photoPlace).subscribe(v -> {
            if (lastGeoLocation == null) {
                return;
            }
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + lastGeoLocation + "?q=" + lastGeoLocation + "(" + photoShowInfo.title + ")"));
            startActivity(intent);

        });

    }

    private boolean recomputePhotoAndScrollingMetrics() {
        scrollView.getViewTreeObserver().removeOnPreDrawListener(mGlobalLayoutListener);
        headerHeightPixels = toolbar.getHeight();

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

    private void applyTheme() {
        final Drawable drawable = photoImageView.getDrawable();
        final Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        Palette.generateAsync(bitmap, PhotoDetailActivity.this::applyTheme);
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
        final Observable<Long> downloadObservable = Observable.just(downloadUri)
                .map(uri -> new DownloadManager.Request(downloadUri))
                .doOnNext(request -> request.setAllowedNetworkTypes(
                        DownloadManager.Request.NETWORK_WIFI
                                | DownloadManager.Request.NETWORK_MOBILE))
                .map(downloadManager::enqueue);
        Observable.zip(
                ContentObservable
                        .fromBroadcast(this, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)),
                downloadObservable,
                (intent, downloadId) -> intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId ? downloadId : null).filter(downloadId -> downloadId != null)
                .subscribe(downloadId->{
                    Intent openFileIntent = new Intent();
                    openFileIntent.setAction(Intent.ACTION_VIEW);
                    openFileIntent.setDataAndType(downloadManager.getUriForDownloadedFile(downloadId), "image/*");
                    startActivity(openFileIntent);

                    Toast toast = Toast.makeText(PhotoDetailActivity.this,
                            "Downloading of data just finished", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP, 25, 400);
                    toast.show();

                });


    }


}
