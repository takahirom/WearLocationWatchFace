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
import android.support.v7.graphics.Palette;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import com.kogitune.wearhttp.WearGetImage;
import com.kogitune.wearhttp.WearGetText;

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

    @Override
    public Engine onCreateEngine() {


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
            public void onDraw(Canvas canvas, Rect bounds) {
                Log.d(TAG, "onDraw");
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);

                final Paint whiteMediumPaint = new Paint();
                whiteMediumPaint.setFilterBitmap(true);
                whiteMediumPaint.setAntiAlias(true);
                final Paint whiteBigPaint = new Paint(whiteMediumPaint);
                whiteMediumPaint.setTextSize(24);
                whiteBigPaint.setTextSize(72);
                Log.d(TAG, "test1");
                float rate = 1.16f;
                if (bitmap != null) {
                    Log.d(TAG, "test2");

                    rate = (float) bounds.right / bitmap.getWidth();


                    //BitmapからBitmapDrawableを生成
                    BitmapDrawable drawable = new BitmapDrawable(bitmap);
                    //drawableの描画領域設定（必須）
                    drawable.setBounds(0, 0, (int) (bitmap.getWidth() * rate), (int) (bitmap.getHeight() * rate));
                    //canvasに描画
                    drawable.draw(canvas);

                    final Palette palette = Palette.generate(bitmap);
                    if (palette != null) {
                        Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
                        if (vibrantSwatch != null) {
                            final Paint paint = new Paint();
                            paint.setColor(vibrantSwatch.getRgb());
                            if (getPeekCardPosition().top > 0) {
                                canvas.drawRect(0, getPeekCardPosition().top - 40, bounds.right, bounds.bottom, paint);
                            } else {
                                canvas.drawRect(0, (float) (bitmap.getHeight() * rate) - 1, bounds.right, bounds.bottom, paint);
                            }
                            whiteMediumPaint.setColor(vibrantSwatch.getTitleTextColor());
                            whiteBigPaint.setColor(Color.WHITE);
                        }
                    }

                }
                String timeText = timeFormat.format(new Date());
                drawText(canvas,whiteBigPaint,timeText,rate);
                canvas.drawText(dateFormat.format(new Date()), 20, (float) (bounds.right / 1.5) + 25, whiteMediumPaint);
                canvas.save();
            }
            public void drawText(Canvas canvas, Paint paint , String text,float rate) {
                Rect bounds = new Rect();
                paint.getTextBounds(text, 0, text.length(), bounds);
                int x = (canvas.getWidth() / 2) - (bounds.width() / 2);
                float y;
                if (bitmap == null) {
                    y = canvas.getHeight() - 1 - bounds.height();
                }else {
                    y = bitmap.getHeight() * rate - 8 ;
                }

                canvas.drawText(text, x, y, paint);
            }

            @Override
            public void onVisibilityChanged(boolean visible) {
                if (visible) {
                    setPhoto();
                }
            }
        };


        return engine;
    }

    public void setPhoto() {
        new WearGetText(this).get("https://api.flickr.com/services/rest/?method=flickr.photos.search&group_id=1463451@N25&api_key="+BuildConfig.FLICKR_API_KEY+"&license=1%2C2%2C3%2C4%2C5%2C6&sort=interestingness-desc&lat=35.68937&lon=139.724754&radius=30&extras=url_s&per_page=30&format=json&nojsoncallback=1", new WearGetText.WearGetCallBack() {
            @Override
            public void onGet(String s) {
                try {
                    final JSONArray photosArray = new JSONObject(s).getJSONObject("photos").getJSONArray("photo");
                    final int nextIndex = new Random().nextInt(photosArray.length());
                    String showStr = photosArray.getJSONObject(nextIndex).getString("url_s").toString();
                    Log.d(TAG, "str:" + showStr);
                    getAndSetBitmap(showStr);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail(Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void getAndSetBitmap(String url) {
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
