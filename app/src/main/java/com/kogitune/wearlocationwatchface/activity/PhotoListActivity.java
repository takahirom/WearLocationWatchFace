package com.kogitune.wearlocationwatchface.activity;

import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.kogitune.wearlocationwatchface.R;
import com.kogitune.wearlocationwatchface.adapter.MenuAdapter;
import com.kogitune.wearlocationwatchface.observable.FlickrObservable;
import com.kogitune.wearlocationwatchface.util.UIUtils;
import com.kogitune.wearsharedpreference.WearSharedPreference;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class PhotoListActivity extends ActionBarActivity {

    private static final int ANIM_DURATION_TOOLBAR = 300;
    String TITLES[] = {"Home", "Settings"};
    int ICONS[] = {R.drawable.ic_photo_library_grey600_36dp, R.drawable.ic_settings_grey600_36dp};
    @InjectView(R.id.tool_bar)
    Toolbar toolbar;
    @InjectView(R.id.toolbar_title)
    TextView titleTextView;

    @InjectView(R.id.menu)
    RecyclerView menuRecyclerView;
    @InjectView(R.id.drawer)
    DrawerLayout drawer;

    RecyclerView.Adapter menuAdapter;
    RecyclerView.LayoutManager menuLayoutManager;

    ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);
        ButterKnife.inject(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        setupDrawer();
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

        final WearSharedPreference wearSharedPreference = new WearSharedPreference(this);
        final String photoIds = wearSharedPreference.get(getString(R.string.key_preference_photo_ids), "");
        final String[] photoIdArray = photoIds.split(",");

        Observable.from(photoIdArray).flatMap(new Func1<String, Observable<FlickrObservable.PhotoShowInfo>>() {
            @Override
            public Observable<FlickrObservable.PhotoShowInfo> call(String s) {
                return new FlickrObservable(PhotoListActivity.this).fetchPhotoInfo(s);
            }
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<FlickrObservable.PhotoShowInfo>() {

            @Override
            public void call(FlickrObservable.PhotoShowInfo photoInfo) {
                Log.d("TAG", photoInfo.title);
                Log.d("TAG", photoInfo.url);
                Toast.makeText(PhotoListActivity.this, photoInfo.title, Toast.LENGTH_SHORT).show();
            }
        },new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }

    private void setupDrawer() {
        menuRecyclerView.setHasFixedSize(true);
        menuAdapter = new MenuAdapter(TITLES, ICONS);
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
}