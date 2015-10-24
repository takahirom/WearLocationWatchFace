package com.kogitune.wearlocationwatchface;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.WindowInsets;

import com.kogitune.wearsharedpreference.WearSharedPreference;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by takam on 2015/04/12.
 */
public class WatchFaceDrawer {
    private final WatchFaceService.WatchFaceEngine watchFaceEngine;
    private WatchFaceService watchFaceService;
    private Paint whiteMediumFontPaint;
    private Paint whiteBigFontPaint;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private SimpleDateFormat hourFormat = new SimpleDateFormat("HH:");
    private SimpleDateFormat minuteFormat = new SimpleDateFormat("mm");
    private Bitmap bitmap = null;
    private Bitmap drawingBitmap;
    private State state = State.NORMAL;
    float radius = 0;
    private WatchFaceLayoutCalculator layoutCalc;
    private WatchFaceLayoutCalculator drawingLayoutCalc;
    private Paint bottomPaperPaint;
    private WindowInsets insets;

    public void setInsets(WindowInsets insets) {
        this.insets = insets;
    }

    enum State {
        NORMAL, DRAWING,
    }

    public WatchFaceDrawer(WatchFaceService.WatchFaceEngine watchFaceEngine, WatchFaceService watchFaceService) {
        this.watchFaceEngine = watchFaceEngine;
        this.watchFaceService = watchFaceService;

        whiteMediumFontPaint = new Paint();
        whiteMediumFontPaint.setFilterBitmap(true);
        whiteMediumFontPaint.setAntiAlias(true);
        whiteMediumFontPaint.setColor(Color.WHITE);
        whiteBigFontPaint = new Paint(whiteMediumFontPaint);

        watchFaceService.refreshIfNeed();
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public void onDraw(final Canvas canvas, final Rect wearRect) {
        // FIXME: Please clean and high performance code
        final Resources res = watchFaceService.getResources();
        if (drawingBitmap != bitmap && state != State.DRAWING) {
            state = State.DRAWING;
            // start Drawing

            layoutCalc = new WatchFaceLayoutCalculator();
            layoutCalc.calc(res, bitmap, wearRect, insets, watchFaceEngine.getPeekCardPosition().top);
            watchFaceService.updateFab(res.getColor(R.color.floating_button_color), layoutCalc.actionButtonX, layoutCalc.actionButtonY);
            whiteBigFontPaint.setTextSize(layoutCalc.bigTextSize);
            whiteMediumFontPaint.setTextSize(layoutCalc.mediumTextSize);
            whiteMediumFontPaint.setColor(layoutCalc.dateTextColor);
            whiteBigFontPaint.setColor(layoutCalc.timeTextColor);
        }

        //canvas.drawColor(0, PorterDuff.Mode.CLEAR);

        if (bitmap == null) {
            // for Emergency that bitmap not exists

            final int mediumTextSize = 24;
            final int bigTextSize = 72;
            whiteMediumFontPaint.setTextSize(mediumTextSize);
            whiteBigFontPaint.setTextSize(bigTextSize);
            helplesslyShowText(canvas, whiteMediumFontPaint, whiteBigFontPaint);
            return;
        }


        if (drawingBitmap != null) {
            // already have bitmap . draw for animation background
            Paint paint = new Paint();
            canvas.drawBitmap(drawingBitmap, null, new Rect(0, 0, drawingLayoutCalc.locationImageLayoutWidth, drawingLayoutCalc.locationImageLayoutHeight), paint);


            canvas.drawRect(0, layoutCalc.bottomPaperTop, wearRect.right, wearRect.bottom, bottomPaperPaint);
        }

        if (state == State.DRAWING) {
            drawRefreshCircle(layoutCalc, canvas, wearRect);
        }


        // FIXME: use dp
        final int X_POSITION = insets.isRound() ? 80 : 20;
        // draw time
        if (layoutCalc.isCenterTimeText) {
            final Paint transparentPaint = new Paint(whiteBigFontPaint);
            transparentPaint.setColor(Color.TRANSPARENT);
            // draw for just measure size
            Rect drawRect = drawCenterText(canvas, transparentPaint, timeFormat.format(new Date()), layoutCalc.timeTextTop);
            canvas.drawText(hourFormat.format(new Date()), drawRect.left, drawRect.top, whiteBigFontPaint);

            final Paint minutePaint = new Paint(whiteBigFontPaint);

            final boolean isTextColorAccent = new WearSharedPreference(watchFaceService).get(watchFaceService.getString(R.string.key_preference_time_text_accent), res.getBoolean(R.bool.time_text_accent_default));
            if (isTextColorAccent) {
                minutePaint.setColor(res.getColor(R.color.floating_button_color));
            }
            String minute = minuteFormat.format(new Date());

            final Rect minuteRect = new Rect();
            minutePaint.getTextBounds(minute, 0, minute.length(), minuteRect);

            canvas.drawText(minute, drawRect.right - minuteRect.width(), drawRect.top, minutePaint);
        } else {
            if (insets.isRound()) {
                canvas.drawText(timeFormat.format(new Date()), X_POSITION, layoutCalc.timeTextTop, whiteBigFontPaint);
            }else{
                canvas.drawText(timeFormat.format(new Date()), X_POSITION, layoutCalc.timeTextTop, whiteBigFontPaint);
            }
        }

        // draw date
        canvas.drawText(dateFormat.format(new Date()), X_POSITION, layoutCalc.dateTextTop, whiteMediumFontPaint);
        if (state == State.DRAWING) {
            if (radius / 2 > wearRect.width()) {
                radius = 0;
                state = State.NORMAL;
                drawingBitmap = bitmap;

                drawingLayoutCalc = new WatchFaceLayoutCalculator();
                drawingLayoutCalc.calc(res, drawingBitmap, wearRect, insets, watchFaceEngine.getPeekCardPosition().top);

                // draw paper
                bottomPaperPaint = new Paint();
                bottomPaperPaint.setColor(drawingLayoutCalc.bottomPaperColor);
                bottomPaperPaint.setAntiAlias(true);
                bottomPaperPaint.setShadowLayer(12, 0, -2, 0xFF000000);
            }
            watchFaceEngine.invalidate();
        }
    }

    private void drawRefreshCircle(WatchFaceLayoutCalculator layoutCalc, Canvas canvas, Rect wearRect) {
        drawCirclePhoto(layoutCalc, canvas, wearRect);
        drawCirclePaper(layoutCalc, canvas, wearRect);
        radius += wearRect.width() / 4;
    }

    private void drawCirclePaper(WatchFaceLayoutCalculator layoutCalc, Canvas canvas, Rect wearRect) {
        final float cardHeight = wearRect.bottom - layoutCalc.bottomPaperTop;
        Bitmap output = Bitmap.createBitmap((int) layoutCalc.locationImageLayoutWidth,
                (int) cardHeight, Bitmap.Config.ARGB_8888);
        Canvas drawingCanvas = new Canvas(output);
        drawingCanvas.drawARGB(0, 0, 0, 0);

        final Paint paperCirclePaint = new Paint();
        paperCirclePaint.setAntiAlias(true);
        paperCirclePaint.setColor(layoutCalc.bottomPaperColor);
        int actionButtonRadius = watchFaceService.getResources().getDimensionPixelSize(R.dimen.action_button_radius);
        int circleCenterX = layoutCalc.actionButtonX + actionButtonRadius;
        drawingCanvas.drawCircle(circleCenterX, 0, radius, paperCirclePaint);

        Paint shadowDrawPaint = new Paint();
        shadowDrawPaint.setShadowLayer(12, 0, -2, 0xFF000000);
        canvas.drawRect(circleCenterX - radius + 5, layoutCalc.bottomPaperTop, circleCenterX + radius - 5, layoutCalc.bottomPaperTop + 10, shadowDrawPaint);

        Paint bitmapDrawPaint = new Paint();
        bitmapDrawPaint.setAntiAlias(true);
        canvas.drawBitmap(output, 0f, layoutCalc.bottomPaperTop, bitmapDrawPaint);

    }

    private void drawCirclePhoto(WatchFaceLayoutCalculator layoutCalc, Canvas canvas, Rect wearRect) {
        Bitmap output = Bitmap.createBitmap(layoutCalc.locationImageLayoutWidth,
                layoutCalc.locationImageLayoutHeight, Bitmap.Config.ARGB_8888);
        Canvas drawingCanvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, layoutCalc.locationImageLayoutWidth,
                layoutCalc.locationImageLayoutHeight);

        paint.setAntiAlias(true);
        paint.setShadowLayer(12, 12, 12, 0xFF555555);
        drawingCanvas.drawARGB(0, 0, 0, 0);
        //paint.setColor(color);
        int actionButtonRadius = watchFaceService.getResources().getDimensionPixelSize(R.dimen.action_button_radius);
        drawingCanvas.drawCircle(layoutCalc.actionButtonX + actionButtonRadius,
                layoutCalc.actionButtonY + actionButtonRadius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        drawingCanvas.drawBitmap(bitmap, null, rect, paint);
        Paint bitmapDrawPaint = new Paint();
        bitmapDrawPaint.setAntiAlias(true);
        canvas.drawBitmap(output, 0f, 0f, bitmapDrawPaint);

    }

    private void helplesslyShowText(Canvas canvas, Paint whiteMediumPaint, Paint whiteBigPaint) {
        final int X_POSITION = insets.isRound() ? 80 : 20;
        drawCenterText(canvas, whiteBigPaint, hourFormat.format(new Date()), 0);
        drawCenterText(canvas, whiteBigPaint, minuteFormat.format(new Date()), 0);
        canvas.drawText(dateFormat.format(new Date()), X_POSITION, 0, whiteMediumPaint);
    }

    // return draw rect
    public Rect drawCenterText(Canvas canvas, Paint paint, String text, float height) {
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
        return new Rect(x, (int) y, x + bounds.width(), (int) (y + bounds.height()));
    }


}
