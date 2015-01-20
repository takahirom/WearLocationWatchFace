package com.kogitune.wearlocationwatchface;

/**
 * Created by takam on 2014/12/29.
 */

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.location.Location;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Toast;

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
    private Bitmap drawingBitmap = null;
    private Engine engine;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private LocationGetter locationGetter;
    public long settingPhotoTime;
    private static final int INTERVAL_SETTING_PHOTO = 60 * 60 * 1000;
    private FloatingActionBarManager floatingActionBarManager;
    private String largePhotoUrl;
    private String photoTitle;
    private State state = State.NORMAL;

    enum State {
        NORMAL,
        DRAWING
    }

    @Override
    public Engine onCreateEngine() {

        locationGetter = new LocationGetter(this);
        engine = new WatchFaceEngine();
        floatingActionBarManager = new FloatingActionBarManager(this);
        floatingActionBarManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPhoto();
            }
        });

        return engine;
    }

    class WatchFaceEngine extends Engine {
        private SurfaceHolder holder;
        private int canvasState = -1;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            this.holder = holder;
            setWatchFaceStyle(
                    new WatchFaceStyle.Builder(WatchFaceService.this)
                            .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                            .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                            .build());
            setPhotoIfNeed();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onPeekCardPositionUpdate(Rect rect) {
            invalidate();
        }

        @Override
        public void onDraw(final Canvas canvas, final Rect wearRect) {
            if (drawingBitmap != bitmap) {
                state = State.DRAWING;
            }

            //canvas.drawColor(0, PorterDuff.Mode.CLEAR);

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

            if (state == State.NORMAL) {
                // Create bitmapDrwable from bitmap
                Paint paint = new Paint();
                paint.setFilterBitmap(true);
                canvas.drawBitmap(bitmap, null, new Rect(0, 0, layoutCalc.getLocationImageWidth(), layoutCalc.getLocationImageHeight()), paint);
                drawingBitmap = bitmap;
            } else {
                drawStateDrawing(layoutCalc, canvas, wearRect);
            }


            bottomPaperPaint.setColor(layoutCalc.getBottomPaperColor());
            whiteMediumFontPaint.setColor(layoutCalc.getDateTextColor());
            whiteBigFontPaint.setColor(layoutCalc.getTimeTextColor());
            // draw paper
            canvas.drawRect(0, layoutCalc.getBottomPaperTop(), wearRect.right, wearRect.bottom, bottomPaperPaint);

            final Resources resources = getResources();
            int actionBarMargin = resources.getDimensionPixelSize(R.dimen.action_button_margin);
            int actionBarRadius = resources.getDimensionPixelSize(R.dimen.action_button_diameter);
            floatingActionBarManager.update(resources.getColor(R.color.floating_button_color), (int) (wearRect.right - actionBarMargin - actionBarRadius), (int) (layoutCalc.getBottomPaperTop() - actionBarRadius / 2));

            // draw time
            if (layoutCalc.isCenterTimeText()) {
                drawCenterText(canvas, whiteBigFontPaint, timeFormat.format(new Date()), layoutCalc.getTimeTextTop());
            } else {
                canvas.drawText(timeFormat.format(new Date()), 20, layoutCalc.getTimeTextTop(), whiteBigFontPaint);
            }
            // draw date
            canvas.drawText(dateFormat.format(new Date()), 20, layoutCalc.getDateTextTop(), whiteMediumFontPaint);
            if (state == State.DRAWING) {
                invalidate();
            }
        }

        float radius = 0;

        private void drawStateDrawing(WatchFaceLayoutCalculator layoutCalc, Canvas canvas, Rect wearRect) {
            layoutCalc.calc(bitmap, wearRect, getPeekCardPosition().top);

            Bitmap output = Bitmap.createBitmap(layoutCalc.getLocationImageWidth(),
                    layoutCalc.getLocationImageHeight(), Bitmap.Config.ARGB_8888);
            Canvas drawingCanvas = new Canvas(output);
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, layoutCalc.getLocationImageWidth(),
                    layoutCalc.getLocationImageHeight());

            paint.setAntiAlias(true);
            drawingCanvas.drawARGB(0, 0, 0, 0);
            //paint.setColor(color);
            drawingCanvas.drawCircle(layoutCalc.getLocationImageWidth() / 2,
                    layoutCalc.getLocationImageHeight() / 2, radius, paint);
            radius += 10;
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            drawingCanvas.drawBitmap(bitmap, rect, rect, paint);
            Paint bitmapDrawPaint = new Paint();
            bitmapDrawPaint.setAntiAlias(true);
            canvas.drawBitmap(output, 0f, 0f, bitmapDrawPaint);
            if (radius > wearRect.width()) {
                radius = 0;
                state = State.NORMAL;
                drawingBitmap = bitmap;
            }

        }


        private void helplesslyShow(Canvas canvas, Paint whiteMediumPaint, Paint whiteBigPaint) {
            drawCenterText(canvas, whiteBigPaint, timeFormat.format(new Date()), 0);
            canvas.drawText(dateFormat.format(new Date()), 20, 0, whiteMediumPaint);
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
            floatingActionBarManager.setVisible(visible);
            if (visible) {
                setPhotoIfNeed();
            }
        }

    }

    private void setPhotoIfNeed() {
        if (settingPhotoTime + INTERVAL_SETTING_PHOTO < System.currentTimeMillis()) {
            locationGetter.updateLocation();
            setPhoto();
        }
    }

    public void setPhoto() {
        floatingActionBarManager.startRefresh();
        final Location location = locationGetter.getLastLocation();
        if (location == null) {
            Log.d(TAG, "setPhoto location null");
            floatingActionBarManager.stopRefresh();
            return;
        }
        int range = new WearSharedPreference(this).get(getString(R.string.key_preference_search_range), getResources().getInteger(R.integer.search_range_default));
        String flickrApiUrl = "https://api.flickr.com/services/rest/?method=flickr.photos.search&group_id=1463451@N25&api_key=" + BuildConfig.FLICKR_API_KEY + "&license=1%2C2%2C3%2C4%2C5%2C6&sort=interestingness-desc&lat=" + location.getLatitude() + "&lon=" + location.getLongitude() + "&radius=" + range + "&extras=url_n,url_l&per_page=30&format=json&nojsoncallback=1";
        Log.d(TAG, "api url:" + flickrApiUrl);
        new WearGetText(this).get(flickrApiUrl, new WearGetText.WearGetCallBack() {
            @Override
            public void onGet(String s) {
                try {
                    final JSONArray photosArray = new JSONObject(s).getJSONObject("photos").getJSONArray("photo");
                    final int nextIndex = new Random().nextInt(photosArray.length());
                    String photoUrl = photosArray.getJSONObject(nextIndex).getString("url_n").toString();

                    Log.d(TAG, "str:" + photoUrl);
                    getAndSettingPhotoBitmap(photoUrl);

                    largePhotoUrl = photosArray.getJSONObject(nextIndex).getString("url_l").toString();
                    photoTitle = photosArray.getJSONObject(nextIndex).getString("title").toString();

                } catch (JSONException e) {
                    Log.d(TAG, "json" + s);
                    e.printStackTrace();
                    floatingActionBarManager.stopRefresh();
                }
            }

            @Override
            public void onFail(Exception e) {
                floatingActionBarManager.stopRefresh();
                e.printStackTrace();
            }
        }, 10);
    }

    public void getAndSettingPhotoBitmap(String url) {
        new WearGetImage(this).get(url, new WearGetImage.WearGetCallBack() {
            @Override
            public void onGet(Bitmap bitmap) {
                WatchFaceService.this.bitmap = bitmap;
                engine.invalidate();
                settingPhotoTime = System.currentTimeMillis();
                floatingActionBarManager.stopRefresh();

                savePhotoUrl(photoTitle, largePhotoUrl);
            }

            @Override
            public void onFail(Exception e) {
                e.printStackTrace();
                Toast.makeText(WatchFaceService.this, "Can't get photo.Please retry later.", Toast.LENGTH_LONG).show();
                floatingActionBarManager.problemStopRefresh();
            }
        }, 50);

    }

    private void savePhotoUrl(String title, String photoUrl) {
        final WearSharedPreference preference = new WearSharedPreference(this);
        preference.put(getString(R.string.key_preference_photo_url), photoUrl);
        preference.put(getString(R.string.key_preference_photo_title), title);
        preference.sync(new WearSharedPreference.OnSyncListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFail(Exception e) {
            }
        });
    }

}
