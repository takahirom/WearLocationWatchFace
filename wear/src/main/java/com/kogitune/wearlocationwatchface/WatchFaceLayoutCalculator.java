package com.kogitune.wearlocationwatchface;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.v7.graphics.Palette;
import android.view.WindowInsets;

/**
 * Created by takam on 2015/01/15.
 */
public class WatchFaceLayoutCalculator {
    private static final String TAG = "LayoutCalculator";
    public float bottomPaperTop;
    public int mediumTextSize = 28;
    public int bigTextSize = 72;
    public float dateTextTop;
    public float timeTextTop;
    public int locationImageLayoutHeight;
    public int locationImageLayoutWidth;
    public int bottomPaperColor = Color.BLACK;
    public int dateTextColor = Color.WHITE;
    public int timeTextColor = Color.WHITE;
    public boolean isCenterTimeText = true;
    public int actionButtonX;
    public int actionButtonY;

    public void calc(Resources res, Bitmap locationImageBitmap, Rect wearRect, WindowInsets insets, int peekCardPosition) {
        final float imageSizeRate = (float) wearRect.right / locationImageBitmap.getWidth();
        float imageRatio = locationImageBitmap.getWidth() / (float) locationImageBitmap.getHeight();
        final Palette palette = Palette.generate(locationImageBitmap);
        if (imageRatio < 1.5) {
            imageRatio = 1.5f;
        }
        if (imageRatio > 1.6) {
            imageRatio = 1.6f;
        }

        bottomPaperTop = wearRect.bottom / imageRatio - 1;
        locationImageLayoutHeight = (int) (bottomPaperTop + 1);
        locationImageLayoutWidth = (int) (locationImageBitmap.getWidth() * imageSizeRate);
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

}


