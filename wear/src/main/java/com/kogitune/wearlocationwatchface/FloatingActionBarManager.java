package com.kogitune.wearlocationwatchface;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.support.wearable.view.CircledImageView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;

/**
 * Created by takam on 2015/01/18.
 */
public class FloatingActionBarManager {

    private static final String TAG = "FloatingActionBarManager";
    private final CircledImageView circledImageView;
    private final WindowManager windowManager;
    private final Context context;
    private int lastX;
    private int lastY;
    private FrameLayout parentView;

    public FloatingActionBarManager(Context context) {
        this.context = context;
        WindowManager.LayoutParams params = createLayoutParams(0, 0);  // viewを透明にする
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        circledImageView = new CircledImageView(context);
        circledImageView.setElevation(10f);
        parentView = new FrameLayout(context);
        windowManager.addView(parentView, params);
        parentView.addView(circledImageView);
    }

    public void update(int color, int x, int y) {
        lastX = x;
        lastY = y;
        circledImageView.setImageResource(R.drawable.ic_sync_white_18dp);
        circledImageView.setCircleColor(color);

        circledImageView.setCircleRadius(context.getResources().getDimensionPixelSize(R.dimen.action_button_radius));
        if (parentView.getParent() != null) {
            windowManager.updateViewLayout(parentView, createLayoutParams(x, y));
        }
    }
    public void startRefresh() {
        circledImageView.setImageResource(R.drawable.ic_sync_white_18dp);
        final RotateAnimation rotateAnimation = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

        rotateAnimation.setStartOffset(0);
        rotateAnimation.setDuration(500);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setRepeatMode(Animation.RESTART);
        rotateAnimation.setInterpolator(new Interpolator() {
            @Override
            public float getInterpolation(float input) {
                return Math.abs(input - 1f);
            }
        });

        circledImageView.startAnimation(rotateAnimation);
    }

    public void stopRefresh(){
        circledImageView.clearAnimation();
    }

    public void problemStopRefresh() {
        circledImageView.setImageResource(R.drawable.ic_sync_problem_white_18dp);
        stopRefresh();
    }

    public void setVisible(boolean visible){
        if (visible) {
            if (parentView.getParent() == null) {
                windowManager.addView(parentView, createLayoutParams(lastX, lastY));
            }
        }else{
            if (parentView.getParent() != null) {
                windowManager.removeView(parentView);
            }
        }
    }


    private WindowManager.LayoutParams createLayoutParams(int x, int y) {
        Log.d(TAG, "x:" + x + " y:" + y);
        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                context.getResources().getDimensionPixelSize(R.dimen.action_button_diameter),
                context.getResources().getDimensionPixelSize(R.dimen.action_button_diameter),
                x,
                y,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        return layoutParams;
    }

    public void setOnClickListener(View.OnClickListener listener) {
        circledImageView.setOnClickListener(listener);
    }

}
