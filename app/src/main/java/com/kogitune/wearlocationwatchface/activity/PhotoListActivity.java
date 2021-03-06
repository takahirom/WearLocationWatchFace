package com.kogitune.wearlocationwatchface.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.kogitune.wearlocationwatchface.R;
import com.kogitune.wearlocationwatchface.adapter.MenuAdapter;
import com.kogitune.wearlocationwatchface.adapter.PhotoListAdapter;
import com.kogitune.wearlocationwatchface.data.PhotoShowInfo;
import com.kogitune.wearlocationwatchface.observable.FlickrObservable;
import com.kogitune.wearlocationwatchface.util.UIUtils;
import com.kogitune.wearsharedpreference.WearSharedPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.android.lifecycle.LifecycleObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class PhotoListActivity extends RxActionBarActivity {

    // constants
    private static final int GLIDE_DISK_CACHE_SIZE_IN_BYTES = 512 * 1024 * 1024;

    private static final int ANIM_DURATION_TOOLBAR = 300;
    @InjectView(R.id.tool_bar)
    Toolbar toolbar;
    @InjectView(R.id.toolbar_title)
    TextView titleTextView;

    @InjectView(R.id.photo_list)
    RecyclerView photoListRecyclerView;

    @InjectView(R.id.menu)
    RecyclerView menuRecyclerView;
    @InjectView(R.id.drawer)
    DrawerLayout drawer;

    RecyclerView.Adapter menuAdapter;
    RecyclerView.LayoutManager menuLayoutManager;

    ActionBarDrawerToggle drawerToggle;
    PhotoListAdapter photoListAdapter;
    RecyclerView.LayoutManager photoLayoutManager;

    private WearSharedPreference wearPref;
    private FlickrObservable flickrObservable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);

        if (!Glide.isSetup()) {
            Glide.setup(new GlideBuilder(this)
                    .setDiskCache(DiskLruCacheWrapper.get(Glide.getPhotoCacheDir(this), GLIDE_DISK_CACHE_SIZE_IN_BYTES))
                    .setDecodeFormat(DecodeFormat.PREFER_RGB_565));
        }
        ButterKnife.inject(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        setupDrawer();

        startFirstAnimation();

        photoListAdapter = new PhotoListAdapter();
        photoListRecyclerView.setAdapter(photoListAdapter);
        photoListRecyclerView.setHasFixedSize(true);

        photoLayoutManager = new LinearLayoutManager(this);
        photoListRecyclerView.setLayoutManager(photoLayoutManager);
        flickrObservable = new FlickrObservable(PhotoListActivity.this);

        wearPref = new WearSharedPreference(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupPhotoList();
    }

    private void setupPhotoList() {
        final WearSharedPreference wearSharedPreference = new WearSharedPreference(this);
        final String photoIds = wearSharedPreference.get(getString(R.string.key_preference_photo_ids), "");
        final String[] savedPhotoIdArray = photoIds.split(",");
        final List<String> savedPhotoIdList = new ArrayList<>(Arrays.asList(savedPhotoIdArray));

        final int shownItemCount = photoListAdapter.getItemCount();
        int startIndex = shownItemCount < savedPhotoIdList.size() ? shownItemCount - 1 : savedPhotoIdList.size() - 1;
        if (startIndex < 0) {
            startIndex = 0;
        }
        final List<String> addPhotoIdList = savedPhotoIdList.subList(startIndex, savedPhotoIdList.size() - 1);

        final long startTime = AnimationUtils.currentAnimationTimeMillis();
        LifecycleObservable.bindActivityLifecycle(lifecycle(), Observable.from(addPhotoIdList).concatMap(new Func1<String, Observable<PhotoShowInfo>>() {

            @Override
            public Observable<PhotoShowInfo> call(String photoId) {
                return flickrObservable.fetchPhotoInfo(photoId);
            }
        })).map(new Func1<PhotoShowInfo, PhotoShowInfo>() {
            @Override
            public PhotoShowInfo call(PhotoShowInfo photoShowInfo) {
                final long currentTime = AnimationUtils.currentAnimationTimeMillis();
                final long sleepTimeForAnimation = 600 - (currentTime - startTime);
                if (sleepTimeForAnimation < 0) {
                    return photoShowInfo;
                }
                try {
                    Thread.sleep(sleepTimeForAnimation);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return photoShowInfo;
            }
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(photoListAdapter::addPhotoShowInfo
                        , throwable -> throwable.printStackTrace());


    }

    private void startFirstAnimation() {
        int actionbarSize = UIUtils.dpToPx(56);
        toolbar.setTranslationY(-actionbarSize);
        titleTextView.setTranslationY(-actionbarSize);
        toolbar.animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setStartDelay(300);
        titleTextView.animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setStartDelay(400);
    }

    private void setupDrawer() {
        menuRecyclerView.setHasFixedSize(true);
        List<MenuAdapter.MenuItem> menuItemList = new ArrayList<>();
        menuItemList.add(new MenuAdapter.MenuItem(new Intent(this, PhotoListActivity.class), "Home", R.drawable.ic_photo_library_grey600_36dp));
        menuItemList.add(new MenuAdapter.MenuItem(new Intent(this, SettingActivity.class), "Settings", R.drawable.ic_settings_grey600_36dp));
        menuAdapter = new MenuAdapter(menuItemList);
        menuRecyclerView.setAdapter(menuAdapter);

        menuLayoutManager = new LinearLayoutManager(this);
        menuRecyclerView.setLayoutManager(menuLayoutManager);
        drawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.open_drawer, R.string.close_drawer) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

            }
        };
        drawer.setDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        wearPref.registerOnPreferenceChangeListener((preference, key, bundle) -> {
            if (!TextUtils.equals(getString(R.string.key_preference_photo_ids), key)) {
                return;
            }
            final String ids = bundle.getString(key);
            if (TextUtils.isEmpty(ids)){
                return;
            }
            final String addedId = ids.split(",")[0];
            flickrObservable.fetchPhotoInfo(addedId).subscribe(new Action1<PhotoShowInfo>() {
                @Override
                public void call(PhotoShowInfo photoShowInfo) {
                    photoListAdapter.addNewPhoto(photoShowInfo);
                    photoLayoutManager.scrollToPosition(0);
                }
            });
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        wearPref.unregisterOnPreferenceChangeListener();
    }
}
