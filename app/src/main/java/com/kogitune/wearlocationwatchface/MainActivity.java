package com.kogitune.wearlocationwatchface;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ImageView;

import com.kogitune.wearsharedpreference.WearSharedPreference;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(new WearSharedPreference(this).get(getString(R.string.key_preference_photo_title),""));
        setSupportActionBar(toolbar);

        setupPhotoAndApplyTheme();
    }

    private void setupPhotoAndApplyTheme() {
        final ImageView beforePhoto = (ImageView) findViewById(R.id.beforePhoto);
        String beforePhotoUrl = new WearSharedPreference(this).get(getString(R.string.key_preference_photo_url), "");
        Log.d(TAG, "beforePhotoUrl:" + beforePhotoUrl);
        if (beforePhotoUrl.length() == 0) {
            return;
        }
        Picasso.with(this).load(beforePhotoUrl).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                beforePhoto.setImageBitmap(bitmap);
                applyTheme(Palette.generate(bitmap));
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

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
}
