package com.kogitune.wearlocationwatchface;

/**
 * Created by takam on 2014/12/29.
 */

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import com.kogitune.wearhttp.WearGetImage;
import com.kogitune.wearhttp.WearGetText;
import com.kogitune.wearsharedpreference.WearSharedPreference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class WatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "WatchFaceService";
    private Bitmap bitmap = null;
    private Engine engine;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private LocationGetter locationGetter;
    public long settingPhotoTime;
    private static final int INTERVAL_SETTING_PHOTO = 60 * 60 * 60 * 1000;

    @Override
    public Engine onCreateEngine() {
        locationGetter = new LocationGetter(this);

        engine = new Engine() {

            @Override
            public void onCreate(SurfaceHolder holder) {
                super.onCreate(holder);
                setWatchFaceStyle(
                        new WatchFaceStyle.Builder(WatchFaceService.this)
                                .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                                .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                                .build());
            }

            @Override
            public void onTimeTick() {
                super.onTimeTick();
                invalidate();
            }

            @Override
            public void onDraw(Canvas canvas, Rect wearRect) {
                Log.d(TAG, "onDraw");
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);

                final Paint whiteMediumFontPaint = new Paint();
                whiteMediumFontPaint.setFilterBitmap(true);
                whiteMediumFontPaint.setAntiAlias(true);
                whiteMediumFontPaint.setColor(Color.WHITE);
                final Paint whiteBigFontPaint = new Paint(whiteMediumFontPaint);


                final Paint bottomPaperPaint = new Paint();

                if (bitmap == null) {
                    final int mediumTextSize = 24;
                    final int bigTextSize = 72;
                    whiteMediumFontPaint.setTextSize(mediumTextSize);
                    whiteBigFontPaint.setTextSize(bigTextSize);
                    helplesslyShow(canvas, whiteMediumFontPaint, whiteBigFontPaint);
                    return;
                }

                final WatchFaceLayoutCalculator layoutCalc = new WatchFaceLayoutCalculator();

                layoutCalc.calc(bitmap, wearRect, getPeekCardPosition().top);
                whiteBigFontPaint.setTextSize(layoutCalc.getBigTextSize());
                whiteMediumFontPaint.setTextSize(layoutCalc.getMediumTextSize());

                // Create bitmapDrwable from bitmap
                BitmapDrawable drawable = new BitmapDrawable(bitmap);
                drawable.setBounds(0, 0, layoutCalc.getLocationImageWidth(), layoutCalc.getLocationImageHeight());
                drawable.draw(canvas);

                bottomPaperPaint.setColor(layoutCalc.getBottomPaperColor());
                whiteMediumFontPaint.setColor(layoutCalc.getDateTextColor());
                whiteBigFontPaint.setColor(layoutCalc.getTimeTextColor());
                canvas.drawRect(0, layoutCalc.getBottomPaperTop(), wearRect.right, wearRect.bottom, bottomPaperPaint);

                if (layoutCalc.isCenterTimeText()) {
                    drawCenterText(canvas, whiteBigFontPaint, timeFormat.format(new Date()), layoutCalc.getTimeTextTop());
                }else{
                    canvas.drawText(timeFormat.format(new Date()), 20, layoutCalc.getTimeTextTop() , whiteBigFontPaint);
                }
                canvas.drawText(dateFormat.format(new Date()), 20, layoutCalc.getDateTextTop() , whiteMediumFontPaint);

                canvas.save();
            }

            private void helplesslyShow(Canvas canvas, Paint whiteMediumPaint, Paint whiteBigPaint) {
                drawCenterText(canvas, whiteBigPaint, timeFormat.format(new Date()), 0);
                canvas.drawText(dateFormat.format(new Date()), 20, 0, whiteMediumPaint);

                canvas.save();
            }

            public void drawCenterText(Canvas canvas, Paint paint, String text, float height) {
                Rect bounds = new Rect();
                paint.getTextBounds(text, 0, text.length(), bounds);
                int x = (canvas.getWidth() / 2) - (bounds.width() / 2);
                float y;
                if (height == 0) {
                    y = canvas.getHeight() - 1 - bounds.height();
                } else {
                    y = height;
                }
                canvas.drawText(text, x, y, paint);
            }


            @Override
            public void onVisibilityChanged(boolean visible) {
                if (visible) {
                    if (settingPhotoTime + INTERVAL_SETTING_PHOTO < System.currentTimeMillis()) {
                        locationGetter.updateLocation();
                        setPhoto();
                    }
                }
            }

        };


        return engine;
    }

    public void setPhoto() {
        final Location location = locationGetter.getLastLocation();
        if (location == null) {
            Log.d(TAG, "setPhoto location null");
            return;
        }
        String flickrApiUrl = "https://api.flickr.com/services/rest/?method=flickr.photos.search&group_id=1463451@N25&api_key=" + BuildConfig.FLICKR_API_KEY + "&license=1%2C2%2C3%2C4%2C5%2C6&sort=interestingness-desc&lat=" + location.getLatitude() + "&lon=" + location.getLongitude() + "&radius=30&extras=url_s&per_page=30&format=json&nojsoncallback=1";
        Log.d(TAG, "api url:" + flickrApiUrl);
        new WearGetText(this).get(flickrApiUrl, new WearGetText.WearGetCallBack() {
            @Override
            public void onGet(String s) {
                try {
                    final JSONArray photosArray = new JSONObject(s).getJSONObject("photos").getJSONArray("photo");
                    final int nextIndex = new Random().nextInt(photosArray.length());
                    String photoUrl = photosArray.getJSONObject(nextIndex).getString("url_s").toString();
                    savePhotoUrl(photoUrl);

                    Log.d(TAG, "str:" + photoUrl);
                    getAndSettingPhotoBitmap(photoUrl);
                } catch (JSONException e) {
                    Log.d(TAG, "json" + s);
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail(Exception e) {
                e.printStackTrace();
            }
        }, 30);
    }

    private void savePhotoUrl(String photoUrl) {
        final WearSharedPreference preference = new WearSharedPreference(this);
        preference.put(getString(R.string.key_preference_photo_url), photoUrl);
        preference.sync(new WearSharedPreference.OnSyncListener() {
            @Override
            public void onSuccess() {
                settingPhotoTime = System.currentTimeMillis();
            }

            @Override
            public void onFail(Exception e) {

            }
        });
    }

    public void getAndSettingPhotoBitmap(String url) {
        new WearGetImage(this).get(url, new WearGetImage.WearGetCallBack() {
            @Override
            public void onGet(Bitmap bitmap) {
                WatchFaceService.this.bitmap = bitmap;
                engine.invalidate();
            }

            @Override
            public void onFail(Exception e) {
                e.printStackTrace();
            }
        }, 30);

    }
}
