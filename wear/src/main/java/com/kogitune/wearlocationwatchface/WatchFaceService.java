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

import rx.Observable;

import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.newThread;

public class WatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "WatchFaceService";
    private Bitmap bitmap = null;
    private Bitmap drawingBitmap = null;
    private Engine engine;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private LocationGetObservable locationGetObservable;
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
            final Resources res = getResources();
            if (drawingBitmap != bitmap) {
                state = State.DRAWING;
            }

            //canvas.drawColor(0, PorterDuff.Mode.CLEAR);

            final Paint whiteMediumFontPaint = new Paint();
            whiteMediumFontPaint.setFilterBitmap(true);
            whiteMediumFontPaint.setAntiAlias(true);
            whiteMediumFontPaint.setColor(Color.WHITE);
            final Paint whiteBigFontPaint = new Paint(whiteMediumFontPaint);

            if (bitmap == null) {
                final int mediumTextSize = 24;
                final int bigTextSize = 72;
                whiteMediumFontPaint.setTextSize(mediumTextSize);
                whiteBigFontPaint.setTextSize(bigTextSize);
                helplesslyShow(canvas, whiteMediumFontPaint, whiteBigFontPaint);
                return;
            }

            final WatchFaceLayoutCalculator layoutCalc = new WatchFaceLayoutCalculator();

            layoutCalc.calc(res, bitmap, wearRect, getPeekCardPosition().top);
            whiteBigFontPaint.setTextSize(layoutCalc.getBigTextSize());
            whiteMediumFontPaint.setTextSize(layoutCalc.getMediumTextSize());

            if (drawingBitmap != null) {
                Paint paint = new Paint();
                WatchFaceLayoutCalculator drawingCalc = new WatchFaceLayoutCalculator();
                drawingCalc.calc(res, drawingBitmap, wearRect, getPeekCardPosition().top);
                canvas.drawBitmap(drawingBitmap, null, new Rect(0, 0, drawingCalc.getLocationImageWidth(), drawingCalc.getLocationImageHeight()), paint);

                // draw paper
                final Paint bottomPaperPaint = new Paint();
                bottomPaperPaint.setColor(drawingCalc.getBottomPaperColor());
                bottomPaperPaint.setAntiAlias(true);
                bottomPaperPaint.setShadowLayer(12, 0, -2, 0xFF000000);
                canvas.drawRect(0, layoutCalc.getBottomPaperTop(), wearRect.right, wearRect.bottom, bottomPaperPaint);
            }

            if (state == State.DRAWING) {
                drawRefreshCircle(layoutCalc, canvas, wearRect);
            }


            whiteMediumFontPaint.setColor(layoutCalc.getDateTextColor());
            whiteBigFontPaint.setColor(layoutCalc.getTimeTextColor());

            floatingActionBarManager.update(res.getColor(R.color.floating_button_color), layoutCalc.getActionButtonX(), layoutCalc.getActionButtonY());

            // draw time
            if (layoutCalc.isCenterTimeText()) {
                drawCenterText(canvas, whiteBigFontPaint, timeFormat.format(new Date()), layoutCalc.getTimeTextTop());
            } else {
                canvas.drawText(timeFormat.format(new Date()), 20, layoutCalc.getTimeTextTop(), whiteBigFontPaint);
            }
            // draw date
            canvas.drawText(dateFormat.format(new Date()), 20, layoutCalc.getDateTextTop(), whiteMediumFontPaint);
            if (state == State.DRAWING) {
                if (radius / 2 > wearRect.width()) {
                    radius = 0;
                    state = State.NORMAL;
                    drawingBitmap = bitmap;
                }
                invalidate();
            }
        }

        float radius = 0;

        private void drawRefreshCircle(WatchFaceLayoutCalculator layoutCalc, Canvas canvas, Rect wearRect) {
            drawCirclePhoto(layoutCalc, canvas, wearRect);
            drawCirclePaper(layoutCalc, canvas, wearRect);
            radius += wearRect.width() / 2;
        }


        private void drawCirclePaper(WatchFaceLayoutCalculator layoutCalc, Canvas canvas, Rect wearRect) {
            final float cardHeight = wearRect.bottom - layoutCalc.getBottomPaperTop();
            Bitmap output = Bitmap.createBitmap(layoutCalc.getLocationImageWidth(),
                    (int) cardHeight, Bitmap.Config.ARGB_8888);
            Canvas drawingCanvas = new Canvas(output);
            drawingCanvas.drawARGB(0, 0, 0, 0);

            final Paint paperCirclePaint = new Paint();
            paperCirclePaint.setAntiAlias(true);
            paperCirclePaint.setColor(layoutCalc.getBottomPaperColor());
            int actionButtonRadius = getResources().getDimensionPixelSize(R.dimen.action_button_radius);
            int circleCenterX = layoutCalc.getActionButtonX() + actionButtonRadius;
            drawingCanvas.drawCircle(circleCenterX, 0, radius, paperCirclePaint);

            Paint shadowDrawPaint = new Paint();
            shadowDrawPaint.setShadowLayer(12, 0, -2, 0xFF000000);
            canvas.drawRect(circleCenterX - radius + 5, layoutCalc.getBottomPaperTop(), circleCenterX + radius - 5, layoutCalc.getBottomPaperTop() + 10, shadowDrawPaint);

            Paint bitmapDrawPaint = new Paint();
            bitmapDrawPaint.setAntiAlias(true);
            canvas.drawBitmap(output, 0f, layoutCalc.getBottomPaperTop(), bitmapDrawPaint);

        }


        private void drawCirclePhoto(WatchFaceLayoutCalculator layoutCalc, Canvas canvas, Rect wearRect) {
            Bitmap output = Bitmap.createBitmap(layoutCalc.getLocationImageWidth(),
                    layoutCalc.getLocationImageHeight(), Bitmap.Config.ARGB_8888);
            Canvas drawingCanvas = new Canvas(output);
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, layoutCalc.getLocationImageWidth(),
                    layoutCalc.getLocationImageHeight());

            paint.setAntiAlias(true);
            paint.setShadowLayer(12, 12, 12, 0xFF555555);
            drawingCanvas.drawARGB(0, 0, 0, 0);
            //paint.setColor(color);
            int actionButtonRadius = getResources().getDimensionPixelSize(R.dimen.action_button_radius);
            drawingCanvas.drawCircle(layoutCalc.getActionButtonX() + actionButtonRadius,
                    layoutCalc.getActionButtonY() + actionButtonRadius, radius, paint);

            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            drawingCanvas.drawBitmap(bitmap, null, rect, paint);
            Paint bitmapDrawPaint = new Paint();
            bitmapDrawPaint.setAntiAlias(true);
            canvas.drawBitmap(output, 0f, 0f, bitmapDrawPaint);

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
            super.onVisibilityChanged(visible);
            floatingActionBarManager.setVisible(visible);
            if (visible) {
                setPhotoIfNeed();
            }
        }

    }

    private void setPhotoIfNeed() {
        if (bitmap == null || settingPhotoTime + INTERVAL_SETTING_PHOTO < System.currentTimeMillis()) {
            setPhoto();
        }
    }

    public void setPhoto() {
        floatingActionBarManager.startRefresh();
        Observable.create(new GoogleAPIClientConnectionObservable(this)).switchMap((apiClient) -> {
            return Observable.create(new LocationGetObservable(apiClient));
        }).map(location -> {
            int range = new WearSharedPreference(this).get(getString(R.string.key_preference_search_range), getResources().getInteger(R.integer.search_range_default));
            return "https://api.flickr.com/services/rest/?method=flickr.photos.search&group_id=1463451@N25&api_key=" + BuildConfig.FLICKR_API_KEY + "&license=1%2C2%2C3%2C4%2C5%2C6&lat=" + location.getLatitude() + "&lon=" + location.getLongitude() + "&radius=" + range + "&extras=url_n,url_l&per_page=30&format=json&nojsoncallback=1";
        }).switchMap(url -> Observable.create(new WearHttpObservable(this, url))).observeOn(mainThread()).subscribeOn(mainThread()).subscribe(jsonString -> {
            try {
                final JSONArray photosArray = new JSONObject(jsonString).getJSONObject("photos").getJSONArray("photo");
                Log.d(TAG, "photoArray" + photosArray.length() + photosArray);
                if (photosArray.length() == 0) {
                    Toast.makeText(WatchFaceService.this, "Photo not found", Toast.LENGTH_LONG).show();
                    floatingActionBarManager.stopRefresh();
                    return;
                }
                final int nextIndex = new Random().nextInt(photosArray.length());
                String photoUrl = photosArray.getJSONObject(nextIndex).getString("url_n").toString();

                Log.d(TAG, "str:" + photoUrl);
                getAndSettingPhotoBitmap(photoUrl);

                largePhotoUrl = photosArray.getJSONObject(nextIndex).getString("url_l").toString();
                photoTitle = photosArray.getJSONObject(nextIndex).getString("title").toString();

            } catch (JSONException e) {
                Log.d(TAG, "json" + jsonString);
                e.printStackTrace();
                floatingActionBarManager.stopRefresh();
            }
        },e->{
            floatingActionBarManager.stopRefresh();
            e.printStackTrace();
        });
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
