package com.kogitune.activity_transition;

import android.animation.TimeInterpolator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

/**
 * Created by takam on 2015/03/26.
 */
public class ActivityTransition {
    private static final TimeInterpolator sDecelerator = new DecelerateInterpolator();
    private int duration = 1000;
    private View toView;
    private Intent fromIntent;
    private int leftDelta;
    private int topDelta;
    private float widthScale;
    private float heightScale;

    public ActivityTransition(Intent intent) {
        this.fromIntent = intent;
    }

    public static ActivityTransition with(Intent intent) {
        return new ActivityTransition(intent);
    }

    public ActivityTransition to(View toView) {
        this.toView = toView;
        return this;
    }

    public ActivityTransition duration(int duration){
        this.duration = duration;
        return this;
    }


    public void start(Bundle savedInstanceState) {
        final Bundle bundle = fromIntent.getExtras();
        final int thumbnailTop =    bundle.getInt(ActivityTransitionLauncher.EXTRA_IMAGE_TOP);
        final int thumbnailLeft =   bundle.getInt(ActivityTransitionLauncher.EXTRA_IMAGE_LEFT);
        final int thumbnailWidth =  bundle.getInt(ActivityTransitionLauncher.EXTRA_IMAGE_WIDTH);
        final int thumbnailHeight = bundle.getInt(ActivityTransitionLauncher.EXTRA_IMAGE_HEIGHT);
        final String imageFilePath =  bundle.getString(ActivityTransitionLauncher.EXTRA_IMAGE_PATH);
        if (imageFilePath != null) {
            setImageToView(imageFilePath);
        }
        if (savedInstanceState == null) {

            ViewTreeObserver observer = toView.getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                @Override
                public boolean onPreDraw() {
                    toView.getViewTreeObserver().removeOnPreDrawListener(this);

                    int[] screenLocation = new int[2];
                    toView.getLocationOnScreen(screenLocation);
                    leftDelta = thumbnailLeft - screenLocation[0];
                    topDelta = thumbnailTop - screenLocation[1];

                    widthScale = (float) thumbnailWidth / toView.getWidth();
                    heightScale = (float) thumbnailHeight / toView.getHeight();

                    runEnterAnimation();

                    return true;
                }
            });
        }
    }

    private void setImageToView(String imageFilePath) {
        Bitmap bitmap;
        if ((bitmap = ActivityTransitionLauncher.bitmapCache.get()) == null) {
            bitmap = BitmapFactory.decodeFile(imageFilePath);
        } else {
            ActivityTransitionLauncher.bitmapCache.clear();
        }
        if (toView instanceof ImageView){
            final ImageView toImageView = (ImageView)toView;
            toImageView.setImageBitmap(bitmap);
        }else {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                toView.setBackground(new BitmapDrawable(toView.getResources(), bitmap));
            } else {
                toView.setBackgroundDrawable(new BitmapDrawable(toView.getResources(), bitmap));
            }
        }
    }

    private void runEnterAnimation() {
        toView.setPivotX(0);
        toView.setPivotY(0);
        toView.setScaleX(widthScale);
        toView.setScaleY(heightScale);
        toView.setTranslationX(leftDelta);
        toView.setTranslationY(topDelta);

        toView.animate().setDuration(duration).
                scaleX(1).scaleY(1).
                translationX(0).translationY(0).
                setInterpolator(sDecelerator);
    }
}
