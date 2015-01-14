package com.kogitune.wearlocationwatchface;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

/**
 * Created by takam on 2015/01/15.
 */
public class WatchFaceLayoutCalculator {
    private static final String TAG = "WatchFaceLayoutCalculator";
    private float locationImageLayoutHeight;
    private float locationImageLayoutWidth;
    private float bottomPaperTop;
    private float dateTextTop;
    private float timeTextTop;

    public void calc(Bitmap locationImageBitmap, Rect wearRect, int peekCardPosition, int mediumTextSize, int bigTextSize) {
        final float imageSizeRate = (float) wearRect.right / locationImageBitmap.getWidth();
        final float imageRatio = locationImageBitmap.getWidth() / locationImageBitmap.getHeight();
        locationImageLayoutHeight = locationImageBitmap.getWidth() * imageSizeRate;
        locationImageLayoutWidth = locationImageBitmap.getWidth() * imageSizeRate;
        bottomPaperTop = locationImageBitmap.getHeight() * imageSizeRate - 1;
        timeTextTop = wearRect.right / imageRatio - bigTextSize - 40;
        dateTextTop = wearRect.right / imageRatio - mediumTextSize - 40;
    }

    public int getLocationImageHeight() {
        return (int) locationImageLayoutHeight;
    }

    public int getLocationImageWidth() {
        return (int) locationImageLayoutWidth;
    }

    public float getBottomPaperTop() {
        return bottomPaperTop;
    }

    public float getTimeTextTop() {
        return timeTextTop;
    }

    public float getDateTextTop() {
        return dateTextTop;
    }
}


