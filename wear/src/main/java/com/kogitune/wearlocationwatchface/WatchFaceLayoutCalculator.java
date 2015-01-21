package com.kogitune.wearlocationwatchface;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.v7.graphics.Palette;
import android.support.wearable.view.CircledImageView;
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
    private int bottomPaperColor = Color.BLACK;
    private int dateTextColor = Color.WHITE;
    private int timeTextColor = Color.WHITE;
    private int mediumTextSize = 28;
    private int bigTextSize = 72;
    private boolean isCenterTimeText = true;
    private int actionButtonX;
    private int actionButtonY;

    public void calc(Resources res,Bitmap locationImageBitmap, Rect wearRect, int peekCardPosition) {
        final float imageSizeRate = (float) wearRect.right / locationImageBitmap.getWidth();
        float imageRatio = locationImageBitmap.getWidth() / (float)locationImageBitmap.getHeight();
        final Palette palette = Palette.generate(locationImageBitmap);
        Log.d(TAG, "imageSizeRate" + imageSizeRate);
        Log.d(TAG, "imageRatio" + imageRatio);
        if (imageRatio < 1.5) {
            imageRatio = 1.5f;
        }
        if (imageRatio > 1.6) {
            imageRatio = 1.6f;
        }

        bottomPaperTop = wearRect.bottom / imageRatio - 1;
        locationImageLayoutHeight = bottomPaperTop + 1;
        locationImageLayoutWidth = locationImageBitmap.getWidth() * imageSizeRate;
        if (palette != null) {
            Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
            if (vibrantSwatch != null) {
                bottomPaperColor = vibrantSwatch.getRgb();
            }
        }

        int actionButtonMargin = res.getDimensionPixelSize(R.dimen.action_button_margin);
        int actionButtonRadius = res.getDimensionPixelSize(R.dimen.action_button_diameter);

        actionButtonX = wearRect.right - actionButtonMargin - actionButtonRadius;
        actionButtonY = (int) (bottomPaperTop - actionButtonRadius / 2);

        if (peekCardPosition > 0) {
            timeInPictureMode(wearRect, imageRatio, palette);
            return;
        }
        timeInPaperMode(wearRect, imageRatio, palette);


    }

    private void timeInPictureMode(Rect wearRect, float imageRatio, Palette palette) {
        timeTextTop = wearRect.right / imageRatio - bigTextSize / 2;
        dateTextTop = wearRect.right / imageRatio + mediumTextSize;

        if (palette != null) {
            Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
            if (vibrantSwatch != null) {
                dateTextColor = vibrantSwatch.getTitleTextColor();
                timeTextColor = Color.WHITE;
            }
        }
    }


    private void timeInPaperMode(Rect wearRect, float imageRatio, Palette palette) {
        isCenterTimeText = false;
        bigTextSize = 48;
        timeTextTop = wearRect.right / imageRatio + bigTextSize;
        dateTextTop = wearRect.right / imageRatio + bigTextSize + mediumTextSize + 5;

        if (palette != null) {
            Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
            if (vibrantSwatch != null) {
                dateTextColor = vibrantSwatch.getTitleTextColor();
                timeTextColor = vibrantSwatch.getTitleTextColor();
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

    public int getBigTextSize() {
        return bigTextSize;
    }

    public int getMediumTextSize() {
        return mediumTextSize;
    }

    public boolean isCenterTimeText() {
        return isCenterTimeText;
    }

    public int getActionButtonX() {
        return actionButtonX;
    }

    public int getActionButtonY() {
        return actionButtonY;
    }
}


