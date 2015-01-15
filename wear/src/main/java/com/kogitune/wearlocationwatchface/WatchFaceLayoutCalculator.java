package com.kogitune.wearlocationwatchface;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.v7.graphics.Palette;

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
    private int bottomPaperColor = Color.WHITE;
    private int dateTextColor = Color.WHITE;
    private int timeTextColor = Color.WHITE;

    public void calc(Bitmap locationImageBitmap, Rect wearRect, int peekCardPosition, int mediumTextSize, int bigTextSize) {
        final float imageSizeRate = (float) wearRect.right / locationImageBitmap.getWidth();
        float imageRatio = locationImageBitmap.getWidth() / locationImageBitmap.getHeight();
        if (imageRatio < 1.4) {
            imageRatio = 1.4f;
        }

        locationImageLayoutHeight = locationImageBitmap.getWidth() * imageSizeRate;
        locationImageLayoutWidth = locationImageBitmap.getWidth() * imageSizeRate;
        bottomPaperTop = locationImageBitmap.getHeight() * imageSizeRate - 1;
        timeTextTop = wearRect.right / imageRatio - bigTextSize - 40;
        dateTextTop = wearRect.right / imageRatio - mediumTextSize - 40;

        final Palette palette = Palette.generate(locationImageBitmap);
        if (palette != null) {
            Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
            if (vibrantSwatch != null) {
                bottomPaperColor = vibrantSwatch.getRgb();
                dateTextColor = vibrantSwatch.getTitleTextColor();
                timeTextColor = Color.WHITE;
            }
        }
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

    public int getBottomPaperColor() {
        return bottomPaperColor;
    }

    public int getDateTextColor() {
        return dateTextColor;
    }

    public int getTimeTextColor() {
        return timeTextColor;
    }
}


