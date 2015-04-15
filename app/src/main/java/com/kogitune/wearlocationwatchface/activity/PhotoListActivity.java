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
        final String[] photoIdArray = photoIds.split(",");
        ArrayList list = new ArrayList();
        final List<String> photoIdList = new ArrayList<>(Arrays.asList(photoIdArray));

        final int itemCount = photoListAdapter.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            photoIdList.remove(0);
        }

        final long startTime = AnimationUtils.currentAnimationTimeMillis();
        LifecycleObservable.bindActivityLifecycle(lifecycle(), Observable.from(photoIdList).concatMap(new Func1<String, Observable<PhotoShowInfo>>() {
            @Override
            public Observable<PhotoShowInfo> call(String s) {
                return new FlickrObservable(PhotoListActivity.this).fetchPhotoInfo(s);
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
                .observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<PhotoShowInfo>() {
            @Override
            public void call(PhotoShowInfo photoShowInfo) {
                photoListAdapter.addPhotoShowInfo(photoShowInfo);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                throwable.printStackTrace();
            }
        });


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
            final String photoUrl = bundle.getString(key);
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        wearPref.unregisterOnPreferenceChangeListener();
    }
}
